package com.clearpath.routing

import com.clearpath.data.route.LatLon
import com.clearpath.data.route.RoutingMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class RouteResult(
    val polyline: List<LatLon>,
    val distanceMetres: Int,
    val durationSeconds: Int,
    val profile: RoutingMode,
)

/**
 * OSRM-based routing engine (MVP, fully online with local caching).
 *
 * To swap in GraphHopper for full offline operation:
 *   1. Download a routing graph (.ghz) for the region alongside the MBTiles.
 *   2. Initialise GraphHopper with the graph file path.
 *   3. Replace osrmRoute() with graphHopperRoute().
 *
 * OSRM endpoints used:
 *   https://router.project-osrm.org/route/v1/{profile}/{lon1,lat1};{lon2,lat2}
 *   ?overview=full&geometries=geojson&alternatives=true
 */
class RoutingEngine {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    /**
     * Request up to [maxAlternatives] routes from [origin] to [destination].
     * Returns an empty list on failure.
     */
    suspend fun route(
        origin: LatLon,
        destination: LatLon,
        mode: RoutingMode,
        maxAlternatives: Int = 3,
    ): List<RouteResult> {
        val profile = mode.osrmProfile()
        val coords  = "${origin.lon},${origin.lat};${destination.lon},${destination.lat}"
        val url = "https://router.project-osrm.org/route/v1/$profile/$coords" +
                  "?overview=full&geometries=geojson&alternatives=${maxAlternatives > 1}"

        return try {
            val raw = client.get(url) {
                header("User-Agent", "ClearPath/1.0")
            }.bodyAsText()

            parseOsrmResponse(raw, mode)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseOsrmResponse(raw: String, mode: RoutingMode): List<RouteResult> {
        val root   = json.parseToJsonElement(raw)
        val routes = root.jsonObject["routes"]?.jsonArray ?: return emptyList()

        return routes.mapNotNull { routeEl ->
            try {
                val route    = routeEl.jsonObject
                val distance = route["distance"]?.jsonPrimitive?.double?.toInt() ?: 0
                val duration = route["duration"]?.jsonPrimitive?.double?.toInt() ?: 0
                val geometry = route["geometry"]?.jsonObject
                val coordArr = geometry?.get("coordinates")?.jsonArray ?: return@mapNotNull null

                val polyline = coordArr.map { pt ->
                    val arr = pt.jsonArray
                    LatLon(arr[1].jsonPrimitive.double, arr[0].jsonPrimitive.double)
                }

                RouteResult(
                    polyline        = polyline,
                    distanceMetres  = distance,
                    durationSeconds = duration,
                    profile         = mode,
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun close() = client.close()
}

private fun RoutingMode.osrmProfile() = when (this) {
    RoutingMode.FOOT    -> "foot"
    RoutingMode.BICYCLE -> "bike"
    RoutingMode.CAR     -> "driving"
}
