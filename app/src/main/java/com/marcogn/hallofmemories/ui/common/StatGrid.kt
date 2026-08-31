package com.marcogn.hallofmemories.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

enum class StatKey { HP, ATK, DEF, SP_ATK, SP_DEF, SPE }

private fun StatKey.shortLabel(): String = when (this) {
    StatKey.HP -> "HP"
    StatKey.ATK -> "Atk"
    StatKey.DEF -> "Def"
    StatKey.SP_ATK -> "SpA"
    StatKey.SP_DEF -> "SpD"
    StatKey.SPE -> "Spe"
}

/**
 * The six-stat IV/EV grid (spec §3.3): two rows of three text fields, each validated against
 * [range] individually — an out-of-range entry shows inline as an error but is still whatever the
 * user typed (committing/discarding it is the caller's job, see `SlotEditorDialog`). Pass an
 * already-localized [totalLabel] (EVs only) to show a running total, coloured as an error when
 * [isTotalError] — the one save-blocking rule in the whole slot editor (spec §3.4).
 */
@Composable
fun StatGrid(
    title: String,
    values: Map<StatKey, String>,
    onValueChange: (StatKey, String) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    totalLabel: String? = null,
    isTotalError: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatField(StatKey.HP, values, onValueChange, range, Modifier.weight(1f))
            StatField(StatKey.ATK, values, onValueChange, range, Modifier.weight(1f))
            StatField(StatKey.DEF, values, onValueChange, range, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatField(StatKey.SP_ATK, values, onValueChange, range, Modifier.weight(1f))
            StatField(StatKey.SP_DEF, values, onValueChange, range, Modifier.weight(1f))
            StatField(StatKey.SPE, values, onValueChange, range, Modifier.weight(1f))
        }
        if (totalLabel != null) {
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (isTotalError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatField(
    key: StatKey,
    values: Map<StatKey, String>,
    onValueChange: (StatKey, String) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
) {
    val value = values[key].orEmpty()
    val isError = value.isNotBlank() && value.trim().toIntOrNull()?.let { it in range } != true
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(key, it) },
        label = { Text(key.shortLabel()) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
