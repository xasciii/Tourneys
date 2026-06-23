package org.asc.tourneys.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.team.TeamManager
import org.bukkit.OfflinePlayer

class TourneysPlaceholderExpansion(
    private val configManager: ConfigManager,
    private val teamManager: TeamManager
) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return configManager.settings.placeholderIdentifier
    }

    override fun getAuthor(): String {
        return "ASC"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String {
        if (player == null) {
            return configManager.settings.placeholderNoTeam
        }

        val team = teamManager.findTeam(player.uniqueId)

        return when (params.lowercase()) {
            "team" -> team?.name ?: configManager.settings.placeholderNoTeam
            "team_plain" -> team?.name?.replace(Regex("&[0-9a-fk-orA-FK-OR]"), "") ?: configManager.settings.placeholderNoTeam
            "team_color" -> team?.color ?: ""
            "team_number" -> team?.number?.toString() ?: ""
            "in_team" -> if (team == null) "false" else "true"
            "nametag" -> nametag(team?.name)
            else -> ""
        }
    }

    private fun nametag(teamName: String?): String {
        if (teamName == null) {
            return configManager.settings.placeholderNoTeam
        }

        return configManager.settings.placeholderNametagFormat.replace("{team}", teamName)
    }
}
