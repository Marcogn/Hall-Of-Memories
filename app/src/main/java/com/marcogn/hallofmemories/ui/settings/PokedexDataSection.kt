package com.marcogn.hallofmemories.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.PokedexStageStatus
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.ui.common.displayName
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SpritesSection(
    alwaysUseLatestSprites: Boolean,
    onAlwaysUseLatestSpritesChange: (Boolean) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_sprites)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_sprites_always_latest))
                Text(
                    text = stringResource(R.string.settings_sprites_always_latest_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = alwaysUseLatestSprites, onCheckedChange = onAlwaysUseLatestSpritesChange)
        }
    }
}

@Composable
fun PokedexDataSection(
    stageStatuses: List<PokedexStageStatus>,
    syncState: SyncState,
    onRetry: () -> Unit,
    onInvalidateAndRedownload: () -> Unit,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    SettingsSection(title = stringResource(R.string.settings_section_pokedex_data)) {
        stageStatuses.forEach { status -> PokedexStageRow(status) }

        when (syncState) {
            is SyncState.Running -> {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(
                            R.string.settings_pokedex_syncing,
                            syncState.stage.displayName(),
                            syncState.done,
                            syncState.total,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = { if (syncState.total > 0) syncState.done.toFloat() / syncState.total else 0f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            is SyncState.Failed -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_pokedex_sync_failed, syncState.message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.settings_pokedex_retry)) }
                }
            }
            else -> {}
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextButton(onClick = { showConfirmDialog = true }) {
                Text(stringResource(R.string.settings_pokedex_redownload_button))
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.settings_pokedex_redownload_confirm_title)) },
            text = { Text(stringResource(R.string.settings_pokedex_redownload_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onInvalidateAndRedownload()
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun PokedexStageRow(status: PokedexStageStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(text = status.stage.displayName(), modifier = Modifier.weight(1f))
        Text(
            text = status.lastSyncedAt?.let { formatSyncTimestamp(it, status.itemCount) }
                ?: stringResource(R.string.settings_pokedex_never_synced),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun formatSyncTimestamp(instant: Instant, itemCount: Int): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    val formatted = formatter.format(instant.atZone(ZoneId.systemDefault()))
    return stringResource(R.string.settings_pokedex_stage_summary, itemCount, formatted)
}
