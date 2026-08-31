package com.marcogn.hallofmemories.domain.filter

/**
 * Prefix-then-contains filter feeding `ui/common/EditableComboBox` (nature/ability/held
 * item/move suggestions, spec §3.3): prefix matches first, then contains matches, capped at
 * [limit]. A blank query yields no suggestions — the field is a typing aid, not a browsable list.
 */
fun <T> filterComboBoxSuggestions(query: String, items: List<T>, limit: Int = 30, searchNameOf: (T) -> String): List<T> {
    if (query.isBlank()) return emptyList()
    val key = query.trim().lowercase()
    val prefixMatches = items.filter { searchNameOf(it).startsWith(key) }
    if (prefixMatches.size >= limit) return prefixMatches.take(limit)
    val containsMatches = items.filter { searchNameOf(it).contains(key) && !searchNameOf(it).startsWith(key) }
    return (prefixMatches + containsMatches).take(limit)
}
