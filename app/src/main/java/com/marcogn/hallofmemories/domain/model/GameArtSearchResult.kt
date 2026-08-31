package com.marcogn.hallofmemories.domain.model

/**
 * One TheGamesDB search result — the *base commercial game*, not the ROM hack itself (spec §3.1:
 * TheGamesDB has no ROM hacks, so a hack search is framed as "find the base game's artwork").
 * [boxArtUrl] is the row-preview thumbnail; the full-resolution box art (and any logo) is fetched
 * separately, only for the result the user picks — see `GameArtSearchCoordinator.downloadArt()`.
 */
data class GameArtSearchResult(
    val externalId: Long,
    val title: String,
    val platformName: String?,
    val releaseYear: Int?,
    val boxArtUrl: String?,
    val boxArtThumbnailUrl: String? = boxArtUrl,
)
