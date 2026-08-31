package com.marcogn.hallofmemories.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.hallofmemories.data.settings.SpritePreferences
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val spritePreferences: SpritePreferences,
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        spritePreferences.alwaysUseLatestSprites,
        pokedexRepository.observeStageStatuses(),
        pokedexRepository.syncState,
    ) { alwaysUseLatest, stageStatuses, syncState ->
        SettingsUiState(
            alwaysUseLatestSprites = alwaysUseLatest,
            stageStatuses = stageStatuses,
            syncState = syncState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setAlwaysUseLatestSprites(value: Boolean) {
        viewModelScope.launch { spritePreferences.setAlwaysUseLatestSprites(value) }
    }

    fun startSyncIfNeeded() = pokedexRepository.startSyncIfNeeded()

    fun forceResync() = pokedexRepository.forceResync()
}
