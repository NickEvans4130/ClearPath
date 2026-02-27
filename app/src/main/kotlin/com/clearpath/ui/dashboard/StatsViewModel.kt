package com.clearpath.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.ClearPathApp
import com.clearpath.data.camera.CameraType
import com.clearpath.data.route.SavedRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsState(
    val totalRoutes: Int                     = 0,
    val totalExposureSeconds: Int            = 0,
    val totalCameras: Int                    = 0,
    val anprCount: Int                       = 0,
    val typeBreakdown: Map<CameraType, Int>  = emptyMap(),
    val mostSurveilledRoute: SavedRoute?     = null,
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val application get() = getApplication<ClearPathApp>()

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val routes  = application.routeRepository.observeAll()
            val cameras = application.cameraRepository.observeAll()

            routes.collect { routeList ->
                val exposure = routeList.sumOf { r ->
                    r.cameraEncounters.sumOf { it.estimatedExposureSeconds }
                }
                _state.value = _state.value.copy(
                    totalRoutes           = routeList.size,
                    totalExposureSeconds  = exposure,
                    mostSurveilledRoute   = routeList.maxByOrNull { it.exposureScore },
                )
            }
        }

        viewModelScope.launch {
            application.cameraRepository.observeAll().collect { list ->
                val breakdown = list.groupingBy { it.type }.eachCount()
                _state.value = _state.value.copy(
                    totalCameras  = list.size,
                    anprCount     = list.count { it.type == CameraType.ANPR },
                    typeBreakdown = breakdown,
                )
            }
        }
    }
}
