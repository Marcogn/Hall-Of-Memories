package com.marcogn.hallofmemories.ui.settings

import com.marcogn.hallofmemories.domain.model.PokedexStageStatus
import com.marcogn.hallofmemories.domain.model.SyncState

data class SettingsUiState(
    val alwaysUseLatestSprites: Boolean = false,
    val stageStatuses: List<PokedexStageStatus> = emptyList(),
    val syncState: SyncState = SyncState.Idle,
    val isBackupBusy: Boolean = false,
    val backupMessage: String? = null,
)
