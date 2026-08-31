package com.marcogn.hallofmemories.domain.model

import com.marcogn.hallofmemories.domain.sprite.SpriteVariant

/**
 * The generation a hack belongs to. Determines its default sprite set (see [spriteVariant] and
 * `domain/sprite/SpriteUrlResolver`) — never legality or move/ability data, which come from the
 * PokéAPI cache regardless of generation (spec §3.5/§3.6).
 *
 * [generationNumber] is the PokéAPI generation number, used to skip a sprite candidate whose
 * species was introduced after the hack's generation before a request is ever made (a species
 * that didn't exist yet has no sprite in an earlier generation's directory — verified 404, see
 * `docs/plan/reference-pokeapi.md`). [OTHER] uses [Int.MAX_VALUE] so nothing is ever skipped for
 * it: a hack with no clear base generation accepts any species.
 */
enum class GameGeneration(val spriteVariant: SpriteVariant, val generationNumber: Int) {
    RB(SpriteVariant.RED_BLUE, 1),
    GSC(SpriteVariant.CRYSTAL, 2),
    RSE(SpriteVariant.EMERALD, 3),
    FRLG(SpriteVariant.FIRERED_LEAFGREEN, 3),
    DPPT(SpriteVariant.PLATINUM, 4),
    HGSS(SpriteVariant.HEARTGOLD_SOULSILVER, 4),
    BW(SpriteVariant.BLACK_WHITE, 5),
    XY(SpriteVariant.X_Y, 6),
    ORAS(SpriteVariant.OMEGARUBY_ALPHASAPPHIRE, 6),
    SM(SpriteVariant.ULTRA_SUN_ULTRA_MOON, 7),
    USUM(SpriteVariant.ULTRA_SUN_ULTRA_MOON, 7),
    SWSH(SpriteVariant.HOME, 8),
    SV(SpriteVariant.HOME, 9),
    OTHER(SpriteVariant.OFFICIAL_ARTWORK, Int.MAX_VALUE),
}
