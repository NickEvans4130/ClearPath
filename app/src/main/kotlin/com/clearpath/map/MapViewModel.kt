package com.clearpath.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.ClearPathApp
import com.clearpath.data.camera.CameraNode
import com.clearpath.data.camera.CameraType
import com.clearpath.data.route.LatLon
import com.clearpath.data.route.RoutingMode
import com.clearpath.data.route.SavedRoute
import com.clearpath.geocoding.NominatimClient
import com.clearpath.geocoding.SearchResult
import com.clearpath.overpass.OverpassSyncWorker
import com.clearpath.routing.NavigationEngine
import com.clearpath.routing.NavigationState
import com.clearpath.routing.RouteAlternative
import com.clearpath.routing.RouteAlternativesEngine
import com.clearpath.routing.RouteOptimisation
import com.clearpath.routing.RoutingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class LayerSettings(
    val showCameras: Boolean     = true,
    val showHeatmap: Boolean     = false,
    val showRoute: Boolean       = true,
    val showCoverage: Boolean    = true,
    val visibleTypes: Set<CameraType> = CameraType.entries.toSet(),
)

data class MapUiState(
    val cameras: List<CameraNode>              = emptyList(),
    val cameraCount: Int                       = 0,
    val dataFreshnessMs: Long                  = 0L,
    val selectedCamera: CameraNode?            = null,
    val routeAlternatives: List<RouteAlternative> = emptyList(),
    val selectedRoute: SavedRoute?             = null,
    val origin: LatLon?                        = null,
    val destination: LatLon?                   = null,
    val searchResults: List<SearchResult>      = emptyList(),
    val isSearching: Boolean                   = false,
    val isRouting: Boolean                     = false,
    val isSyncing: Boolean                     = false,
    val layers: LayerSettings                  = LayerSettings(),
    val routingMode: RoutingMode               = RoutingMode.FOOT,
    val optimisation: RouteOptimisation        = RouteOptimisation.BALANCED,
    val navigationState: NavigationState       = NavigationState(),
    val activeTilesFile: File?                 = null,
    val alias: String?                         = null,
    val error: String?                         = null,
)

class MapViewModel(app: Application) : AndroidViewModel(app) {

    private val application  get() = getApplication<ClearPathApp>()
    private val cameraRepo   get() = application.cameraRepository
    private val routeRepo    get() = application.routeRepository
    private val geocodeCache get() = application.geocodingCache

    private val routingEngine    = RoutingEngine()
    private val nominatim        = NominatimClient(geocodeCache)
    private val navigationEngine = NavigationEngine()
    private val alternativesEngine = RouteAlternativesEngine(routingEngine, cameraRepo)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val navState: StateFlow<NavigationState> = navigationEngine.state

    init {
        viewModelScope.launch { cameraRepo.seedBundledDataIfEmpty() }
        viewModelScope.launch {
            cameraRepo.observeAll().collect { list ->
                _uiState.update { it.copy(cameras = list, cameraCount = list.size) }
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(dataFreshnessMs = OverpassSyncWorker.lastSyncTime(application))
            }
        }
    }

    // ── Camera selection ──────────────────────────────────────────────────────

    fun selectCamera(node: CameraNode?) =
        _uiState.update { it.copy(selectedCamera = node) }

    // ── Layer toggles ─────────────────────────────────────────────────────────

    fun updateLayers(layers: LayerSettings) =
        _uiState.update { it.copy(layers = layers) }

    // ── Search ────────────────────────────────────────────────────────────────

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val results = nominatim.search(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Search failed: ${e.message}", isSearching = false) }
            }
        }
    }

    fun clearSearch() = _uiState.update { it.copy(searchResults = emptyList()) }

    // ── Route planning ────────────────────────────────────────────────────────

    fun setOrigin(latLon: LatLon)      = _uiState.update { it.copy(origin = latLon) }
    fun setDestination(latLon: LatLon) = _uiState.update { it.copy(destination = latLon) }
    fun setRoutingMode(mode: RoutingMode) = _uiState.update { it.copy(routingMode = mode) }
    fun setOptimisation(opt: RouteOptimisation) = _uiState.update { it.copy(optimisation = opt) }
    fun setAlias(alias: String?)       = _uiState.update { it.copy(alias = alias) }

    fun planRoute() {
        val origin = _uiState.value.origin ?: return
        val dest   = _uiState.value.destination ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRouting = true, error = null) }
            try {
                val alts = alternativesEngine.computeAlternatives(
                    origin        = origin,
                    destination   = dest,
                    mode          = _uiState.value.routingMode,
                    optimisation  = _uiState.value.optimisation,
                    alias         = _uiState.value.alias,
                )
                _uiState.update {
                    it.copy(
                        routeAlternatives = alts,
                        selectedRoute     = alts.firstOrNull()?.route,
                        isRouting         = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Routing failed: ${e.message}", isRouting = false) }
            }
        }
    }

    fun selectRoute(route: SavedRoute) = _uiState.update { it.copy(selectedRoute = route) }

    fun saveSelectedRoute() {
        val route = _uiState.value.selectedRoute ?: return
        viewModelScope.launch { routeRepo.save(route) }
    }

    // ── Overpass sync ─────────────────────────────────────────────────────────

    fun syncCamerasForBounds(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double) {
        _uiState.update { it.copy(isSyncing = true) }
        OverpassSyncWorker.enqueue(application, minLat, minLon, maxLat, maxLon)
        _uiState.update { it.copy(isSyncing = false) }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun startNavigation() {
        val route = _uiState.value.selectedRoute ?: return
        navigationEngine.startNavigation(route, _uiState.value.cameras)
    }

    fun stopNavigation() = navigationEngine.stopNavigation()

    fun updateUserLocation(lat: Double, lon: Double) {
        navigationEngine.updateLocation(lat, lon)
    }

    // ── Tiles ─────────────────────────────────────────────────────────────────

    fun setActiveTilesFile(file: File?) = _uiState.update { it.copy(activeTilesFile = file) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    override fun onCleared() {
        super.onCleared()
        routingEngine.close()
        nominatim.close()
    }
}
