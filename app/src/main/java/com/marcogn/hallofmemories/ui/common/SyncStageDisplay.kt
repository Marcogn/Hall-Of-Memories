package com.marcogn.hallofmemories.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.pokeapi.SyncStage

/** Localized label for a [SyncStage], shown in Settings' pokédex-data section. */
@Composable
fun SyncStage.displayName(): String = stringResource(
    when (this) {
        SyncStage.SPECIES -> R.string.pokedex_stage_species
        SyncStage.MOVES -> R.string.pokedex_stage_moves
        SyncStage.TYPES -> R.string.pokedex_stage_types
        SyncStage.NATURES -> R.string.pokedex_stage_natures
        SyncStage.ABILITIES -> R.string.pokedex_stage_abilities
        SyncStage.ITEMS -> R.string.pokedex_stage_items
        SyncStage.GENERATIONS -> R.string.pokedex_stage_generations
    },
)
