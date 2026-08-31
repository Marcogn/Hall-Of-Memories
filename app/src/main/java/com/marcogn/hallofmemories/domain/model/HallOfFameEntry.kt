package com.marcogn.hallofmemories.domain.model

import java.time.Instant

data class HallOfFameEntry(
    val id: String,
    val hackId: String,
    val playerName: String,
    val playerId: String,
    val playtimeText: String,
    val playtimeMinutes: Int?,
    val screenshotPath: String?,
    val insertedAt: Instant,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** A [HallOfFameEntry] with its (always exactly six) [PokemonSlot] rows, ordered by slot index. */
data class HallOfFameEntryWithSlots(
    val entry: HallOfFameEntry,
    val slots: List<PokemonSlot>,
)
