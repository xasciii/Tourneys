package org.asc.tourneys.team

import org.asc.tourneys.TourneyPlugin
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.message.MessageService
import org.asc.tourneys.model.TournamentState
import org.asc.tourneys.model.TournamentTeam
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.random.Random

class TeamManager(
    private val plugin: TourneyPlugin,
    private val configManager: ConfigManager,
    private val messages: MessageService
) {
    private val teams = linkedMapOf<String, TournamentTeam>()

    fun reset() {
        teams.clear()
    }

    fun createTeam(player: Player, state: TournamentState, requestedName: String? = null): Boolean {
        if (state != TournamentState.OPEN) {
            messages.send(player, "registration-not-open")
            return false
        }
        if (findTeam(player.uniqueId) != null) {
            messages.send(player, "already-in-team")
            return false
        }
        if (teams.size >= configManager.settings.maximumTeams) {
            messages.send(player, "max-teams")
            return false
        }
        val generated = requestedName
            ?.takeIf { configManager.settings.customTeamNames }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { GeneratedTeamName(it, null, nextColor()) }
            ?: nextTeamName()

        val name = generated.name

        if (teams.values.any { it.name.equals(name, true) }) {
            messages.send(player, "team-name-taken")
            return false
        }

        val team = TournamentTeam(UUID.randomUUID().toString(), name, generated.color, generated.number, player.uniqueId)
        teams[team.id] = team
        messages.send(player, "team-created", mapOf("team" to team.name))
        return true
    }

    fun invite(captain: Player, target: Player, state: TournamentState): Boolean {
        if (state != TournamentState.OPEN) {
            messages.send(captain, "registration-not-open")
            return false
        }
        val team = findTeam(captain.uniqueId)
        if (team == null || team.captain != captain.uniqueId) {
            messages.send(captain, "captain-required")
            return false
        }
        if (team.members.size >= configManager.settings.teamSize) {
            messages.send(captain, "team-full")
            return false
        }
        if (findTeam(target.uniqueId) != null) {
            messages.send(captain, "target-in-team", mapOf("player" to target.name))
            return false
        }
        team.invited += target.uniqueId
        messages.send(captain, "invite-sent", mapOf("player" to target.name, "team" to team.name))
        messages.send(target, "invite-received", mapOf("player" to captain.name, "team" to team.name))
        return true
    }

    fun accept(player: Player, teamName: String, state: TournamentState): Boolean {
        if (state != TournamentState.OPEN) {
            messages.send(player, "registration-not-open")
            return false
        }
        if (findTeam(player.uniqueId) != null) {
            messages.send(player, "already-in-team")
            return false
        }
        val team = findByName(teamName)
        if (team == null || !team.invited.contains(player.uniqueId)) {
            messages.send(player, "no-invite")
            return false
        }
        if (team.members.size >= configManager.settings.teamSize) {
            messages.send(player, "team-full")
            return false
        }
        team.invited -= player.uniqueId
        team.members += player.uniqueId
        messages.send(player, "joined-team", mapOf("team" to team.name))
        Bukkit.getPlayer(team.captain)?.let { messages.send(it, "player-joined-team", mapOf("player" to player.name, "team" to team.name)) }
        return true
    }

    fun leave(player: Player, state: TournamentState): Boolean {
        if (state != TournamentState.OPEN) {
            messages.send(player, "cannot-leave-now")
            return false
        }
        val team = findTeam(player.uniqueId)
        if (team == null) {
            messages.send(player, "not-in-team")
            return false
        }
        if (team.captain == player.uniqueId) {
            teams.remove(team.id)
            team.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { messages.send(it, "team-disbanded", mapOf("team" to team.name)) }
        } else {
            team.members -= player.uniqueId
            messages.send(player, "left-team", mapOf("team" to team.name))
            Bukkit.getPlayer(team.captain)?.let { messages.send(it, "player-left-team", mapOf("player" to player.name, "team" to team.name)) }
        }
        return true
    }

    fun removeIncompleteTeams(): Int {
        val incomplete = teams.values.filter { it.members.size < configManager.settings.teamSize }
        incomplete.forEach { teams.remove(it.id) }
        return incomplete.size
    }

    fun registeredTeams(): List<TournamentTeam> = teams.values.toList()

    fun findTeam(playerId: UUID): TournamentTeam? = teams.values.firstOrNull { it.members.contains(playerId) }

    fun findByName(name: String): TournamentTeam? = teams.values.firstOrNull { it.name.equals(name, true) }

    private fun nextTeamName(): GeneratedTeamName {
        val settings = configManager.settings
        if (!settings.randomTeamNames) {
            val color = nextColor()
            return GeneratedTeamName("Team-${teams.size + 1}", null, color)
        }
        repeat(500) {
            val number = Random.nextInt(settings.teamNameNumberMin, settings.teamNameNumberMax + 1)
            val color = randomColor()
            val name = settings.teamNameFormat
                .replace("{color}", color)
                .replace("{number}", number.toString())

            if (teams.values.none { it.name.equals(name, true) }) {
                return GeneratedTeamName(name, number, color)
            }
        }

        val color = nextColor()
        return GeneratedTeamName("Team-${teams.size + 1}", null, color)
    }

    private fun nextColor(): String {
        val colors = configManager.settings.teamColors

        return colors.getOrElse(teams.size % colors.size) { "&f" }
    }

    private fun randomColor(): String {
        val colors = configManager.settings.teamColors

        return colors.randomOrNull() ?: "&f"
    }
}

private data class GeneratedTeamName(
    val name: String,
    val number: Int?,
    val color: String
)
