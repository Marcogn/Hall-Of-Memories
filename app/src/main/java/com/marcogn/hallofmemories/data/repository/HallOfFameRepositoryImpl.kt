package com.marcogn.hallofmemories.data.repository

import com.marcogn.hallofmemories.data.local.dao.HallOfFameDao
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import com.marcogn.hallofmemories.domain.repository.HallOfFameRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HallOfFameRepositoryImpl @Inject constructor(
    private val hallOfFameDao: HallOfFameDao,
) : HallOfFameRepository {

    override fun observeByHack(hackId: String): Flow<List<HallOfFameEntryWithSlots>> =
        hallOfFameDao.observeByHack(hackId).map { rows -> rows.map { it.toDomain() } }

    override fun observeById(entryId: String): Flow<HallOfFameEntryWithSlots?> =
        hallOfFameDao.observeById(entryId).map { it?.toDomain() }

    override fun countByHack(hackId: String): Flow<Int> = hallOfFameDao.countByHack(hackId)

    override suspend fun save(entry: HallOfFameEntry, slots: List<PokemonSlot>) {
        hallOfFameDao.saveEntryWithSlots(entry.toEntity(), slots.map { it.toEntity() })
    }

    override suspend fun deleteEntry(entryId: String) {
        hallOfFameDao.deleteEntry(entryId)
    }
}
