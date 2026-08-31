package com.marcogn.hallofmemories.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.GameGeneration

/** Localized label for a [GameGeneration] — kept off the enum so `domain/` stays Android-free. */
@Composable
fun GameGeneration.displayName(): String = stringResource(
    when (this) {
        GameGeneration.RB -> R.string.generation_rb
        GameGeneration.GSC -> R.string.generation_gsc
        GameGeneration.RSE -> R.string.generation_rse
        GameGeneration.FRLG -> R.string.generation_frlg
        GameGeneration.DPPT -> R.string.generation_dppt
        GameGeneration.HGSS -> R.string.generation_hgss
        GameGeneration.BW -> R.string.generation_bw
        GameGeneration.XY -> R.string.generation_xy
        GameGeneration.ORAS -> R.string.generation_oras
        GameGeneration.SM -> R.string.generation_sm
        GameGeneration.USUM -> R.string.generation_usum
        GameGeneration.SWSH -> R.string.generation_swsh
        GameGeneration.SV -> R.string.generation_sv
        GameGeneration.OTHER -> R.string.generation_other
    },
)
