package com.marcogn.hallofmemories.ui.hack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import com.marcogn.hallofmemories.ui.common.HackArtwork
import com.marcogn.hallofmemories.ui.common.PokemonSprite
import com.marcogn.hallofmemories.ui.common.displayName
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HackDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    onAddEntry: (String) -> Unit,
    onEntryClick: (String) -> Unit,
    viewModel: HackDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val hack = uiState.hack

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(hack?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (hack != null) {
                        IconButton(onClick = { onEdit(hack.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (hack != null) {
                FloatingActionButton(onClick = { onAddEntry(hack.id) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.hack_detail_add_hof))
                }
            }
        },
    ) { padding ->
        if (uiState.isLoading || hack == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Logo only here — box art is already the identity shown in the library list/grid;
            // repeating it as a second, larger image on this screen was visual noise.
            HackArtwork(
                name = hack.name,
                boxArtPath = null,
                logoPath = hack.logoPath,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = hack.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = hack.generation.displayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                hack.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Text(text = notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }

            when {
                uiState.entries.isEmpty() -> HackDetailEmptyState(onAddEntry = { onAddEntry(hack.id) })
                uiState.entries.size <= 6 -> HallOfFameCarousel(
                    entries = uiState.sortedEntries,
                    hackGeneration = hack.generation,
                    alwaysUseLatestSprites = uiState.alwaysUseLatestSprites,
                    onEntryClick = onEntryClick,
                )
                else -> HallOfFameEntryList(
                    entries = uiState.sortedEntries,
                    sortNewestFirst = uiState.sortNewestFirst,
                    hackGeneration = hack.generation,
                    alwaysUseLatestSprites = uiState.alwaysUseLatestSprites,
                    onToggleSortOrder = viewModel::onToggleSortOrder,
                    onEntryClick = onEntryClick,
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.hack_detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.hack_detail_delete_confirm_message, uiState.entries.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.deleteHack(onDeleted)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun HackDetailEmptyState(onAddEntry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.hack_detail_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.hack_detail_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(onClick = onAddEntry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.hack_detail_add_first_hof))
        }
    }
}

@Composable
private fun HallOfFameEntryList(
    entries: List<HallOfFameEntryWithSlots>,
    sortNewestFirst: Boolean,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    onToggleSortOrder: () -> Unit,
    onEntryClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onToggleSortOrder) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(
                    stringResource(
                        if (sortNewestFirst) R.string.hack_detail_sort_newest_first else R.string.hack_detail_sort_oldest_first,
                    ),
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries, key = { it.entry.id }) { entryWithSlots ->
                val entry = entryWithSlots.entry
                val slot0 = entryWithSlots.slots.firstOrNull { it.slotIndex == 0 }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEntryClick(entry.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EntryThumbnail(
                        entry = entry,
                        slot0 = slot0,
                        hackGeneration = hackGeneration,
                        alwaysUseLatestSprites = alwaysUseLatestSprites,
                        modifier = Modifier.size(48.dp),
                    )
                    Column {
                        Text(text = entry.playerName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = listOfNotNull(
                                entry.playtimeText.takeIf { it.isNotBlank() },
                                dateFormatter.format(entry.insertedAt.atZone(ZoneId.systemDefault())),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A horizontally scrollable row of entry previews (spec §3.1) for a hack with 2–6 entries — a
 * hack with more than six falls back to [HallOfFameEntryList], where a carousel would stop being
 * scannable (phase plan §1).
 */
@Composable
private fun HallOfFameCarousel(
    entries: List<HallOfFameEntryWithSlots>,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    onEntryClick: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { it.entry.id }) { entryWithSlots ->
            val entry = entryWithSlots.entry
            val slot0 = entryWithSlots.slots.firstOrNull { it.slotIndex == 0 }
            Card(
                onClick = { onEntryClick(entry.id) },
                modifier = Modifier.width(140.dp),
            ) {
                EntryThumbnail(
                    entry = entry,
                    slot0 = slot0,
                    hackGeneration = hackGeneration,
                    alwaysUseLatestSprites = alwaysUseLatestSprites,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = entry.playerName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = dateFormatter.format(entry.insertedAt.atZone(ZoneId.systemDefault())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Screenshot → slot-0 sprite → placeholder icon (spec §3.1), shared by the list row and the carousel tile. */
@Composable
private fun EntryThumbnail(
    entry: HallOfFameEntry,
    slot0: PokemonSlot?,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            entry.screenshotPath != null -> AsyncImage(
                model = entry.screenshotPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            slot0?.speciesId != null -> PokemonSprite(
                speciesId = slot0.speciesId,
                generation = hackGeneration,
                shiny = slot0.isShiny,
                alwaysUseLatest = alwaysUseLatestSprites,
                speciesGeneration = null,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Icon(
                imageVector = Icons.Filled.CatchingPokemon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
