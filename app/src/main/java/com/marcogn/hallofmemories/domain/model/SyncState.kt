package com.marcogn.hallofmemories.domain.model

import com.marcogn.hallofmemories.domain.pokeapi.SyncStage
import java.time.Instant

/** The pokédex sync's current state, observed by [com.marcogn.hallofmemories.data.pokeapi.PokedexSyncManager]. */
sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val stage: SyncStage, val done: Int, val total: Int) : SyncState
    data class Success(val at: Instant) : SyncState
    data class Failed(val message: String) : SyncState
}
