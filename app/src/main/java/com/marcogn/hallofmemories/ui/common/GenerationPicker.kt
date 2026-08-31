package com.marcogn.hallofmemories.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.GameGeneration

/** Dropdown over every [GameGeneration], localized label shown per spec §3.5. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationPicker(
    selected: GameGeneration,
    onSelectedChange: (GameGeneration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.hack_generation_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GameGeneration.entries.forEach { generation ->
                DropdownMenuItem(
                    text = { Text(generation.displayName()) },
                    onClick = {
                        onSelectedChange(generation)
                        expanded = false
                    },
                )
            }
        }
    }
}
