package com.marcogn.hallofmemories.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** One suggestion row: the text that gets written into the field on tap, plus optional trailing content (a move's type chip, a nature's stat arrows). */
data class ComboBoxSuggestion(
    val displayText: String,
    val trailing: (@Composable () -> Unit)? = null,
)

/**
 * The shared "typing aid over free text" control spec A9 requires: suggests cached PokéAPI values
 * (nature/ability/held item/moves) but never rejects or rewrites what the user typed. Filtering
 * (prefix-then-contains, capped at 30 rows) is the caller's job — this composable only renders
 * whatever [suggestions] it's given, already filtered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableComboBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<ComboBoxSuggestion>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val showMenu = expanded && suggestions.isNotEmpty()

    ExposedDropdownMenuBox(expanded = showMenu, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { expanded = false }) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.displayText) },
                    trailingIcon = suggestion.trailing,
                    onClick = {
                        onValueChange(suggestion.displayText)
                        expanded = false
                    },
                )
            }
        }
    }
}
