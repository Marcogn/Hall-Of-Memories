package com.marcogn.hallofmemories.domain.pokeapi

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchMergingTest {

    @Test
    fun `prefix matches come before contains matches`() {
        val merged = mergeSearchResults(
            prefixMatches = listOf("pikachu", "pichu"),
            containsMatches = listOf("raichu", "pikachu"),
            limit = 10,
            keyOf = { it },
        )

        assertEquals(listOf("pikachu", "pichu", "raichu"), merged)
    }

    @Test
    fun `duplicates across the two passes are not repeated`() {
        val merged = mergeSearchResults(
            prefixMatches = listOf("pikachu"),
            containsMatches = listOf("pikachu", "raichu"),
            limit = 10,
            keyOf = { it },
        )

        assertEquals(listOf("pikachu", "raichu"), merged)
    }

    @Test
    fun `the result never exceeds the limit`() {
        val merged = mergeSearchResults(
            prefixMatches = listOf("a", "b", "c"),
            containsMatches = listOf("d", "e"),
            limit = 2,
            keyOf = { it },
        )

        assertEquals(listOf("a", "b"), merged)
    }
}
