package com.marcogn.hallofmemories.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Result row of `HackDao.observeAll()` — a plain aggregate JOIN, not a one-to-many [Relation]. */
data class HackWithEntryCountRow(
    @Embedded val hack: HackEntity,
    val entryCount: Int,
)

/**
 * A [HallOfFameEntryEntity] with its slot rows. [Relation] does not guarantee ordering, so the
 * mapper into the domain model (`data/repository/Mappers.kt`) sorts by `slotIndex` explicitly.
 */
data class HallOfFameEntryWithSlotsRelation(
    @Embedded val entry: HallOfFameEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val slots: List<PokemonSlotEntity>,
)
