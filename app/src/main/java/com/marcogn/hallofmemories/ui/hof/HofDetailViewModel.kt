package com.marcogn.hallofmemories.ui.hof

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.data.settings.SpritePreferences
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.domain.repository.HallOfFameRepository
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import com.marcogn.hallofmemories.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HofDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hallOfFameRepository: HallOfFameRepository,
    hackRepository: HackRepository,
    private val pokedexRepository: PokedexRepository,
    spritePreferences: SpritePreferences,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val entryId = savedStateHandle.toRoute<Destination.HofDetail>().entryId

    private val entryFlow = hallOfFameRepository.observeById(entryId)
    private val hackFlow = entryFlow.flatMapLatest { entry ->
        if (entry == null) flowOf(null) else hackRepository.observeById(entry.entry.hackId)
    }

    val uiState: StateFlow<HofDetailUiState> = combine(
        entryFlow, hackFlow, spritePreferences.alwaysUseLatestSprites,
    ) { entry, hack, sprites ->
        HofDetailUiState(
            entryWithSlots = entry,
            hackName = hack?.name.orEmpty(),
            hackGeneration = hack?.generation ?: GameGeneration.OTHER,
            alwaysUseLatestSprites = sprites,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HofDetailUiState(),
    )

    suspend fun lookupMoveType(name: String): String? = pokedexRepository.getMoveType(name)

    fun deleteEntry(onDeleted: () -> Unit) {
        val current = uiState.value.entryWithSlots ?: return
        viewModelScope.launch {
            hallOfFameRepository.deleteEntry(current.entry.id)
            imageStorage.delete(current.entry.screenshotPath)
            onDeleted()
        }
    }
}
