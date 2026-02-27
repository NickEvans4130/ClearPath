package com.clearpath.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.clearpath.data.route.LatLon
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws one or more routes as colour-coded polylines.
 *
 * Each route segment is coloured by its local exposure score:
 *   score < 0.3  → green (#10B981)
 *   score < 0.6  → amber (#F59E0B)
 *   score ≥ 0.6  → red   (#EF4444)
 *
 * The selected route is drawn at full opacity; alternatives at 40%.
 */
class RouteOverlayManager(private val mapView: MapView) {

    private val overlay = RouteOverlay()

    init {
        mapView.overlays.add(overlay)
    }

    fun setRoutes(
        routes: List<Pair<List<LatLon>, List<Float>>>,   // polyline + per-point scores
        selectedIndex: Int = 0,
    ) {
        overlay.routes        = routes
        overlay.selectedIndex = selectedIndex
        mapView.invalidate()
    }

    fun clear() {
        overlay.routes = emptyList()
        mapView.invalidate()
    }

    fun removeFromMap() {
        mapView.overlays.remove(overlay)
    }

    // ── Inner overlay ─────────────────────────────────────────────────────────

    private class RouteOverlay : Overlay() {
        var routes: List<Pair<List<LatLon>, List<Float>>> = emptyList()
        var selectedIndex: Int = 0

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap   = Paint.Cap.ROUND
            strokeJoin  = Paint.Join.ROUND
        }

        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            if (shadow) return
            val projection = mapView.projection

            routes.forEachIndexed { idx, (polyline, scores) ->
                val isSelected = idx == selectedIndex
                val baseAlpha  = if (isSelected) 220 else 80
                paint.strokeWidth = if (isSelected) 10f else 6f

                if (polyline.size < 2) return@forEachIndexed

                polyline.zipWithNext().forEachIndexed { segIdx, (a, b) ->
                    val score = scores.getOrElse(segIdx) { 0f }
                    paint.color = exposureColor(score, baseAlpha)

                    val ptA = android.graphics.Point()
                    val ptB = android.graphics.Point()
                    projection.toPixels(GeoPoint(a.lat, a.lon), ptA)
                    projection.toPixels(GeoPoint(b.lat, b.lon), ptB)

                    canvas.drawLine(
                        ptA.x.toFloat(), ptA.y.toFloat(),
                        ptB.x.toFloat(), ptB.y.toFloat(),
                        paint,
                    )
                }
            }
        }

        private fun exposureColor(score: Float, alpha: Int): Int {
            val base = when {
                score < 0.3f -> Color.parseColor("#10B981")  // green
                score < 0.6f -> Color.parseColor("#F59E0B")  // amber
                else         -> Color.parseColor("#EF4444")  // red
            }
            return (base and 0x00FFFFFF) or (alpha shl 24)
        }
    }
}
