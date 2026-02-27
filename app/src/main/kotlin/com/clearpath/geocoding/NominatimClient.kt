package com.clearpath.geocoding

import com.clearpath.data.route.LatLon
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String,
    val place_id: Long = 0,
)

data class SearchResult(
    val latLon: LatLon,
    val displayName: String,
)

/**
 * Thin Nominatim wrapper with mandatory 1100ms inter-request delay and
 * permanent Room-backed cache so the same query never hits the network twice.
 */
class NominatimClient(private val cache: GeocodingCache) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
        }
    }

    // Ensures we never exceed 1 req/s to Nominatim
    private var lastRequestAt: Long = 0L

    suspend fun search(query: String): List<SearchResult> {
        val normalised = query.trim().lowercase()

        // Check cache first
        cache.get(normalised)?.let { cached ->
            return listOf(SearchResult(LatLon(cached.lat, cached.lon), cached.displayName))
        }

        // Rate-limit: ensure at least 1100 ms since last request
        val elapsed = System.currentTimeMillis() - lastRequestAt
        if (elapsed < 1100) delay(1100 - elapsed)

        lastRequestAt = System.currentTimeMillis()

        val raw = client.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", "5")
            parameter("countrycodes", "gb")
            header("User-Agent", "ClearPath/1.0 (offline-route-planner)")
            header("Accept-Language", "en")
        }.bodyAsText()

        val results = json.decodeFromString<List<NominatimResult>>(raw)
        if (results.isEmpty()) return emptyList()

        // Cache the top result permanently
        results.firstOrNull()?.let { top ->
            cache.put(
                GeocodingResult(
                    query       = normalised,
                    lat         = top.lat.toDouble(),
                    lon         = top.lon.toDouble(),
                    displayName = top.display_name,
                )
            )
        }

        return results.map { r ->
            SearchResult(
                latLon      = LatLon(r.lat.toDouble(), r.lon.toDouble()),
                displayName = r.display_name,
            )
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String? {
        return try {
            if (System.currentTimeMillis() - lastRequestAt < 1100) {
                delay(1100)
            }
            lastRequestAt = System.currentTimeMillis()

            val raw = client.get("https://nominatim.openstreetmap.org/reverse") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("format", "json")
                parameter("zoom", "16")
                header("User-Agent", "ClearPath/1.0 (offline-route-planner)")
            }.bodyAsText()

            val obj = json.parseToJsonElement(raw)
            obj.toString().let {
                json.decodeFromString<NominatimResult>(it).display_name
            }
        } catch (e: Exception) {
            null
        }
    }

    fun close() = client.close()
}
