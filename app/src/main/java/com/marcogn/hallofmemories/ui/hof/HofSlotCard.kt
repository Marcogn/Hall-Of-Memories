package com.marcogn.hallofmemories.ui.hof

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.ui.common.PokemonSprite

/** Collapsed summary of one team slot in the six-slot list — tap opens `SlotEditorDialog` for it. */
@Composable
fun HofSlotCard(
    draft: SlotDraft,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    speciesGeneration: Int?,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (draft.speciesId != null) {
                    PokemonSprite(
                        speciesId = draft.speciesId,
                        generation = hackGeneration,
                        shiny = draft.isShiny,
                        alwaysUseLatest = alwaysUseLatestSprites,
                        speciesGeneration = speciesGeneration,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (draft.isEmpty) {
                    Text(
                        text = stringResource(R.string.hof_slot_empty, draft.slotIndex + 1),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = draft.nickname.ifBlank { draft.speciesName.orEmpty() },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.hof_slot_level, draft.level ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (draft.isShiny) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = stringResource(R.string.hof_slot_shiny),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
