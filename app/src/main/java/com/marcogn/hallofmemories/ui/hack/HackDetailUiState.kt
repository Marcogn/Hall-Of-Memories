package com.marcogn.hallofmemories.ui.hack

import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots

data class HackDetailUiState(
    val hack: Hack? = null,
    val entries: List<HallOfFameEntryWithSlots> = emptyList(),
    val alwaysUseLatestSprites: Boolean = false,
    val sortNewestFirst: Boolean = true,
    val isLoading: Boolean = true,
) {
    val sortedEntries: List<HallOfFameEntryWithSlots>
        get() = if (sortNewestFirst) entries else entries.reversed()
}
