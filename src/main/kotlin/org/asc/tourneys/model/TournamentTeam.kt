package org.asc.tourneys.model

import java.util.UUID

data class TournamentTeam(
    val id: String,
    val name: String,
    val color: String,
    val number: Int?,
    val captain: UUID,
    val members: MutableSet<UUID> = mutableSetOf(captain),
    val invited: MutableSet<UUID> = mutableSetOf(),
    var eliminated: Boolean = false
)
