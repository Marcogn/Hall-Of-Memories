package com.marcogn.hallofmemories.ui.hof

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import com.marcogn.hallofmemories.ui.common.PokemonSprite
import com.marcogn.hallofmemories.ui.common.ScreenshotViewerDialog
import com.marcogn.hallofmemories.ui.common.typeColorFor
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val insertedAtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * The read-only body of a Hall of Fame entry — shared by `HofDetailScreen` (its own top bar) and
 * `HackDetailScreen`'s single-entry case (embedded directly under the hack header, spec §3.1).
 */
@Composable
fun HallOfFameContent(
    entryWithSlots: HallOfFameEntryWithSlots,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    onLookupMoveType: suspend (String) -> String?,
    modifier: Modifier = Modifier,
) {
    val entry = entryWithSlots.entry
    var showScreenshotViewer by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        entry.screenshotPath?.let { path ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showScreenshotViewer = true },
            ) {
                AsyncImage(model = path, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.playerName, style = MaterialTheme.typography.titleMedium)
                if (entry.playerId.isNotBlank()) {
                    Text(stringResource(R.string.hof_detail_player_id, entry.playerId), style = MaterialTheme.typography.bodyMedium)
                }
                if (entry.playtimeText.isNotBlank()) {
                    Text(stringResource(R.string.hof_detail_playtime, entry.playtimeText), style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = stringResource(R.string.hof_detail_inserted_at, insertedAtFormatter.format(entry.insertedAt.atZone(ZoneId.systemDefault()))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(stringResource(R.string.hof_form_team_title), style = MaterialTheme.typography.titleMedium)
        entryWithSlots.slots.forEach { slot ->
            HofSlotDetailCard(
                slot = slot,
                hackGeneration = hackGeneration,
                alwaysUseLatestSprites = alwaysUseLatestSprites,
                onLookupMoveType = onLookupMoveType,
            )
        }

        entry.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Column {
                Text(stringResource(R.string.hack_notes_label), style = MaterialTheme.typography.titleSmall)
                Text(notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showScreenshotViewer) {
        entry.screenshotPath?.let { path ->
            ScreenshotViewerDialog(imagePath = path, onDismiss = { showScreenshotViewer = false })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HofSlotDetailCard(
    slot: PokemonSlot,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    onLookupMoveType: suspend (String) -> String?,
) {
    if (slot.speciesId == null) {
        Card {
            Text(
                text = stringResource(R.string.hof_slot_empty, slot.slotIndex + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PokemonSprite(
                    speciesId = slot.speciesId,
                    generation = hackGeneration,
                    shiny = slot.isShiny,
                    alwaysUseLatest = alwaysUseLatestSprites,
                    speciesGeneration = null,
                    modifier = Modifier.size(48.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(slot.nickname?.takeIf { it.isNotBlank() } ?: slot.speciesName.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        genderSymbol(slot.gender)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                        if (slot.isShiny) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = stringResource(R.string.hof_slot_shiny),
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (slot.nickname?.isNotBlank() == true) {
                        Text(slot.speciesName.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = listOfNotNull(
                            slot.level?.let { stringResource(R.string.hof_slot_level, it) },
                            slot.nature.takeIf { !it.isNullOrBlank() },
                            slot.ability.takeIf { !it.isNullOrBlank() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    slot.heldItem?.takeIf { it.isNotBlank() }?.let {
                        Text(stringResource(R.string.hof_detail_held_item, it), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            val moves = listOfNotNull(slot.move1, slot.move2, slot.move3, slot.move4).filter { it.isNotBlank() }
            if (moves.isNotEmpty()) {
                var moveTypes by remember(slot.id) { mutableStateOf<Map<String, String?>>(emptyMap()) }
                LaunchedEffect(slot.id) {
                    moveTypes = moves.associateWith { onLookupMoveType(it) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    moves.forEach { move -> MoveChip(move, moveTypes[move]) }
                }
            }

            if (hasAnyIvOrEv(slot)) {
                Text(
                    text = stringResource(
                        R.string.hof_detail_iv_ev,
                        ivSummary(slot),
                        evSummary(slot),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MoveChip(name: String, type: String?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(typeColorFor(type).copy(alpha = 0.25f)),
    ) {
        Text(text = name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

private fun genderSymbol(gender: PokemonGender): String? = when (gender) {
    PokemonGender.MALE -> "♂"
    PokemonGender.FEMALE -> "♀"
    PokemonGender.UNKNOWN -> null
}

private fun hasAnyIvOrEv(slot: PokemonSlot): Boolean =
    listOf(slot.ivHp, slot.ivAtk, slot.ivDef, slot.ivSpAtk, slot.ivSpDef, slot.ivSpe, slot.evHp, slot.evAtk, slot.evDef, slot.evSpAtk, slot.evSpDef, slot.evSpe)
        .any { it != null }

private fun ivSummary(slot: PokemonSlot): String =
    "${slot.ivHp ?: 0}/${slot.ivAtk ?: 0}/${slot.ivDef ?: 0}/${slot.ivSpAtk ?: 0}/${slot.ivSpDef ?: 0}/${slot.ivSpe ?: 0}"

private fun evSummary(slot: PokemonSlot): String =
    "${slot.evHp ?: 0}/${slot.evAtk ?: 0}/${slot.evDef ?: 0}/${slot.evSpAtk ?: 0}/${slot.evSpDef ?: 0}/${slot.evSpe ?: 0}"
