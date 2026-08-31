package com.marcogn.hallofmemories.domain.filter

import com.marcogn.hallofmemories.domain.model.PokemonTemplate

/** Accent/case-insensitive search over a template's label and species name (spec §3.8). */
fun filterTemplates(templates: List<PokemonTemplate>, query: String): List<PokemonTemplate> {
    val normalizedQuery = query.normalizedForSearch()
    if (normalizedQuery.isBlank()) return templates
    return templates.filter { template ->
        template.label.normalizedForSearch().contains(normalizedQuery) ||
            template.speciesName?.normalizedForSearch()?.contains(normalizedQuery) == true
    }
}
