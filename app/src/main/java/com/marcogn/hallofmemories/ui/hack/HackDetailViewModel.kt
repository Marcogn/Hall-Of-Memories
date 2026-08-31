package com.marcogn.hallofmemories.ui.hack

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.domain.repository.HallOfFameRepository
import com.marcogn.hallofmemories.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HackDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hackRepository: HackRepository,
    hallOfFameRepository: HallOfFameRepository,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val hackId: String = savedStateHandle.toRoute<Destination.HackDetail>().hackId

    val uiState: StateFlow<HackDetailUiState> = combine(
        hackRepository.observeById(hackId),
        hallOfFameRepository.observeByHack(hackId),
    ) { hack, entries ->
        HackDetailUiState(hack = hack, entries = entries, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HackDetailUiState(),
    )

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
