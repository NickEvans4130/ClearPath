package com.clearpath.ui.download

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.data.tiles.Region
import com.clearpath.data.tiles.TileDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DownloadState(
    val activeRegionId: String?       = null,
    val downloadingRegionId: String?  = null,
    val progress: Int                 = 0,
)

class DownloadViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs: SharedPreferences =
        app.getSharedPreferences("clearpath", android.content.Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        DownloadState(activeRegionId = prefs.getString("active_region", null))
    )
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    fun download(region: Region) {
        _state.update { it.copy(downloadingRegionId = region.id, progress = 0) }
        val workId = TileDownloadWorker.enqueue(getApplication(), region.id)

        // Observe work progress
        TileDownloadWorker.observe(getApplication(), workId).observeForever { info ->
            val progress = info?.progress?.getInt(TileDownloadWorker.KEY_PROGRESS, 0) ?: 0
            _state.update { it.copy(progress = progress) }

            if (info?.state?.isFinished == true) {
                _state.update { it.copy(downloadingRegionId = null) }
            }
        }
    }

    fun activate(region: Region) {
        prefs.edit().putString("active_region", region.id).apply()
        _state.update { it.copy(activeRegionId = region.id) }
    }

    fun importCustomFile() {
        // Trigger file picker — handled via activity result in MainActivity
        // For now, a stub that the Activity can wire via a shared event
    }
}
