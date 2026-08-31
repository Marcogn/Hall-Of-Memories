package com.marcogn.hallofmemories.ui.hof

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.data.settings.SpritePreferences
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.PokedexAbility
import com.marcogn.hallofmemories.domain.model.PokedexItem
import com.marcogn.hallofmemories.domain.model.PokedexMove
import com.marcogn.hallofmemories.domain.model.PokedexNature
import com.marcogn.hallofmemories.domain.model.PokedexSpecies
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import com.marcogn.hallofmemories.domain.model.parsePlaytimeMinutes
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.domain.repository.HallOfFameRepository
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import com.marcogn.hallofmemories.domain.repository.PokemonTemplateRepository
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

/** Intermediate combine result — [combine] tops out at four typed flows, so [uiState] combines this with the pokédex flows in a second step. */
private data class FormCore(
    val draft: HofFormDraft,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val errorMessage: String?,
    val hackGeneration: GameGeneration,
)

private data class PokedexBundle(
    val natures: List<PokedexNature>,
    val abilities: List<PokedexAbility>,
    val alwaysUseLatestSprites: Boolean,
    val templates: List<PokemonTemplate>,
)

@HiltViewModel
class HofFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val hallOfFameRepository: HallOfFameRepository,
    private val hackRepository: HackRepository,
    private val pokedexRepository: PokedexRepository,
    private val templateRepository: PokemonTemplateRepository,
    private val spritePreferences: SpritePreferences,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Destination.HofForm>()
    private val hackId = route.hackId
    private val editingId: String? = route.entryId

    private val draft = MutableStateFlow(HofFormDraft())
    private val isLoading = MutableStateFlow(true)
    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val hackGeneration = MutableStateFlow(GameGeneration.OTHER)

    private var existingSlotIds: Map<Int, String> = emptyMap()
    private var existingCreatedAt: Instant? = null
    private var originalScreenshotPath: String? = null
    private var initialDraft: HofFormDraft = HofFormDraft()

    private val formCore = combine(draft, isLoading, isSaving, errorMessage, hackGeneration) { d, loading, saving, error, generation ->
        FormCore(d, loading, saving, error, generation)
    }

    private val pokedexBundle = combine(
        pokedexRepository.observeNatures(),
        pokedexRepository.observeAbilities(),
        spritePreferences.alwaysUseLatestSprites,
        templateRepository.observeAll(),
    ) { natures, abilities, sprites, templates -> PokedexBundle(natures, abilities, sprites, templates) }

    val uiState: StateFlow<HofFormUiState> = combine(formCore, pokedexBundle) { core, pokedex ->
        HofFormUiState(
            draft = core.draft,
            hackGeneration = core.hackGeneration,
            alwaysUseLatestSprites = pokedex.alwaysUseLatestSprites,
            natures = pokedex.natures,
            abilities = pokedex.abilities,
            templates = pokedex.templates,
            isEditMode = editingId != null,
            isLoading = core.isLoading,
            isSaving = core.isSaving,
            errorMessage = core.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HofFormUiState(isLoading = true, isEditMode = editingId != null),
    )

    init {
        pokedexRepository.startSyncIfNeeded()
        viewModelScope.launch {
            hackGeneration.value = hackRepository.observeById(hackId).first()?.generation ?: GameGeneration.OTHER

            val id = editingId
            val loadedDraft = if (id != null) {
                val existing = hallOfFameRepository.observeById(id).first()
                if (existing != null) {
                    existingCreatedAt = existing.entry.createdAt
                    originalScreenshotPath = existing.entry.screenshotPath
                    existingSlotIds = existing.slots.associate { it.slotIndex to it.id }
                    HofFormDraft(
                        playerName = existing.entry.playerName,
                        playerId = existing.entry.playerId,
                        playtimeText = existing.entry.playtimeText,
                        screenshotPath = existing.entry.screenshotPath,
                        insertedAt = existing.entry.insertedAt,
                        notes = existing.entry.notes.orEmpty(),
                        slots = (0..5).map { index ->
                            existing.slots.firstOrNull { it.slotIndex == index }?.toDraft() ?: SlotDraft.empty(index)
                        },
                    )
                } else {
                    errorMessage.value = appContext.getString(R.string.hof_entry_not_found)
                    HofFormDraft()
                }
            } else {
                HofFormDraft()
            }

            draft.value = loadedDraft
            initialDraft = loadedDraft
            isLoading.value = false
        }
    }

    fun hasUnsavedChanges(): Boolean = draft.value != initialDraft

    fun onPlayerNameChange(value: String) = updateDraft { it.copy(playerName = value) }
    fun onPlayerIdChange(value: String) = updateDraft { it.copy(playerId = value) }
    fun onPlaytimeTextChange(value: String) = updateDraft { it.copy(playtimeText = value) }
    fun onInsertedAtChange(value: Instant) = updateDraft { it.copy(insertedAt = value) }
    fun onNotesChange(value: String) = updateDraft { it.copy(notes = value) }

    // Not deleted from disk here — same deferred-deletion pattern as HackFormViewModel: the
    // change isn't committed until save(), so a discarded edit must not blow away the file the
    // still-saved entry points to.
    fun onScreenshotPicked(uri: Uri) {
        viewModelScope.launch {
            val newPath = imageStorage.persist(uri)
            updateDraft { it.copy(screenshotPath = newPath) }
        }
    }

    fun onScreenshotRemoved() = updateDraft { it.copy(screenshotPath = null) }

    fun onSlotConfirmed(slotIndex: Int, updated: SlotDraft) = updateDraft { current ->
        current.copy(slots = current.slots.map { if (it.slotIndex == slotIndex) updated else it })
    }

    fun retryPokedexSync() = pokedexRepository.startSyncIfNeeded()

    suspend fun searchSpecies(query: String): List<PokedexSpecies> = pokedexRepository.searchSpecies(query)
    suspend fun searchItems(query: String): List<PokedexItem> = pokedexRepository.searchItems(query)
    suspend fun searchMoves(query: String): List<PokedexMove> = pokedexRepository.searchMoves(query)

    /** [overwriteId] non-null overwrites that existing template's row (keeping its `createdAt`); null always creates a new one (spec's "overwrite" vs "save as a copy"). */
    fun saveAsTemplate(draft: SlotDraft, label: String, overwriteId: String?) {
        viewModelScope.launch {
            val now = Instant.now()
            val existingCreatedAt = overwriteId?.let { templateRepository.getById(it)?.createdAt }
            templateRepository.upsert(
                draft.toTemplate(
                    id = overwriteId ?: UUID.randomUUID().toString(),
                    label = label,
                    createdAt = existingCreatedAt ?: now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun save(onSaved: () -> Unit) {
        val current = draft.value
        if (current.playerName.isBlank()) {
            errorMessage.value = appContext.getString(R.string.hof_form_validation_player_name_required)
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            val id = editingId ?: UUID.randomUUID().toString()
            val now = Instant.now()
            val entry = HallOfFameEntry(
                id = id,
                hackId = hackId,
                playerName = current.playerName.trim(),
                playerId = current.playerId.trim(),
                playtimeText = current.playtimeText.trim(),
                playtimeMinutes = parsePlaytimeMinutes(current.playtimeText),
                screenshotPath = current.screenshotPath,
                insertedAt = current.insertedAt,
                notes = current.notes.trim().takeIf { it.isNotBlank() },
                createdAt = existingCreatedAt ?: now,
                updatedAt = now,
            )
            val slots = current.slots.map { slotDraft ->
                val slotId = existingSlotIds[slotDraft.slotIndex] ?: UUID.randomUUID().toString()
                slotDraft.toDomain(id = slotId, entryId = id)
            }
            hallOfFameRepository.save(entry, slots)
            originalScreenshotPath?.takeIf { it != current.screenshotPath }?.let { imageStorage.delete(it) }
            isSaving.value = false
            initialDraft = current
            onSaved()
        }
    }

    private fun updateDraft(transform: (HofFormDraft) -> HofFormDraft) {
        draft.update(transform)
        errorMessage.value = null
    }
}
