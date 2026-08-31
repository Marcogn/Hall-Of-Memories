package com.marcogn.hallofmemories.domain.repository

import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import kotlinx.coroutines.flow.Flow

interface HallOfFameRepository {
    fun observeByHack(hackId: String): Flow<List<HallOfFameEntryWithSlots>>
    fun observeById(entryId: String): Flow<HallOfFameEntryWithSlots?>
    fun countByHack(hackId: String): Flow<Int>

    /** Upserts [entry] and replaces its slots in one transaction — see `HallOfFameDao.saveEntryWithSlots`. */
    suspend fun save(entry: HallOfFameEntry, slots: List<PokemonSlot>)
    suspend fun deleteEntry(entryId: String)
}
