package org.asc.tourneys.tournament

import net.kyori.adventure.text.minimessage.MiniMessage
import org.asc.tourneys.TourneyPlugin
import org.asc.tourneys.arena.ArenaManager
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.message.MessageService
import org.asc.tourneys.model.KitItem
import org.asc.tourneys.model.MatchStatus
import org.asc.tourneys.model.TieBehavior
import org.asc.tourneys.model.TournamentMatch
import org.asc.tourneys.model.TournamentState
import org.asc.tourneys.model.TournamentTeam
import org.asc.tourneys.team.TeamManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.UUID
import kotlin.math.min

class TournamentManager(
    private val plugin: TourneyPlugin,
    private val configManager: ConfigManager,
    private val messages: MessageService,
    private val arenaManager: ArenaManager,
    private val teamManager: TeamManager
) {
    var state: TournamentState = TournamentState.IDLE
        private set

    private val matches = mutableListOf<TournamentMatch>()
    private val activeArenaNames = mutableSetOf<String>()
    private val tasks = mutableListOf<BukkitTask>()
    private val mini = MiniMessage.miniMessage()

    fun open(sender: org.bukkit.command.CommandSender) {
        if (state != TournamentState.IDLE && state != TournamentState.FINISHED) {
            messages.send(sender, "bad-state", mapOf("state" to state.name))
            return
        }
        teamManager.reset()
        matches.clear()
        activeArenaNames.clear()
        state = TournamentState.OPEN
        Bukkit.broadcast(messages.component("registration-open", mapOf("tournament" to configManager.settings.displayName)))
    }

    fun close(sender: org.bukkit.command.CommandSender) {
        if (state != TournamentState.OPEN) {
            messages.send(sender, "bad-state", mapOf("state" to state.name))
            return
        }
        if (!configManager.settings.allowIncompleteTeamsOnClose && teamManager.registeredTeams().any { it.members.size < configManager.settings.teamSize }) {
            if (configManager.settings.removeIncompleteTeamsOnClose) {
                val removed = teamManager.removeIncompleteTeams()
                messages.send(sender, "removed-incomplete-teams", mapOf("count" to removed.toString()))
            } else {
                messages.send(sender, "incomplete-teams-block-close")
                return
            }
        }
        val teams = teamManager.registeredTeams()
        if (teams.size < configManager.settings.minimumTeams) {
            messages.send(sender, "not-enough-teams", mapOf("minimum" to configManager.settings.minimumTeams.toString()))
            return
        }
        generateBracket(teams.shuffled())
        state = TournamentState.CLOSED
        Bukkit.broadcast(messages.component("registration-closed", mapOf("teams" to teams.size.toString())))
        if (configManager.settings.bracketUrlEnabled && configManager.settings.bracketUrlValue.isNotBlank()) {
            Bukkit.broadcast(messages.component("bracket-url", mapOf("url" to configManager.settings.bracketUrlValue)))
        }
        writeSnapshot()
    }

    fun start(sender: org.bukkit.command.CommandSender) {
        if (state != TournamentState.CLOSED) {
            messages.send(sender, "bad-state", mapOf("state" to state.name))
            return
        }
        if (arenaManager.ready().isEmpty()) {
            messages.send(sender, "no-ready-arenas")
            return
        }
        state = TournamentState.RUNNING
        Bukkit.broadcast(messages.component("tournament-started"))
        scheduleAvailableMatches()
    }

    fun cancel(broadcast: Boolean = true) {
        tasks.forEach { it.cancel() }
        tasks.clear()
        activeArenaNames.clear()
        matches.clear()
        teamManager.reset()
        state = TournamentState.IDLE
        if (broadcast) Bukkit.broadcast(messages.component("tournament-cancelled"))
    }

    fun status(): String {
        return "State: ${state.name}, teams: ${teamManager.registeredTeams().size}, matches: ${matches.size}, active: ${matches.count { it.status == MatchStatus.ACTIVE || it.status == MatchStatus.COUNTDOWN }}"
    }

    fun matches(): List<TournamentMatch> = matches.toList()

    fun activeMatch(playerId: UUID): TournamentMatch? {
        return matches.firstOrNull { (it.status == MatchStatus.ACTIVE || it.status == MatchStatus.COUNTDOWN) && it.contains(playerId) }
    }

    fun isSpectating(playerId: UUID): Boolean {
        val team = teamManager.findTeam(playerId) ?: return false
        return matches.any { it.contains(playerId) && it.status == MatchStatus.ACTIVE && !it.aliveTeamA.contains(playerId) && !it.aliveTeamB.contains(playerId) } || team.eliminated
    }

    fun handleDeath(player: Player) {
        val match = activeMatch(player.uniqueId) ?: return
        match.aliveTeamA -= player.uniqueId
        match.aliveTeamB -= player.uniqueId
        if (configManager.settings.deathLightningEffectEnabled) {
            player.world.strikeLightningEffect(player.location)
        }
        if (configManager.settings.deathSoundsEnabled) {
            matchPlayers(match).forEach { it.playSound(it.location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.2f) }
        }
        if (configManager.settings.sendEliminatedPlayersToSpectator) {
            tasks += Bukkit.getScheduler().runTaskLater(plugin, Runnable { sendToSpectator(player, match) }, 2L)
        }
        if (match.aliveTeamA.isEmpty() || match.aliveTeamB.isEmpty()) {
            tasks += Bukkit.getScheduler().runTaskLater(plugin, Runnable { finishMatch(match) }, 20L)
        }
    }

    fun handleQuit(player: Player) {
        if (!configManager.settings.allowRejoinDuringActiveMatch) {
            handleDeath(player)
        }
    }

    fun handleJoin(player: Player) {
        val match = activeMatch(player.uniqueId) ?: return
        if (!configManager.settings.allowRejoinDuringActiveMatch) return
        val arena = match.arenaName?.let { arenaManager.get(it) } ?: return
        if (match.teamA?.members?.contains(player.uniqueId) == true) {
            arena.teamASpawn?.let { player.teleport(it) }
        } else {
            arena.teamBSpawn?.let { player.teleport(it) }
        }
    }

    fun sendToLobby(player: Player) {
        configManager.settings.lobbySpawn?.let { player.teleport(it) }
    }

    private fun generateBracket(teams: List<TournamentTeam>) {
        matches.clear()
        createRound(1, teams)
    }

    private fun createRound(round: Int, teams: List<TournamentTeam>) {
        val mutable = teams.toMutableList()
        var id = matches.size + 1
        while (mutable.isNotEmpty()) {
            val teamA = mutable.removeFirst()
            val teamB = if (mutable.isNotEmpty()) mutable.removeFirst() else null
            val match = TournamentMatch(id++, round, teamA, teamB)
            if (teamB == null) {
                match.status = MatchStatus.FINISHED
                match.winner = teamA
            }
            matches += match
        }
    }

    private fun scheduleAvailableMatches() {
        if (state != TournamentState.RUNNING) return
        val currentRound = matches.filter { it.winner == null }.minOfOrNull { it.round }
        if (currentRound == null) {
            val winner = matches.lastOrNull()?.winner
            finishTournament(winner)
            return
        }
        val roundMatches = matches.filter { it.round == currentRound }
        if (roundMatches.all { it.status == MatchStatus.FINISHED }) {
            val winners = roundMatches.mapNotNull { it.winner }
            if (winners.size <= 1) {
                finishTournament(winners.firstOrNull())
                return
            }
            createRound(currentRound + 1, winners.shuffled())
            writeSnapshot()
            scheduleAvailableMatches()
            return
        }
        val activeCount = matches.count { it.status == MatchStatus.ACTIVE || it.status == MatchStatus.COUNTDOWN }
        val room = min(configManager.settings.maxActiveMatches, arenaManager.ready().size) - activeCount
        if (room <= 0) return
        val waiting = roundMatches.filter { it.status == MatchStatus.WAITING && it.teamB != null }.take(room)
        for (match in waiting) {
            val arena = arenaManager.ready().firstOrNull { !activeArenaNames.contains(it.name.lowercase()) } ?: return
            startCountdown(match, arena.name)
        }
    }

    private fun startCountdown(match: TournamentMatch, arenaName: String) {
        val arena = arenaManager.get(arenaName) ?: return
        match.status = MatchStatus.COUNTDOWN
        match.arenaName = arena.name
        activeArenaNames += arena.name.lowercase()
        match.aliveTeamA.clear()
        match.aliveTeamB.clear()
        match.teamA?.members?.forEach { match.aliveTeamA += it }
        match.teamB?.members?.forEach { match.aliveTeamB += it }
        val players = matchPlayers(match)
        players.forEach {
            it.gameMode = GameMode.SURVIVAL
            if (configManager.settings.clearInventoriesOnMatchStart) it.inventory.clear()
            if (configManager.settings.giveKitOnMatchStart) giveKit(it)
        }

        match.teamA?.members?.mapNotNull { Bukkit.getPlayer(it) }?.forEach { player ->
            arena.teamASpawn?.let { player.teleport(it) }
        }

        match.teamB?.members?.mapNotNull { Bukkit.getPlayer(it) }?.forEach { player ->
            arena.teamBSpawn?.let { player.teleport(it) }
        }

        Bukkit.broadcast(messages.component("match-countdown", mapOf("match" to match.id.toString(), "arena" to arena.name)))

        tasks += Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            match.status = MatchStatus.ACTIVE
            matchPlayers(match).forEach { messages.title(it, "match-start", mapOf("match" to match.id.toString())) }
            Bukkit.broadcast(messages.component("match-started", matchPlaceholders(match)))
            writeSnapshot()
        }, configManager.settings.countdownBeforeMatchStarts * 20L)
    }

    private fun finishMatch(match: TournamentMatch) {
        if (match.status == MatchStatus.FINISHED) return
        val winner = when {
            match.aliveTeamA.isNotEmpty() && match.aliveTeamB.isEmpty() -> match.teamA
            match.aliveTeamB.isNotEmpty() && match.aliveTeamA.isEmpty() -> match.teamB
            else -> tieWinner(match)
        }
        if (winner == null) {
            match.status = MatchStatus.WAITING
            match.aliveTeamA.clear()
            match.aliveTeamB.clear()
            activeArenaNames -= match.arenaName?.lowercase().orEmpty()
            match.arenaName = null
            Bukkit.broadcast(messages.component("match-rematch", matchPlaceholders(match)))
            scheduleAvailableMatches()
            return
        }
        match.winner = winner
        match.status = MatchStatus.FINISHED
        val loser = listOfNotNull(match.teamA, match.teamB).firstOrNull { it.id != winner.id }
        loser?.eliminated = true
        Bukkit.broadcast(messages.component("match-finished", matchPlaceholders(match) + ("winner" to winner.name)))
        tasks += Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            matchPlayers(match).forEach { sendToLobby(it) }
            activeArenaNames -= match.arenaName?.lowercase().orEmpty()
            scheduleAvailableMatches()
            writeSnapshot()
        }, configManager.settings.delayAfterMatchEnds * 20L)
    }

    private fun finishTournament(winner: TournamentTeam?) {
        state = TournamentState.FINISHED
        Bukkit.broadcast(messages.component("tournament-finished", mapOf("winner" to (winner?.name ?: "None"))))
        writeSnapshot()
        tasks += Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.getOnlinePlayers().forEach { sendToLobby(it) }
        }, configManager.settings.delayBeforeReturningToLobby * 20L)
    }

    private fun tieWinner(match: TournamentMatch): TournamentTeam? {
        return when (configManager.settings.tieBehavior) {
            TieBehavior.TEAMA -> match.teamA
            TieBehavior.TEAMB -> match.teamB
            TieBehavior.RANDOM -> listOfNotNull(match.teamA, match.teamB).randomOrNull()
            TieBehavior.REMATCH -> null
        }
    }

    private fun sendToSpectator(player: Player, match: TournamentMatch) {
        val arena = match.arenaName?.let { arenaManager.get(it) }
        player.gameMode = GameMode.SPECTATOR
        arena?.spectatorSpawn?.let { player.teleport(it) } ?: configManager.settings.fallbackSpectatorSpawn?.let { player.teleport(it) }
    }

    private fun matchPlayers(match: TournamentMatch): List<Player> {
        return listOfNotNull(match.teamA, match.teamB).flatMap { team -> team.members.mapNotNull { Bukkit.getPlayer(it) } }
    }

    private fun giveKit(player: Player) {
        configManager.settings.kitItems.forEach {
            placeKitItem(player, it, createKitItem(it))
        }
    }

    private fun createKitItem(kitItem: KitItem): ItemStack {
        kitItem.itemStack?.let {
            return it.clone()
        }

        val item = ItemStack(kitItem.material, kitItem.amount)
        val meta = item.itemMeta

        kitItem.displayName?.let {
            meta.displayName(mini.deserialize(it))
        }

        if (kitItem.lore.isNotEmpty()) {
            meta.lore(kitItem.lore.map { mini.deserialize(it) })
        }

        kitItem.enchantments.forEach {
            meta.addEnchant(it.key, it.value, true)
        }

        meta.isUnbreakable = kitItem.unbreakable

        kitItem.customModelData?.let {
            meta.setCustomModelData(it)
        }

        if (meta is PotionMeta) {
            applyPotionEffects(meta, kitItem)
        }

        item.itemMeta = meta

        return item
    }

    private fun applyPotionEffects(meta: PotionMeta, kitItem: KitItem) {
        kitItem.potionEffects.forEach {
            meta.addCustomEffect(
                PotionEffect(
                    it.type,
                    it.durationSeconds * 20,
                    it.amplifier,
                    it.ambient,
                    it.particles,
                    it.icon
                ),
                true
            )
        }
    }

    private fun placeKitItem(player: Player, kitItem: KitItem, item: ItemStack) {
        if (kitItem.slot != null) {
            player.inventory.setItem(kitItem.slot, item)
            return
        }

        player.inventory.addItem(item)
    }

    private fun matchPlaceholders(match: TournamentMatch): Map<String, String> {
        return mapOf(
            "match" to match.id.toString(),
            "round" to match.round.toString(),
            "teamA" to (match.teamA?.name ?: "Bye"),
            "teamB" to (match.teamB?.name ?: "Bye"),
            "arena" to (match.arenaName ?: "None"),
            "status" to match.status.name
        )
    }

    private fun writeSnapshot() {
        if (!configManager.settings.apiSnapshotEnabled) return
        val file = File(plugin.dataFolder, "snapshot.json")
        plugin.dataFolder.mkdirs()
        val json = buildString {
            append("{\"state\":\"").append(state.name).append("\",")
            append("\"teams\":[")
            teamManager.registeredTeams().forEachIndexed { index, team ->
                if (index > 0) append(',')
                append("{\"id\":\"").append(escape(team.id)).append("\",\"name\":\"").append(escape(team.name)).append("\",\"members\":[")
                team.members.forEachIndexed { memberIndex, id ->
                    if (memberIndex > 0) append(',')
                    append("\"").append(escape(Bukkit.getOfflinePlayer(id).name ?: id.toString())).append("\"")
                }
                append("]}")
            }
            append("],\"matches\":[")
            matches.forEachIndexed { index, match ->
                if (index > 0) append(',')
                append("{\"id\":").append(match.id)
                append(",\"round\":").append(match.round)
                append(",\"status\":\"").append(match.status.name).append("\"")
                append(",\"arena\":\"").append(escape(match.arenaName ?: "")).append("\"")
                append(",\"teamA\":\"").append(escape(match.teamA?.name ?: "Bye")).append("\"")
                append(",\"teamB\":\"").append(escape(match.teamB?.name ?: "Bye")).append("\"")
                append(",\"winner\":\"").append(escape(match.winner?.name ?: "")).append("\"}")
            }
            append("]}")
        }
        file.writeText(json)
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
