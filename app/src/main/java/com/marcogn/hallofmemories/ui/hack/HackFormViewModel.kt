package com.marcogn.hallofmemories.ui.hack

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.data.image.ImageFormat
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.data.thegamesdb.GameArtSearchCoordinator
import com.marcogn.hallofmemories.domain.model.GameArtSearchResult
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HackFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val hackRepository: HackRepository,
    private val imageStorage: ImageStorage,
    private val searchCoordinator: GameArtSearchCoordinator,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Destination.HackForm>()
    private val editingId: String? = route.hackId

    private val draft = MutableStateFlow(HackFormDraft())
    private val isLoading = MutableStateFlow(editingId != null)
    private val isSaving = MutableStateFlow(false)
    private val isDownloadingArt = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val search = MutableStateFlow(SearchState())

    private var existingCreatedAt: Instant? = null
    private var originalBoxArtPath: String? = null
    private var originalLogoPath: String? = null

    init {
        val id = editingId
        if (id != null) {
            viewModelScope.launch {
                val existing = hackRepository.observeById(id).first()
                if (existing != null) {
                    existingCreatedAt = existing.createdAt
                    originalBoxArtPath = existing.boxArtPath
                    originalLogoPath = existing.logoPath
                    draft.value = HackFormDraft(
                        name = existing.name,
                        generation = existing.generation,
                        baseGameTitle = existing.baseGameTitle.orEmpty(),
                        notes = existing.notes.orEmpty(),
                        boxArtPath = existing.boxArtPath,
                        boxArtUrl = existing.boxArtUrl,
                        logoPath = existing.logoPath,
                        logoUrl = existing.logoUrl,
                        theGamesDbId = existing.theGamesDbId,
                    )
                } else {
                    errorMessage.value = appContext.getString(R.string.hack_not_found)
                }
                isLoading.value = false
            }
        }
    }

    private val formCore = combine(draft, isLoading, isSaving, isDownloadingArt, errorMessage) { d, loading, saving, downloadingArt, error ->
        HackFormUiState(
            draft = d,
            isEditMode = editingId != null,
            isLoading = loading,
            isSaving = saving,
            isDownloadingArt = downloadingArt,
            errorMessage = error,
        )
    }

    val uiState: StateFlow<HackFormUiState> = combine(formCore, search) { core, search -> core.copy(search = search) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HackFormUiState(isLoading = editingId != null, isEditMode = editingId != null),
        )

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }
    fun onGenerationChange(value: GameGeneration) = updateDraft { it.copy(generation = value) }
    fun onBaseGameTitleChange(value: String) = updateDraft { it.copy(baseGameTitle = value) }
    fun onNotesChange(value: String) = updateDraft { it.copy(notes = value) }

    // Deliberately not deleted from disk here: the change isn't committed until save(), and the
    // user can still cancel back to the hack's current artwork. save() reconciles once the new
    // state is actually persisted — see its comment.
    fun onBoxArtPicked(uri: Uri) {
        viewModelScope.launch {
            val newPath = imageStorage.persist(uri)
            updateDraft { it.copy(boxArtPath = newPath, boxArtUrl = null) }
        }
    }

    fun onBoxArtRemoved() = updateDraft { it.copy(boxArtPath = null, boxArtUrl = null) }

    fun onLogoPicked(uri: Uri) {
        viewModelScope.launch {
            val newPath = imageStorage.persist(uri, ImageFormat.PNG)
            updateDraft { it.copy(logoPath = newPath, logoUrl = null) }
        }
    }

    fun onLogoRemoved() = updateDraft { it.copy(logoPath = null, logoUrl = null) }

    fun onSearchOnlineOpened() {
        search.update {
            it.copy(query = draft.value.baseGameTitle.ifBlank { draft.value.name }, results = emptyList(), message = null)
        }
    }

    fun onSearchQueryChange(value: String) {
        search.update { it.copy(query = value) }
    }

    fun onSearchOnline() {
        val query = search.value.query
        if (query.isBlank()) return
        viewModelScope.launch {
            search.update { it.copy(isSearching = true, message = null) }
            val platformHint = draft.value.generation.theGamesDbPlatformHint()
            when (val outcome = searchCoordinator.search(query, platformHint)) {
                is GameArtSearchCoordinator.Outcome.Results ->
                    search.update { it.copy(isSearching = false, results = outcome.results, message = null) }
                is GameArtSearchCoordinator.Outcome.Message ->
                    search.update { it.copy(isSearching = false, results = emptyList(), message = outcome.text) }
            }
        }
    }

    fun onSearchResultSelected(result: GameArtSearchResult) {
        viewModelScope.launch {
            search.value = SearchState()
            isDownloadingArt.value = true
            val downloaded = searchCoordinator.downloadArt(result)
            updateDraft {
                it.copy(
                    baseGameTitle = result.title,
                    theGamesDbId = result.externalId,
                    boxArtPath = downloaded.boxArtPath ?: it.boxArtPath,
                    boxArtUrl = downloaded.boxArtUrl ?: it.boxArtUrl,
                    logoPath = downloaded.logoPath ?: it.logoPath,
                    logoUrl = downloaded.logoUrl ?: it.logoUrl,
                )
            }
            isDownloadingArt.value = false
        }
    }

    fun onSearchDialogDismissed() {
        search.update { it.copy(results = emptyList(), message = null) }
    }

    fun save(onSaved: (String) -> Unit) {
        val current = draft.value
        if (current.name.isBlank()) {
            errorMessage.value = appContext.getString(R.string.hack_form_validation_name_required)
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            val id = editingId ?: UUID.randomUUID().toString()
            val now = Instant.now()
            hackRepository.upsert(
                Hack(
                    id = id,
                    name = current.name.trim(),
                    generation = current.generation,
                    baseGameTitle = current.baseGameTitle.trim().takeIf { it.isNotBlank() },
                    boxArtPath = current.boxArtPath,
                    boxArtUrl = current.boxArtUrl,
                    logoPath = current.logoPath,
                    logoUrl = current.logoUrl,
                    theGamesDbId = current.theGamesDbId,
                    notes = current.notes.trim().takeIf { it.isNotBlank() },
                    createdAt = existingCreatedAt ?: now,
                    updatedAt = now,
                ),
            )
            // Only now that the new paths are actually committed do we delete whatever they
            // replaced — deleting at pick-time instead would leave the still-saved hack pointing
            // at a missing file if the user cancelled the edit instead of saving it.
            originalBoxArtPath?.takeIf { it != current.boxArtPath }?.let { imageStorage.delete(it) }
            originalLogoPath?.takeIf { it != current.logoPath }?.let { imageStorage.delete(it) }
            isSaving.value = false
            onSaved(id)
        }
    }

    private fun updateDraft(transform: (HackFormDraft) -> HackFormDraft) {
        draft.update(transform)
        errorMessage.value = null
    }
}

/** A rough generation -> TheGamesDB platform name mapping to narrow "Search online" results. */
private fun GameGeneration.theGamesDbPlatformHint(): String? = when (this) {
    GameGeneration.RB -> "Game Boy"
    GameGeneration.GSC -> "Game Boy Color"
    GameGeneration.RSE, GameGeneration.FRLG -> "Game Boy Advance"
    GameGeneration.DPPT, GameGeneration.HGSS, GameGeneration.BW -> "Nintendo DS"
    GameGeneration.XY, GameGeneration.ORAS, GameGeneration.SM, GameGeneration.USUM -> "Nintendo 3DS"
    GameGeneration.SWSH, GameGeneration.SV -> "Nintendo Switch"
    GameGeneration.OTHER -> null
}
