package com.marcogn.hallofmemories.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.domain.model.ViewMode
import com.marcogn.hallofmemories.ui.common.EmptyState
import com.marcogn.hallofmemories.ui.common.HackArtwork
import com.marcogn.hallofmemories.ui.common.HallOfMemoriesTopBar
import com.marcogn.hallofmemories.ui.common.ViewModeToggle
import com.marcogn.hallofmemories.ui.common.displayName


/**
 * Hack library (spec §3.1). The pokédex sync banner is non-blocking — CoverDex blocks its whole
 * UI during its much larger first fetch; this app's sync is ~2.5 MB and everything here works
 * without it (see `docs/implementation-decisions.md`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit,
    onAddHack: () -> Unit,
    onHackClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            HallOfMemoriesTopBar(
                title = stringResource(R.string.home_title),
                onMenuClick = onMenuClick,
                actions = { ViewModeToggle(uiState.viewMode, viewModel::onViewModeChange) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHack) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            PokedexSyncBanner(syncState = uiState.syncState, onRetry = viewModel::retrySync)

            if (!uiState.allHacksEmpty) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )

                GenerationFilterRow(
                    selected = uiState.selectedGenerations,
                    onToggle = viewModel::onGenerationFilterToggle,
                )
            }

            when {
                uiState.allHacksEmpty -> EmptyState(
                    title = stringResource(R.string.home_empty_title),
                    subtitle = stringResource(R.string.home_empty_subtitle),
                    actionLabel = stringResource(R.string.home_empty_action),
                    onAction = onAddHack,
                    modifier = Modifier.weight(1f),
                )
                uiState.hacks.isEmpty() -> EmptyState(
                    title = stringResource(R.string.home_no_results_title),
                    subtitle = stringResource(R.string.home_no_results_subtitle),
                    modifier = Modifier.weight(1f),
                )
                uiState.viewMode == ViewMode.LIST -> HackList(
                    hacks = uiState.hacks,
                    onHackClick = onHackClick,
                    modifier = Modifier.weight(1f),
                )
                else -> HackGrid(
                    hacks = uiState.hacks,
                    onHackClick = onHackClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PokedexSyncBanner(syncState: SyncState, onRetry: () -> Unit) {
    when (syncState) {
        is SyncState.Running -> Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.home_pokedex_syncing,
                        syncState.stage.displayName(),
                        syncState.done,
                        syncState.total,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { if (syncState.total > 0) syncState.done.toFloat() / syncState.total else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
        is SyncState.Failed -> Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_pokedex_sync_failed, syncState.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.home_pokedex_retry)) }
            }
        }
        else -> {}
    }
}

@Composable
private fun GenerationFilterRow(selected: Set<GameGeneration>, onToggle: (GameGeneration) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(GameGeneration.entries, key = { it.name }) { generation ->
            FilterChip(
                selected = generation in selected,
                onClick = { onToggle(generation) },
                label = { Text(generation.displayName()) },
            )
        }
    }
}

@Composable
private fun HackList(hacks: List<HackWithEntryCount>, onHackClick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(hacks, key = { it.hack.id }) { entry ->
            HackListRow(entry = entry, onClick = { onHackClick(entry.hack.id) })
        }
    }
}

@Composable
private fun HackListRow(entry: HackWithEntryCount, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(56.dp).aspectRatio(3f / 4f),
        ) {
            HackArtwork(name = entry.hack.name, boxArtPath = entry.hack.boxArtPath, logoPath = entry.hack.logoPath)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.hack.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = entry.hack.generation.displayName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = hofCountText(entry.entryCount), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HackGrid(hacks: List<HackWithEntryCount>, onHackClick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(hacks, key = { it.hack.id }) { entry ->
            HackGridTile(entry = entry, onClick = { onHackClick(entry.hack.id) })
        }
    }
}

@Composable
private fun HackGridTile(entry: HackWithEntryCount, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column {
            HackArtwork(
                name = entry.hack.name,
                boxArtPath = entry.hack.boxArtPath,
                logoPath = entry.hack.logoPath,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = entry.hack.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = hofCountText(entry.entryCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun hofCountText(count: Int): String =
    if (count == 1) stringResource(R.string.home_hof_count_one, count) else stringResource(R.string.home_hof_count_other, count)
