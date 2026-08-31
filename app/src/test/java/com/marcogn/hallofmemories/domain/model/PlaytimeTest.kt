package com.marcogn.hallofmemories.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaytimeTest {

    @Test
    fun `H MM format parses to total minutes`() {
        assertEquals(2537, parsePlaytimeMinutes("42:17"))
    }

    @Test
    fun `HHH MM format parses correctly`() {
        assertEquals(6000, parsePlaytimeMinutes("100:00"))
    }

    @Test
    fun `a bare number is treated as hours`() {
        assertEquals(300, parsePlaytimeMinutes("5"))
    }

    @Test
    fun `blank text is unparseable`() {
        assertNull(parsePlaytimeMinutes(""))
        assertNull(parsePlaytimeMinutes("   "))
    }

    @Test
    fun `free text is unparseable, not rejected`() {
        assertNull(parsePlaytimeMinutes("forever"))
    }

    @Test
    fun `minutes over 59 are not a valid H MM value`() {
        assertNull(parsePlaytimeMinutes("1:75"))
    }

    @Test
    fun `whitespace around a valid value is trimmed`() {
        assertEquals(2537, parsePlaytimeMinutes("  42:17  "))
    }

    @Test
    fun `formatting total minutes round-trips through parsing`() {
        assertEquals("42:17", formatPlaytimeMinutes(2537))
        assertEquals(2537, parsePlaytimeMinutes(formatPlaytimeMinutes(2537)))
    }

    @Test
    fun `formatting pads a single-digit minute remainder`() {
        assertEquals("1:05", formatPlaytimeMinutes(65))
    }

    @Test
    fun `formatting a whole number of hours still shows the colon`() {
        assertEquals("5:00", formatPlaytimeMinutes(300))
    }
}
