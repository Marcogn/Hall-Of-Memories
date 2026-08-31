package com.marcogn.hallofmemories.domain.validation

/**
 * Range-only validation for a [com.marcogn.hallofmemories.domain.model.PokemonSlot] (spec §3.4).
 * No legality checks exist here or anywhere else in the app (A9) — a Magikarp holding Leftovers
 * with Hyper Beam and the wrong ability is legal input as long as every number is in range.
 *
 * Level/IV/EV bounds are informational only: the UI simply does not commit an out-of-range value
 * to the slot draft. [isEvTotalValid] is the one rule that blocks confirming a slot.
 */
object SlotValidation {
    val LEVEL_RANGE = 1..100
    val IV_RANGE = 0..31
    val EV_RANGE = 0..252
    const val MAX_EV_TOTAL = 510

    fun isLevelValid(level: Int): Boolean = level in LEVEL_RANGE
    fun isIvValid(iv: Int): Boolean = iv in IV_RANGE
    fun isEvValid(ev: Int): Boolean = ev in EV_RANGE

    fun evTotal(evHp: Int, evAtk: Int, evDef: Int, evSpAtk: Int, evSpDef: Int, evSpe: Int): Int =
        evHp + evAtk + evDef + evSpAtk + evSpDef + evSpe

    fun isEvTotalValid(total: Int): Boolean = total <= MAX_EV_TOTAL
}
