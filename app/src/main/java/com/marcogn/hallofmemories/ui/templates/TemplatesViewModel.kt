package com.marcogn.hallofmemories.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.hallofmemories.data.settings.SpritePreferences
import com.marcogn.hallofmemories.domain.filter.filterTemplates
import com.marcogn.hallofmemories.domain.model.PokedexAbility
import com.marcogn.hallofmemories.domain.model.PokedexItem
import com.marcogn.hallofmemories.domain.model.PokedexMove
import com.marcogn.hallofmemories.domain.model.PokedexNature
import com.marcogn.hallofmemories.domain.model.PokedexSpecies
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import com.marcogn.hallofmemories.domain.repository.PokemonTemplateRepository
import com.marcogn.hallofmemories.ui.hof.SlotDraft
import com.marcogn.hallofmemories.ui.hof.toSlotDraft
import com.marcogn.hallofmemories.ui.hof.toTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: PokemonTemplateRepository,
    private val pokedexRepository: PokedexRepository,
    spritePreferences: SpritePreferences,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val natures: StateFlow<List<PokedexNature>> = pokedexRepository.observeNatures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val abilities: StateFlow<List<PokedexAbility>> = pokedexRepository.observeAbilities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        pokedexRepository.startSyncIfNeeded()
    }

    val uiState: StateFlow<TemplatesUiState> = combine(
        templateRepository.observeAll(),
        searchQuery,
        spritePreferences.alwaysUseLatestSprites,
    ) { templates, query, sprites ->
        TemplatesUiState(
            isLoading = false,
            allTemplatesEmpty = templates.isEmpty(),
            searchQuery = query,
            templates = filterTemplates(templates, query),
            alwaysUseLatestSprites = sprites,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TemplatesUiState(),
    )

    fun onSearchQueryChange(value: String) = searchQuery.update { value }

    fun retryPokedexSync() = pokedexRepository.startSyncIfNeeded()

    suspend fun searchSpecies(query: String): List<PokedexSpecies> = pokedexRepository.searchSpecies(query)
    suspend fun searchItems(query: String): List<PokedexItem> = pokedexRepository.searchItems(query)
    suspend fun searchMoves(query: String): List<PokedexMove> = pokedexRepository.searchMoves(query)

    /** [id]/[createdAt] null creates a new template; both non-null overwrites the existing one in place. */
    fun saveTemplate(id: String?, label: String, draft: SlotDraft, createdAt: Instant?) {
        viewModelScope.launch {
            val now = Instant.now()
            templateRepository.upsert(
                draft.toTemplate(
                    id = id ?: UUID.randomUUID().toString(),
                    label = label.trim(),
                    createdAt = createdAt ?: now,
                    updatedAt = now,
                ),
            )
        }
    }

    /** A new, independent row with a new id and a "(copy)"-suffixed label — never shares identity with the original. */
    fun duplicateTemplate(template: PokemonTemplate) {
        viewModelScope.launch {
            val now = Instant.now()
            val duplicate = template.toSlotDraft(slotIndex = 0).toTemplate(
                id = UUID.randomUUID().toString(),
                label = "${template.label} (copy)",
                createdAt = now,
                updatedAt = now,
            )
            templateRepository.upsert(duplicate)
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch { templateRepository.deleteById(id) }
    }
}
