package org.asc.tourneys.arena

import org.asc.tourneys.TourneyPlugin
import org.asc.tourneys.config.ConfigManager
import org.asc.tourneys.model.TournamentArena
import org.bukkit.Location

class ArenaManager(
    private val plugin: TourneyPlugin,
    private val configManager: ConfigManager
) {
    private val arenas = configManager.readArenas()

    fun reload() {
        arenas.clear()
        arenas.putAll(configManager.readArenas())
    }

    fun create(name: String): Boolean {
        val key = name.lowercase()
        if (arenas.containsKey(key)) return false
        arenas[key] = TournamentArena(name)
        save()
        return true
    }

    fun delete(name: String): Boolean {
        val removed = arenas.remove(name.lowercase()) ?: return false
        save()
        return removed.name.isNotBlank()
    }

    fun setSpawn(name: String, spawn: String, location: Location): Boolean {
        val arena = arenas[name.lowercase()] ?: return false
        when (spawn.lowercase()) {
            "teama" -> arena.teamASpawn = location
            "teamb" -> arena.teamBSpawn = location
            "spectator" -> arena.spectatorSpawn = location
            else -> return false
        }
        save()
        return true
    }

    fun get(name: String): TournamentArena? = arenas[name.lowercase()]

    fun all(): List<TournamentArena> = arenas.values.sortedBy { it.name.lowercase() }

    fun ready(): List<TournamentArena> = all().filter { it.isReady() }

    private fun save() {
        configManager.saveArenas(arenas.values)
    }
}
