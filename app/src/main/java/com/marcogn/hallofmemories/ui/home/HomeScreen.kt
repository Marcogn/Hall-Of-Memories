package com.marcogn.hallofmemories.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
    onEditHack: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) { viewModel.onClearSelection() }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (uiState.isSelectionMode) {
                HomeSelectionTopBar(
                    selectedCount = uiState.selectedHackIds.size,
                    onClose = viewModel::onClearSelection,
                    onEdit = {
                        uiState.selectedHackIds.singleOrNull()?.let { hackId ->
                            viewModel.onClearSelection()
                            onEditHack(hackId)
                        }
                    },
                    onDelete = { showDeleteConfirmation = true },
                    onSelectAll = viewModel::onSelectAll,
                )
            } else {
                HallOfMemoriesTopBar(
                    title = stringResource(R.string.home_title),
                    onMenuClick = onMenuClick,
                    actions = { ViewModeToggle(uiState.viewMode, viewModel::onViewModeChange) },
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(onClick = onAddHack) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
                }
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
                    selectedHackIds = uiState.selectedHackIds,
                    isSelectionMode = uiState.isSelectionMode,
                    onHackClick = { hackId -> if (uiState.isSelectionMode) viewModel.onToggleSelection(hackId) else onHackClick(hackId) },
                    onHackLongClick = viewModel::onToggleSelection,
                    modifier = Modifier.weight(1f),
                )
                else -> HackGrid(
                    hacks = uiState.hacks,
                    selectedHackIds = uiState.selectedHackIds,
                    isSelectionMode = uiState.isSelectionMode,
                    onHackClick = { hackId -> if (uiState.isSelectionMode) viewModel.onToggleSelection(hackId) else onHackClick(hackId) },
                    onHackLongClick = viewModel::onToggleSelection,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        val selectedCount = uiState.selectedHackIds.size
        val totalEntries = uiState.hacks.filter { it.hack.id in uiState.selectedHackIds }.sumOf { it.entryCount }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    if (selectedCount == 1) {
                        stringResource(R.string.home_delete_selected_confirm_title_one)
                    } else {
                        stringResource(R.string.home_delete_selected_confirm_title_other, selectedCount)
                    },
                )
            },
            text = {
                Text(
                    if (selectedCount == 1) {
                        stringResource(R.string.home_delete_selected_confirm_message_one, totalEntries)
                    } else {
                        stringResource(R.string.home_delete_selected_confirm_message_other, totalEntries)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.deleteSelected()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                if (selectedCount == 1) {
                    stringResource(R.string.home_selection_count_one, selectedCount)
                } else {
                    stringResource(R.string.home_selection_count_other, selectedCount)
                },
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_exit_selection))
            }
        },
        actions = {
            if (selectedCount == 1) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.cd_select_all))
            }
        },
    )
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
private fun HackList(
    hacks: List<HackWithEntryCount>,
    selectedHackIds: Set<String>,
    isSelectionMode: Boolean,
    onHackClick: (String) -> Unit,
    onHackLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(hacks, key = { it.hack.id }) { entry ->
            HackListRow(
                entry = entry,
                isSelected = entry.hack.id in selectedHackIds,
                isSelectionMode = isSelectionMode,
                onClick = { onHackClick(entry.hack.id) },
                onLongClick = { onHackLongClick(entry.hack.id) },
            )
        }
    }
}

@Composable
private fun HackListRow(
    entry: HackWithEntryCount,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(56.dp).aspectRatio(3f / 4f).clip(MaterialTheme.shapes.small)) {
            // Only box art here — a logo overlaid on top of it made both unreadable; the logo-alone
            // fallback for hacks without box art still comes from HackArtwork.
            if (entry.hack.boxArtPath != null) {
                AsyncImage(
                    model = entry.hack.boxArtPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                HackArtwork(
                    name = entry.hack.name,
                    boxArtPath = null,
                    logoPath = entry.hack.logoPath,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (isSelectionMode) {
                SelectionBadge(isSelected = isSelected, modifier = Modifier.align(Alignment.TopStart).padding(2.dp))
            }
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
private fun HackGrid(
    hacks: List<HackWithEntryCount>,
    selectedHackIds: Set<String>,
    isSelectionMode: Boolean,
    onHackClick: (String) -> Unit,
    onHackLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(hacks, key = { it.hack.id }) { entry ->
            HackGridTile(
                entry = entry,
                isSelected = entry.hack.id in selectedHackIds,
                isSelectionMode = isSelectionMode,
                onClick = { onHackClick(entry.hack.id) },
                onLongClick = { onHackLongClick(entry.hack.id) },
            )
        }
    }
}

@Composable
private fun HackGridTile(
    entry: HackWithEntryCount,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column {
            Box {
                if (entry.hack.boxArtPath != null) {
                    // Real box art keeps its own aspect ratio (FillWidth, no forced crop) so tiles of
                    // different heights sit without wasted space in the staggered grid — same pattern
                    // as the sibling app's GameGridTile. Only the generated placeholder gets a fixed
                    // 2:3 box below. No logo overlay — box art alone is the tile's identity here.
                    AsyncImage(
                        model = entry.hack.boxArtPath,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    HackArtwork(
                        name = entry.hack.name,
                        boxArtPath = null,
                        logoPath = entry.hack.logoPath,
                        modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
                if (isSelectionMode) {
                    SelectionBadge(isSelected = isSelected, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
                }
            }
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

/** Small checkmark/circle overlay shown on artwork while selection mode is active. */
@Composable
private fun SelectionBadge(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(24.dp).background(Color.Black.copy(alpha = 0.35f), shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (isSelected) stringResource(R.string.cd_selected) else null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun hofCountText(count: Int): String =
    if (count == 1) stringResource(R.string.home_hof_count_one, count) else stringResource(R.string.home_hof_count_other, count)
