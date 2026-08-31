package com.marcogn.hallofmemories.data.repository

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun `Hack round-trips through its entity unchanged`() {
        val hack = Hack(
            id = "hack-1",
            name = "Radical Red",
            generation = GameGeneration.FRLG,
            baseGameTitle = "Pokémon FireRed Version",
            boxArtPath = "/data/images/box.jpg",
            boxArtUrl = "https://example.com/box.jpg",
            logoPath = "/data/images/logo.png",
            logoUrl = "https://example.com/logo.png",
            theGamesDbId = 12345L,
            notes = "A tough ROM hack",
            createdAt = Instant.ofEpochMilli(1_000),
            updatedAt = Instant.ofEpochMilli(2_000),
        )

        assertEquals(hack, hack.toEntity().toDomain())
    }

    @Test
    fun `a Hack with every optional field null round-trips unchanged`() {
        val hack = Hack(
            id = "hack-2",
            name = "Pokémon Emerald",
            generation = GameGeneration.RSE,
            baseGameTitle = null,
            boxArtPath = null,
            boxArtUrl = null,
            logoPath = null,
            logoUrl = null,
            theGamesDbId = null,
            notes = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

        assertEquals(hack, hack.toEntity().toDomain())
    }

    @Test
    fun `HallOfFameEntry round-trips through its entity unchanged`() {
        val entry = HallOfFameEntry(
            id = "entry-1",
            hackId = "hack-1",
            playerName = "Ash",
            playerId = "12345",
            playtimeText = "42:17",
            playtimeMinutes = 2537,
            screenshotPath = "/data/images/shot.jpg",
            insertedAt = Instant.ofEpochMilli(3_000),
            notes = "First playthrough",
            createdAt = Instant.ofEpochMilli(1_000),
            updatedAt = Instant.ofEpochMilli(2_000),
        )

        assertEquals(entry, entry.toEntity().toDomain())
    }

    @Test
    fun `an empty PokemonSlot round-trips with every nullable field null`() {
        val slot = PokemonSlot.empty(id = "slot-1", entryId = "entry-1", slotIndex = 3)

        assertEquals(slot, slot.toEntity().toDomain())
    }

    @Test
    fun `a fully-filled PokemonSlot round-trips including every IV EV and move`() {
        val slot = PokemonSlot(
            id = "slot-0",
            entryId = "entry-1",
            slotIndex = 0,
            speciesId = 6,
            speciesName = "charizard",
            nickname = "Torchy",
            gender = PokemonGender.MALE,
            level = 100,
            nature = "adamant",
            ability = "blaze",
            isShiny = true,
            heldItem = "leftovers",
            ivHp = 31, ivAtk = 31, ivDef = 30, ivSpAtk = 1, ivSpDef = 29, ivSpe = 31,
            evHp = 4, evAtk = 252, evDef = 0, evSpAtk = 0, evSpDef = 0, evSpe = 252,
            move1 = "flamethrower", move2 = "dragon-claw", move3 = "earthquake", move4 = "roost",
            sourceTemplateId = "template-9",
        )

        assertEquals(slot, slot.toEntity().toDomain())
    }

    @Test
    fun `PokemonTemplate round-trips through its entity unchanged`() {
        val template = PokemonTemplate(
            id = "template-1",
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
            createdAt = Instant.ofEpochMilli(1_000),
            updatedAt = Instant.ofEpochMilli(2_000),
        )

        assertEquals(template, template.toEntity().toDomain())
    }
}
