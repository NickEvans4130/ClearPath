package com.clearpath.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.ui.map.CameraDetailPopup
import com.clearpath.ui.map.LayerToggleSheet
import com.clearpath.ui.map.MapFabs
import com.clearpath.ui.map.MapStatusBar
import com.clearpath.ui.map.OsmMapView
import com.clearpath.ui.map.SearchBar
import com.clearpath.ui.routing.NavigationHUD
import com.clearpath.ui.routing.RouteComparisonSheet
import com.clearpath.ui.routing.RoutePlanningSheet
import com.clearpath.ui.tagging.TagCameraSheet
import com.clearpath.ui.theme.Background
import kotlinx.coroutines.launch

enum class BottomSheetContent { NONE, LAYERS, ROUTE_COMPARISON, ROUTE_PLANNING, TAG_CAMERA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onOpenDownload: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenEducation: () -> Unit,
    vm: MapViewModel = viewModel(),
) {
    val uiState  by vm.uiState.collectAsState()
    val navState by vm.navState.collectAsState()

    var sheetContent      by remember { mutableStateOf(BottomSheetContent.NONE) }
    var centerOnUserToken by remember { mutableLongStateOf(0L) }
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope        = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {

        // ── Full-screen map ───────────────────────────────────────────────────
        OsmMapView(
            modifier           = Modifier.fillMaxSize(),
            cameras            = uiState.cameras,
            layers             = uiState.layers,
            selectedRoute      = uiState.selectedRoute,
            alternatives       = uiState.routeAlternatives,
            tilesFile          = uiState.activeTilesFile,
            centerOnUserToken  = centerOnUserToken,
            onCameraTap        = { vm.selectCamera(it) },
            onMapLongPress     = { latLon ->
                if (uiState.origin == null) vm.setOrigin(latLon)
                else vm.setDestination(latLon)
            },
            onMapCenterChanged = { vm.setMapCenter(it) },
        )

        // ── Search bar ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            SearchBar(
                searchResults = uiState.searchResults,
                isSearching   = uiState.isSearching,
                onSearch      = { vm.search(it) },
                onResultSelected = { result ->
                    vm.clearSearch()
                    vm.setDestination(result.latLon)
                },
            )
        }

        // ── Navigation HUD ────────────────────────────────────────────────────
        if (navState.status.name != "IDLE") {
            NavigationHUD(
                navState = navState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 80.dp, start = 12.dp, end = 12.dp),
                onStop   = { vm.stopNavigation() },
            )
        }

        // ── FABs ──────────────────────────────────────────────────────────────
        MapFabs(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 12.dp, bottom = 80.dp),
            onSearch      = { /* focus search bar */ },
            onMyLocation  = { centerOnUserToken = System.currentTimeMillis() },
            onToggleLayers = {
                sheetContent = BottomSheetContent.LAYERS
                scope.launch { sheetState.show() }
            },
            onTagCamera   = {
                sheetContent = BottomSheetContent.TAG_CAMERA
                scope.launch { sheetState.show() }
            },
            onPlanRoute   = {
                sheetContent = BottomSheetContent.ROUTE_PLANNING
                scope.launch { sheetState.show() }
            },
            onOpenStats   = onOpenStats,
            onOpenDownload = onOpenDownload,
        )

        // ── Status bar ────────────────────────────────────────────────────────
        MapStatusBar(
            cameraCount   = uiState.cameraCount,
            freshnessMs   = uiState.dataFreshnessMs,
            isSyncing     = uiState.isSyncing,
            modifier      = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 8.dp),
        )

        // ── Route comparison (show after routing) ─────────────────────────────
        if (uiState.routeAlternatives.isNotEmpty() && sheetContent == BottomSheetContent.NONE) {
            sheetContent = BottomSheetContent.ROUTE_COMPARISON
            scope.launch { sheetState.show() }
        }

        // ── Camera detail popup ───────────────────────────────────────────────
        uiState.selectedCamera?.let { camera ->
            CameraDetailPopup(
                camera   = camera,
                onDismiss = { vm.selectCamera(null) },
                onOpenEducation = onOpenEducation,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 12.dp, end = 12.dp),
            )
        }
    }

    // ── Bottom sheets ─────────────────────────────────────────────────────────
    if (sheetContent != BottomSheetContent.NONE) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetContent = BottomSheetContent.NONE
            },
            sheetState        = sheetState,
            containerColor    = com.clearpath.ui.theme.Surface,
        ) {
            when (sheetContent) {
                BottomSheetContent.LAYERS -> LayerToggleSheet(
                    layers   = uiState.layers,
                    onChange = { vm.updateLayers(it) },
                )
                BottomSheetContent.ROUTE_COMPARISON -> RouteComparisonSheet(
                    alternatives   = uiState.routeAlternatives,
                    selectedRoute  = uiState.selectedRoute,
                    routingMode    = uiState.routingMode,
                    optimisation   = uiState.optimisation,
                    onSelectRoute  = { vm.selectRoute(it) },
                    onSaveRoute    = { vm.saveSelectedRoute() },
                    onStartNav     = {
                        vm.startNavigation()
                        sheetContent = BottomSheetContent.NONE
                    },
                    onChangeOptimisation = { vm.setOptimisation(it) },
                )
                BottomSheetContent.ROUTE_PLANNING -> RoutePlanningSheet(
                    origin           = uiState.origin,
                    destination      = uiState.destination,
                    routingMode      = uiState.routingMode,
                    isRouting        = uiState.isRouting,
                    error            = uiState.error,
                    onSetRoutingMode = { vm.setRoutingMode(it) },
                    onClearOrigin    = { vm.clearOrigin() },
                    onClearDestination = { vm.clearDestination() },
                    onPlanRoute      = {
                        vm.planRoute()
                        sheetContent = BottomSheetContent.NONE
                    },
                )
                BottomSheetContent.TAG_CAMERA -> TagCameraSheet(
                    position  = uiState.mapCenter,
                    onSave    = { camera ->
                        vm.insertCamera(camera)
                        sheetContent = BottomSheetContent.NONE
                    },
                    onDismiss = { sheetContent = BottomSheetContent.NONE },
                )
                else -> Unit
            }
        }
    }
}
