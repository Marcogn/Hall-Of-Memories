package com.marcogn.hallofmemories.ui.home

import androidx.lifecycle.ViewModel
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    val syncState: StateFlow<SyncState> = pokedexRepository.syncState

    init {
        // No-op if a sync already ran and every stage is fresh — safe to call on every Home
        // composition (e.g. after process death), not just a true first launch.
        pokedexRepository.startSyncIfNeeded()
    }

    fun retrySync() = pokedexRepository.startSyncIfNeeded()
}
