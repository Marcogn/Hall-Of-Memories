package com.marcogn.hallofmemories.ui.hack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.ui.common.HackArtwork
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
    ) { padding ->
        if (uiState.isLoading || hack == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            HackArtwork(
                name = hack.name,
                boxArtPath = hack.boxArtPath,
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
                uiState.entries.size == 1 -> HallOfFameInlinePlaceholder(modifier = Modifier.padding(16.dp))
                else -> HallOfFameEntryList(entries = uiState.entries, onEntryClick = onEntryClick)
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

/** Phase 3 supplies the real `HallOfFameContent` composable; this is the placeholder this phase's plan asks for. */
@Composable
private fun HallOfFameInlinePlaceholder(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.coming_soon_phase, 3),
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HallOfFameEntryList(entries: List<HallOfFameEntryWithSlots>, onEntryClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries, key = { it.entry.id }) { entryWithSlots ->
            val entry = entryWithSlots.entry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntryClick(entry.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Screenshot/slot-0 sprite thumbnail arrives in Phase 3.
                Icon(
                    imageVector = Icons.Filled.CatchingPokemon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
