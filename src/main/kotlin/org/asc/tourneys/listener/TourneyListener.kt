package org.asc.tourneys.listener

import org.asc.tourneys.TourneyPlugin
import org.asc.tourneys.bracket.BracketGui
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.message.MessageService
import org.asc.tourneys.model.TournamentState
import org.asc.tourneys.model.TournamentMatch
import org.asc.tourneys.tournament.TournamentManager
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent

class TourneyListener(
    private val plugin: TourneyPlugin,
    private val configManager: ConfigManager,
    private val messages: MessageService,
    private val tournamentManager: TournamentManager,
    private val bracketGui: BracketGui
) : Listener {

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        tournamentManager.handleDeath(event.entity)

        if (configManager.settings.clearDropsBetweenMatches) {
            event.drops.clear()
        }
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val match = tournamentManager.activeMatch(event.player.uniqueId) ?: return

        if (!configManager.settings.sendEliminatedPlayersToSpectator) {
            return
        }

        spectatorSpawn(match)?.let {
            event.respawnLocation = it
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        tournamentManager.handleQuit(event.player)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        tournamentManager.handleJoin(event.player)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        bracketGui.handleClick(event)
    }

    @EventHandler
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (tournamentManager.state != TournamentState.RUNNING) {
            return
        }

        val label = event.message.trimStart('/').substringBefore(' ').lowercase()

        if (!isBlockedCommand(label)) {
            return
        }

        event.isCancelled = true
        messages.send(event.player, "command-blocked")
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player ?: return
        val damager = event.damager as? Player ?: return

        if (isSpectatorDamage(player, damager)) {
            event.isCancelled = true
        }
    }

    private fun spectatorSpawn(match: TournamentMatch): Location? {
        val arena = match.arenaName?.let { plugin.arenaManager.get(it) }

        return arena?.spectatorSpawn ?: configManager.settings.fallbackSpectatorSpawn
    }

    private fun isBlockedCommand(label: String): Boolean {
        return configManager.settings.blockedCommands.contains(label)
    }

    private fun isSpectatorDamage(player: Player, damager: Player): Boolean {
        return tournamentManager.isSpectating(player.uniqueId) ||
            tournamentManager.isSpectating(damager.uniqueId)
    }
}
