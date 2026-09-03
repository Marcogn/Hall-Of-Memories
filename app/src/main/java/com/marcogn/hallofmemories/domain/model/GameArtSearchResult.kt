package com.marcogn.hallofmemories.domain.model

/**
 * One TheGamesDB search result — usually the hack itself (TheGamesDB catalogues many well-known
 * ROM hacks directly), occasionally the official base game when a hack isn't listed on its own.
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
