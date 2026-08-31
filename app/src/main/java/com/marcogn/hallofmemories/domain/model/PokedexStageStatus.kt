package com.marcogn.hallofmemories.domain.model

import com.marcogn.hallofmemories.domain.pokeapi.SyncStage
import java.time.Instant

/** Per-stage sync status shown in Settings — always one row per [SyncStage], even before a first sync. */
data class PokedexStageStatus(
    val stage: SyncStage,
    val lastSyncedAt: Instant?,
    val itemCount: Int,
)
