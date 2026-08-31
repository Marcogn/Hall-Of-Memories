package com.marcogn.hallofmemories.ui.hof

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots

data class HofDetailUiState(
    val entryWithSlots: HallOfFameEntryWithSlots? = null,
    val hackName: String = "",
    val hackGeneration: GameGeneration = GameGeneration.OTHER,
    val alwaysUseLatestSprites: Boolean = false,
    val isLoading: Boolean = true,
)
