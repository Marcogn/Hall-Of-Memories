package com.marcogn.hallofmemories.data.thegamesdb

import android.content.Context
import android.util.Log
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.data.image.ImageFormat
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.domain.model.GameArtSearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "GameArtSearch"

/** Locally-persisted paths (and their remote origin, kept for reference) after picking a search result. */
data class DownloadedArt(
    val boxArtPath: String?,
    val boxArtUrl: String?,
    val logoPath: String?,
    val logoUrl: String?,
)

/**
 * "Search online" logic used by the hack form: resolves the configured API key, calls
 * [TheGamesDbApiClient], and turns every failure (missing key, no network, no results) into a
 * plain user-facing [Outcome.Message] instead of throwing — manual entry and a gallery pick
 * always stay available underneath (spec §3.1).
 *
 * The message includes the underlying exception's text (also logged via [Log.w] in full) — a
 * purely generic "search failed" with the real cause thrown away is exactly the bug
 * ThePatientGamerHelper had to fix later to make a real failure diagnosable.
 */
class GameArtSearchCoordinator @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val apiClient: TheGamesDbApiClient,
    private val preferences: TheGamesDbPreferences,
    private val imageStorage: ImageStorage,
) {
    sealed interface Outcome {
        data class Results(val results: List<GameArtSearchResult>) : Outcome
        data class Message(val text: String) : Outcome
    }

    suspend fun search(title: String, platformHint: String?): Outcome {
        val apiKey = preferences.apiKey
        if (apiKey.isNullOrBlank()) {
            return Outcome.Message(appContext.getString(R.string.game_search_missing_api_key))
        }
        return runCatching { apiClient.search(apiKey, title, platformHint) }.fold(
            onSuccess = { results ->
                if (results.isEmpty()) {
                    Outcome.Message(appContext.getString(R.string.game_search_no_results))
                } else {
                    Outcome.Results(results)
                }
            },
            onFailure = { throwable ->
                Log.w(TAG, "TheGamesDB search failed for \"$title\"", throwable)
                val detail = throwable.message?.takeIf { it.isNotBlank() }
                val base = appContext.getString(R.string.game_search_failed)
                Outcome.Message(if (detail != null) "$base\n$detail" else base)
            },
        )
    }

    /**
     * Downloads and persists box art + logo for a picked result. Box art prefers whatever
     * `Games/Images` returns, falling back to the search result's own box art if that call fails
     * or has none; the logo has no such fallback (search results never carry one). Every download
     * is independently best-effort — a failed one just leaves that field null, never blocks save.
     */
    suspend fun downloadArt(result: GameArtSearchResult): DownloadedArt {
        val apiKey = preferences.apiKey ?: return DownloadedArt(null, null, null, null)
        val images = runCatching { apiClient.fetchImages(apiKey, result.externalId) }
            .onFailure { Log.w(TAG, "Games/Images failed for game ${result.externalId}", it) }
            .getOrNull()

        val boxArtUrl = images?.boxArtUrl ?: result.boxArtUrl
        val logoUrl = images?.logoUrl

        val boxArtPath = boxArtUrl?.let { url ->
            runCatching { imageStorage.persistBytes(apiClient.downloadImageBytes(url)) }.getOrNull()
        }
        val logoPath = logoUrl?.let { url ->
            runCatching { imageStorage.persistBytes(apiClient.downloadImageBytes(url), ImageFormat.PNG) }.getOrNull()
        }

        return DownloadedArt(boxArtPath, boxArtUrl, logoPath, logoUrl)
    }
}
