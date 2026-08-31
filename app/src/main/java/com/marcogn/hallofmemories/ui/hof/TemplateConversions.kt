package com.marcogn.hallofmemories.ui.hof

import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import java.time.Instant

/**
 * The whole of Phase 4's "reusable Pokémon" feature is these two conversions (plus persistence,
 * which the caller owns — see `SlotEditorDialog`'s "Save as template"/"Load from template").
 * Pure, no Android import, despite living next to [SlotDraft] rather than under `domain/model/` —
 * see `docs/implementation-decisions.md`, "Template conversions live beside SlotDraft, not in
 * domain/model".
 */
fun SlotDraft.toTemplate(id: String, label: String, createdAt: Instant, updatedAt: Instant): PokemonTemplate = PokemonTemplate(
    id = id,
    label = label,
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
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** Sets [SlotDraft.sourceTemplateId] to this template's id — provenance only, no foreign key (spec §2). */
fun PokemonTemplate.toSlotDraft(slotIndex: Int): SlotDraft = SlotDraft(
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
    sourceTemplateId = id,
)
