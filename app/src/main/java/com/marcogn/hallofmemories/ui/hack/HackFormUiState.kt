package com.marcogn.hallofmemories.ui.hack

import com.marcogn.hallofmemories.domain.model.GameArtSearchResult
import com.marcogn.hallofmemories.domain.model.GameGeneration

data class HackFormDraft(
    val name: String = "",
    val generation: GameGeneration = GameGeneration.OTHER,
    val baseGameTitle: String = "",
    val notes: String = "",
    val boxArtPath: String? = null,
    val boxArtUrl: String? = null,
    val logoPath: String? = null,
    val logoUrl: String? = null,
    val theGamesDbId: Long? = null,
)

data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<GameArtSearchResult> = emptyList(),
    val message: String? = null,
)

data class HackFormUiState(
    val draft: HackFormDraft = HackFormDraft(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDownloadingArt: Boolean = false,
    val errorMessage: String? = null,
    val search: SearchState = SearchState(),
)
