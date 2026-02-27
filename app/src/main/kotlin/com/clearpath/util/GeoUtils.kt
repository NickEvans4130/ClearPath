package com.clearpath.util

import com.clearpath.data.route.LatLon
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_METRES = 6_371_000.0

    /** Haversine great-circle distance in metres. */
    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1).toRad()
        val dLon = (lon2 - lon1).toRad()
        val a = sin(dLat / 2).pow(2) +
                cos(lat1.toRad()) * cos(lat2.toRad()) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METRES * asin(sqrt(a))
    }

    fun haversine(a: LatLon, b: LatLon) = haversine(a.lat, a.lon, b.lat, b.lon)

    /**
     * Bearing from (lat1,lon1) → (lat2,lon2) in degrees [0,360).
     */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = (lon2 - lon1).toRad()
        val y = sin(dLon) * cos(lat2.toRad())
        val x = cos(lat1.toRad()) * sin(lat2.toRad()) -
                sin(lat1.toRad()) * cos(lat2.toRad()) * cos(dLon)
        return (atan2(y, x).toDeg() + 360) % 360
    }

    /**
     * Sample points along a polyline at approximately [stepMetres] intervals.
     * Returns the original vertices plus interpolated intermediate points.
     */
    fun samplePolyline(points: List<LatLon>, stepMetres: Double = 5.0): List<LatLon> {
        if (points.size < 2) return points
        val result = mutableListOf<LatLon>()
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val segLen = haversine(a, b)
            if (segLen <= stepMetres) {
                result.add(b)
                continue
            }
            val steps = (segLen / stepMetres).toInt()
            for (j in 1..steps) {
                val t = j.toDouble() / (steps + 1)
                result.add(LatLon(
                    lat = a.lat + t * (b.lat - a.lat),
                    lon = a.lon + t * (b.lon - a.lon),
                ))
            }
            result.add(b)
        }
        return result
    }

    /**
     * Compute a roughly expanded bounding box around a polyline plus [bufferMetres].
     * Returns (minLat, maxLat, minLon, maxLon).
     */
    fun polylineBounds(
        points: List<LatLon>,
        bufferMetres: Double = 100.0,
    ): BoundingBox4 {
        val latDeg = bufferMetres / EARTH_RADIUS_METRES * (180 / PI)
        val latBuf = latDeg
        // Lon buffer varies with latitude — use a generous mid-lat estimate
        val midLat = points.map { it.lat }.average()
        val lonBuf = latDeg / cos(midLat.toRad())
        return BoundingBox4(
            minLat = points.minOf { it.lat } - latBuf,
            maxLat = points.maxOf { it.lat } + latBuf,
            minLon = points.minOf { it.lon } - lonBuf,
            maxLon = points.maxOf { it.lon } + lonBuf,
        )
    }

    data class BoundingBox4(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

    private fun Double.toRad() = this * PI / 180.0
    private fun Double.toDeg() = this * 180.0 / PI
}
