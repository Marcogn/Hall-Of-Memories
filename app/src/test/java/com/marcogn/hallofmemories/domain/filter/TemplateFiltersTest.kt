package com.marcogn.hallofmemories.domain.filter

import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateFiltersTest {

    private fun template(id: String, label: String, speciesName: String?) = PokemonTemplate(
        id = id, label = label, speciesId = null, speciesName = speciesName, nickname = null,
        gender = PokemonGender.UNKNOWN, level = null, nature = null, ability = null, isShiny = false,
        heldItem = null, ivHp = null, ivAtk = null, ivDef = null, ivSpAtk = null, ivSpDef = null, ivSpe = null,
        evHp = null, evAtk = null, evDef = null, evSpAtk = null, evSpDef = null, evSpe = null,
        move1 = null, move2 = null, move3 = null, move4 = null,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    private val templates = listOf(
        template("1", "Competitive Garchomp", "garchomp"),
        template("2", "Physical wall", "pokemon"),
        template("3", "Special sweeper", null),
    )

    @Test
    fun `blank query returns everything`() {
        assertEquals(3, filterTemplates(templates, "").size)
        assertEquals(3, filterTemplates(templates, "   ").size)
    }

    @Test
    fun `matches by label`() {
        assertEquals(listOf("1"), filterTemplates(templates, "garchomp").map { it.id })
    }

    @Test
    fun `matches by species name even when label differs`() {
        assertEquals(listOf("2"), filterTemplates(templates, "pokemon").map { it.id })
    }

    @Test
    fun `is accent and case insensitive`() {
        assertEquals(listOf("2"), filterTemplates(templates, "POKÉMON").map { it.id })
    }

    @Test
    fun `a template with no species name never matches on species`() {
        assertEquals(emptyList<String>(), filterTemplates(templates, "nonexistent").map { it.id })
    }
}
