package com.marcogn.hallofmemories.domain.thegamesdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/thegamesdb/$name")) { "Missing fixture $name" }
        .bufferedReader()
        .readText()

class TheGamesDbResponseParsingTest {

    private val platformNamesById = mapOf(4L to "Game Boy Advance")

    @Test
    fun `parseSearchResults reads every game, including one with explicit null genres`() {
        val results = parseSearchResults(fixture("by_game_name.json"), platformNamesById)

        assertEquals(2, results.size)
        val fireRed = results.first { it.externalId == 1004L }
        assertEquals("Pokemon FireRed Version", fireRed.title)
        assertEquals("Game Boy Advance", fireRed.platformName)
        assertEquals(2004, fireRed.releaseYear)
        assertEquals(
            "https://cdn.thegamesdb.net/images/original/boxart/front/1004-1.jpg",
            fireRed.boxArtUrl,
        )
        assertEquals(
            "https://cdn.thegamesdb.net/images/thumb/boxart/front/1004-1.jpg",
            fireRed.boxArtThumbnailUrl,
        )
    }

    @Test
    fun `parseSearchResults falls back to any boxart when no front side is tagged`() {
        val results = parseSearchResults(fixture("by_game_name.json"), platformNamesById)

        val leafGreen = results.first { it.externalId == 1005L }
        assertEquals(
            "https://cdn.thegamesdb.net/images/original/boxart/back/1005-3.jpg",
            leafGreen.boxArtUrl,
        )
    }

    @Test
    fun `parseGameImages prefers clearlogo over banner and fanart, and the front boxart`() {
        val images = parseGameImages(fixture("games_images_with_clearlogo.json"), gameId = 1004)

        assertEquals("https://cdn.thegamesdb.net/images/original/clearlogo/1004.png", images.logoUrl)
        assertEquals("https://cdn.thegamesdb.net/images/original/boxart/front/1004-1.jpg", images.boxArtUrl)
    }

    @Test
    fun `parseGameImages falls back to fanart when clearlogo and banner are absent`() {
        val images = parseGameImages(fixture("games_images_no_clearlogo.json"), gameId = 1005)

        assertEquals("https://cdn.thegamesdb.net/images/original/fanart/1005.jpg", images.logoUrl)
        assertEquals("https://cdn.thegamesdb.net/images/original/boxart/back/1005-3.jpg", images.boxArtUrl)
    }

    @Test
    fun `parseGameImages has no logo when clearlogo, banner and fanart are all absent`() {
        val images = parseGameImages(fixture("games_images_no_logo_at_all.json"), gameId = 1006)

        assertNull(images.logoUrl)
        assertEquals("https://cdn.thegamesdb.net/images/original/boxart/front/1006-1.jpg", images.boxArtUrl)
    }

    @Test
    fun `parseGameImages degrades to nulls rather than throwing on malformed json`() {
        val images = parseGameImages("{ not json", gameId = 1)

        assertNull(images.boxArtUrl)
        assertNull(images.logoUrl)
    }

    @Test
    fun `parseLookupTable reads an id-to-name map`() {
        val json = """{"data":{"platforms":{"4":{"id":4,"name":"Game Boy Advance"},"7":{"id":7,"name":"Nintendo DS"}}}}"""

        val platforms = parseLookupTable(json, "platforms")

        assertEquals(mapOf(4L to "Game Boy Advance", 7L to "Nintendo DS"), platforms)
    }

    @Test
    fun `bestPlatformMatch prefers an exact case-insensitive match over a partial one`() {
        val platforms = mapOf(4L to "Game Boy Advance", 5L to "Game Boy")

        assertEquals(4L, bestPlatformMatch(platforms, "game boy advance"))
        assertEquals(5L, bestPlatformMatch(platforms, "Game Boy"))
    }
}
