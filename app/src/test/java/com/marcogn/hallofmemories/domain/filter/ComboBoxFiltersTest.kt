package com.marcogn.hallofmemories.domain.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComboBoxFiltersTest {

    private val items = listOf("adamant", "jolly", "bold", "modest", "timid", "quirky")

    @Test
    fun `blank query yields no suggestions`() {
        assertTrue(filterComboBoxSuggestions("", items) { it }.isEmpty())
        assertTrue(filterComboBoxSuggestions("   ", items) { it }.isEmpty())
    }

    @Test
    fun `prefix matches come before contains matches`() {
        val result = filterComboBoxSuggestions("mod", items) { it }
        assertEquals(listOf("modest"), result)
    }

    @Test
    fun `contains matches are included after prefix matches`() {
        // "im" is a prefix of nothing here but appears inside "timid"
        val result = filterComboBoxSuggestions("im", items) { it }
        assertEquals(listOf("timid"), result)
    }

    @Test
    fun `is case-insensitive`() {
        assertEquals(listOf("adamant"), filterComboBoxSuggestions("ADA", items) { it })
    }

    @Test
    fun `caps results at the given limit`() {
        val many = (1..50).map { "move-$it" }
        val result = filterComboBoxSuggestions("move", many, limit = 30) { it }
        assertEquals(30, result.size)
    }
}
