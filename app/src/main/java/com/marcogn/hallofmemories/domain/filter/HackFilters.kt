package com.marcogn.hallofmemories.domain.filter

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import java.text.Normalizer

/**
 * Pure hack-library filtering (spec §3.1's Home search + generation filter) — the ViewModel
 * `combine()`s this with the repository `Flow`, same UDF shape as everywhere else.
 */
fun filterHacks(
    hacks: List<HackWithEntryCount>,
    query: String,
    generations: Set<GameGeneration>,
): List<HackWithEntryCount> {
    val normalizedQuery = query.normalizedForSearch()
    return hacks.filter { entry ->
        val matchesQuery = normalizedQuery.isBlank() || entry.hack.name.normalizedForSearch().contains(normalizedQuery)
        val matchesGeneration = generations.isEmpty() || entry.hack.generation in generations
        matchesQuery && matchesGeneration
    }
}

/** Lowercase with diacritics stripped, so "poke" matches "Pokémon" and vice versa. */
private fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
