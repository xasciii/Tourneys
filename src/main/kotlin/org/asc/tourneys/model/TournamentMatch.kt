package org.asc.tourneys.model

import java.util.UUID

data class TournamentMatch(
    val id: Int,
    val round: Int,
    val teamA: TournamentTeam?,
    val teamB: TournamentTeam?,
    var arenaName: String? = null,
    var status: MatchStatus = MatchStatus.WAITING,
    val aliveTeamA: MutableSet<UUID> = mutableSetOf(),
    val aliveTeamB: MutableSet<UUID> = mutableSetOf(),
    var winner: TournamentTeam? = null
) {
    fun contains(playerId: UUID): Boolean {
        return teamA?.members?.contains(playerId) == true || teamB?.members?.contains(playerId) == true
    }
}
