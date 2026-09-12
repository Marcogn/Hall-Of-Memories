package com.marcogn.hallofmemories.domain.showdown

import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verified against Pokémon Showdown's own grammar (`sim/teams.ts`'s `exportSet`/
 * `parseExportedTeamLine` in smogon/pokemon-showdown), not assumed — see
 * `docs/implementation-decisions.md`, "Showdown format import/export".
 */
class ShowdownFormatTest {

    // ---- parseShowdownSet ----

    @Test
    fun `parses a full set with every optional field`() {
        val paste = listOf(
            "Volt Turtle (Pikachu) (F) @ Light Ball",
            "Ability: Static",
            "Level: 50",
            "Shiny: Yes",
            "EVs: 252 SpA / 4 SpD / 252 Spe",
            "Timid Nature",
            "IVs: 0 Atk",
            "- Thunderbolt",
            "- Volt Tackle",
            "- Iron Tail",
            "- Knock Off",
        ).joinToString("\n")
        val set = parseShowdownSet(paste)
        assertEquals("Volt Turtle", set.nickname)
        assertEquals("Pikachu", set.speciesName)
        assertEquals(PokemonGender.FEMALE, set.gender)
        assertEquals("Light Ball", set.item)
        assertEquals("Static", set.ability)
        assertEquals(50, set.level)
        assertTrue(set.isShiny)
        assertEquals("Timid", set.nature)
        assertEquals(252, set.evSpAtk)
        assertEquals(4, set.evSpDef)
        assertEquals(252, set.evSpe)
        assertEquals(0, set.evHp)
        assertEquals(0, set.ivAtk)
        assertEquals(31, set.ivHp) // untouched IVs default to 31
        assertEquals(listOf("Thunderbolt", "Volt Tackle", "Iron Tail", "Knock Off"), set.moves)
    }

    @Test
    fun `missing optional fields default exactly like the real client`() {
        val set = parseShowdownSet("Pikachu\n- Thunderbolt")
        assertNull(set.nickname)
        assertEquals("Pikachu", set.speciesName)
        assertEquals(PokemonGender.UNKNOWN, set.gender)
        assertNull(set.item)
        assertNull(set.ability)
        assertEquals(100, set.level) // omitted Level: means level 100, not "unknown"
        assertEquals(false, set.isShiny)
        assertNull(set.nature)
        assertEquals(0, set.evHp)
        assertEquals(31, set.ivHp)
        assertEquals(listOf("Thunderbolt"), set.moves)
    }

    @Test
    fun `a Level, Tera Type or Shiny line never corrupts the species`() {
        // Regression coverage for the exact bug fixed in CoverDex's own parser (see its
        // implementation-decisions.md, "Showdown format compatibility") — built correctly here
        // from the start by only ever treating the block's first line as the species line.
        val paste = listOf(
            "Charizard @ Choice Scarf",
            "Ability: Blaze",
            "Level: 50",
            "Tera Type: Water",
            "Shiny: Yes",
            "Happiness: 0",
            "Pokeball: Friend Ball",
            "Hidden Power: Ice",
            "Dynamax Level: 10",
            "Gigantamax: Yes",
            "- Flamethrower",
        ).joinToString("\n")
        val set = parseShowdownSet(paste)
        assertEquals("Charizard", set.speciesName)
        assertEquals("Blaze", set.ability)
        assertEquals("Choice Scarf", set.item)
        assertEquals(50, set.level)
        assertTrue(set.isShiny)
    }

    @Test
    fun `an unrecognized non-first line is ignored rather than overwriting the species`() {
        val set = parseShowdownSet("Pikachu @ Light Ball\ntotally unrecognized junk\n- Thunderbolt")
        assertEquals("Pikachu", set.speciesName)
    }

    @Test
    fun `strips a trailing gender marker with no nickname present`() {
        val set = parseShowdownSet("Pikachu (M) @ Light Ball")
        assertEquals("Pikachu", set.speciesName)
        assertEquals(PokemonGender.MALE, set.gender)
        assertNull(set.nickname)
    }

    @Test
    fun `Trait line is a legacy alias for Ability`() {
        val set = parseShowdownSet("Charizard\nTrait: Blaze\n- Flamethrower")
        assertEquals("Blaze", set.ability)
    }

    @Test
    fun `Hidden Power move brackets are normalized to plain text`() {
        val set = parseShowdownSet("Pikachu\n- Hidden Power [Ice]")
        assertEquals(listOf("Hidden Power Ice"), set.moves)
    }

    @Test
    fun `EV and IV stat aliases all resolve to the same six stats`() {
        val set = parseShowdownSet(
            listOf(
                "Pikachu",
                "EVs: 4 HP / 252 SpAtk / 252 Speed",
                "IVs: 0 Attack / 30 Special Defense",
            ).joinToString("\n"),
        )
        assertEquals(4, set.evHp)
        assertEquals(252, set.evSpAtk)
        assertEquals(252, set.evSpe)
        assertEquals(0, set.ivAtk)
        assertEquals(30, set.ivSpDef)
    }

    @Test
    fun `more than four moves keeps only the first four`() {
        val set = parseShowdownSet(
            listOf("Pikachu", "- A", "- B", "- C", "- D", "- E").joinToString("\n"),
        )
        assertEquals(listOf("A", "B", "C", "D"), set.moves)
    }

    @Test
    fun `handles empty text without crashing`() {
        assertEquals(emptyList<ParsedShowdownSet>(), parseShowdownTeam(""))
    }

    @Test
    fun `parseShowdownTeam splits a blank-line-separated multi-set paste`() {
        val text = listOf(
            "Pikachu\n- Thunderbolt",
            "Charizard\n- Flamethrower",
        ).joinToString("\n\n")
        val sets = parseShowdownTeam(text)
        assertEquals(listOf("Pikachu", "Charizard"), sets.map { it.speciesName })
    }

    // ---- exportSlotToShowdown ----

    private fun emptySlot() = PokemonSlot.empty(id = "id", entryId = "entry", slotIndex = 0)

    @Test
    fun `exporting an empty slot returns null, nothing to export`() {
        assertNull(exportSlotToShowdown(emptySlot()))
    }

    @Test
    fun `omits Ability, Level, EVs, Nature and IVs lines entirely when at their defaults`() {
        val slot = emptySlot().copy(speciesId = 25, speciesName = "Pikachu", level = 100)
        val out = exportSlotToShowdown(slot)!!
        assertEquals("Pikachu", out.lines().first())
        assertTrue(out.lines().none { it.startsWith("Ability:") })
        assertTrue(out.lines().none { it.startsWith("Level:") })
        assertTrue(out.lines().none { it.startsWith("EVs:") })
        assertTrue(out.lines().none { it.startsWith("IVs:") })
        assertTrue(out.lines().none { it.endsWith(" Nature") })
    }

    @Test
    fun `writes a nickname wrapper, gender suffix and item line`() {
        val slot = emptySlot().copy(
            speciesId = 25, speciesName = "Pikachu", nickname = "Sparky",
            gender = PokemonGender.FEMALE, heldItem = "Light Ball",
        )
        val out = exportSlotToShowdown(slot)!!
        assertEquals("Sparky (Pikachu) (F) @ Light Ball", out.lines().first())
    }

    @Test
    fun `writes EVs, IVs and nature lines only for non-default stats`() {
        val slot = emptySlot().copy(
            speciesId = 25, speciesName = "Pikachu", nature = "Timid",
            evSpAtk = 252, evSpe = 252, evHp = 0,
            ivAtk = 0, ivHp = 31,
        )
        val out = exportSlotToShowdown(slot)!!
        assertTrue(out.lines().any { it == "EVs: 252 SpA / 252 Spe" })
        assertTrue(out.lines().any { it == "Timid Nature" })
        assertTrue(out.lines().any { it == "IVs: 0 Atk" })
    }

    @Test
    fun `round-trip export then re-import preserves every field`() {
        val original = emptySlot().copy(
            speciesId = 6, speciesName = "Charizard", nickname = "Firebird",
            gender = PokemonGender.MALE, heldItem = "Charcoal", ability = "Blaze",
            level = 50, isShiny = true, nature = "Modest",
            evSpAtk = 252, evSpe = 252, evHp = 4,
            move1 = "Flamethrower", move2 = "Earthquake",
        )
        val text = exportSlotToShowdown(original)!!
        val reparsed = parseShowdownSet(text)
        assertEquals("Firebird", reparsed.nickname)
        assertEquals("Charizard", reparsed.speciesName)
        assertEquals(PokemonGender.MALE, reparsed.gender)
        assertEquals("Charcoal", reparsed.item)
        assertEquals("Blaze", reparsed.ability)
        assertEquals(50, reparsed.level)
        assertTrue(reparsed.isShiny)
        assertEquals("Modest", reparsed.nature)
        assertEquals(252, reparsed.evSpAtk)
        assertEquals(listOf("Flamethrower", "Earthquake"), reparsed.moves)
    }

    @Test
    fun `exportSlotsToShowdown skips empty slots and blank-line-separates the rest`() {
        val filled1 = emptySlot().copy(speciesId = 25, speciesName = "Pikachu")
        val empty = emptySlot()
        val filled2 = emptySlot().copy(speciesId = 6, speciesName = "Charizard")
        val out = exportSlotsToShowdown(listOf(filled1, empty, filled2))
        assertEquals(2, out.split(Regex("\n\\s*\n")).size)
    }
}
