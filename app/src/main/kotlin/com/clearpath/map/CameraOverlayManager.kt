package com.clearpath.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.clearpath.data.camera.CameraNode
import com.clearpath.data.camera.CameraType
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders camera coverage zones as semi-transparent circles or directional cones.
 * Must be added/removed on the main thread.
 */
class CameraOverlayManager(private val mapView: MapView) {

    private val overlay = CameraOverlay()

    init {
        mapView.overlays.add(overlay)
    }

    fun updateCameras(cameras: List<CameraNode>, visibleTypes: Set<CameraType>) {
        overlay.cameras = cameras.filter { it.type in visibleTypes }
        mapView.invalidate()
    }

    fun clear() {
        mapView.overlays.remove(overlay)
    }

    // ── Inner overlay ─────────────────────────────────────────────────────────

    private class CameraOverlay : Overlay() {
        var cameras: List<CameraNode> = emptyList()

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            if (shadow) return
            val projection = mapView.projection

            cameras.forEach { camera ->
                val screenPoint = android.graphics.Point()
                projection.toPixels(GeoPoint(camera.lat, camera.lon), screenPoint)

                val radiusPx = projection.metersToEquatorPixels(camera.coverageRadius.toFloat())
                val baseColor = camera.type.overlayColor()
                val alpha = (camera.confidence * 80).toInt().coerceIn(20, 120)

                fillPaint.color = colorWithAlpha(baseColor, alpha)
                strokePaint.color = colorWithAlpha(baseColor, 200)

                if (camera.direction != null) {
                    drawCone(
                        canvas       = canvas,
                        cx           = screenPoint.x.toFloat(),
                        cy           = screenPoint.y.toFloat(),
                        radiusPx     = radiusPx,
                        directionDeg = camera.direction,
                        coneAngleDeg = camera.coneAngle,
                    )
                } else {
                    canvas.drawCircle(
                        screenPoint.x.toFloat(),
                        screenPoint.y.toFloat(),
                        radiusPx,
                        fillPaint,
                    )
                    canvas.drawCircle(
                        screenPoint.x.toFloat(),
                        screenPoint.y.toFloat(),
                        radiusPx,
                        strokePaint,
                    )
                }

                // Dot at camera position
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorWithAlpha(baseColor, 230)
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 6f, dotPaint)
            }
        }

        private fun drawCone(
            canvas: Canvas,
            cx: Float, cy: Float,
            radiusPx: Float,
            directionDeg: Int,
            coneAngleDeg: Int,
        ) {
            val halfAngle = coneAngleDeg / 2.0
            // osmdroid bearing: 0° = north, CW.  Canvas: 0° = east, CW from north = -90° offset
            val startDeg = directionDeg - halfAngle - 90.0
            val path = Path().apply {
                moveTo(cx, cy)
                arcTo(
                    cx - radiusPx, cy - radiusPx,
                    cx + radiusPx, cy + radiusPx,
                    startDeg.toFloat(),
                    coneAngleDeg.toFloat(),
                    false,
                )
                close()
            }
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }

        private fun colorWithAlpha(baseArgb: Int, alpha: Int): Int =
            (baseArgb and 0x00FFFFFF) or (alpha shl 24)
    }
}

private fun CameraType.overlayColor(): Int = when (this) {
    CameraType.ANPR    -> Color.parseColor("#EF4444")
    CameraType.FIXED   -> Color.parseColor("#F97316")
    CameraType.PTZ     -> Color.parseColor("#F59E0B")
    CameraType.DOME    -> Color.parseColor("#F59E0B")
    CameraType.UNKNOWN -> Color.parseColor("#6B7280")
}
