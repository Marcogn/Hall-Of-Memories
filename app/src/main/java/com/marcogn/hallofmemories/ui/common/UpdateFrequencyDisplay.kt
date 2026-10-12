package com.marcogn.hallofmemories.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.update.UpdateFrequency

/** Localized label for an [UpdateFrequency], shown in Settings' Updates section. */
@Composable
fun UpdateFrequency.displayName(): String = stringResource(
    when (this) {
        UpdateFrequency.NEVER -> R.string.update_frequency_never
        UpdateFrequency.EVERY_START -> R.string.update_frequency_every_start
        UpdateFrequency.DAILY -> R.string.update_frequency_daily
        UpdateFrequency.WEEKLY -> R.string.update_frequency_weekly
    },
)
