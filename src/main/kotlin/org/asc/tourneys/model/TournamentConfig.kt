package org.asc.tourneys.model

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

data class TournamentConfig(
    val teamSize: Int,
    val minimumTeams: Int,
    val maximumTeams: Int,
    val allowIncompleteTeamsOnClose: Boolean,
    val removeIncompleteTeamsOnClose: Boolean,
    val displayName: String,
    val typeName: String,
    val randomTeamNames: Boolean,
    val customTeamNames: Boolean,
    val teamNameFormat: String,
    val teamNameNumberMin: Int,
    val teamNameNumberMax: Int,
    val teamColors: List<String>,
    val lobbySpawn: Location?,
    val fallbackSpectatorSpawn: Location?,
    val maxActiveMatches: Int,
    val countdownBeforeMatchStarts: Int,
    val delayAfterMatchEnds: Int,
    val delayBeforeReturningToLobby: Int,
    val deathLightningEffectEnabled: Boolean,
    val deathSoundsEnabled: Boolean,
    val allowRejoinDuringActiveMatch: Boolean,
    val sendEliminatedPlayersToSpectator: Boolean,
    val clearDropsBetweenMatches: Boolean,
    val clearInventoriesOnMatchStart: Boolean,
    val giveKitOnMatchStart: Boolean,
    val kitItems: List<KitItem>,
    val blockedCommands: List<String>,
    val bracketInventoryEnabled: Boolean,
    val bracketGuiTitle: String,
    val bracketGuiRows: Int,
    val bracketGuiAutoRows: Boolean,
    val bracketWaitingMaterial: Material,
    val bracketCountdownMaterial: Material,
    val bracketActiveMaterial: Material,
    val bracketFinishedMaterial: Material,
    val bracketItemName: String,
    val bracketItemLore: List<String>,
    val bracketUrlEnabled: Boolean,
    val bracketUrlValue: String,
    val apiSnapshotEnabled: Boolean,
    val placeholderSupportEnabled: Boolean,
    val placeholderIdentifier: String,
    val placeholderNoTeam: String,
    val placeholderNametagFormat: String,
    val tieBehavior: TieBehavior,
    val permissions: PermissionConfig,
    val messagePrefix: String,
    val messages: Map<String, String>,
    val titles: Map<String, TitleText>,
    val actionbars: Map<String, String>
)

data class KitItem(
    val itemStack: ItemStack?,
    val material: Material,
    val amount: Int,
    val slot: Int?,
    val displayName: String?,
    val lore: List<String>,
    val enchantments: Map<Enchantment, Int>,
    val unbreakable: Boolean,
    val customModelData: Int?,
    val potionEffects: List<KitPotionEffect>
)

data class KitPotionEffect(
    val type: PotionEffectType,
    val durationSeconds: Int,
    val amplifier: Int,
    val ambient: Boolean,
    val particles: Boolean,
    val icon: Boolean
)

data class PermissionConfig(
    val admin: String,
    val open: String,
    val close: String,
    val start: String,
    val cancel: String,
    val restart: String,
    val reload: String,
    val arena: String,
    val kit: String,
    val player: String,
    val team: String,
    val bracket: String
)

data class TitleText(
    val title: String,
    val subtitle: String
)
