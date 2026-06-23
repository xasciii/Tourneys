package org.asc.tourneys.command

import org.asc.tourneys.TourneyPlugin
import org.asc.tourneys.arena.ArenaManager
import org.asc.tourneys.bracket.BracketGui
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.message.MessageService
import org.asc.tourneys.model.TournamentState
import org.asc.tourneys.team.TeamManager
import org.asc.tourneys.tournament.TournamentManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TourneyCommand(
    private val plugin: TourneyPlugin,
    private val configManager: ConfigManager,
    private val messages: MessageService,
    private val arenaManager: ArenaManager,
    private val teamManager: TeamManager,
    private val tournamentManager: TournamentManager,
    private val bracketGui: BracketGui
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            messages.send(sender, "usage")
            return true
        }

        when (args[0].lowercase()) {
            "open" -> admin(sender, configManager.settings.permissions.open) { tournamentManager.open(sender) }
            "close" -> admin(sender, configManager.settings.permissions.close) { tournamentManager.close(sender) }
            "start" -> admin(sender, configManager.settings.permissions.start) { tournamentManager.start(sender) }
            "cancel" -> admin(sender, configManager.settings.permissions.cancel) { tournamentManager.cancel(true) }
            "status" -> admin(sender, configManager.settings.permissions.admin) { sender.sendMessage(tournamentManager.status()) }
            "reload" -> admin(sender, configManager.settings.permissions.reload) { reload(sender) }
            "bracket" -> bracket(sender)
            "team" -> team(sender, args.drop(1))
            "arena" -> arena(sender, args.drop(1))
            "kit" -> kit(sender, args.drop(1))
            else -> messages.send(sender, "usage")
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        return when (args.size) {
            1 -> rootTabs(args[0])
            2 -> secondTabs(args)
            3 -> thirdTabs(args)
            4 -> fourthTabs(args)
            else -> mutableListOf()
        }
    }

    private fun reload(sender: CommandSender) {
        if (tournamentManager.state == TournamentState.RUNNING) {
            messages.send(sender, "reload-blocked")
            return
        }

        val loaded = configManager.reload()

        messages.update(configManager.settings)
        arenaManager.reload()
        loaded.errors.forEach { plugin.logger.warning(it) }

        messages.send(sender, "reloaded", mapOf("errors" to loaded.errors.size.toString()))
    }

    private fun bracket(sender: CommandSender) {
        if (!canUseBracket(sender)) {
            messages.send(sender, "no-permission")
            return
        }

        val player = sender as? Player

        if (player == null) {
            sendConsoleBracket(sender)
            return
        }

        bracketGui.open(player)
    }

    private fun team(sender: CommandSender, args: List<String>) {
        val player = sender as? Player

        if (player == null) {
            messages.send(sender, "player-only")
            return
        }

        if (!canUseTeam(player)) {
            messages.send(player, "no-permission")
            return
        }

        when (args.firstOrNull()?.lowercase()) {
            "create" -> createTeam(player, args)
            "invite" -> inviteTeamMember(player, args)
            "accept" -> acceptTeamInvite(player, args)
            "leave" -> teamManager.leave(player, tournamentManager.state)
            "list" -> listTeams(sender)
            else -> messages.send(sender, "usage-team")
        }
    }

    private fun arena(sender: CommandSender, args: List<String>) {
        admin(sender, configManager.settings.permissions.arena) {
            when (args.firstOrNull()?.lowercase()) {
                "create" -> createArena(sender, args)
                "setspawn" -> setArenaSpawn(sender, args)
                "delete" -> deleteArena(sender, args)
                "list" -> listArenas(sender)
                else -> messages.send(sender, "usage-arena")
            }
        }
    }

    private fun kit(sender: CommandSender, args: List<String>) {
        admin(sender, configManager.settings.permissions.kit) {
            when (args.firstOrNull()?.lowercase()) {
                "save" -> saveKit(sender)
                else -> messages.send(sender, "usage-kit")
            }
        }
    }

    private fun createTeam(player: Player, args: List<String>) {
        val requestedName = args.drop(1).joinToString(" ")

        teamManager.createTeam(player, tournamentManager.state, requestedName)
    }

    private fun saveKit(sender: CommandSender) {
        val player = sender as? Player

        if (player == null) {
            messages.send(sender, "player-only")
            return
        }

        val saved = configManager.saveKitFromInventory(player)
        messages.update(configManager.settings)

        messages.send(player, "kit-saved", mapOf("items" to saved.toString()))
    }

    private fun inviteTeamMember(player: Player, args: List<String>) {
        val target = args.getOrNull(1)?.let { Bukkit.getPlayerExact(it) }

        if (target == null) {
            messages.send(player, "player-not-found")
            return
        }

        teamManager.invite(player, target, tournamentManager.state)
    }

    private fun acceptTeamInvite(player: Player, args: List<String>) {
        val teamName = args.drop(1).joinToString(" ")

        if (teamName.isBlank()) {
            messages.send(player, "usage-team")
            return
        }

        teamManager.accept(player, teamName, tournamentManager.state)
    }

    private fun listTeams(sender: CommandSender) {
        val teams = teamManager.registeredTeams()

        if (teams.isEmpty()) {
            messages.send(sender, "no-teams")
            return
        }

        teams.forEach {
            sender.sendMessage("${it.name}: ${it.members.size}/${configManager.settings.teamSize}")
        }
    }

    private fun createArena(sender: CommandSender, args: List<String>) {
        val name = args.getOrNull(1)

        if (name.isNullOrBlank()) {
            messages.send(sender, "usage-arena")
            return
        }

        if (arenaManager.create(name)) {
            messages.send(sender, "arena-created", mapOf("arena" to name))
        } else {
            messages.send(sender, "arena-exists")
        }
    }

    private fun setArenaSpawn(sender: CommandSender, args: List<String>) {
        val player = sender as? Player

        if (player == null) {
            messages.send(sender, "player-only")
            return
        }

        val arena = args.getOrNull(1)
        val spawn = args.getOrNull(2)

        if (arena.isNullOrBlank() || spawn.isNullOrBlank()) {
            messages.send(sender, "usage-arena")
            return
        }

        if (arenaManager.setSpawn(arena, spawn, player.location)) {
            messages.send(sender, "arena-spawn-set", mapOf("arena" to arena, "spawn" to spawn))
        } else {
            messages.send(sender, "arena-not-found")
        }
    }

    private fun deleteArena(sender: CommandSender, args: List<String>) {
        val name = args.getOrNull(1)

        if (name.isNullOrBlank()) {
            messages.send(sender, "usage-arena")
            return
        }

        if (arenaManager.delete(name)) {
            messages.send(sender, "arena-deleted", mapOf("arena" to name))
        } else {
            messages.send(sender, "arena-not-found")
        }
    }

    private fun listArenas(sender: CommandSender) {
        val arenas = arenaManager.all()

        if (arenas.isEmpty()) {
            messages.send(sender, "no-arenas")
            return
        }

        arenas.forEach {
            val status = if (it.isReady()) "ready" else "incomplete"
            sender.sendMessage("${it.name}: $status")
        }
    }

    private fun sendConsoleBracket(sender: CommandSender) {
        tournamentManager.matches().forEach {
            sender.sendMessage(
                "Match ${it.id}: Round ${it.round}, " +
                    "${it.teamA?.name ?: "Bye"} vs ${it.teamB?.name ?: "Bye"}, " +
                    "${it.status}, winner ${it.winner?.name ?: "None"}"
            )
        }
    }

    private fun admin(sender: CommandSender, permission: String, action: () -> Unit) {
        if (!sender.hasPermission(permission) && !sender.hasPermission(configManager.settings.permissions.admin)) {
            messages.send(sender, "no-permission")
            return
        }

        action()
    }

    private fun canUseBracket(sender: CommandSender): Boolean {
        return sender.hasPermission(configManager.settings.permissions.bracket) ||
            sender.hasPermission(configManager.settings.permissions.admin)
    }

    private fun canUseTeam(player: Player): Boolean {
        return player.hasPermission(configManager.settings.permissions.team) ||
            player.hasPermission(configManager.settings.permissions.player)
    }

    private fun rootTabs(prefix: String): MutableList<String> {
        return filter(
            prefix,
            listOf("open", "close", "start", "cancel", "status", "reload", "bracket", "team", "arena", "kit")
        )
    }

    private fun secondTabs(args: Array<out String>): MutableList<String> {
        return when (args[0].lowercase()) {
            "team" -> filter(args[1], listOf("create", "invite", "accept", "leave", "list"))
            "arena" -> filter(args[1], listOf("create", "setspawn", "delete", "list"))
            "kit" -> filter(args[1], listOf("save"))
            else -> mutableListOf()
        }
    }

    private fun thirdTabs(args: Array<out String>): MutableList<String> {
        return when {
            args[0].equals("team", true) && args[1].equals("invite", true) -> {
                filter(args[2], Bukkit.getOnlinePlayers().map { it.name })
            }

            args[0].equals("team", true) && args[1].equals("accept", true) -> {
                filter(args[2], teamManager.registeredTeams().map { it.name })
            }

            args[0].equals("arena", true) &&
                (args[1].equals("setspawn", true) || args[1].equals("delete", true)) -> {
                filter(args[2], arenaManager.all().map { it.name })
            }

            else -> mutableListOf()
        }
    }

    private fun fourthTabs(args: Array<out String>): MutableList<String> {
        return if (args[0].equals("arena", true) && args[1].equals("setspawn", true)) {
            filter(args[3], listOf("teamA", "teamB", "spectator"))
        } else {
            mutableListOf()
        }
    }

    private fun filter(prefix: String, values: Collection<String>): MutableList<String> {
        return values
            .filter { it.startsWith(prefix, true) }
            .sorted()
            .toMutableList()
    }
}
