package org.asc.tourneys.config

import org.asc.tourneys.TourneyPlugin
import org.asc.tourneys.model.KitItem
import org.asc.tourneys.model.KitPotionEffect
import org.asc.tourneys.model.PermissionConfig
import org.asc.tourneys.model.TieBehavior
import org.asc.tourneys.model.TitleText
import org.asc.tourneys.model.TournamentArena
import org.asc.tourneys.model.TournamentConfig
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import java.io.File

class ConfigManager(private val plugin: TourneyPlugin) {

    lateinit var settings: TournamentConfig
        private set

    private lateinit var language: FileConfiguration
    private lateinit var kit: FileConfiguration
    private lateinit var api: FileConfiguration

    fun reload(): ConfigLoadResult {
        plugin.reloadConfig()
        language = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "language.yml"))
        kit = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "kit.yml"))
        api = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "api.yml"))

        val errors = mutableListOf<String>()
        val config = plugin.config
        settings = TournamentConfig(
            teamSize = positiveInt("team.size", 2, errors),
            minimumTeams = positiveInt("team.minimum-teams", 2, errors),
            maximumTeams = positiveInt("team.maximum-teams", 32, errors),
            allowIncompleteTeamsOnClose = config.getBoolean("team.allow-incomplete-teams-on-close", false),
            removeIncompleteTeamsOnClose = config.getBoolean("team.remove-incomplete-teams-on-close", true),
            displayName = config.getString("tournament.display-name", "<gold>Tournament</gold>") ?: "<gold>Tournament</gold>",
            typeName = config.getString("tournament.type-name", "Team Tournament") ?: "Team Tournament",
            randomTeamNames = config.getBoolean("team.names.random", true),
            customTeamNames = config.getBoolean("team.names.custom", false),
            teamNameFormat = config.getString("team.names.format", "{color}[{number}]") ?: "{color}[{number}]",
            teamNameNumberMin = config.getInt("team.names.number-min", 100),
            teamNameNumberMax = config.getInt("team.names.number-max", 999),
            teamColors = config.getStringList("team.colors").ifEmpty { listOf("&a", "&b", "&e", "&d") },
            lobbySpawn = readLocation(config.getConfigurationSection("spawns.lobby"), errors, false),
            fallbackSpectatorSpawn = readLocation(config.getConfigurationSection("spawns.fallback-spectator"), errors, false),
            maxActiveMatches = positiveInt("matches.max-active-matches", 1, errors),
            countdownBeforeMatchStarts = nonNegativeInt("matches.countdown-before-match-starts", 10, errors),
            delayAfterMatchEnds = nonNegativeInt("matches.delay-after-match-ends", 5, errors),
            delayBeforeReturningToLobby = nonNegativeInt("matches.delay-before-returning-to-lobby", 5, errors),
            deathLightningEffectEnabled = config.getBoolean("matches.death-lightning-effect-enabled", true),
            deathSoundsEnabled = config.getBoolean("matches.death-sounds-enabled", true),
            allowRejoinDuringActiveMatch = config.getBoolean("matches.allow-rejoin-during-active-match", true),
            sendEliminatedPlayersToSpectator = config.getBoolean("matches.send-eliminated-players-to-spectator", true),
            clearDropsBetweenMatches = config.getBoolean("matches.clear-drops-between-matches", true),
            clearInventoriesOnMatchStart = config.getBoolean("matches.clear-inventories-on-match-start", true),
            giveKitOnMatchStart = config.getBoolean("matches.give-kit-on-match-start", true),
            kitItems = readKit(errors),
            blockedCommands = config.getStringList("blocked-commands").map { it.lowercase().trimStart('/') },
            bracketInventoryEnabled = config.getBoolean("bracket.inventory.enabled", true),
            bracketGuiTitle = config.getString("bracket.inventory.title", "<gold>{type} Bracket</gold>") ?: "<gold>{type} Bracket</gold>",
            bracketGuiRows = config.getInt("bracket.inventory.rows", 6).coerceIn(1, 6),
            bracketGuiAutoRows = config.getBoolean("bracket.inventory.auto-rows", true),
            bracketWaitingMaterial = material("bracket.inventory.materials.waiting", "GRAY_CONCRETE", errors),
            bracketCountdownMaterial = material("bracket.inventory.materials.countdown", "YELLOW_CONCRETE", errors),
            bracketActiveMaterial = material("bracket.inventory.materials.active", "YELLOW_CONCRETE", errors),
            bracketFinishedMaterial = material("bracket.inventory.materials.finished", "LIME_CONCRETE", errors),
            bracketItemName = config.getString("bracket.inventory.item.name", "<gold>Match {match}</gold> <dark_gray>|</dark_gray> <white>Round {round}</white>")
                ?: "<gold>Match {match}</gold> <dark_gray>|</dark_gray> <white>Round {round}</white>",
            bracketItemLore = config.getStringList("bracket.inventory.item.lore").ifEmpty {
                listOf(
                    "<gray>Status:</gray> <white>{status}</white>",
                    "<gray>Arena:</gray> <white>{arena}</white>",
                    "<gray>Team A:</gray> <white>{teamA}</white>",
                    "<gray>Players:</gray> <white>{playersA}</white>",
                    "<gray>Team B:</gray> <white>{teamB}</white>",
                    "<gray>Players:</gray> <white>{playersB}</white>",
                    "<gray>Winner:</gray> <green>{winner}</green>"
                )
            },
            bracketUrlEnabled = api.getBoolean("bracket-url.enabled", false),
            bracketUrlValue = api.getString("bracket-url.value", "") ?: "",
            apiSnapshotEnabled = api.getBoolean("snapshot.enabled", false),
            placeholderSupportEnabled = api.getBoolean("placeholders.enabled", false),
            placeholderIdentifier = api.getString("placeholders.identifier", "tourneys") ?: "tourneys",
            placeholderNoTeam = api.getString("placeholders.no-team", "") ?: "",
            placeholderNametagFormat = api.getString("placeholders.nametag-format", "{team} ") ?: "{team} ",
            tieBehavior = runCatching {
                TieBehavior.valueOf((config.getString("matches.tie-behavior", "random") ?: "random").uppercase())
            }.getOrElse {
                errors += "Invalid matches.tie-behavior. Use teamA, teamB, random, or rematch."
                TieBehavior.RANDOM
            },
            permissions = PermissionConfig(
                admin = config.getString("permissions.admin", "tourney.admin") ?: "tourney.admin",
                open = config.getString("permissions.open", "tourney.admin.open") ?: "tourney.admin.open",
                close = config.getString("permissions.close", "tourney.admin.close") ?: "tourney.admin.close",
                start = config.getString("permissions.start", "tourney.admin.start") ?: "tourney.admin.start",
                cancel = config.getString("permissions.cancel", "tourney.admin.cancel") ?: "tourney.admin.cancel",
                reload = config.getString("permissions.reload", "tourney.admin.reload") ?: "tourney.admin.reload",
                arena = config.getString("permissions.arena", "tourney.admin.arena") ?: "tourney.admin.arena",
                kit = config.getString("permissions.kit", "tourney.admin.kit") ?: "tourney.admin.kit",
                player = config.getString("permissions.player", "tourney.player") ?: "tourney.player",
                team = config.getString("permissions.team", "tourney.player.team") ?: "tourney.player.team",
                bracket = config.getString("permissions.bracket", "tourney.player.bracket") ?: "tourney.player.bracket"
            ),
            messagePrefix = language.getString("prefix", "") ?: "",
            messages = readStringMap(language.getConfigurationSection("messages")),
            titles = readTitles(language.getConfigurationSection("titles")),
            actionbars = readStringMap(language.getConfigurationSection("actionbars"))
        )
        if (settings.maximumTeams < settings.minimumTeams) {
            errors += "team.maximum-teams must be greater than or equal to team.minimum-teams."
        }
        if (settings.teamNameNumberMax < settings.teamNameNumberMin) {
            errors += "team.names.number-max must be greater than or equal to team.names.number-min."
        }
        return ConfigLoadResult(errors)
    }

    fun readArenas(): MutableMap<String, TournamentArena> {
        val arenas = mutableMapOf<String, TournamentArena>()
        val section = plugin.config.getConfigurationSection("arenas") ?: return arenas
        for (name in section.getKeys(false)) {
            val arenaSection = section.getConfigurationSection(name) ?: continue
            arenas[name.lowercase()] = TournamentArena(
                name = name,
                teamASpawn = readLocation(arenaSection.getConfigurationSection("teamA"), mutableListOf(), false),
                teamBSpawn = readLocation(arenaSection.getConfigurationSection("teamB"), mutableListOf(), false),
                spectatorSpawn = readLocation(arenaSection.getConfigurationSection("spectator"), mutableListOf(), false)
            )
        }
        return arenas
    }

    fun saveArenas(arenas: Collection<TournamentArena>) {
        plugin.config.set("arenas", null)
        for (arena in arenas) {
            writeLocation("arenas.${arena.name}.teamA", arena.teamASpawn)
            writeLocation("arenas.${arena.name}.teamB", arena.teamBSpawn)
            writeLocation("arenas.${arena.name}.spectator", arena.spectatorSpawn)
        }
        plugin.saveConfig()
    }

    fun saveKitFromInventory(player: Player): Int {
        kit.set("saved-from-inventory", true)
        kit.set("items", null)

        var saved = 0

        for (slot in 0 until player.inventory.size) {
            val item = player.inventory.getItem(slot)

            if (item == null || item.type == Material.AIR) {
                continue
            }

            kit.set("items.slot_$slot.slot", slot)
            kit.set("items.slot_$slot.item", item)
            saved++
        }

        kit.save(File(plugin.dataFolder, "kit.yml"))
        reload()

        return saved
    }

    private fun positiveInt(path: String, fallback: Int, errors: MutableList<String>): Int {
        val value = plugin.config.getInt(path, fallback)
        if (value <= 0) {
            errors += "$path must be greater than 0."
            return fallback
        }
        return value
    }

    private fun nonNegativeInt(path: String, fallback: Int, errors: MutableList<String>): Int {
        val value = plugin.config.getInt(path, fallback)
        if (value < 0) {
            errors += "$path must be 0 or greater."
            return fallback
        }
        return value
    }

    private fun readLocation(section: ConfigurationSection?, errors: MutableList<String>, required: Boolean): Location? {
        if (section == null) {
            if (required) errors += "Missing location section."
            return null
        }
        val worldName = section.getString("world")
        if (worldName.isNullOrBlank()) {
            if (required) errors += "Location is missing world."
            return null
        }
        val world = Bukkit.getWorld(worldName)
        if (world == null) {
            errors += "World '$worldName' is not loaded."
            return null
        }
        return Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            section.getDouble("yaw").toFloat(),
            section.getDouble("pitch").toFloat()
        )
    }

    private fun writeLocation(path: String, location: Location?) {
        if (location == null) return
        plugin.config.set("$path.world", location.world.name)
        plugin.config.set("$path.x", location.x)
        plugin.config.set("$path.y", location.y)
        plugin.config.set("$path.z", location.z)
        plugin.config.set("$path.yaw", location.yaw.toDouble())
        plugin.config.set("$path.pitch", location.pitch.toDouble())
    }

    private fun readKit(errors: MutableList<String>): List<KitItem> {
        val section = kit.getConfigurationSection("items") ?: return emptyList()
        return section.getKeys(false).mapNotNull { key ->
            val itemSection = section.getConfigurationSection(key) ?: return@mapNotNull null
            val itemStack = itemSection.getItemStack("item")
            val materialName = itemSection.getString("material")
            val material = itemStack?.type ?: materialName?.let { Material.matchMaterial(it) }

            if (material == null) {
                errors += "Missing or invalid kit item at kit.items.$key."
                return@mapNotNull null
            }

            if (material == Material.AIR) {
                errors += "kit.items.$key cannot use AIR."
                return@mapNotNull null
            }

            KitItem(
                itemStack = itemStack,
                material = material,
                amount = itemSection.getInt("amount", 1).coerceIn(1, material.maxStackSize.coerceAtLeast(1)),
                slot = itemSection.takeIf { it.contains("slot") }?.getInt("slot")?.takeIf { it in 0..40 },
                displayName = itemSection.getString("name"),
                lore = itemSection.getStringList("lore"),
                enchantments = readEnchantments(itemSection.getConfigurationSection("enchantments"), "kit.items.$key", errors),
                unbreakable = itemSection.getBoolean("unbreakable", false),
                customModelData = itemSection.takeIf { it.contains("custom-model-data") }?.getInt("custom-model-data"),
                potionEffects = readPotionEffects(itemSection.getConfigurationSection("potion-effects"), "kit.items.$key", errors)
            )
        }
    }

    private fun material(path: String, fallback: String, errors: MutableList<String>): Material {
        val value = plugin.config.getString(path, fallback) ?: fallback
        val material = Material.matchMaterial(value)

        if (material == null || material == Material.AIR) {
            errors += "$path must be a valid non-air material."
            return Material.matchMaterial(fallback) ?: Material.STONE
        }

        return material
    }

    private fun readEnchantments(section: ConfigurationSection?, path: String, errors: MutableList<String>): Map<Enchantment, Int> {
        if (section == null) return emptyMap()
        return section.getKeys(false).mapNotNull { key ->
            val enchantment = Enchantment.getByName(key.uppercase())
            if (enchantment == null) {
                errors += "Invalid enchantment '$key' at $path.enchantments."
                null
            } else {
                enchantment to section.getInt(key, 1).coerceAtLeast(1)
            }
        }.toMap()
    }

    private fun readPotionEffects(section: ConfigurationSection?, path: String, errors: MutableList<String>): List<KitPotionEffect> {
        if (section == null) return emptyList()
        return section.getKeys(false).mapNotNull { key ->
            val child = section.getConfigurationSection(key) ?: return@mapNotNull null
            val typeName = child.getString("type") ?: key
            val type = PotionEffectType.getByName(typeName.uppercase())
            if (type == null) {
                errors += "Invalid potion effect '$typeName' at $path.potion-effects.$key."
                return@mapNotNull null
            }
            KitPotionEffect(
                type = type,
                durationSeconds = child.getInt("duration-seconds", 30).coerceAtLeast(1),
                amplifier = child.getInt("amplifier", 0).coerceAtLeast(0),
                ambient = child.getBoolean("ambient", false),
                particles = child.getBoolean("particles", true),
                icon = child.getBoolean("icon", true)
            )
        }
    }

    private fun readStringMap(section: ConfigurationSection?): Map<String, String> {
        if (section == null) return emptyMap()
        return section.getKeys(false).associateWith { section.getString(it, "") ?: "" }
    }

    private fun readTitles(section: ConfigurationSection?): Map<String, TitleText> {
        if (section == null) return emptyMap()
        return section.getKeys(false).associateWith {
            val child = section.getConfigurationSection(it)
            TitleText(child?.getString("title", "") ?: "", child?.getString("subtitle", "") ?: "")
        }
    }
}

data class ConfigLoadResult(val errors: List<String>)
