package com.marcogn.hallofmemories.ui.common

import androidx.compose.ui.graphics.Color

/**
 * Standard Pokémon type colours, keyed by PokéAPI's lowercase type name. Used for move chips in
 * [HallOfFameContent] — a move whose `type` never got filled in (blank/unrecognized) falls back to
 * [typeColorUnknown] rather than throwing or rendering nothing.
 */
private val TYPE_COLORS: Map<String, Color> = mapOf(
    "normal" to Color(0xFFA8A878),
    "fire" to Color(0xFFF08030),
    "water" to Color(0xFF6890F0),
    "electric" to Color(0xFFF8D030),
    "grass" to Color(0xFF78C850),
    "ice" to Color(0xFF98D8D8),
    "fighting" to Color(0xFFC03028),
    "poison" to Color(0xFFA040A0),
    "ground" to Color(0xFFE0C068),
    "flying" to Color(0xFFA890F0),
    "psychic" to Color(0xFFF85888),
    "bug" to Color(0xFFA8B820),
    "rock" to Color(0xFFB8A038),
    "ghost" to Color(0xFF705898),
    "dragon" to Color(0xFF7038F8),
    "dark" to Color(0xFF705848),
    "steel" to Color(0xFFB8B8D0),
    "fairy" to Color(0xFFEE99AC),
)

val typeColorUnknown: Color = Color(0xFF68A090)

fun typeColorFor(typeName: String?): Color = TYPE_COLORS[typeName?.trim()?.lowercase()] ?: typeColorUnknown
