package com.clearpath.routing

import com.clearpath.data.camera.CameraNode
import com.clearpath.data.route.CameraEncounter
import com.clearpath.data.route.LatLon
import com.clearpath.data.route.SavedRoute
import com.clearpath.util.GeoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavigationStatus { IDLE, ACTIVE, ARRIVED, OFF_ROUTE }
enum class ZoneStatus        { CLEAR, ENTERING, IN_ZONE }

data class NavigationState(
    val status: NavigationStatus            = NavigationStatus.IDLE,
    val currentPosition: LatLon?            = null,
    val progressFraction: Float             = 0f,      // 0-1 along route
    val distanceRemainingMetres: Int        = 0,
    val durationRemainingSeconds: Int       = 0,
    val nextCameraDistanceMetres: Float?    = null,    // null if none ahead
    val zoneStatus: ZoneStatus              = ZoneStatus.CLEAR,
    val totalExposureSeconds: Int           = 0,       // accumulated in-coverage time
    val currentCameras: List<CameraNode>    = emptyList(),
    val nextCamera: CameraEncounter?        = null,
)

/**
 * Turn-by-turn navigation state machine.
 * Feed GPS locations via [updateLocation].
 */
class NavigationEngine {

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private var activeRoute: SavedRoute? = null
    private var activeCameras: List<CameraNode> = emptyList()
    private var lastUpdateAt: Long = 0L
    private var inZoneSince: Long? = null

    fun startNavigation(route: SavedRoute, cameras: List<CameraNode>) {
        activeRoute   = route
        activeCameras = cameras
        inZoneSince   = null
        _state.value  = NavigationState(
            status                 = NavigationStatus.ACTIVE,
            distanceRemainingMetres = route.distanceMetres,
            durationRemainingSeconds = route.durationSeconds,
        )
    }

    fun stopNavigation() {
        activeRoute  = null
        inZoneSince  = null
        _state.value = NavigationState(status = NavigationStatus.IDLE)
    }

    fun updateLocation(lat: Double, lon: Double) {
        val route = activeRoute ?: return
        val now   = System.currentTimeMillis()
        val pos   = LatLon(lat, lon)

        // Find nearest point on route
        val (closestIdx, closestPoint, progressFraction) = findClosestOnRoute(pos, route.polyline)
        val distRemaining = approximateRemainingDistance(closestIdx, route.polyline)

        // Walking speed estimate from last update
        val elapsedSecs = if (lastUpdateAt > 0) (now - lastUpdateAt) / 1000 else 0L
        lastUpdateAt = now

        // Camera proximity
        val nearby = activeCameras.filter {
            GeoUtils.haversine(pos, LatLon(it.lat, it.lon)) <= it.coverageRadius + 20
        }
        val inZone = activeCameras.any {
            GeoUtils.haversine(pos, LatLon(it.lat, it.lon)) <= it.coverageRadius
        }

        // Accumulate in-zone time
        val prev = _state.value
        var newExposure = prev.totalExposureSeconds
        if (inZone && elapsedSecs > 0) newExposure += elapsedSecs.toInt()

        // Next camera ahead on route
        val nextEncounter = route.cameraEncounters
            .filter { it.atRoutePosition > progressFraction }
            .minByOrNull { it.atRoutePosition }

        val nextCameraDist = nextEncounter?.let {
            val nextPos = interpolateRoutePosition(it.atRoutePosition, route.polyline)
            GeoUtils.haversine(pos, nextPos).toFloat()
        }

        // Detect arrival
        val arrived = GeoUtils.haversine(pos, route.destination) < 20

        val zoneStatus = when {
            inZone && prev.zoneStatus != ZoneStatus.IN_ZONE -> ZoneStatus.ENTERING
            inZone -> ZoneStatus.IN_ZONE
            else   -> ZoneStatus.CLEAR
        }

        _state.value = prev.copy(
            status                   = if (arrived) NavigationStatus.ARRIVED else NavigationStatus.ACTIVE,
            currentPosition          = pos,
            progressFraction         = progressFraction,
            distanceRemainingMetres  = distRemaining,
            durationRemainingSeconds = (distRemaining / 1.4).toInt(),
            nextCameraDistanceMetres = nextCameraDist,
            zoneStatus               = zoneStatus,
            totalExposureSeconds     = newExposure,
            currentCameras           = nearby,
            nextCamera               = nextEncounter,
        )
    }

    private fun findClosestOnRoute(pos: LatLon, polyline: List<LatLon>): Triple<Int, LatLon, Float> {
        var minDist = Double.MAX_VALUE
        var closestIdx = 0
        polyline.forEachIndexed { i, p ->
            val d = GeoUtils.haversine(pos, p)
            if (d < minDist) { minDist = d; closestIdx = i }
        }
        val fraction = closestIdx.toFloat() / polyline.size
        return Triple(closestIdx, polyline[closestIdx], fraction)
    }

    private fun approximateRemainingDistance(fromIdx: Int, polyline: List<LatLon>): Int {
        var dist = 0.0
        for (i in fromIdx until polyline.size - 1) {
            dist += GeoUtils.haversine(polyline[i], polyline[i + 1])
        }
        return dist.toInt()
    }

    private fun interpolateRoutePosition(fraction: Float, polyline: List<LatLon>): LatLon {
        val idx = (fraction * polyline.size).toInt().coerceIn(0, polyline.size - 1)
        return polyline[idx]
    }
}
