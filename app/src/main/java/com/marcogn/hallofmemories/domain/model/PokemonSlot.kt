package com.marcogn.hallofmemories.domain.model

/**
 * One of a Hall of Fame entry's six team slots. `speciesId == null` means the slot is empty; the
 * row still exists (spec §2) so `(entryId, slotIndex)` stays stable across edits.
 *
 * [speciesName]/[nature]/[ability]/[heldItem]/[move1]..[move4] are **denormalized snapshots**,
 * never references into the PokéAPI cache — see `docs/implementation-decisions.md`, "The PokéAPI
 * cache is a typing aid, never a dependency of user data". [sourceTemplateId] is provenance
 * metadata only: it carries no foreign key, so deleting the template it points to never affects
 * this slot.
 */
data class PokemonSlot(
    val id: String,
    val entryId: String,
    val slotIndex: Int,
    val speciesId: Int?,
    val speciesName: String?,
    val nickname: String?,
    val gender: PokemonGender,
    val level: Int?,
    val nature: String?,
    val ability: String?,
    val isShiny: Boolean,
    val heldItem: String?,
    val ivHp: Int?,
    val ivAtk: Int?,
    val ivDef: Int?,
    val ivSpAtk: Int?,
    val ivSpDef: Int?,
    val ivSpe: Int?,
    val evHp: Int?,
    val evAtk: Int?,
    val evDef: Int?,
    val evSpAtk: Int?,
    val evSpDef: Int?,
    val evSpe: Int?,
    val move1: String?,
    val move2: String?,
    val move3: String?,
    val move4: String?,
    val sourceTemplateId: String?,
) {
    companion object {
        /** An untouched slot at [entryId]/[slotIndex] — every field empty/default. */
        fun empty(id: String, entryId: String, slotIndex: Int): PokemonSlot = PokemonSlot(
            id = id,
            entryId = entryId,
            slotIndex = slotIndex,
            speciesId = null,
            speciesName = null,
            nickname = null,
            gender = PokemonGender.UNKNOWN,
            level = null,
            nature = null,
            ability = null,
            isShiny = false,
            heldItem = null,
            ivHp = null, ivAtk = null, ivDef = null, ivSpAtk = null, ivSpDef = null, ivSpe = null,
            evHp = null, evAtk = null, evDef = null, evSpAtk = null, evSpDef = null, evSpe = null,
            move1 = null, move2 = null, move3 = null, move4 = null,
            sourceTemplateId = null,
        )
    }
}
