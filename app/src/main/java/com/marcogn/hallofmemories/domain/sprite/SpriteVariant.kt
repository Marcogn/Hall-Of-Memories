package com.marcogn.hallofmemories.domain.sprite

/**
 * A sprite directory under the PokeAPI/sprites mirror. [path] is `null` for [DEFAULT], the flat
 * `SPRITES/<id>.png` fallback with no subdirectory. Every path and [supportsShiny] flag here was
 * verified against the live mirror during planning — see `docs/plan/reference-pokeapi.md` §4.
 * Transcribed exactly, including [RED_BLUE] having no shiny directory and there being no
 * generation-ix entry at all (Generation IX games use [HOME] instead).
 */
enum class SpriteVariant(val path: String?, val supportsShiny: Boolean) {
    RED_BLUE("versions/generation-i/red-blue", supportsShiny = false),
    YELLOW("versions/generation-i/yellow", supportsShiny = false),
    GOLD("versions/generation-ii/gold", supportsShiny = true),
    SILVER("versions/generation-ii/silver", supportsShiny = true),
    CRYSTAL("versions/generation-ii/crystal", supportsShiny = true),
    RUBY_SAPPHIRE("versions/generation-iii/ruby-sapphire", supportsShiny = true),
    EMERALD("versions/generation-iii/emerald", supportsShiny = true),
    FIRERED_LEAFGREEN("versions/generation-iii/firered-leafgreen", supportsShiny = true),
    DIAMOND_PEARL("versions/generation-iv/diamond-pearl", supportsShiny = true),
    PLATINUM("versions/generation-iv/platinum", supportsShiny = true),
    HEARTGOLD_SOULSILVER("versions/generation-iv/heartgold-soulsilver", supportsShiny = true),
    BLACK_WHITE("versions/generation-v/black-white", supportsShiny = true),
    X_Y("versions/generation-vi/x-y", supportsShiny = true),
    OMEGARUBY_ALPHASAPPHIRE("versions/generation-vi/omegaruby-alphasapphire", supportsShiny = true),
    ULTRA_SUN_ULTRA_MOON("versions/generation-vii/ultra-sun-ultra-moon", supportsShiny = true),
    HOME("other/home", supportsShiny = true),
    OFFICIAL_ARTWORK("other/official-artwork", supportsShiny = true),
    DEFAULT(null, supportsShiny = true),
}
