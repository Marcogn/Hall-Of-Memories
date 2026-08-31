package com.marcogn.hallofmemories.domain.thegamesdb

import com.marcogn.hallofmemories.domain.model.GameArtSearchResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Pure parsers over TheGamesDB's JSON responses — no Android or network import, ported from
 * ThePatientGamerHelper's `TheGamesDbApiClient` (a debugged, working client) minus the
 * genre/developer lookups this app has no use for. `Games/Images`' exact shape could not be
 * verified while planning (it needs a key) — [parseGameImages] is deliberately defensive:
 * `JsonObject` navigation with `?:` fallbacks, never a thrown exception on an unexpected shape.
 */

internal val theGamesDbJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

/** id -> name, shared shape of the `Platforms` (and, upstream, `Genres`/`Developers`) endpoints. */
fun parseLookupTable(json: String, dataKey: String): Map<Long, String> {
    val entries = theGamesDbJson.parseToJsonElement(json).jsonObject["data"]?.jsonObject?.get(dataKey)?.jsonObject
        ?: return emptyMap()
    return entries.entries.mapNotNull { (_, value) ->
        val obj = value.jsonObject
        val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        id to name
    }.toMap()
}

fun parseSearchResults(json: String, platformNamesById: Map<Long, String>): List<GameArtSearchResult> {
    val root = theGamesDbJson.parseToJsonElement(json).jsonObject
    val games = root["data"]?.jsonObject?.get("games")
        ?.let { theGamesDbJson.decodeFromJsonElement<List<GameDto>>(it) }
        .orEmpty()

    val boxart = root["include"]?.jsonObject?.get("boxart")?.jsonObject
    val baseUrls = boxart?.get("base_url")?.jsonObject
    val boxartBaseUrl = baseUrls?.get("original")?.jsonPrimitive?.contentOrNull
    // "thumb" is a tiny crop meant for exactly this kind of picker row — full-size "original" for
    // a 48dp row icon needlessly downloads (and Coil-disk-caches) several MB per search result.
    val boxartThumbBaseUrl = baseUrls?.get("thumb")?.jsonPrimitive?.contentOrNull ?: boxartBaseUrl
    val boxartData = boxart?.get("data")?.jsonObject

    return games.map { game -> game.toDomain(platformNamesById, boxartBaseUrl, boxartThumbBaseUrl, boxartData) }
}

data class GameArtImages(val boxArtUrl: String?, val logoUrl: String?)

/**
 * Logo priority: clearlogo -> banner -> fanart -> none. Box art: front boxart -> any boxart ->
 * none (the caller falls back to the search result's own box art in that last case).
 */
fun parseGameImages(json: String, gameId: Long): GameArtImages {
    val root = runCatching { theGamesDbJson.parseToJsonElement(json).jsonObject }.getOrNull()
        ?: return GameArtImages(null, null)

    val data = root["data"]?.jsonObject
    val baseUrl = data?.get("base_url")?.jsonObject?.get("original")?.jsonPrimitive?.contentOrNull
    val images = data?.get("images")?.jsonObject?.get(gameId.toString())?.jsonArray

    fun firstFilenameOfType(type: String): String? =
        images?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == type }
            ?.jsonObject?.get("filename")?.jsonPrimitive?.contentOrNull

    val logoFilename = firstFilenameOfType("clearlogo") ?: firstFilenameOfType("banner") ?: firstFilenameOfType("fanart")

    val frontBoxArtFilename = images?.firstOrNull {
        it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "boxart" &&
            it.jsonObject["side"]?.jsonPrimitive?.contentOrNull == "front"
    }?.jsonObject?.get("filename")?.jsonPrimitive?.contentOrNull
    val boxArtFilename = frontBoxArtFilename ?: firstFilenameOfType("boxart")

    return GameArtImages(
        boxArtUrl = if (baseUrl != null && boxArtFilename != null) baseUrl + boxArtFilename else null,
        logoUrl = if (baseUrl != null && logoFilename != null) baseUrl + logoFilename else null,
    )
}

/**
 * TheGamesDB returns `null` (not a missing key) for fields a game has none catalogued for — a
 * default value only covers a missing key, not an explicit JSON `null`. This DTO doesn't declare
 * `genres`/`developers` at all (this app has no use for them), so `ignoreUnknownKeys` drops that
 * field regardless of whether it's present, absent, or explicitly null.
 */
@Serializable
private data class GameDto(
    val id: Long,
    @SerialName("game_title") val title: String,
    val platform: Long? = null,
    @SerialName("release_date") val releaseDate: String? = null,
)

private fun GameDto.toDomain(
    platformNamesById: Map<Long, String>,
    boxartBaseUrl: String?,
    boxartThumbBaseUrl: String?,
    boxartData: JsonObject?,
): GameArtSearchResult {
    val images = boxartData?.get(id.toString())?.jsonArray
    val filename = images?.firstOrNull { it.jsonObject["side"]?.jsonPrimitive?.contentOrNull == "front" }
        ?.jsonObject?.get("filename")?.jsonPrimitive?.contentOrNull
        ?: images?.firstOrNull()?.jsonObject?.get("filename")?.jsonPrimitive?.contentOrNull

    return GameArtSearchResult(
        externalId = id,
        title = title,
        platformName = platform?.let { platformNamesById[it] },
        releaseYear = releaseDate?.take(4)?.toIntOrNull(),
        boxArtUrl = if (boxartBaseUrl != null && filename != null) boxartBaseUrl + filename else null,
        boxArtThumbnailUrl = if (boxartThumbBaseUrl != null && filename != null) boxartThumbBaseUrl + filename else null,
    )
}

fun bestPlatformMatch(platforms: Map<Long, String>, hint: String): Long? =
    platforms.entries.firstOrNull { it.value.equals(hint, ignoreCase = true) }?.key
        ?: platforms.entries.firstOrNull { it.value.contains(hint, ignoreCase = true) || hint.contains(it.value, ignoreCase = true) }?.key
