package com.marcogn.hallofmemories.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotValidationTest {

    @Test
    fun `level bounds`() {
        assertFalse(SlotValidation.isLevelValid(0))
        assertTrue(SlotValidation.isLevelValid(1))
        assertTrue(SlotValidation.isLevelValid(100))
        assertFalse(SlotValidation.isLevelValid(101))
    }

    @Test
    fun `IV bounds`() {
        assertFalse(SlotValidation.isIvValid(-1))
        assertTrue(SlotValidation.isIvValid(0))
        assertTrue(SlotValidation.isIvValid(31))
        assertFalse(SlotValidation.isIvValid(32))
    }

    @Test
    fun `EV per-stat bounds`() {
        assertFalse(SlotValidation.isEvValid(-1))
        assertTrue(SlotValidation.isEvValid(0))
        assertTrue(SlotValidation.isEvValid(252))
        assertFalse(SlotValidation.isEvValid(253))
    }

    @Test
    fun `EV total at the 510 boundary is valid, one over is not`() {
        assertTrue(SlotValidation.isEvTotalValid(510))
        assertFalse(SlotValidation.isEvTotalValid(511))
    }

    @Test
    fun `evTotal sums all six stats`() {
        assertEquals(508, SlotValidation.evTotal(4, 252, 0, 0, 0, 252))
    }

    @Test
    fun `a legality-nonsense but range-valid slot has no failing check`() {
        // No move/ability/nature legality function exists anywhere in domain/validation — a
        // Magikarp with Hyper Beam and the wrong ability is simply never checked. Only the
        // numeric bounds above exist, and this combination passes every one of them.
        assertTrue(SlotValidation.isLevelValid(100))
        assertTrue(SlotValidation.isIvValid(31))
        assertTrue(SlotValidation.isEvValid(252))
        assertTrue(SlotValidation.isEvTotalValid(252))
    }
}
