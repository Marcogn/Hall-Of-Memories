package com.marcogn.hallofmemories.ui.hack

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.data.settings.SpritePreferences
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.domain.repository.HallOfFameRepository
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import com.marcogn.hallofmemories.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HackDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hackRepository: HackRepository,
    hallOfFameRepository: HallOfFameRepository,
    private val pokedexRepository: PokedexRepository,
    spritePreferences: SpritePreferences,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val hackId: String = savedStateHandle.toRoute<Destination.HackDetail>().hackId
    private val sortNewestFirst = MutableStateFlow(true)

    val uiState: StateFlow<HackDetailUiState> = combine(
        hackRepository.observeById(hackId),
        hallOfFameRepository.observeByHack(hackId),
        spritePreferences.alwaysUseLatestSprites,
        sortNewestFirst,
    ) { hack, entries, sprites, newestFirst ->
        HackDetailUiState(hack = hack, entries = entries, alwaysUseLatestSprites = sprites, sortNewestFirst = newestFirst, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HackDetailUiState(),
    )

    fun onToggleSortOrder() = sortNewestFirst.update { !it }

    suspend fun lookupMoveType(name: String): String? = pokedexRepository.getMoveType(name)

    /** Deletes the hack (cascades to its entries/slots) and its own artwork files, then invokes [onDeleted]. */
    fun deleteHack(onDeleted: () -> Unit) {
        val hack = uiState.value.hack ?: return
        viewModelScope.launch {
            hackRepository.deleteById(hackId)
            imageStorage.delete(hack.boxArtPath)
            imageStorage.delete(hack.logoPath)
            onDeleted()
        }
    }
}
