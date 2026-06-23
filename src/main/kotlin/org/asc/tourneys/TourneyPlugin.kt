package org.asc.tourneys

import org.asc.tourneys.arena.ArenaManager
import org.asc.tourneys.bracket.BracketGui
import org.asc.tourneys.command.TourneyCommand
import org.asc.tourneys.config.ConfigLoadResult
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.listener.TourneyListener
import org.asc.tourneys.message.MessageService
import org.asc.tourneys.placeholder.TourneysPlaceholderExpansion
import org.asc.tourneys.team.TeamManager
import org.asc.tourneys.tournament.TournamentManager
import org.bukkit.plugin.java.JavaPlugin

open class TourneyPlugin : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set

    lateinit var messages: MessageService
        private set

    lateinit var arenaManager: ArenaManager
        private set

    lateinit var teamManager: TeamManager
        private set

    lateinit var tournamentManager: TournamentManager
        private set

    lateinit var bracketGui: BracketGui
        private set

    override fun onEnable() {
        saveDefaultConfig()
        saveResource("language.yml", false)
        saveResource("kit.yml", false)
        saveResource("api.yml", false)

        val loaded = configure()

        registerCommand()
        registerListeners()

        loaded.errors.forEach { logger.warning(it) }
    }

    override fun onDisable() {
        if (::tournamentManager.isInitialized) {
            tournamentManager.cancel(false)
        }
    }

    private fun configure(): ConfigLoadResult {
        configManager = ConfigManager(this)

        val loaded = configManager.reload()

        messages = MessageService(configManager.settings)
        arenaManager = ArenaManager(this, configManager)
        teamManager = TeamManager(this, configManager, messages)
        tournamentManager = TournamentManager(this, configManager, messages, arenaManager, teamManager)
        bracketGui = BracketGui(this, configManager, tournamentManager)

        return loaded
    }

    private fun registerCommand() {
        val command = TourneyCommand(
            plugin = this,
            configManager = configManager,
            messages = messages,
            arenaManager = arenaManager,
            teamManager = teamManager,
            tournamentManager = tournamentManager,
            bracketGui = bracketGui
        )

        getCommand("tourney")?.setExecutor(command)
        getCommand("tourney")?.tabCompleter = command
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(
            TourneyListener(
                plugin = this,
                configManager = configManager,
                messages = messages,
                tournamentManager = tournamentManager,
                bracketGui = bracketGui
            ),
            this
        )

        registerPlaceholders()
    }

    private fun registerPlaceholders() {
        if (!configManager.settings.placeholderSupportEnabled) {
            return
        }

        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            logger.warning("Placeholder support is enabled, but PlaceholderAPI is not installed.")
            return
        }

        TourneysPlaceholderExpansion(configManager, teamManager).register()
    }
}
