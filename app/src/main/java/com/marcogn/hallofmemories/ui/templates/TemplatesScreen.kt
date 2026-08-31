package com.marcogn.hallofmemories.ui.templates

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.ui.common.EmptyState
import com.marcogn.hallofmemories.ui.common.HallOfMemoriesTopBar

/** Reusable Pokémon templates (Phase 4). Always empty until then. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(onMenuClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { HallOfMemoriesTopBar(title = stringResource(R.string.templates_title), onMenuClick = onMenuClick) },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.templates_empty_title),
            subtitle = stringResource(R.string.templates_empty_subtitle),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
