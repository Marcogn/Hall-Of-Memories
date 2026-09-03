package com.marcogn.hallofmemories.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.hallofmemories.data.settings.ViewModePreferences
import com.marcogn.hallofmemories.domain.filter.filterHacks
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.domain.model.ViewMode
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
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
class HomeViewModel @Inject constructor(
    private val hackRepository: HackRepository,
    private val pokedexRepository: PokedexRepository,
    private val viewModePreferences: ViewModePreferences,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedGenerations = MutableStateFlow<Set<GameGeneration>>(emptySet())
    private val viewMode = MutableStateFlow(viewModePreferences.homeViewMode)
    private val selectedHackIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HomeUiState> = combine(
        hackRepository.observeAll(),
        searchQuery,
        selectedGenerations,
        viewMode,
        pokedexRepository.syncState,
    ) { hacks, query, generations, viewMode, syncState ->
        HomeFilterState(hacks, query, generations, viewMode, syncState)
    }.combine(selectedHackIds) { filterState, selectedIds ->
        HomeUiState(
            isLoading = false,
            hacks = filterHacks(filterState.hacks, filterState.query, filterState.generations),
            allHacksEmpty = filterState.hacks.isEmpty(),
            searchQuery = filterState.query,
            selectedGenerations = filterState.generations,
            viewMode = filterState.viewMode,
            syncState = filterState.syncState,
            selectedHackIds = selectedIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(viewMode = viewModePreferences.homeViewMode),
    )

    private data class HomeFilterState(
        val hacks: List<HackWithEntryCount>,
        val query: String,
        val generations: Set<GameGeneration>,
        val viewMode: ViewMode,
        val syncState: SyncState,
    )

    init {
        // No-op if a sync already ran and every stage is fresh — safe to call on every Home
        // composition (e.g. after process death), not just a true first launch.
        pokedexRepository.startSyncIfNeeded()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onGenerationFilterToggle(generation: GameGeneration) {
        selectedGenerations.update { current -> if (generation in current) current - generation else current + generation }
    }

    fun onClearGenerationFilters() {
        selectedGenerations.value = emptySet()
    }

    fun onViewModeChange(mode: ViewMode) {
        viewMode.value = mode
        viewModePreferences.homeViewMode = mode
    }

    fun retrySync() = pokedexRepository.startSyncIfNeeded()

    /** Long-press starts selection mode; while active, tapping any other hack toggles it too. */
    fun onToggleSelection(hackId: String) {
        selectedHackIds.update { current -> if (hackId in current) current - hackId else current + hackId }
    }

    fun onSelectAll() {
        selectedHackIds.value = uiState.value.hacks.map { it.hack.id }.toSet()
    }

    fun onClearSelection() {
        selectedHackIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = selectedHackIds.value
        selectedHackIds.value = emptySet()
        viewModelScope.launch {
            ids.forEach { hackRepository.deleteById(it) }
        }
    }
}
