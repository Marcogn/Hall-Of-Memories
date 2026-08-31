package com.marcogn.hallofmemories.ui.hof

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.filter.filterComboBoxSuggestions
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.PokedexAbility
import com.marcogn.hallofmemories.domain.model.PokedexItem
import com.marcogn.hallofmemories.domain.model.PokedexMove
import com.marcogn.hallofmemories.domain.model.PokedexNature
import com.marcogn.hallofmemories.domain.model.PokedexSpecies
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import com.marcogn.hallofmemories.domain.validation.SlotValidation
import com.marcogn.hallofmemories.ui.common.ComboBoxSuggestion
import com.marcogn.hallofmemories.ui.common.EditableComboBox
import com.marcogn.hallofmemories.ui.common.PokemonSprite
import com.marcogn.hallofmemories.ui.common.StatGrid
import com.marcogn.hallofmemories.ui.common.StatKey
import com.marcogn.hallofmemories.ui.common.displayName
import com.marcogn.hallofmemories.ui.common.typeColorFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SlotEditorMode { SLOT, TEMPLATE }

/** All-string mirror of [SlotDraft]'s numeric fields, so a text field can hold whatever the user is mid-typing without forcing it to parse. */
private data class SlotFormFields(
    val speciesId: Int?,
    val speciesName: String,
    val nickname: String,
    val gender: PokemonGender,
    val levelText: String,
    val nature: String,
    val ability: String,
    val isShiny: Boolean,
    val heldItem: String,
    val ivHpText: String,
    val ivAtkText: String,
    val ivDefText: String,
    val ivSpAtkText: String,
    val ivSpDefText: String,
    val ivSpeText: String,
    val evHpText: String,
    val evAtkText: String,
    val evDefText: String,
    val evSpAtkText: String,
    val evSpDefText: String,
    val evSpeText: String,
    val move1: String,
    val move2: String,
    val move3: String,
    val move4: String,
    val sourceTemplateId: String?,
) {
    companion object {
        fun from(draft: SlotDraft): SlotFormFields = SlotFormFields(
            speciesId = draft.speciesId,
            speciesName = draft.speciesName.orEmpty(),
            nickname = draft.nickname,
            gender = draft.gender,
            levelText = draft.level?.toString().orEmpty(),
            nature = draft.nature,
            ability = draft.ability,
            isShiny = draft.isShiny,
            heldItem = draft.heldItem,
            ivHpText = draft.ivHp?.toString().orEmpty(),
            ivAtkText = draft.ivAtk?.toString().orEmpty(),
            ivDefText = draft.ivDef?.toString().orEmpty(),
            ivSpAtkText = draft.ivSpAtk?.toString().orEmpty(),
            ivSpDefText = draft.ivSpDef?.toString().orEmpty(),
            ivSpeText = draft.ivSpe?.toString().orEmpty(),
            evHpText = draft.evHp?.toString().orEmpty(),
            evAtkText = draft.evAtk?.toString().orEmpty(),
            evDefText = draft.evDef?.toString().orEmpty(),
            evSpAtkText = draft.evSpAtk?.toString().orEmpty(),
            evSpDefText = draft.evSpDef?.toString().orEmpty(),
            evSpeText = draft.evSpe?.toString().orEmpty(),
            move1 = draft.move1,
            move2 = draft.move2,
            move3 = draft.move3,
            move4 = draft.move4,
            sourceTemplateId = draft.sourceTemplateId,
        )
    }
}

private fun String.toValidIntOrNull(range: IntRange): Int? = trim().toIntOrNull()?.takeIf { it in range }

private fun SlotFormFields.evTotal(): Int = SlotValidation.evTotal(
    evHpText.toValidIntOrNull(SlotValidation.EV_RANGE) ?: 0,
    evAtkText.toValidIntOrNull(SlotValidation.EV_RANGE) ?: 0,
    evDefText.toValidIntOrNull(SlotValidation.EV_RANGE) ?: 0,
    evSpAtkText.toValidIntOrNull(SlotValidation.EV_RANGE) ?: 0,
    evSpDefText.toValidIntOrNull(SlotValidation.EV_RANGE) ?: 0,
    evSpeText.toValidIntOrNull(SlotValidation.EV_RANGE) ?: 0,
)

private fun SlotFormFields.toSlotDraft(slotIndex: Int): SlotDraft = SlotDraft(
    slotIndex = slotIndex,
    speciesId = speciesId,
    speciesName = speciesName.ifBlank { null },
    nickname = nickname,
    gender = gender,
    level = levelText.toValidIntOrNull(SlotValidation.LEVEL_RANGE),
    nature = nature,
    ability = ability,
    isShiny = isShiny,
    heldItem = heldItem,
    ivHp = ivHpText.toValidIntOrNull(SlotValidation.IV_RANGE),
    ivAtk = ivAtkText.toValidIntOrNull(SlotValidation.IV_RANGE),
    ivDef = ivDefText.toValidIntOrNull(SlotValidation.IV_RANGE),
    ivSpAtk = ivSpAtkText.toValidIntOrNull(SlotValidation.IV_RANGE),
    ivSpDef = ivSpDefText.toValidIntOrNull(SlotValidation.IV_RANGE),
    ivSpe = ivSpeText.toValidIntOrNull(SlotValidation.IV_RANGE),
    evHp = evHpText.toValidIntOrNull(SlotValidation.EV_RANGE),
    evAtk = evAtkText.toValidIntOrNull(SlotValidation.EV_RANGE),
    evDef = evDefText.toValidIntOrNull(SlotValidation.EV_RANGE),
    evSpAtk = evSpAtkText.toValidIntOrNull(SlotValidation.EV_RANGE),
    evSpDef = evSpDefText.toValidIntOrNull(SlotValidation.EV_RANGE),
    evSpe = evSpeText.toValidIntOrNull(SlotValidation.EV_RANGE),
    move1 = move1,
    move2 = move2,
    move3 = move3,
    move4 = move4,
    sourceTemplateId = sourceTemplateId,
)

/**
 * Full-screen editor for one team slot. Holds its own local state — `remember(slotIndex)`, per the
 * phase plan's pitfall about slot 3 showing slot 1's stale state — seeded from [initialDraft] and
 * only reported back via [onConfirm] when the user taps the confirm action, never per keystroke.
 * An out-of-range level/IV/EV value never blocks confirming; only the EV total over 510 does
 * (spec §3.4) — [toSlotDraft] simply nulls out whatever didn't parse validly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotEditorDialog(
    slotIndex: Int,
    initialDraft: SlotDraft,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    natures: List<PokedexNature>,
    abilities: List<PokedexAbility>,
    isPokedexEmpty: Boolean,
    onSearchSpecies: suspend (String) -> List<PokedexSpecies>,
    onSearchItems: suspend (String) -> List<PokedexItem>,
    onSearchMoves: suspend (String) -> List<PokedexMove>,
    onDownloadPokedex: () -> Unit,
    onConfirm: (SlotDraft) -> Unit,
    onDismiss: () -> Unit,
    mode: SlotEditorMode = SlotEditorMode.SLOT,
    templateLabel: String = "",
    onTemplateLabelChange: (String) -> Unit = {},
    templates: List<PokemonTemplate> = emptyList(),
    onSaveAsTemplate: (draft: SlotDraft, label: String, overwriteId: String?) -> Unit = { _, _, _ -> },
) {
    var fields by remember(slotIndex) { mutableStateOf(SlotFormFields.from(initialDraft)) }
    var previousFieldsForUndo by remember(slotIndex) { mutableStateOf<SlotFormFields?>(null) }
    var showLoadTemplateSheet by remember(slotIndex) { mutableStateOf(false) }
    var showSaveAsTemplateDialog by remember(slotIndex) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember(slotIndex) { SnackbarHostState() }

    var speciesQuery by remember(slotIndex) { mutableStateOf("") }
    var speciesResults by remember(slotIndex) { mutableStateOf<List<PokedexSpecies>>(emptyList()) }
    var itemResults by remember(slotIndex) { mutableStateOf<List<PokedexItem>>(emptyList()) }
    var moveResults by remember(slotIndex) { mutableStateOf(List(4) { emptyList<PokedexMove>() }) }

    LaunchedEffect(speciesQuery) {
        speciesResults = if (speciesQuery.isBlank()) emptyList() else onSearchSpecies(speciesQuery)
    }

    val evTotal = fields.evTotal()
    val isEvTotalValid = SlotValidation.isEvTotalValid(evTotal)
    val canConfirm = isEvTotalValid && (mode == SlotEditorMode.SLOT || templateLabel.isNotBlank())
    val loadedFromTemplateMessage = stringResource(R.string.hof_slot_loaded_from_template)
    val undoLabel = stringResource(R.string.hof_slot_undo)

    fun applyTemplate(template: PokemonTemplate) {
        previousFieldsForUndo = fields
        fields = SlotFormFields.from(template.toSlotDraft(slotIndex))
        showLoadTemplateSheet = false
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = loadedFromTemplateMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                previousFieldsForUndo?.let { fields = it }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (mode == SlotEditorMode.TEMPLATE) {
                                stringResource(R.string.hof_template_editor_title)
                            } else {
                                stringResource(R.string.hof_slot_editor_title, slotIndex + 1)
                            },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { onConfirm(fields.toSlotDraft(slotIndex)) }, enabled = canConfirm) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_confirm))
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (mode == SlotEditorMode.TEMPLATE) {
                    OutlinedTextField(
                        value = templateLabel,
                        onValueChange = onTemplateLabelChange,
                        label = { Text(stringResource(R.string.hof_template_label_label)) },
                        singleLine = true,
                        isError = templateLabel.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showLoadTemplateSheet = true }, enabled = templates.isNotEmpty()) {
                            Text(stringResource(R.string.hof_slot_load_from_template))
                        }
                        TextButton(onClick = { showSaveAsTemplateDialog = true }) {
                            Text(stringResource(R.string.hof_slot_save_as_template))
                        }
                    }
                }

                if (isPokedexEmpty) {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.hof_pokedex_empty_message), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onDownloadPokedex, modifier = Modifier.padding(top = 8.dp)) {
                                Text(stringResource(R.string.hof_pokedex_empty_download))
                            }
                        }
                    }
                }

                SpeciesPicker(
                    speciesId = fields.speciesId,
                    speciesName = fields.speciesName,
                    query = speciesQuery,
                    onQueryChange = { speciesQuery = it },
                    results = speciesResults,
                    hackGeneration = hackGeneration,
                    alwaysUseLatestSprites = alwaysUseLatestSprites,
                    onSelected = { species ->
                        fields = fields.copy(speciesId = species.id, speciesName = species.displayName)
                        speciesQuery = ""
                        speciesResults = emptyList()
                    },
                    onCleared = { fields = fields.copy(speciesId = null, speciesName = "") },
                )

                OutlinedTextField(
                    value = fields.nickname,
                    onValueChange = { fields = fields.copy(nickname = it) },
                    label = { Text(stringResource(R.string.hof_slot_nickname_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                GenderPicker(gender = fields.gender, onGenderChange = { fields = fields.copy(gender = it) })

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = fields.levelText,
                        onValueChange = { fields = fields.copy(levelText = it) },
                        label = { Text(stringResource(R.string.hof_slot_level_label)) },
                        singleLine = true,
                        isError = fields.levelText.isNotBlank() && fields.levelText.toValidIntOrNull(SlotValidation.LEVEL_RANGE) == null,
                        modifier = Modifier.weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.hof_slot_shiny_label))
                        Switch(
                            checked = fields.isShiny,
                            onCheckedChange = { fields = fields.copy(isShiny = it) },
                            colors = SwitchDefaults.colors(),
                        )
                    }
                }

                EditableComboBox(
                    value = fields.nature,
                    onValueChange = { fields = fields.copy(nature = it) },
                    label = stringResource(R.string.hof_slot_nature_label),
                    suggestions = filterComboBoxSuggestions(fields.nature, natures) { it.searchName }.map { nature ->
                        ComboBoxSuggestion(nature.displayName, trailing = { NatureStatHint(nature) })
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                EditableComboBox(
                    value = fields.ability,
                    onValueChange = { fields = fields.copy(ability = it) },
                    label = stringResource(R.string.hof_slot_ability_label),
                    suggestions = filterComboBoxSuggestions(fields.ability, abilities) { it.searchName }
                        .map { ComboBoxSuggestion(it.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                )

                SearchedComboBox(
                    value = fields.heldItem,
                    onValueChange = { fields = fields.copy(heldItem = it) },
                    label = stringResource(R.string.hof_slot_held_item_label),
                    results = itemResults,
                    onQueryChange = { query -> scope.launch { itemResults = if (query.isBlank()) emptyList() else onSearchItems(query) } },
                    suggestionOf = { ComboBoxSuggestion(it.displayName) },
                )

                Text(stringResource(R.string.hof_slot_moves_title), style = MaterialTheme.typography.titleSmall)
                MoveEditor(index = 0, value = fields.move1, onValueChange = { fields = fields.copy(move1 = it) }, onSearchMoves = onSearchMoves, scope = scope, results = moveResults[0], onResults = { moveResults = moveResults.toMutableList().also { r -> r[0] = it } })
                MoveEditor(index = 1, value = fields.move2, onValueChange = { fields = fields.copy(move2 = it) }, onSearchMoves = onSearchMoves, scope = scope, results = moveResults[1], onResults = { moveResults = moveResults.toMutableList().also { r -> r[1] = it } })
                MoveEditor(index = 2, value = fields.move3, onValueChange = { fields = fields.copy(move3 = it) }, onSearchMoves = onSearchMoves, scope = scope, results = moveResults[2], onResults = { moveResults = moveResults.toMutableList().also { r -> r[2] = it } })
                MoveEditor(index = 3, value = fields.move4, onValueChange = { fields = fields.copy(move4 = it) }, onSearchMoves = onSearchMoves, scope = scope, results = moveResults[3], onResults = { moveResults = moveResults.toMutableList().also { r -> r[3] = it } })

                HorizontalDivider()

                StatGrid(
                    title = stringResource(R.string.hof_slot_ivs_title),
                    values = mapOf(
                        StatKey.HP to fields.ivHpText, StatKey.ATK to fields.ivAtkText, StatKey.DEF to fields.ivDefText,
                        StatKey.SP_ATK to fields.ivSpAtkText, StatKey.SP_DEF to fields.ivSpDefText, StatKey.SPE to fields.ivSpeText,
                    ),
                    onValueChange = { key, value ->
                        fields = when (key) {
                            StatKey.HP -> fields.copy(ivHpText = value)
                            StatKey.ATK -> fields.copy(ivAtkText = value)
                            StatKey.DEF -> fields.copy(ivDefText = value)
                            StatKey.SP_ATK -> fields.copy(ivSpAtkText = value)
                            StatKey.SP_DEF -> fields.copy(ivSpDefText = value)
                            StatKey.SPE -> fields.copy(ivSpeText = value)
                        }
                    },
                    range = SlotValidation.IV_RANGE,
                    modifier = Modifier.fillMaxWidth(),
                )

                StatGrid(
                    title = stringResource(R.string.hof_slot_evs_title),
                    values = mapOf(
                        StatKey.HP to fields.evHpText, StatKey.ATK to fields.evAtkText, StatKey.DEF to fields.evDefText,
                        StatKey.SP_ATK to fields.evSpAtkText, StatKey.SP_DEF to fields.evSpDefText, StatKey.SPE to fields.evSpeText,
                    ),
                    onValueChange = { key, value ->
                        fields = when (key) {
                            StatKey.HP -> fields.copy(evHpText = value)
                            StatKey.ATK -> fields.copy(evAtkText = value)
                            StatKey.DEF -> fields.copy(evDefText = value)
                            StatKey.SP_ATK -> fields.copy(evSpAtkText = value)
                            StatKey.SP_DEF -> fields.copy(evSpDefText = value)
                            StatKey.SPE -> fields.copy(evSpeText = value)
                        }
                    },
                    range = SlotValidation.EV_RANGE,
                    totalLabel = stringResource(R.string.hof_slot_ev_total, evTotal, SlotValidation.MAX_EV_TOTAL),
                    isTotalError = !isEvTotalValid,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!isEvTotalValid) {
                    Text(
                        text = stringResource(R.string.hof_slot_ev_total_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showLoadTemplateSheet) {
        LoadTemplateSheet(
            templates = templates,
            onSelected = ::applyTemplate,
            onDismiss = { showLoadTemplateSheet = false },
        )
    }

    if (showSaveAsTemplateDialog) {
        SaveAsTemplateDialog(
            initialLabel = fields.nickname.ifBlank { fields.speciesName },
            existingTemplates = templates,
            onSave = { label, overwriteId ->
                onSaveAsTemplate(fields.toSlotDraft(slotIndex), label, overwriteId)
                showSaveAsTemplateDialog = false
            },
            onDismiss = { showSaveAsTemplateDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadTemplateSheet(
    templates: List<PokemonTemplate>,
    onSelected: (PokemonTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.hof_slot_load_from_template), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.hof_slot_load_from_template_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                items(templates, key = { it.id }) { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(template) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (template.speciesId != null) {
                            PokemonSprite(
                                speciesId = template.speciesId,
                                generation = GameGeneration.OTHER,
                                shiny = template.isShiny,
                                alwaysUseLatest = false,
                                speciesGeneration = null,
                                modifier = Modifier.size(40.dp),
                            )
                        } else {
                            Box(modifier = Modifier.size(40.dp))
                        }
                        Column {
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
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveAsTemplateDialog(
    initialLabel: String,
    existingTemplates: List<PokemonTemplate>,
    onSave: (label: String, overwriteId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(initialLabel) }
    val trimmedLabel = label.trim()
    val collision = existingTemplates.firstOrNull { it.label.equals(trimmedLabel, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hof_slot_save_as_template)) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.hof_template_label_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (collision != null) {
                    Text(
                        text = stringResource(R.string.hof_template_label_collision, trimmedLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (collision != null) {
                TextButton(onClick = { onSave(trimmedLabel, collision.id) }, enabled = trimmedLabel.isNotBlank()) {
                    Text(stringResource(R.string.hof_template_overwrite))
                }
            } else {
                TextButton(onClick = { onSave(trimmedLabel, null) }, enabled = trimmedLabel.isNotBlank()) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            if (collision != null) {
                TextButton(onClick = { onSave(trimmedLabel, null) }, enabled = trimmedLabel.isNotBlank()) {
                    Text(stringResource(R.string.hof_template_save_as_copy))
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun SpeciesPicker(
    speciesId: Int?,
    speciesName: String,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<PokedexSpecies>,
    hackGeneration: GameGeneration,
    alwaysUseLatestSprites: Boolean,
    onSelected: (PokedexSpecies) -> Unit,
    onCleared: () -> Unit,
) {
    Column {
        Text(stringResource(R.string.hof_slot_species_label), style = MaterialTheme.typography.titleSmall)
        if (speciesId != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PokemonSprite(
                    speciesId = speciesId,
                    generation = hackGeneration,
                    shiny = false,
                    alwaysUseLatest = alwaysUseLatestSprites,
                    speciesGeneration = null,
                    modifier = Modifier.size(40.dp),
                )
                Text(speciesName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onCleared) { Text(stringResource(R.string.hof_slot_species_change)) }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.hof_slot_species_search_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (results.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    items(results, key = { it.id }) { species ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelected(species) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(modifier = Modifier.size(32.dp)) {
                                PokemonSprite(
                                    speciesId = species.id,
                                    generation = hackGeneration,
                                    shiny = false,
                                    alwaysUseLatest = alwaysUseLatestSprites,
                                    speciesGeneration = species.generationIntroduced,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                text = species.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenderPicker(gender: PokemonGender, onGenderChange: (PokemonGender) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.hof_slot_gender_label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(end = 8.dp))
        PokemonGender.entries.forEach { candidate ->
            val symbol = when (candidate) {
                PokemonGender.MALE -> "♂"
                PokemonGender.FEMALE -> "♀"
                PokemonGender.UNKNOWN -> "—"
            }
            val selected = gender == candidate
            TextButton(onClick = { onGenderChange(candidate) }) {
                Text(
                    text = symbol,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/** PokéAPI's raw stat name (e.g. `"special-attack"`) to the short form the spec asks for ("+Atk / −SpA"). */
private fun statAbbreviation(pokeApiStatName: String): String = when (pokeApiStatName) {
    "hp" -> "HP"
    "attack" -> "Atk"
    "defense" -> "Def"
    "special-attack" -> "SpA"
    "special-defense" -> "SpD"
    "speed" -> "Spe"
    else -> pokeApiStatName
}

@Composable
private fun NatureStatHint(nature: PokedexNature) {
    val increased = nature.increasedStat
    val decreased = nature.decreasedStat
    if (increased != null && decreased != null && increased != decreased) {
        Text(
            text = "+${statAbbreviation(increased)} / -${statAbbreviation(decreased)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T> SearchedComboBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    results: List<T>,
    onQueryChange: (String) -> Unit,
    suggestionOf: (T) -> ComboBoxSuggestion,
) {
    EditableComboBox(
        value = value,
        onValueChange = {
            onValueChange(it)
            onQueryChange(it)
        },
        label = label,
        suggestions = results.map(suggestionOf),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MoveEditor(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onSearchMoves: suspend (String) -> List<PokedexMove>,
    scope: CoroutineScope,
    results: List<PokedexMove>,
    onResults: (List<PokedexMove>) -> Unit,
) {
    EditableComboBox(
        value = value,
        onValueChange = { text ->
            onValueChange(text)
            scope.launch { onResults(if (text.isBlank()) emptyList() else onSearchMoves(text)) }
        },
        label = stringResource(R.string.hof_slot_move_label, index + 1),
        suggestions = results.map { move ->
            ComboBoxSuggestion(
                displayText = move.displayName,
                trailing = {
                    move.type?.let { type ->
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(10.dp)
                                .background(typeColorFor(type), CircleShape),
                        )
                    }
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

