package com.marcogn.hallofmemories.domain.pokeapi

/**
 * Merges a prefix-match pass with a contains-match pass for autocomplete, prefix matches first,
 * deduplicated by [keyOf], capped at [limit]. Pure so it is testable without a database — the two
 * raw passes live in `PokedexDao`, the merge lives here (see
 * `docs/plan/phase-1-data-and-pokedex-sync.md` §1).
 */
fun <T, K> mergeSearchResults(
    prefixMatches: List<T>,
    containsMatches: List<T>,
    limit: Int,
    keyOf: (T) -> K,
): List<T> {
    val merged = LinkedHashMap<K, T>()
    for (item in prefixMatches) {
        if (merged.size >= limit) break
        merged.putIfAbsent(keyOf(item), item)
    }
    for (item in containsMatches) {
        if (merged.size >= limit) break
        merged.putIfAbsent(keyOf(item), item)
    }
    return merged.values.toList()
}
