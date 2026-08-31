package com.marcogn.hallofmemories.domain.sprite

import com.marcogn.hallofmemories.domain.model.GameGeneration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpriteUrlResolverTest {

    @Test
    fun `a gen-3 hack with a gen-3 species leads with the emerald sprite`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 252,
            generation = GameGeneration.RSE,
            shiny = false,
            alwaysUseLatest = false,
            speciesGeneration = 3,
        )

        assertEquals(
            "${SpriteUrlResolver.SPRITES_BASE}/versions/generation-iii/emerald/252.png",
            candidates.first(),
        )
    }

    @Test
    fun `a gen-9 species in a gen-3 hack skips the emerald candidate entirely`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 906,
            generation = GameGeneration.RSE,
            shiny = false,
            alwaysUseLatest = false,
            speciesGeneration = 9,
        )

        assertTrue(candidates.none { it.contains("generation-iii") })
        assertEquals("${SpriteUrlResolver.SPRITES_BASE}/other/home/906.png", candidates.first())
    }

    @Test
    fun `an unknown species generation is never skipped`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 10034,
            generation = GameGeneration.RSE,
            shiny = false,
            alwaysUseLatest = false,
            speciesGeneration = null,
        )

        assertTrue(candidates.first().contains("generation-iii/emerald"))
    }

    @Test
    fun `shiny in red-blue never requests the nonexistent shiny directory`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 6,
            generation = GameGeneration.RB,
            shiny = true,
            alwaysUseLatest = false,
            speciesGeneration = 1,
        )

        assertTrue(candidates.none { it.contains("red-blue/shiny") })
        assertEquals(
            "${SpriteUrlResolver.SPRITES_BASE}/versions/generation-i/red-blue/6.png",
            candidates.first(),
        )
    }

    @Test
    fun `a shiny-supporting variant offers the shiny url before its own non-shiny fallback`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 6,
            generation = GameGeneration.RSE,
            shiny = true,
            alwaysUseLatest = false,
            speciesGeneration = 1,
        )

        assertEquals(
            "${SpriteUrlResolver.SPRITES_BASE}/versions/generation-iii/emerald/shiny/6.png",
            candidates[0],
        )
        assertEquals(
            "${SpriteUrlResolver.SPRITES_BASE}/versions/generation-iii/emerald/6.png",
            candidates[1],
        )
    }

    @Test
    fun `alwaysUseLatest ignores the hack generation entirely`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 6,
            generation = GameGeneration.RB,
            shiny = false,
            alwaysUseLatest = true,
            speciesGeneration = 1,
        )

        assertTrue(candidates.none { it.contains("red-blue") })
        assertEquals("${SpriteUrlResolver.SPRITES_BASE}/other/home/6.png", candidates.first())
    }

    @Test
    fun `every candidate is a well-formed sprites url with no duplicates`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 6,
            generation = GameGeneration.SWSH,
            shiny = true,
            alwaysUseLatest = false,
            speciesGeneration = 1,
        )

        candidates.forEach { url ->
            assertTrue(url.startsWith(SpriteUrlResolver.SPRITES_BASE))
            assertTrue(url.endsWith(".png"))
        }
        assertEquals(candidates.size, candidates.toSet().size)
    }

    @Test
    fun `OTHER generation never skips a candidate regardless of species generation`() {
        val candidates = SpriteUrlResolver.candidates(
            speciesId = 1010,
            generation = GameGeneration.OTHER,
            shiny = false,
            alwaysUseLatest = false,
            speciesGeneration = 9,
        )

        assertFalse(candidates.isEmpty())
        assertTrue(candidates.first().contains("official-artwork"))
    }
}
