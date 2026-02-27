package com.clearpath.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.clearpath.data.camera.CameraNode
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Camera density heatmap overlay.
 * Renders a CPU-side heatmap bitmap by accumulating radial blobs for each camera.
 * Rebuilt on camera list or viewport change.
 */
class HeatmapOverlayManager(private val mapView: MapView) {

    private val overlay = HeatmapOverlay()

    init {
        mapView.overlays.add(overlay)
    }

    fun update(cameras: List<CameraNode>) {
        overlay.cameras = cameras
        mapView.invalidate()
    }

    fun removeFromMap() {
        mapView.overlays.remove(overlay)
    }

    // ── Overlay ───────────────────────────────────────────────────────────────

    private class HeatmapOverlay : Overlay() {
        var cameras: List<CameraNode> = emptyList()

        private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }

        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            if (shadow || cameras.isEmpty()) return

            val width  = mapView.width
            val height = mapView.height
            if (width <= 0 || height <= 0) return

            val heatmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val hCanvas = Canvas(heatmap)

            val projection = mapView.projection
            val radiusPx = projection.metersToEquatorPixels(50f)

            cameras.forEach { camera ->
                val pt = android.graphics.Point()
                projection.toPixels(GeoPoint(camera.lat, camera.lon), pt)

                // Clip to visible area with margin
                if (pt.x < -radiusPx || pt.x > width + radiusPx ||
                    pt.y < -radiusPx || pt.y > height + radiusPx) return@forEach

                val gradient = RadialGradient(
                    pt.x.toFloat(), pt.y.toFloat(), radiusPx,
                    intArrayOf(
                        Color.argb(120, 239, 68, 68),   // red core
                        Color.argb(60,  245, 158, 11),  // amber mid
                        Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP,
                )
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
                hCanvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), radiusPx, p)
            }

            canvas.drawBitmap(heatmap, 0f, 0f, blobPaint)
            heatmap.recycle()
        }
    }
}
