package com.marcogn.hallofmemories.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.ui.common.EmptyState
import com.marcogn.hallofmemories.ui.common.HallOfMemoriesTopBar

/**
 * Hack library. No hacks exist yet in this phase (Room lands in Phase 1), so this always renders
 * the empty state; the FAB is wired up once hack creation exists (Phase 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onMenuClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { HallOfMemoriesTopBar(title = stringResource(R.string.home_title), onMenuClick = onMenuClick) },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Phase 2: navigate to HackForm */ }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
            }
        },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.home_empty_title),
            subtitle = stringResource(R.string.home_empty_subtitle),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
