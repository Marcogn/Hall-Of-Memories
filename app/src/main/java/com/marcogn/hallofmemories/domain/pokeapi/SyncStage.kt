package com.marcogn.hallofmemories.domain.pokeapi

/**
 * A stage of the PokéAPI sync, in dependency order: [TYPES] updates species/move rows that
 * [SPECIES]/[MOVES] must have already inserted, so it runs after both. [requestCount] is the
 * number of HTTP requests the stage makes — used as `SyncState.Running.total`. Measured figures
 * for each: `docs/plan/reference-pokeapi.md` §2.
 */
enum class SyncStage(val requestCount: Int) {
    SPECIES(1),
    MOVES(1),
    TYPES(18),
    NATURES(26),
    ABILITIES(1),
    ITEMS(1),
    GENERATIONS(9),
}
