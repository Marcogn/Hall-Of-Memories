package com.marcogn.hallofmemories.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local backup export/import (spec §5) plus the disabled, badged Google Drive row that is v2's whole seam. */
@Composable
fun BackupSection(isBusy: Boolean, onExport: (Uri) -> Unit, onImport: (Uri) -> Unit) {
    var showImportConfirmation by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { showImportConfirmation = it } }

    SettingsSection(title = stringResource(R.string.settings_section_backup)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.settings_backup_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                TextButton(
                    onClick = { exportLauncher.launch(suggestedBackupFileName()) },
                    enabled = !isBusy,
                ) { Text(stringResource(R.string.settings_backup_export)) }
                TextButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !isBusy,
                ) { Text(stringResource(R.string.settings_backup_import)) }
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp).size(20.dp), strokeWidth = 2.dp)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_backup_drive_title), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.settings_backup_drive_coming_soon)) })
        }
    }

    showImportConfirmation?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirmation = null },
            title = { Text(stringResource(R.string.settings_backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirmation = null
                    onImport(uri)
                }) { Text(stringResource(R.string.settings_backup_import_confirm_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmation = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun suggestedBackupFileName(): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
    return "hall-of-memories-backup-$timestamp.zip"
}
