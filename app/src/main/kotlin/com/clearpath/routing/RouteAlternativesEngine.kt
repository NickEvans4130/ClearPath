package com.clearpath.routing

import com.clearpath.data.camera.CameraRepository
import com.clearpath.data.route.LatLon
import com.clearpath.data.route.RoutingMode
import com.clearpath.data.route.SavedRoute
import com.clearpath.util.GeoUtils
import java.util.UUID

enum class RouteOptimisation { FASTEST, LOWEST_EXPOSURE, BALANCED }

data class RouteAlternative(
    val route: SavedRoute,
    val label: String,           // "A", "B", "C"
    val isRecommended: Boolean,
)

/**
 * Orchestrates OSRM routing → exposure scoring → returns up to 3 labelled alternatives.
 */
class RouteAlternativesEngine(
    private val routingEngine: RoutingEngine,
    private val cameraRepository: CameraRepository,
) {

    suspend fun computeAlternatives(
        origin: LatLon,
        destination: LatLon,
        mode: RoutingMode,
        optimisation: RouteOptimisation = RouteOptimisation.BALANCED,
        alias: String? = null,
    ): List<RouteAlternative> {

        val rawRoutes = routingEngine.route(origin, destination, mode, maxAlternatives = 3)
        if (rawRoutes.isEmpty()) return emptyList()

        // Load cameras in the combined corridor of all routes
        val allPoints = rawRoutes.flatMap { it.polyline }
        val bounds    = GeoUtils.polylineBounds(allPoints, 100.0)
        val cameras   = cameraRepository.getInBounds(
            bounds.minLat, bounds.maxLat, bounds.minLon, bounds.maxLon
        )

        // Score each route
        val scored = rawRoutes.mapIndexed { i, raw ->
            val result = ExposureCalculator.calculate(raw.polyline, cameras)
            val originArea  = "Origin"
            val destArea    = "Destination"
            SavedRoute(
                id              = UUID.randomUUID().toString(),
                name            = buildRouteName(alias, originArea, destArea, i),
                origin          = origin,
                destination     = destination,
                polyline        = raw.polyline,
                distanceMetres  = raw.distanceMetres,
                durationSeconds = raw.durationSeconds,
                exposureScore   = result.score,
                cameraCount     = result.cameraCount,
                cameraEncounters = result.encounters,
                routingMode     = mode,
                alias           = alias,
            )
        }

        // Sort & label
        val sorted = when (optimisation) {
            RouteOptimisation.FASTEST         -> scored.sortedBy { it.durationSeconds }
            RouteOptimisation.LOWEST_EXPOSURE -> scored.sortedBy { it.exposureScore }
            RouteOptimisation.BALANCED        -> scored.sortedBy {
                it.durationSeconds * 0.4 + it.exposureScore * 0.6
            }
        }

        return sorted.take(3).mapIndexed { i, route ->
            RouteAlternative(
                route         = route,
                label         = listOf("A", "B", "C")[i],
                isRecommended = i == 0,
            )
        }
    }

    private fun buildRouteName(alias: String?, origin: String, dest: String, index: Int): String {
        val prefix = if (alias != null) "$alias: " else ""
        val label  = listOf("A", "B", "C").getOrElse(index) { "$index" }
        return "${prefix}$origin → $dest [$label]"
    }
}
