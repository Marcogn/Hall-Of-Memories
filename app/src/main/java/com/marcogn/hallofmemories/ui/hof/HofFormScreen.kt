package com.marcogn.hallofmemories.ui.hof

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.ui.common.ScreenshotPicker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val insertedAtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HofFormScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: HofFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var slotEditorIndex by remember { mutableStateOf<Int?>(null) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    fun requestExit(onConfirmedExit: () -> Unit) {
        if (viewModel.hasUnsavedChanges()) showDiscardConfirmation = true else onConfirmedExit()
    }

    BackHandler { requestExit(onCancel) }

    fun openDateTimePicker() {
        val zone = ZoneId.systemDefault()
        val current = uiState.draft.insertedAt.atZone(zone)
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val updated = current
                            .withYear(year).withMonth(month + 1).withDayOfMonth(day)
                            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                        viewModel.onInsertedAtChange(updated.toInstant())
                    },
                    current.hour,
                    current.minute,
                    true,
                ).show()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth,
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (uiState.isEditMode) R.string.hof_form_edit_title else R.string.hof_form_new_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { requestExit(onCancel) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onSaved) }, enabled = !uiState.isSaving) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.hof_form_team_title), style = MaterialTheme.typography.titleMedium)
            uiState.draft.slots.forEach { slotDraft ->
                HofSlotCard(
                    draft = slotDraft,
                    hackGeneration = uiState.hackGeneration,
                    alwaysUseLatestSprites = uiState.alwaysUseLatestSprites,
                    speciesGeneration = null,
                    onClick = { slotEditorIndex = slotDraft.slotIndex },
                )
            }

            Text(stringResource(R.string.hof_form_trainer_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.draft.playerName,
                onValueChange = viewModel::onPlayerNameChange,
                label = { Text(stringResource(R.string.hof_form_player_name_label)) },
                singleLine = true,
                isError = uiState.errorMessage != null && uiState.draft.playerName.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.draft.playerId,
                onValueChange = viewModel::onPlayerIdChange,
                label = { Text(stringResource(R.string.hof_form_player_id_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.draft.playtimeText,
                onValueChange = viewModel::onPlaytimeTextChange,
                label = { Text(stringResource(R.string.hof_form_playtime_label)) },
                supportingText = { Text(stringResource(R.string.hof_form_playtime_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            ScreenshotPicker(
                imagePath = uiState.draft.screenshotPath,
                onPicked = viewModel::onScreenshotPicked,
                onRemove = viewModel::onScreenshotRemoved,
            )

            Column {
                Text(stringResource(R.string.hof_form_inserted_at_label), style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(insertedAtFormatter.format(uiState.draft.insertedAt.atZone(ZoneId.systemDefault())))
                    TextButton(onClick = ::openDateTimePicker) { Text(stringResource(R.string.hof_form_inserted_at_change)) }
                }
            }

            OutlinedTextField(
                value = uiState.draft.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.hack_notes_label)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    slotEditorIndex?.let { index ->
        SlotEditorDialog(
            slotIndex = index,
            initialDraft = uiState.draft.slots[index],
            hackGeneration = uiState.hackGeneration,
            alwaysUseLatestSprites = uiState.alwaysUseLatestSprites,
            natures = uiState.natures,
            abilities = uiState.abilities,
            isPokedexEmpty = uiState.isPokedexEmpty,
            onSearchSpecies = viewModel::searchSpecies,
            onSearchItems = viewModel::searchItems,
            onSearchMoves = viewModel::searchMoves,
            onDownloadPokedex = viewModel::retryPokedexSync,
            onConfirm = { updated ->
                viewModel.onSlotConfirmed(index, updated)
                slotEditorIndex = null
            },
            onDismiss = { slotEditorIndex = null },
        )
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.hof_form_discard_title)) },
            text = { Text(stringResource(R.string.hof_form_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirmation = false
                    onCancel()
                }) { Text(stringResource(R.string.hof_form_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
