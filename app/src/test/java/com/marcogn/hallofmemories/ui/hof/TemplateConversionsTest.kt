package com.marcogn.hallofmemories.ui.hof

import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemplateConversionsTest {

    private val filledDraft = SlotDraft(
        slotIndex = 0,
        speciesId = 6,
        speciesName = "Charizard",
        nickname = "Torchy",
        gender = PokemonGender.MALE,
        level = 100,
        nature = "Adamant",
        ability = "Blaze",
        isShiny = true,
        heldItem = "Leftovers",
        ivHp = 31, ivAtk = 31, ivDef = 30, ivSpAtk = 1, ivSpDef = 29, ivSpe = 31,
        evHp = 4, evAtk = 252, evDef = 0, evSpAtk = 0, evSpDef = 0, evSpe = 252,
        move1 = "Flamethrower", move2 = "Dragon Claw", move3 = "Earthquake", move4 = "Roost",
        sourceTemplateId = "some-other-template",
    )

    @Test
    fun `toTemplate keeps every payload field`() {
        val template = filledDraft.toTemplate(
            id = "template-1",
            label = "Competitive Charizard",
            createdAt = Instant.ofEpochMilli(1_000),
            updatedAt = Instant.ofEpochMilli(2_000),
        )

        assertEquals("template-1", template.id)
        assertEquals("Competitive Charizard", template.label)
        assertEquals(6, template.speciesId)
        assertEquals("Charizard", template.speciesName)
        assertEquals("Torchy", template.nickname)
        assertEquals(PokemonGender.MALE, template.gender)
        assertEquals(100, template.level)
        assertEquals("Adamant", template.nature)
        assertEquals("Blaze", template.ability)
        assertEquals(true, template.isShiny)
        assertEquals("Leftovers", template.heldItem)
        assertEquals(31, template.ivHp); assertEquals(31, template.ivAtk); assertEquals(30, template.ivDef)
        assertEquals(1, template.ivSpAtk); assertEquals(29, template.ivSpDef); assertEquals(31, template.ivSpe)
        assertEquals(4, template.evHp); assertEquals(252, template.evAtk); assertEquals(0, template.evDef)
        assertEquals(0, template.evSpAtk); assertEquals(0, template.evSpDef); assertEquals(252, template.evSpe)
        assertEquals("Flamethrower", template.move1); assertEquals("Dragon Claw", template.move2)
        assertEquals("Earthquake", template.move3); assertEquals("Roost", template.move4)
        assertEquals(Instant.ofEpochMilli(1_000), template.createdAt)
        assertEquals(Instant.ofEpochMilli(2_000), template.updatedAt)
    }

    @Test
    fun `blank optional fields become null`() {
        val blankDraft = SlotDraft.empty(0).copy(nickname = "   ", nature = "", ability = "  ")
        val template = blankDraft.toTemplate("t", "label", Instant.EPOCH, Instant.EPOCH)

        assertNull(template.nickname)
        assertNull(template.nature)
        assertNull(template.ability)
    }

    @Test
    fun `toSlotDraft sets sourceTemplateId to the template's own id and keeps every payload field`() {
        val template = PokemonTemplate(
            id = "template-9",
            label = "Competitive Garchomp",
            speciesId = 445,
            speciesName = "garchomp",
            nickname = null,
            gender = PokemonGender.FEMALE,
            level = 100,
            nature = "jolly",
            ability = "rough-skin",
            isShiny = false,
            heldItem = "choice-scarf",
            ivHp = 31, ivAtk = 31, ivDef = 31, ivSpAtk = 0, ivSpDef = 31, ivSpe = 31,
            evHp = 0, evAtk = 252, evDef = 0, evSpAtk = 0, evSpDef = 4, evSpe = 252,
            move1 = "earthquake", move2 = "dragon-claw", move3 = "stone-edge", move4 = "swords-dance",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

        val draft = template.toSlotDraft(slotIndex = 3)

        assertEquals(3, draft.slotIndex)
        assertEquals("template-9", draft.sourceTemplateId)
        assertEquals(445, draft.speciesId)
        assertEquals("garchomp", draft.speciesName)
        assertEquals("", draft.nickname)
        assertEquals(PokemonGender.FEMALE, draft.gender)
        assertEquals(100, draft.level)
        assertEquals("jolly", draft.nature)
        assertEquals("rough-skin", draft.ability)
        assertEquals(false, draft.isShiny)
        assertEquals("choice-scarf", draft.heldItem)
        assertEquals(31, draft.ivHp); assertEquals(31, draft.ivAtk); assertEquals(31, draft.ivDef)
        assertEquals(0, draft.ivSpAtk); assertEquals(31, draft.ivSpDef); assertEquals(31, draft.ivSpe)
        assertEquals(0, draft.evHp); assertEquals(252, draft.evAtk); assertEquals(0, draft.evDef)
        assertEquals(0, draft.evSpAtk); assertEquals(4, draft.evSpDef); assertEquals(252, draft.evSpe)
        assertEquals("earthquake", draft.move1); assertEquals("dragon-claw", draft.move2)
        assertEquals("stone-edge", draft.move3); assertEquals("swords-dance", draft.move4)
    }
}
