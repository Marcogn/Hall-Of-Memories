package com.marcogn.hallofmemories.ui.home

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.domain.model.ViewMode

data class HomeUiState(
    val isLoading: Boolean = true,
    val hacks: List<HackWithEntryCount> = emptyList(),
    val allHacksEmpty: Boolean = false,
    val searchQuery: String = "",
    val selectedGenerations: Set<GameGeneration> = emptySet(),
    val viewMode: ViewMode = ViewMode.LIST,
    val syncState: SyncState = SyncState.Idle,
)
