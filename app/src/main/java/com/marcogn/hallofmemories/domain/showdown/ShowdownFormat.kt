package com.marcogn.hallofmemories.domain.showdown

import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot

/**
 * Pokémon Showdown's own plain-text set format (`sim/teams.ts`'s `exportSet`/
 * `parseExportedTeamLine` in smogon/pokemon-showdown — verified against that source, not
 * assumed), read and written independently of the PokéAPI cache. A Hall of Fame slot already
 * models nearly every field a Showdown set has (species, nickname, gender, item, ability, level,
 * shiny, nature, EVs, IVs, up to four moves) — see `docs/implementation-decisions.md`, "Showdown
 * format import/export", for why this exists and what it deliberately leaves out (Tera Type,
 * Happiness, Pokeball, Hidden Power type, Dynamax Level, Gigantamax: none of those have a slot
 * field to hold them, so they are recognized only well enough to not corrupt anything else).
 *
 * Species/move/ability/item name resolution against the cache happens one layer up
 * ([com.marcogn.hallofmemories.domain.repository.PokedexRepository]'s `resolve*ByName`
 * functions) — this file stays pure and Android-free, same convention as every other file under
 * `domain/`.
 */
data class ParsedShowdownSet(
    val nickname: String?,
    val speciesName: String,
    val gender: PokemonGender,
    val item: String?,
    val ability: String?,
    val level: Int,
    val isShiny: Boolean,
    val nature: String?,
    val evHp: Int,
    val evAtk: Int,
    val evDef: Int,
    val evSpAtk: Int,
    val evSpDef: Int,
    val evSpe: Int,
    val ivHp: Int,
    val ivAtk: Int,
    val ivDef: Int,
    val ivSpAtk: Int,
    val ivSpDef: Int,
    val ivSpe: Int,
    /** Up to four, in order — never more, matching the real client's own four-move limit. */
    val moves: List<String>,
)

private val ABILITY_LINE_REGEX = Regex("^(Ability|Trait):\\s*", RegexOption.IGNORE_CASE)
private val NATURE_LINE_REGEX = Regex("^([A-Za-z]+) Nature", RegexOption.IGNORE_CASE)

/**
 * Line prefixes the real client recognizes for fields no [PokemonSlot] field holds. Checked so
 * these common real-Showdown-paste lines are dropped rather than mistaken for the species line
 * (only the block's first line is ever that — see [parseShowdownSet]).
 */
private val IGNORED_DETAIL_PREFIXES = listOf(
    "happiness:", "pokeball:", "hidden power:", "dynamax level:", "gigantamax:", "tera type:",
)

private val STAT_ALIASES: Map<String, String> = mapOf(
    "hp" to "hp", "hitpoints" to "hp",
    "atk" to "atk", "attack" to "atk",
    "def" to "def", "defense" to "def", "defence" to "def",
    "spa" to "spa", "specialattack" to "spa", "spatk" to "spa", "spattack" to "spa",
    "specialatk" to "spa", "special" to "spa", "spc" to "spa",
    "spd" to "spd", "specialdefense" to "spd", "spdef" to "spd", "spdefense" to "spd",
    "specialdef" to "spd",
    "spe" to "spe", "speed" to "spe",
)

private fun statId(rawName: String): String? = STAT_ALIASES[rawName.lowercase().filter { it.isLetterOrDigit() }]

private fun parseStatLine(remainder: String, target: MutableMap<String, Int>) {
    remainder.split("/").forEach { part ->
        val tokens = part.trim().split(Regex("\\s+"), limit = 2)
        val value = tokens.getOrNull(0)?.toIntOrNull() ?: return@forEach
        val id = tokens.getOrNull(1)?.let { statId(it) } ?: return@forEach
        target[id] = value
    }
}

/** `"Hidden Power [Ice]"` -> `"Hidden Power Ice"`, the same un-bracketing the real client's own
 * parser does — this app has no separate Hidden Power type field, so the type stays inline in
 * the move's own text. */
private fun normalizeHiddenPower(moveName: String): String {
    if (moveName.startsWith("Hidden Power [") && moveName.endsWith("]")) {
        return "Hidden Power " + moveName.removePrefix("Hidden Power [").removeSuffix("]")
    }
    return moveName
}

/**
 * Parse one Showdown set block. Defaults exactly match what the real client implies when a line
 * is absent: level 100, IVs 31, EVs 0, no nature/ability/item/shiny — never left ambiguous,
 * since an absent `Level:` line genuinely means "level 100" in Showdown's own format, not
 * "unknown".
 */
fun parseShowdownSet(block: String): ParsedShowdownSet {
    val lines = block.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }
    var nickname: String? = null
    var speciesName = "Unknown"
    var gender = PokemonGender.UNKNOWN
    var item: String? = null
    var ability: String? = null
    var level = 100
    var isShiny = false
    var nature: String? = null
    val evs = mutableMapOf("hp" to 0, "atk" to 0, "def" to 0, "spa" to 0, "spd" to 0, "spe" to 0)
    val ivs = mutableMapOf("hp" to 31, "atk" to 31, "def" to 31, "spa" to 31, "spd" to 31, "spe" to 31)
    val moves = mutableListOf<String>()

    lines.forEachIndexed { index, line ->
        val lower = line.lowercase()
        when {
            // Only the block's first line is ever the species/item/gender/nickname line —
            // matches the real client's own `isFirstLine` split, so every other unrecognized
            // line is ignored rather than clobbering the species already parsed.
            index == 0 -> {
                var speciesLine = line.substringBefore("@").trim()
                if (line.contains("@")) {
                    val itemValue = line.substringAfter("@").trim()
                    if (itemValue.isNotEmpty()) item = itemValue
                }
                if (speciesLine.endsWith(" (M)")) {
                    gender = PokemonGender.MALE
                    speciesLine = speciesLine.dropLast(4)
                } else if (speciesLine.endsWith(" (F)")) {
                    gender = PokemonGender.FEMALE
                    speciesLine = speciesLine.dropLast(4)
                }
                if (speciesLine.endsWith(")") && speciesLine.contains("(")) {
                    val nick = speciesLine.substringBefore("(").trim()
                    val species = speciesLine.dropLast(1).substringAfter("(").trim()
                    if (species.isNotEmpty()) {
                        nickname = nick.takeIf { it.isNotEmpty() }
                        speciesLine = species
                    }
                }
                if (speciesLine.isNotEmpty()) speciesName = speciesLine
            }
            line.startsWith("-") || line.startsWith("~") -> {
                val body = line.drop(1).let { if (it.startsWith(" ")) it.drop(1) else it }
                val moveName = normalizeHiddenPower(body.trim())
                if (moveName.isNotEmpty()) moves += moveName
            }
            lower.startsWith("ability:") || lower.startsWith("trait:") -> {
                val value = line.replaceFirst(ABILITY_LINE_REGEX, "").trim()
                if (value.isNotEmpty()) ability = value
            }
            lower.startsWith("level:") -> {
                line.substringAfter(":").trim().toIntOrNull()?.let { level = it }
            }
            lower == "shiny: yes" -> isShiny = true
            lower.startsWith("evs:") -> parseStatLine(line.substringAfter(":"), evs)
            lower.startsWith("ivs:") -> parseStatLine(line.substringAfter(":"), ivs)
            NATURE_LINE_REGEX.containsMatchIn(line) -> {
                nature = NATURE_LINE_REGEX.find(line)?.groupValues?.get(1)
            }
            IGNORED_DETAIL_PREFIXES.any { lower.startsWith(it) } -> Unit // recognized, untracked
            else -> Unit // unrecognized line: ignored, never overwrites the species
        }
    }

    return ParsedShowdownSet(
        nickname = nickname,
        speciesName = speciesName,
        gender = gender,
        item = item,
        ability = ability,
        level = level,
        isShiny = isShiny,
        nature = nature,
        evHp = evs.getValue("hp"), evAtk = evs.getValue("atk"), evDef = evs.getValue("def"),
        evSpAtk = evs.getValue("spa"), evSpDef = evs.getValue("spd"), evSpe = evs.getValue("spe"),
        ivHp = ivs.getValue("hp"), ivAtk = ivs.getValue("atk"), ivDef = ivs.getValue("def"),
        ivSpAtk = ivs.getValue("spa"), ivSpDef = ivs.getValue("spd"), ivSpe = ivs.getValue("spe"),
        moves = moves.take(4),
    )
}

/** Split a multi-set paste (blank-line-separated, same shape as a full team export) and parse
 * each block — lets one paste fill more than one Hall of Fame slot at once. */
fun parseShowdownTeam(text: String): List<ParsedShowdownSet> =
    text.split(Regex("\n\\s*\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { parseShowdownSet(it) }

/**
 * Convert a filled [PokemonSlot] to a Showdown-style block, matching the real client's own
 * `exportSet` grammar exactly: a field's line is omitted entirely when it's at its default
 * (level 100, IV 31, EV 0, no nature/ability/item/shiny/nickname), never written blank. Returns
 * `null` for an empty slot (`speciesName == null`) — there is nothing to export.
 */
fun exportSlotToShowdown(slot: PokemonSlot): String? {
    val species = slot.speciesName ?: return null
    val lines = mutableListOf<String>()

    val namePart = if (!slot.nickname.isNullOrBlank() && slot.nickname != species) {
        "${slot.nickname} ($species)"
    } else {
        species
    }
    val genderSuffix = when (slot.gender) {
        PokemonGender.MALE -> " (M)"
        PokemonGender.FEMALE -> " (F)"
        PokemonGender.UNKNOWN -> ""
    }
    lines += if (slot.heldItem != null) "$namePart$genderSuffix @ ${slot.heldItem}" else "$namePart$genderSuffix"
    if (slot.ability != null) lines += "Ability: ${slot.ability}"
    if (slot.level != null && slot.level != 100) lines += "Level: ${slot.level}"
    if (slot.isShiny) lines += "Shiny: Yes"

    val evParts = listOfNotNull(
        slot.evHp?.takeIf { it != 0 }?.let { "$it HP" },
        slot.evAtk?.takeIf { it != 0 }?.let { "$it Atk" },
        slot.evDef?.takeIf { it != 0 }?.let { "$it Def" },
        slot.evSpAtk?.takeIf { it != 0 }?.let { "$it SpA" },
        slot.evSpDef?.takeIf { it != 0 }?.let { "$it SpD" },
        slot.evSpe?.takeIf { it != 0 }?.let { "$it Spe" },
    )
    if (evParts.isNotEmpty()) lines += "EVs: ${evParts.joinToString(" / ")}"
    if (!slot.nature.isNullOrBlank()) lines += "${slot.nature} Nature"
    val ivParts = listOfNotNull(
        slot.ivHp?.takeIf { it != 31 }?.let { "$it HP" },
        slot.ivAtk?.takeIf { it != 31 }?.let { "$it Atk" },
        slot.ivDef?.takeIf { it != 31 }?.let { "$it Def" },
        slot.ivSpAtk?.takeIf { it != 31 }?.let { "$it SpA" },
        slot.ivSpDef?.takeIf { it != 31 }?.let { "$it SpD" },
        slot.ivSpe?.takeIf { it != 31 }?.let { "$it Spe" },
    )
    if (ivParts.isNotEmpty()) lines += "IVs: ${ivParts.joinToString(" / ")}"

    listOfNotNull(slot.move1, slot.move2, slot.move3, slot.move4).forEach { lines += "- $it" }
    return lines.joinToString("\n")
}

/** Export every non-empty slot of a Hall of Fame, blank-line-separated — the same team-list
 * shape the real client uses for a multi-Pokémon paste. */
fun exportSlotsToShowdown(slots: List<PokemonSlot>): String =
    slots.mapNotNull { exportSlotToShowdown(it) }.joinToString("\n\n")
