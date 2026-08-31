package com.marcogn.hallofmemories.domain.model

/**
 * The PokéAPI cache, in domain form — a typing aid for the slot editor, never a dependency of
 * saved user data (see `docs/implementation-decisions.md`). [name] is the PokéAPI kebab-case
 * identity; [displayName] is `prettify(name)`; [searchName] is the normalized column the
 * autocomplete `LIKE` query runs against. See `docs/plan/reference-pokeapi.md` §3.
 */
data class PokedexSpecies(
    val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
    val generationIntroduced: Int?,
    val primaryType: String?,
    val secondaryType: String?,
)

data class PokedexMove(
    val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
    val type: String?,
)

data class PokedexNature(
    val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
    val increasedStat: String?,
    val decreasedStat: String?,
)

data class PokedexAbility(
    val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
)

data class PokedexItem(
    val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
)
