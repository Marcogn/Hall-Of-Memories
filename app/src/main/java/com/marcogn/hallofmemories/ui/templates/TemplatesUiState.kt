package com.marcogn.hallofmemories.ui.templates

import com.marcogn.hallofmemories.domain.model.PokemonTemplate

data class TemplatesUiState(
    val isLoading: Boolean = true,
    val allTemplatesEmpty: Boolean = false,
    val searchQuery: String = "",
    val templates: List<PokemonTemplate> = emptyList(),
    val alwaysUseLatestSprites: Boolean = false,
)
