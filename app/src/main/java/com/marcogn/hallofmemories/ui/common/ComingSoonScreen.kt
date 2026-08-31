package com.marcogn.hallofmemories.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R

/**
 * Placeholder body for a navigation destination whose real screen ships in a later phase.
 * Exists purely so every route in [com.marcogn.hallofmemories.ui.navigation.Destinations]
 * compiles and is reachable from Phase 0 onward.
 */
@Composable
fun ComingSoonScreen(phase: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.coming_soon_phase, phase),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
