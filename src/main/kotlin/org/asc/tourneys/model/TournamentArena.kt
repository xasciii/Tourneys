package org.asc.tourneys.model

import org.bukkit.Location

data class TournamentArena(
    val name: String,
    var teamASpawn: Location? = null,
    var teamBSpawn: Location? = null,
    var spectatorSpawn: Location? = null
) {
    fun isReady(): Boolean = teamASpawn != null && teamBSpawn != null && spectatorSpawn != null
}
