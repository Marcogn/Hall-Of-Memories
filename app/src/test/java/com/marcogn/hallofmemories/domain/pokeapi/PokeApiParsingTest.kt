package com.marcogn.hallofmemories.domain.pokeapi

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/pokeapi/$name")) { "Missing fixture $name" }
        .bufferedReader()
        .readText()

class PokeApiParsingTest {

    @Test
    fun `parseIndex resolves names and ids from the real url shape`() {
        val entries = parseIndex(fixture("index_sample.json"))

        assertEquals(
            listOf(
                IndexEntry("bulbasaur", 1),
                IndexEntry("charmander", 4),
                IndexEntry("mr-mime", 122),
            ),
            entries,
        )
    }

    @Test
    fun `extractIdFromUrl resolves the trailing numeric segment`() {
        assertEquals(6, extractIdFromUrl("/api/v2/pokemon/6/"))
        assertEquals(6, extractIdFromUrl("/api/v2/pokemon/6"))
    }

    @Test
    fun `extractIdFromUrl returns null for garbage`() {
        assertEquals(null, extractIdFromUrl(null))
        assertEquals(null, extractIdFromUrl(""))
        assertEquals(null, extractIdFromUrl("/api/v2/pokemon/bulbasaur/"))
        assertEquals(null, extractIdFromUrl("not a url at all"))
    }

    @Test
    fun `prettify splits on hyphens and capitalizes each part`() {
        assertEquals("Mr Mime", prettify("mr-mime"))
        assertEquals("Fire Punch", prettify("fire-punch"))
        assertEquals("Bulbasaur", prettify("bulbasaur"))
    }

    @Test
    fun `searchKey strips separators and case`() {
        assertEquals("mrmime", searchKey("mr-mime"))
        assertEquals("mrmime", searchKey("Mr Mime"))
    }

    @Test
    fun `parseTypeDetail produces species slots and move ids`() {
        val detail = parseTypeDetail(fixture("type_detail_electric.json"))

        assertEquals("electric", detail.typeName)
        assertEquals(listOf(25 to 1, 479 to 2), detail.speciesSlots)
        assertEquals(listOf(84, 85), detail.moveIds)
    }

    @Test
    fun `parseNatureDetail handles a neutral nature with explicit null stats`() {
        val nature = parseNatureDetail(fixture("nature_hardy_neutral.json"))

        assertEquals(1, nature.id)
        assertEquals("hardy", nature.name)
        assertEquals("Hardy", nature.displayName)
        assertEquals(null, nature.increasedStat)
        assertEquals(null, nature.decreasedStat)
    }

    @Test
    fun `parseNatureDetail reads a non-neutral nature's stats`() {
        val nature = parseNatureDetail(fixture("nature_lonely.json"))

        assertEquals("lonely", nature.name)
        assertEquals("attack", nature.increasedStat)
        assertEquals("defense", nature.decreasedStat)
    }

    @Test
    fun `parseGenerationDetail reads the generation number and species names`() {
        val detail = parseGenerationDetail(fixture("generation_iii.json"))

        assertEquals(3, detail.number)
        assertEquals(listOf("treecko", "torchic"), detail.speciesNames)
    }

    @Test
    fun `malformed json throws rather than returning an empty result`() {
        assertThrows(SerializationException::class.java) {
            parseIndex("{ not valid json")
        }
    }

    @Test
    fun `an index entry with an unresolvable url is dropped, not crashed on`() {
        val entries = parseIndex(
            """{"results":[{"name":"good","url":"/api/v2/pokemon/1/"},{"name":"bad","url":"/api/v2/pokemon/oops/"}]}""",
        )

        assertEquals(listOf(IndexEntry("good", 1)), entries)
        assertTrue(entries.none { it.name == "bad" })
    }
}
