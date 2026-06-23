package org.asc.tourneys.bracket

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.asc.tourneys.TourneyPlugin
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.model.MatchStatus
import org.asc.tourneys.model.TournamentMatch
import org.asc.tourneys.tournament.TournamentManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BracketGui(
    private val plugin: TourneyPlugin,
    private val configManager: ConfigManager,
    private val tournamentManager: TournamentManager
) {
    private val mini = MiniMessage.miniMessage()
    private val holder = BracketHolder()

    fun open(player: Player) {
        if (!configManager.settings.bracketInventoryEnabled) {
            val message = "${configManager.settings.messagePrefix}${configManager.settings.messages["bracket-disabled"] ?: "<red>The bracket GUI is disabled.</red>"}"
            player.sendMessage(mini.deserialize(message))
            return
        }
        val matches = tournamentManager.matches()
        val size = inventorySize(matches.size)
        val title = configManager.settings.bracketGuiTitle
            .replace("{type}", configManager.settings.typeName)
            .replace("{display}", configManager.settings.displayName)
        val inventory = Bukkit.createInventory(holder, size, mini.deserialize(title))

        matches.take(size).forEachIndexed { index, match ->
            inventory.setItem(index, item(match))
        }

        player.openInventory(inventory)
    }

    fun handleClick(event: InventoryClickEvent) {
        if (event.inventory.holder is BracketHolder) {
            event.isCancelled = true
        }
    }

    private fun item(match: TournamentMatch): ItemStack {
        val material = when (match.status) {
            MatchStatus.WAITING -> configManager.settings.bracketWaitingMaterial
            MatchStatus.COUNTDOWN -> configManager.settings.bracketCountdownMaterial
            MatchStatus.ACTIVE -> configManager.settings.bracketActiveMaterial
            MatchStatus.FINISHED -> configManager.settings.bracketFinishedMaterial
        }
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(mini.deserialize(apply(match, configManager.settings.bracketItemName)))
        meta.lore(lore(match))
        item.itemMeta = meta
        return item
    }

    private fun lore(match: TournamentMatch): List<Component> {
        return configManager.settings.bracketItemLore.map { mini.deserialize(apply(match, it)) }
    }

    private fun inventorySize(matchCount: Int): Int {
        if (!configManager.settings.bracketGuiAutoRows) {
            return configManager.settings.bracketGuiRows * 9
        }

        return ((matchCount.coerceAtLeast(1) + 8) / 9 * 9).coerceIn(9, configManager.settings.bracketGuiRows * 9)
    }

    private fun apply(match: TournamentMatch, text: String): String {
        return text
            .replace("{match}", match.id.toString())
            .replace("{round}", match.round.toString())
            .replace("{status}", match.status.name)
            .replace("{arena}", match.arenaName ?: "Waiting")
            .replace("{teamA}", match.teamA?.name ?: "Bye")
            .replace("{teamB}", match.teamB?.name ?: "Bye")
            .replace("{playersA}", players(match.teamA))
            .replace("{playersB}", players(match.teamB))
            .replace("{winner}", match.winner?.name ?: "None")
    }

    private fun players(team: org.asc.tourneys.model.TournamentTeam?): String {
        if (team == null) return "None"
        return team.members.joinToString(", ") { Bukkit.getOfflinePlayer(it).name ?: it.toString().take(8) }
    }
}

class BracketHolder : org.bukkit.inventory.InventoryHolder {
    override fun getInventory(): Inventory = Bukkit.createInventory(this, 9)
}
