package com.marcogn.hallofmemories.ui.hof

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.PokedexAbility
import com.marcogn.hallofmemories.domain.model.PokedexNature
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import java.time.Instant

/**
 * One of the six team slots while being edited in [HofFormViewModel] — the in-memory mirror of
 * [PokemonSlot] before it has an `id`/`entryId` (assigned only at save time). [speciesId] `null`
 * means the slot is empty, same convention as the saved row (spec §2).
 */
data class SlotDraft(
    val slotIndex: Int,
    val speciesId: Int? = null,
    val speciesName: String? = null,
    val nickname: String = "",
    val gender: PokemonGender = PokemonGender.UNKNOWN,
    val level: Int? = null,
    val nature: String = "",
    val ability: String = "",
    val isShiny: Boolean = false,
    val heldItem: String = "",
    val ivHp: Int? = null,
    val ivAtk: Int? = null,
    val ivDef: Int? = null,
    val ivSpAtk: Int? = null,
    val ivSpDef: Int? = null,
    val ivSpe: Int? = null,
    val evHp: Int? = null,
    val evAtk: Int? = null,
    val evDef: Int? = null,
    val evSpAtk: Int? = null,
    val evSpDef: Int? = null,
    val evSpe: Int? = null,
    val move1: String = "",
    val move2: String = "",
    val move3: String = "",
    val move4: String = "",
    val sourceTemplateId: String? = null,
) {
    val isEmpty: Boolean get() = speciesId == null

    companion object {
        fun empty(slotIndex: Int) = SlotDraft(slotIndex = slotIndex)
    }
}

fun PokemonSlot.toDraft(): SlotDraft = SlotDraft(
    slotIndex = slotIndex,
    speciesId = speciesId,
    speciesName = speciesName,
    nickname = nickname.orEmpty(),
    gender = gender,
    level = level,
    nature = nature.orEmpty(),
    ability = ability.orEmpty(),
    isShiny = isShiny,
    heldItem = heldItem.orEmpty(),
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1.orEmpty(), move2 = move2.orEmpty(), move3 = move3.orEmpty(), move4 = move4.orEmpty(),
    sourceTemplateId = sourceTemplateId,
)

/** [id] is preserved from the loaded slot when editing, or freshly generated for a new entry — see `HofFormViewModel.save()`. */
fun SlotDraft.toDomain(id: String, entryId: String): PokemonSlot = PokemonSlot(
    id = id,
    entryId = entryId,
    slotIndex = slotIndex,
    speciesId = speciesId,
    speciesName = speciesName,
    nickname = nickname.trim().takeIf { it.isNotBlank() },
    gender = gender,
    level = level,
    nature = nature.trim().takeIf { it.isNotBlank() },
    ability = ability.trim().takeIf { it.isNotBlank() },
    isShiny = isShiny,
    heldItem = heldItem.trim().takeIf { it.isNotBlank() },
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1.trim().takeIf { it.isNotBlank() },
    move2 = move2.trim().takeIf { it.isNotBlank() },
    move3 = move3.trim().takeIf { it.isNotBlank() },
    move4 = move4.trim().takeIf { it.isNotBlank() },
    sourceTemplateId = sourceTemplateId,
)

data class HofFormDraft(
    val playerName: String = "",
    val playerId: String = "",
    val playtimeText: String = "",
    val screenshotPath: String? = null,
    val insertedAt: Instant = Instant.now(),
    val notes: String = "",
    val slots: List<SlotDraft> = (0..5).map { SlotDraft.empty(it) },
)

data class HofFormUiState(
    val draft: HofFormDraft = HofFormDraft(),
    val hackGeneration: GameGeneration = GameGeneration.OTHER,
    val alwaysUseLatestSprites: Boolean = false,
    val natures: List<PokedexNature> = emptyList(),
    val abilities: List<PokedexAbility> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Natures/abilities only populate once the pokédex sync has run at least once (spec §3.3, "when the cache is empty"). */
    val isPokedexEmpty: Boolean get() = natures.isEmpty() && abilities.isEmpty()
}
