package com.marcogn.hallofmemories.domain.model

private val COLON_FORMAT = Regex("""^(\d+):(\d{1,2})$""")

/**
 * Parses a free-text playtime (spec §3.4) into minutes, for sorting/stats only —
 * `HallOfFameEntry.playtimeText` always keeps the original text verbatim regardless of whether
 * this parses. Accepts `H:MM`/`HH:MM`/`HHH:MM` and a bare number of hours; anything else
 * (including blank/empty text) returns `null` rather than throwing — an unparseable value is
 * never rejected, only left unparsed.
 */
fun parsePlaytimeMinutes(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val colonMatch = COLON_FORMAT.matchEntire(trimmed)
    if (colonMatch != null) {
        val hours = colonMatch.groupValues[1].toIntOrNull() ?: return null
        val minutes = colonMatch.groupValues[2].toIntOrNull() ?: return null
        if (minutes > 59) return null
        return hours * 60 + minutes
    }

    return trimmed.toIntOrNull()?.let { hours -> hours * 60 }
}
