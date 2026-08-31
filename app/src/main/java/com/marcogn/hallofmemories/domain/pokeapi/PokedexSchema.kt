package com.marcogn.hallofmemories.domain.pokeapi

/**
 * Bumped whenever a pokédex cache table's shape changes. Compared against every
 * `PokeCacheMeta.schemaVersion` row; a mismatch is treated as "absent" and silently re-synced —
 * never a crash on a stale row. See `docs/plan/reference-pokeapi.md` §5.
 */
const val POKEDEX_SCHEMA_VERSION = 1
