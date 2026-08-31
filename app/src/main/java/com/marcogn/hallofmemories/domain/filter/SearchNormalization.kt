package com.marcogn.hallofmemories.domain.filter

import java.text.Normalizer

/** Lowercase with diacritics stripped, so "poke" matches "Pokémon" and vice versa. Shared by every accent/case-insensitive search in `domain/filter/`. */
internal fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
