package com.marcogn.hallofmemories.domain.filter

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class HackFiltersTest {

    private fun hack(name: String, generation: GameGeneration = GameGeneration.OTHER) = HackWithEntryCount(
        hack = Hack(
            id = name,
            name = name,
            generation = generation,
            baseGameTitle = null,
            boxArtPath = null,
            boxArtUrl = null,
            logoPath = null,
            logoUrl = null,
            theGamesDbId = null,
            notes = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        ),
        entryCount = 0,
    )

    private val hacks = listOf(
        hack("Radical Red", GameGeneration.FRLG),
        hack("Pokémon Emerald", GameGeneration.RSE),
        hack("Crystal Clear", GameGeneration.GSC),
    )

    @Test
    fun `an empty query returns everything in the original order`() {
        assertEquals(hacks, filterHacks(hacks, query = "", generations = emptySet()))
    }

    @Test
    fun `query matching is case-insensitive`() {
        val result = filterHacks(hacks, query = "RADICAL", generations = emptySet())

        assertEquals(listOf("Radical Red"), result.map { it.hack.name })
    }

    @Test
    fun `query matching is accent-insensitive`() {
        val result = filterHacks(hacks, query = "pokemon", generations = emptySet())

        assertEquals(listOf("Pokémon Emerald"), result.map { it.hack.name })
    }

    @Test
    fun `generation filter keeps only the selected generations`() {
        val result = filterHacks(hacks, query = "", generations = setOf(GameGeneration.RSE, GameGeneration.GSC))

        assertEquals(listOf("Pokémon Emerald", "Crystal Clear"), result.map { it.hack.name })
    }

    @Test
    fun `query and generation filter combine with AND`() {
        val result = filterHacks(hacks, query = "clear", generations = setOf(GameGeneration.RSE))

        assertEquals(emptyList<HackWithEntryCount>(), result)
    }

    @Test
    fun `query and generation filter both matching returns the entry`() {
        val result = filterHacks(hacks, query = "crystal", generations = setOf(GameGeneration.GSC))

        assertEquals(listOf("Crystal Clear"), result.map { it.hack.name })
    }
}
