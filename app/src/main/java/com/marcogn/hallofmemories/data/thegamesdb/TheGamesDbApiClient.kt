package com.marcogn.hallofmemories.data.thegamesdb

import com.marcogn.hallofmemories.domain.model.GameArtSearchResult
import com.marcogn.hallofmemories.domain.thegamesdb.GameArtImages
import com.marcogn.hallofmemories.domain.thegamesdb.bestPlatformMatch
import com.marcogn.hallofmemories.domain.thegamesdb.parseGameImages
import com.marcogn.hallofmemories.domain.thegamesdb.parseLookupTable
import com.marcogn.hallofmemories.domain.thegamesdb.parseSearchResults
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val BASE_URL = "https://api.thegamesdb.net/v1"
// A realistic desktop Chrome UA, not an app-identifying one: TheGamesDB's anti-bot hardening
// answers an app-like UA with a misleading "Invalid API key" for a perfectly valid key — see
// CLAUDE.md, "Known gotchas". Ported verbatim from ThePatientGamerHelper's TheGamesDbApiClient.
private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000

/**
 * Hand-rolled TheGamesDB REST v1 client — no Retrofit/Ktor, same `HttpURLConnection` pattern as
 * `PokeApiClient` and ThePatientGamerHelper's `TheGamesDbApiClient` (which this is ported from,
 * minus the genre/developer lookups this app has no use for). Response parsing itself is pure and
 * lives in `domain/thegamesdb/TheGamesDbParsing.kt`, unit-tested against fixtures — this class is
 * the network I/O only.
 *
 * Every endpoint requires an `apikey` query param — TheGamesDB dropped anonymous access in a
 * 2026-02-17 policy change (verified during ThePatientGamerHelper's implementation).
 */
@Singleton
class TheGamesDbApiClient @Inject constructor() {

    private val cacheMutex = Mutex()
    private var platformsCache: Map<Long, String>? = null

    /** Searches by title, optionally narrowed to the platform whose name best matches [platformHint]. */
    suspend fun search(apiKey: String, title: String, platformHint: String?): List<GameArtSearchResult> =
        withContext(Dispatchers.IO) {
            val platformNamesById = platforms(apiKey)
            val platformId = platformHint?.takeIf { it.isNotBlank() }?.let { hint -> bestPlatformMatch(platformNamesById, hint) }

            val query = buildString {
                append(BASE_URL).append("/Games/ByGameName")
                append("?apikey=").append(apiKey.urlEncode())
                append("&name=").append(title.urlEncode())
                append("&fields=").append("overview,release_date".urlEncode())
                append("&include=").append("boxart".urlEncode())
                // TheGamesDB uses PHP/Laravel-style indexed array query params for filters.
                if (platformId != null) append("&filter%5Bplatform%5D%5B0%5D=").append(platformId)
            }

            val connection = openConnection(query)
            val body = connection.readTextBody()
            connection.ensureSuccessful(body)

            parseSearchResults(body, platformNamesById)
        }

    /** Called only for the game the user picks — fetches its logo and (when available) a better box art. */
    suspend fun fetchImages(apiKey: String, gameId: Long): GameArtImages = withContext(Dispatchers.IO) {
        val connection = openConnection("$BASE_URL/Games/Images?apikey=${apiKey.urlEncode()}&games_id=$gameId")
        val body = connection.readTextBody()
        connection.ensureSuccessful(body)
        parseGameImages(body, gameId)
    }

    suspend fun downloadImageBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }
        check(connection.responseCode in 200..299) { "Image download failed (HTTP ${connection.responseCode})" }
        connection.inputStream.use { it.readBytes() }
    }

    private suspend fun platforms(apiKey: String): Map<Long, String> = cacheMutex.withLock {
        platformsCache ?: run {
            val connection = openConnection("$BASE_URL/Platforms?apikey=${apiKey.urlEncode()}")
            val body = connection.readTextBody()
            connection.ensureSuccessful(body)
            parseLookupTable(body, "platforms").also { platformsCache = it }
        }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

private fun HttpURLConnection.readTextBody(): String =
    (if (responseCode in 200..299) inputStream else errorStream)
        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

private fun HttpURLConnection.ensureSuccessful(body: String) {
    if (responseCode !in 200..299) {
        val excerpt = body.trim().take(300)
        error("HTTP $responseCode @ $url${if (excerpt.isNotEmpty()) ": $excerpt" else ""}")
    }
}
