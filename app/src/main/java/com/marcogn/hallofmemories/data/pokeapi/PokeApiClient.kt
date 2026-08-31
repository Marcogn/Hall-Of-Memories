package com.marcogn.hallofmemories.data.pokeapi

import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val BASE_URL = "https://raw.githubusercontent.com/PokeAPI/api-data/master/data/api/v2"
private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000
private const val RETRY_DELAY_MS = 500L

/**
 * Hand-rolled client for the static PokeAPI/api-data mirror — no Retrofit/Ktor, same
 * `HttpURLConnection` pattern as ThePatientGamerHelper's `TheGamesDbApiClient`
 * (see `docs/implementation-decisions.md`, "No Retrofit or Ktor"). The mirror serves numeric-id
 * paths only (`.../pokemon/6/index.json`, never `.../pokemon/bulbasaur/index.json`) and needs no
 * API key or auth.
 */
@Singleton
class PokeApiClient @Inject constructor() {

    /** Fetches `$BASE_URL/$path` as text, one retry after [RETRY_DELAY_MS] on failure. */
    suspend fun getJson(path: String): String = withContext(Dispatchers.IO) {
        try {
            fetch(path)
        } catch (e: Exception) {
            delay(RETRY_DELAY_MS)
            fetch(path)
        }
    }

    /** [getJson] for every path in [paths], at most [concurrency] in flight at once. */
    suspend fun getJsonBatch(paths: List<String>, concurrency: Int = 6): List<String> = coroutineScope {
        val semaphore = Semaphore(concurrency)
        paths.map { path ->
            async { semaphore.withPermit { getJson(path) } }
        }.awaitAll()
    }

    private fun fetch(path: String): String {
        val connection = (URL("$BASE_URL/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
            // Deliberately no Accept-Encoding header: HttpURLConnection negotiates gzip and
            // decompresses transparently on its own. Setting it by hand hands back raw gzip
            // bytes instead — see CLAUDE.md, "Known gotchas".
        }
        try {
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode} @ $BASE_URL/$path" }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
