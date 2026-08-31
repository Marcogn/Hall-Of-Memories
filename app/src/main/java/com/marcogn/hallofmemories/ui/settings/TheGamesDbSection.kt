package com.marcogn.hallofmemories.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcogn.hallofmemories.R

/** Spec §3.9: the key is entered at runtime, never baked into a build. */
@Composable
fun TheGamesDbSection(currentApiKey: String, onSave: (String) -> Unit) {
    var input by remember { mutableStateOf(currentApiKey) }

    SettingsSection(title = stringResource(R.string.settings_section_thegamesdb)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.settings_thegamesdb_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.settings_thegamesdb_api_key_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row {
                TextButton(onClick = { onSave(input) }, enabled = input != currentApiKey) {
                    Text(stringResource(R.string.settings_thegamesdb_save))
                }
            }
        }
    }
}
