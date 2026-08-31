package com.marcogn.hallofmemories.domain.repository

import com.marcogn.hallofmemories.domain.model.PokedexAbility
import com.marcogn.hallofmemories.domain.model.PokedexItem
import com.marcogn.hallofmemories.domain.model.PokedexMove
import com.marcogn.hallofmemories.domain.model.PokedexNature
import com.marcogn.hallofmemories.domain.model.PokedexSpecies
import com.marcogn.hallofmemories.domain.model.PokedexStageStatus
import com.marcogn.hallofmemories.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PokedexRepository {

    val syncState: StateFlow<SyncState>

    /** Always one row per [com.marcogn.hallofmemories.domain.pokeapi.SyncStage], in stage order. */
    fun observeStageStatuses(): Flow<List<PokedexStageStatus>>

    /** No-op if every stage is already fresh, otherwise runs the missing/stale ones. */
    fun startSyncIfNeeded()

    /** Clears every pokédex cache table and meta row, then runs every stage. User data is untouched. */
    fun forceResync()

    suspend fun searchSpecies(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): List<PokedexSpecies>
    suspend fun getSpeciesById(id: Int): PokedexSpecies?
    suspend fun searchMoves(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): List<PokedexMove>
    fun observeNatures(): Flow<List<PokedexNature>>
    fun observeAbilities(): Flow<List<PokedexAbility>>
    suspend fun searchItems(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): List<PokedexItem>

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 30
    }
}
