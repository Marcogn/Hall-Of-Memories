package com.marcogn.hallofmemories.domain.pokeapi

import com.marcogn.hallofmemories.domain.model.PokedexNature
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pure parsers over the PokéAPI mirror's JSON strings — no Android or network import, so every
 * rule here is unit-testable against the trimmed fixtures in `app/src/test/resources/pokeapi/`.
 * Response shapes are the ones verified in `docs/plan/reference-pokeapi.md`; do not guess a shape
 * that wasn't measured there.
 */

private val pokeApiJson = Json {
    ignoreUnknownKeys = true
    // Several fields the mirror returns as an explicit JSON `null` (e.g. a neutral nature's
    // increased_stat/decreased_stat) would otherwise throw — a default only covers a *missing*
    // key. See docs/plan/reference-pokeapi.md §5.
    coerceInputValues = true
}

data class IndexEntry(val name: String, val id: Int)

data class TypeDetail(
    val typeName: String,
    /** (speciesId, slot) pairs — slot 1 is primary, slot 2 is secondary. */
    val speciesSlots: List<Pair<Int, Int>>,
    val moveIds: List<Int>,
)

data class GenerationDetail(val number: Int, val speciesNames: List<String>)

/** `BASE/pokemon/bulbasaur/index.json` is a 404 — the mirror only serves numeric-id paths. */
fun extractIdFromUrl(url: String?): Int? {
    if (url.isNullOrBlank()) return null
    return url.trim('/').substringAfterLast('/').toIntOrNull()
}

/** `"mr-mime"` -> `"Mr Mime"`. */
fun prettify(kebabName: String): String =
    kebabName.split("-").joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }

/** `"mr-mime"` -> `"mrmime"`, so "mrmime"/"mr mime"/"mr-mime" all match the same autocomplete row. */
fun searchKey(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

fun parseIndex(json: String): List<IndexEntry> {
    val dto = pokeApiJson.decodeFromString<IndexResponseDto>(json)
    return dto.results.mapNotNull { result ->
        val id = extractIdFromUrl(result.url) ?: return@mapNotNull null
        IndexEntry(name = result.name, id = id)
    }
}

fun parseTypeDetail(json: String): TypeDetail {
    val dto = pokeApiJson.decodeFromString<TypeDetailDto>(json)
    val speciesSlots = dto.pokemon.mapNotNull { entry ->
        val speciesId = extractIdFromUrl(entry.pokemon.url) ?: return@mapNotNull null
        speciesId to entry.slot
    }
    val moveIds = dto.moves.mapNotNull { extractIdFromUrl(it.url) }
    return TypeDetail(typeName = dto.name, speciesSlots = speciesSlots, moveIds = moveIds)
}

fun parseNatureDetail(json: String): PokedexNature {
    val dto = pokeApiJson.decodeFromString<NatureDetailDto>(json)
    return PokedexNature(
        id = dto.id,
        name = dto.name,
        displayName = prettify(dto.name),
        searchName = searchKey(dto.name),
        increasedStat = dto.increasedStat?.name,
        decreasedStat = dto.decreasedStat?.name,
    )
}

fun parseGenerationDetail(json: String): GenerationDetail {
    val dto = pokeApiJson.decodeFromString<GenerationDetailDto>(json)
    return GenerationDetail(number = dto.id, speciesNames = dto.pokemonSpecies.map { it.name })
}

@Serializable
private data class IndexResponseDto(val results: List<NameUrlDto> = emptyList())

@Serializable
private data class NameUrlDto(val name: String, val url: String)

@Serializable
private data class TypePokemonSlotDto(val slot: Int, val pokemon: NameUrlDto)

@Serializable
private data class TypeDetailDto(
    val name: String,
    val pokemon: List<TypePokemonSlotDto> = emptyList(),
    val moves: List<NameUrlDto> = emptyList(),
)

@Serializable
private data class NatureDetailDto(
    val id: Int,
    val name: String,
    @SerialName("increased_stat") val increasedStat: NameUrlDto? = null,
    @SerialName("decreased_stat") val decreasedStat: NameUrlDto? = null,
)

@Serializable
private data class GenerationDetailDto(
    val id: Int,
    @SerialName("pokemon_species") val pokemonSpecies: List<NameUrlDto> = emptyList(),
)
