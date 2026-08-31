package com.marcogn.hallofmemories.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import com.marcogn.hallofmemories.ui.common.EmptyState
import com.marcogn.hallofmemories.ui.common.HallOfMemoriesTopBar
import com.marcogn.hallofmemories.ui.common.PokemonSprite
import com.marcogn.hallofmemories.ui.hof.SlotDraft
import com.marcogn.hallofmemories.ui.hof.SlotEditorDialog
import com.marcogn.hallofmemories.ui.hof.SlotEditorMode
import com.marcogn.hallofmemories.ui.hof.toSlotDraft
import java.time.Instant

/** Which template, if any, the [SlotEditorDialog] overlay is currently editing — `null` means the overlay is closed. */
private data class TemplateEditorTarget(
    val id: String?,
    val label: String,
    val initialDraft: SlotDraft,
    val createdAt: Instant?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(onMenuClick: () -> Unit, modifier: Modifier = Modifier, viewModel: TemplatesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val natures by viewModel.natures.collectAsState()
    val abilities by viewModel.abilities.collectAsState()
    var editorTarget by remember { mutableStateOf<TemplateEditorTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<PokemonTemplate?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { HallOfMemoriesTopBar(title = stringResource(R.string.templates_title), onMenuClick = onMenuClick) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editorTarget = TemplateEditorTarget(id = null, label = "", initialDraft = SlotDraft.empty(0), createdAt = null)
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.allTemplatesEmpty) {
                EmptyState(
                    title = stringResource(R.string.templates_empty_title),
                    subtitle = stringResource(R.string.templates_empty_subtitle),
                )
                return@Scaffold
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text(stringResource(R.string.templates_search_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            if (uiState.templates.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.templates_no_results_title),
                    subtitle = stringResource(R.string.templates_no_results_subtitle),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.templates, key = { it.id }) { template ->
                        TemplateRow(
                            template = template,
                            alwaysUseLatestSprites = uiState.alwaysUseLatestSprites,
                            onEdit = {
                                editorTarget = TemplateEditorTarget(
                                    id = template.id,
                                    label = template.label,
                                    initialDraft = template.toSlotDraft(0),
                                    createdAt = template.createdAt,
                                )
                            },
                            onDuplicate = { viewModel.duplicateTemplate(template) },
                            onDelete = { pendingDelete = template },
                        )
                    }
                }
            }
        }
    }

    editorTarget?.let { target ->
        var label by remember(target) { mutableStateOf(target.label) }
        SlotEditorDialog(
            slotIndex = 0,
            initialDraft = target.initialDraft,
            mode = SlotEditorMode.TEMPLATE,
            templateLabel = label,
            onTemplateLabelChange = { label = it },
            hackGeneration = GameGeneration.OTHER,
            alwaysUseLatestSprites = uiState.alwaysUseLatestSprites,
            natures = natures,
            abilities = abilities,
            isPokedexEmpty = natures.isEmpty() && abilities.isEmpty(),
            onSearchSpecies = viewModel::searchSpecies,
            onSearchItems = viewModel::searchItems,
            onSearchMoves = viewModel::searchMoves,
            onDownloadPokedex = viewModel::retryPokedexSync,
            onConfirm = { draft ->
                viewModel.saveTemplate(id = target.id, label = label, draft = draft, createdAt = target.createdAt)
                editorTarget = null
            },
            onDismiss = { editorTarget = null },
        )
    }

    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.templates_delete_confirm_title, template.label)) },
            text = { Text(stringResource(R.string.templates_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(template.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun TemplateRow(
    template: PokemonTemplate,
    alwaysUseLatestSprites: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (template.speciesId != null) {
                PokemonSprite(
                    speciesId = template.speciesId,
                    generation = GameGeneration.OTHER,
                    shiny = template.isShiny,
                    alwaysUseLatest = alwaysUseLatestSprites,
                    speciesGeneration = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CatchingPokemon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(template.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = listOfNotNull(
                    template.speciesName,
                    template.level?.let { stringResource(R.string.hof_slot_level, it) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
        }
        IconButton(onClick = onDuplicate) {
            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.templates_duplicate))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}
