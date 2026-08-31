package com.marcogn.hallofmemories.domain.sprite

import com.marcogn.hallofmemories.domain.model.GameGeneration

/**
 * Pure sprite URL derivation — no sprite URL is ever stored in the database (spec §3.6). See
 * `docs/plan/reference-pokeapi.md` §4 for the verified table and fallback-chain reasoning this
 * implements.
 */
object SpriteUrlResolver {

    const val SPRITES_BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

    fun url(speciesId: Int, variant: SpriteVariant, shiny: Boolean): String {
        val shinySegment = if (shiny) "shiny/" else ""
        val variantSegment = variant.path?.let { "$it/" } ?: ""
        return "$SPRITES_BASE/$variantSegment$shinySegment$speciesId.png"
    }

    /**
     * Ordered candidates, most specific first — the UI (`ui/common/PokemonSprite.kt`) tries them
     * in order and advances on load failure, since Coil has no built-in fallback chain.
     *
     * [speciesGeneration] is `PokedexSpecies.generationIntroduced` when known: a species
     * introduced after [generation] has no sprite in that generation's directory (verified 404),
     * so its variant is skipped before a request is ever made. `null` means "unknown, don't
     * skip" — covers alternate forms (ids > 10000), which never appear in a `generation/{n}`
     * response and so never get a known `generationIntroduced`.
     */
    fun candidates(
        speciesId: Int,
        generation: GameGeneration,
        shiny: Boolean,
        alwaysUseLatest: Boolean,
        speciesGeneration: Int?,
    ): List<String> {
        val urls = LinkedHashSet<String>()

        if (!alwaysUseLatest) {
            val variant = generation.spriteVariant
            val generationCandidateApplies = speciesGeneration == null || speciesGeneration <= generation.generationNumber
            if (generationCandidateApplies) {
                if (shiny && variant.supportsShiny) {
                    urls += url(speciesId, variant, shiny = true)
                }
                urls += url(speciesId, variant, shiny = false)
            }
        }

        urls += url(speciesId, SpriteVariant.HOME, shiny)
        urls += url(speciesId, SpriteVariant.OFFICIAL_ARTWORK, shiny)
        urls += url(speciesId, SpriteVariant.DEFAULT, shiny)

        return urls.toList()
    }
}
