package com.clearpath.ui.map

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.clearpath.data.camera.CameraNode
import com.clearpath.data.camera.CameraType
import com.clearpath.data.route.LatLon
import com.clearpath.data.route.SavedRoute
import com.clearpath.data.tiles.MBTilesArchive
import com.clearpath.map.CameraOverlayManager
import com.clearpath.map.HeatmapOverlayManager
import com.clearpath.map.LayerSettings
import com.clearpath.map.RouteOverlayManager
import com.clearpath.routing.RouteAlternative
import com.clearpath.util.toGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    cameras: List<CameraNode>,
    layers: LayerSettings,
    selectedRoute: SavedRoute?,
    alternatives: List<RouteAlternative>,
    tilesFile: File?,
    onCameraTap: (CameraNode) -> Unit,
    onMapLongPress: (LatLon) -> Unit,
) {
    val context = LocalContext.current

    // osmdroid configuration applied in ClearPathApp.onCreate —
    // just need a reference here for the shared prefs load.

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(51.5074, -0.1278))  // default: London

            // Dark colour filter — invert lightness to produce a dark map
            overlayManager.tilesOverlay.setColorFilter(
                org.osmdroid.views.overlay.TilesOverlay.INVERT_COLORS
            )
        }
    }

    val cameraOverlay  = remember { CameraOverlayManager(mapView) }
    val routeOverlay   = remember { RouteOverlayManager(mapView) }
    val heatmapOverlay = remember { HeatmapOverlayManager(mapView) }

    // ── Tile source ───────────────────────────────────────────────────────────
    LaunchedEffect(tilesFile) {
        if (tilesFile != null && tilesFile.exists()) {
            MBTilesArchive.applyToMapView(context, mapView, tilesFile)
        }
    }

    // ── Camera layer ──────────────────────────────────────────────────────────
    LaunchedEffect(cameras, layers.showCameras, layers.visibleTypes, layers.showHeatmap) {
        val visible = cameras.filter { it.type in layers.visibleTypes }
        if (layers.showCameras && !layers.showHeatmap) {
            cameraOverlay.updateCameras(visible, layers.visibleTypes)
        } else {
            cameraOverlay.updateCameras(emptyList(), emptySet())
        }
        if (layers.showHeatmap) heatmapOverlay.update(visible)
        else heatmapOverlay.update(emptyList())
    }

    // ── Route overlay ─────────────────────────────────────────────────────────
    LaunchedEffect(alternatives, selectedRoute) {
        if (alternatives.isEmpty()) {
            routeOverlay.clear()
            return@LaunchedEffect
        }
        val routeData = alternatives.map { alt ->
            val scores = alt.route.cameraEncounters.let { encounters ->
                alt.route.polyline.mapIndexed { idx, _ ->
                    val pos = idx.toFloat() / alt.route.polyline.size
                    encounters.filter { kotlin.math.abs(it.atRoutePosition - pos) < 0.05f }
                        .sumOf { 1.0 }.toFloat().coerceIn(0f, 1f)
                }
            }
            alt.route.polyline to scores
        }
        val selectedIdx = alternatives.indexOfFirst { it.route.id == selectedRoute?.id }
        routeOverlay.setRoutes(routeData, if (selectedIdx >= 0) selectedIdx else 0)
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
            cameraOverlay.clear()
        }
    }

    AndroidView(
        modifier = modifier,
        factory  = { mapView },
        update   = { /* state driven via LaunchedEffect */ },
    )
}
