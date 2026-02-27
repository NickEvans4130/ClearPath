package com.clearpath.routing

import com.clearpath.data.camera.CameraNode
import com.clearpath.data.route.CameraEncounter
import com.clearpath.data.route.LatLon
import com.clearpath.util.ConeContainment
import com.clearpath.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Scores a route against a camera dataset.
 *
 * Algorithm (per spec):
 *   For each sampled point P on the route (every 5m):
 *     For each camera C within 100m of P (spatial index pre-filter):
 *       if distance(P,C) > C.coverageRadius → skip
 *       if C has direction AND P is outside coverage cone → skip
 *       exposure += (C.coverageRadius − d) / C.coverageRadius × C.confidence
 *   exposure_score = normalise(Σ exposure) to 0–100
 *
 * Spatial index: bounding box divided into ~50m grid cells.
 * Each camera is placed in all cells it could influence (coverageRadius / cellSize + 1 cells).
 */
object ExposureCalculator {

    private const val SAMPLE_STEP_METRES   = 5.0
    private const val MAX_LOOKUP_RADIUS    = 100.0  // metres pre-filter
    private const val GRID_CELL_METRES     = 50.0   // spatial index resolution

    data class ExposureResult(
        val score: Float,                        // 0–100
        val encounters: List<CameraEncounter>,
        val cameraCount: Int,
        val segmentScores: List<Float>,          // per sampled point, 0–1
    )

    suspend fun calculate(
        polyline: List<LatLon>,
        cameras: List<CameraNode>,
    ): ExposureResult = withContext(Dispatchers.Default) {

        if (polyline.isEmpty() || cameras.isEmpty()) {
            return@withContext ExposureResult(0f, emptyList(), 0, emptyList())
        }

        val sampled = GeoUtils.samplePolyline(polyline, SAMPLE_STEP_METRES)
        val index   = buildSpatialIndex(cameras)

        val encounterMap = mutableMapOf<Long, CameraEncounter>()
        val segmentScores = FloatArray(sampled.size)
        var rawTotal = 0.0

        sampled.forEachIndexed { idx, point ->
            val nearby = index.query(point.lat, point.lon, MAX_LOOKUP_RADIUS)
            var pointScore = 0.0

            for (camera in nearby) {
                val dist = GeoUtils.haversine(point, LatLon(camera.lat, camera.lon))
                if (dist > camera.coverageRadius) continue

                // Cone containment check
                if (camera.direction != null) {
                    val bearing = GeoUtils.bearing(camera.lat, camera.lon, point.lat, point.lon)
                    if (!ConeContainment.isInCone(camera.direction, camera.coneAngle, bearing)) {
                        continue
                    }
                }

                val contribution =
                    (camera.coverageRadius - dist) / camera.coverageRadius * camera.confidence

                pointScore += contribution

                // Track worst (closest) encounter per camera
                val exposureSecs = (SAMPLE_STEP_METRES / walkingSpeedMs()).toInt()
                val prev = encounterMap[camera.id]
                if (prev == null || dist < prev.distanceFromRoute) {
                    encounterMap[camera.id] = CameraEncounter(
                        nodeId                  = camera.id,
                        distanceFromRoute       = dist.toFloat(),
                        estimatedExposureSeconds = exposureSecs,
                        atRoutePosition         = idx.toFloat() / sampled.size,
                    )
                } else {
                    encounterMap[camera.id] = prev.copy(
                        estimatedExposureSeconds = prev.estimatedExposureSeconds + exposureSecs
                    )
                }
            }

            segmentScores[idx] = min(pointScore, 5.0).toFloat()  // cap per-point for display
            rawTotal += pointScore
        }

        // Normalise to 0–100 (calibrated so a route past 3 average cameras ≈ 60)
        val normalised = min(rawTotal / (sampled.size * 0.15) * 100.0, 100.0).toFloat()

        ExposureResult(
            score         = normalised,
            encounters    = encounterMap.values.toList(),
            cameraCount   = encounterMap.size,
            segmentScores = segmentScores.toList(),
        )
    }

    // ── Spatial index ─────────────────────────────────────────────────────────

    private class SpatialIndex(
        private val cells: Map<Long, List<CameraNode>>,
        private val originLat: Double,
        private val originLon: Double,
        private val cellSizeMetres: Double,
    ) {
        private val latPerCell = cellSizeMetres / 111_320.0
        private val lonPerCell get() = latPerCell  // close enough for small areas

        fun query(lat: Double, lon: Double, radiusMetres: Double): List<CameraNode> {
            val spread = (radiusMetres / cellSizeMetres).toInt() + 1
            val cx = ((lon - originLon) / lonPerCell).toInt()
            val cy = ((lat - originLat) / latPerCell).toInt()
            val result = mutableListOf<CameraNode>()
            for (dx in -spread..spread) {
                for (dy in -spread..spread) {
                    val key = cellKey(cx + dx, cy + dy)
                    cells[key]?.let { result.addAll(it) }
                }
            }
            return result
        }

        private fun cellKey(x: Int, y: Int) = x.toLong() shl 32 or (y.toLong() and 0xFFFF_FFFFL)
    }

    private fun buildSpatialIndex(cameras: List<CameraNode>): SpatialIndex {
        if (cameras.isEmpty()) return SpatialIndex(emptyMap(), 0.0, 0.0, GRID_CELL_METRES)
        val originLat = cameras.minOf { it.lat }
        val originLon = cameras.minOf { it.lon }
        val latPerCell = GRID_CELL_METRES / 111_320.0
        val lonPerCell = latPerCell

        val cells = mutableMapOf<Long, MutableList<CameraNode>>()

        for (camera in cameras) {
            val cx = ((camera.lon - originLon) / lonPerCell).toInt()
            val cy = ((camera.lat - originLat) / latPerCell).toInt()
            val spread = (camera.coverageRadius / GRID_CELL_METRES).toInt() + 1
            for (dx in -spread..spread) {
                for (dy in -spread..spread) {
                    val key = (cx + dx).toLong() shl 32 or ((cy + dy).toLong() and 0xFFFF_FFFFL)
                    cells.getOrPut(key) { mutableListOf() }.add(camera)
                }
            }
        }

        return SpatialIndex(cells, originLat, originLon, GRID_CELL_METRES)
    }

    private fun walkingSpeedMs() = 1.4  // m/s ≈ 5 km/h
}
