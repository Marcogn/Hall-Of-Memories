package com.marcogn.hallofmemories.domain.model

import java.time.Instant

data class Hack(
    val id: String,
    val name: String,
    val generation: GameGeneration,
    val baseGameTitle: String?,
    val boxArtPath: String?,
    val boxArtUrl: String?,
    val logoPath: String?,
    val logoUrl: String?,
    val theGamesDbId: Long?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** A [Hack] plus how many [HallOfFameEntry] rows it has, for the home library list (spec §3.1). */
data class HackWithEntryCount(
    val hack: Hack,
    val entryCount: Int,
)
