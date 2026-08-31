package com.marcogn.hallofmemories.data.repository

import com.marcogn.hallofmemories.data.local.dao.PokedexDao
import com.marcogn.hallofmemories.data.pokeapi.PokedexSyncManager
import com.marcogn.hallofmemories.domain.model.PokedexAbility
import com.marcogn.hallofmemories.domain.model.PokedexItem
import com.marcogn.hallofmemories.domain.model.PokedexMove
import com.marcogn.hallofmemories.domain.model.PokedexNature
import com.marcogn.hallofmemories.domain.model.PokedexSpecies
import com.marcogn.hallofmemories.domain.model.PokedexStageStatus
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.domain.pokeapi.SyncStage
import com.marcogn.hallofmemories.domain.pokeapi.mergeSearchResults
import com.marcogn.hallofmemories.domain.pokeapi.searchKey
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@Singleton
class PokedexRepositoryImpl @Inject constructor(
    private val pokedexDao: PokedexDao,
    private val syncManager: PokedexSyncManager,
) : PokedexRepository {

    override val syncState: StateFlow<SyncState> = syncManager.state

    override fun observeStageStatuses(): Flow<List<PokedexStageStatus>> =
        pokedexDao.observeAllMeta().map { metaRows ->
            val byStageName = metaRows.associateBy { it.key }
            SyncStage.entries.map { stage ->
                val meta = byStageName[stage.name]
                PokedexStageStatus(stage = stage, lastSyncedAt = meta?.lastSyncedAt, itemCount = meta?.itemCount ?: 0)
            }
        }

    override fun startSyncIfNeeded() = syncManager.startIfNeeded()

    override fun forceResync() = syncManager.forceResync()

    override suspend fun searchSpecies(query: String, limit: Int): List<PokedexSpecies> {
        val key = searchKey(query)
        if (key.isBlank()) return emptyList()
        val prefixMatches = pokedexDao.searchSpeciesByPrefix(key, limit)
        val merged = if (prefixMatches.size >= limit) {
            prefixMatches
        } else {
            mergeSearchResults(prefixMatches, pokedexDao.searchSpeciesByContains(key, limit), limit) { it.id }
        }
        return merged.map { it.toDomain() }
    }

    override suspend fun getSpeciesById(id: Int): PokedexSpecies? = pokedexDao.getSpeciesById(id)?.toDomain()

    override suspend fun searchMoves(query: String, limit: Int): List<PokedexMove> {
        val key = searchKey(query)
        if (key.isBlank()) return emptyList()
        val prefixMatches = pokedexDao.searchMovesByPrefix(key, limit)
        val merged = if (prefixMatches.size >= limit) {
            prefixMatches
        } else {
            mergeSearchResults(prefixMatches, pokedexDao.searchMovesByContains(key, limit), limit) { it.id }
        }
        return merged.map { it.toDomain() }
    }

    override fun observeNatures(): Flow<List<PokedexNature>> =
        pokedexDao.observeNatures().map { rows -> rows.map { it.toDomain() } }

    override fun observeAbilities(): Flow<List<PokedexAbility>> =
        pokedexDao.observeAbilities().map { rows -> rows.map { it.toDomain() } }

    override suspend fun searchItems(query: String, limit: Int): List<PokedexItem> {
        val key = searchKey(query)
        if (key.isBlank()) return emptyList()
        val prefixMatches = pokedexDao.searchItemsByPrefix(key, limit)
        val merged = if (prefixMatches.size >= limit) {
            prefixMatches
        } else {
            mergeSearchResults(prefixMatches, pokedexDao.searchItemsByContains(key, limit), limit) { it.id }
        }
        return merged.map { it.toDomain() }
    }
}
