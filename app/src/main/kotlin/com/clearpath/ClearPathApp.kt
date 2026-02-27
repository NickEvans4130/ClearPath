package com.clearpath

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.clearpath.data.AppDatabase
import com.clearpath.data.camera.CameraRepository
import com.clearpath.data.route.RouteRepository
import com.clearpath.geocoding.GeocodingCache
import org.osmdroid.config.Configuration as OsmConfiguration
import java.io.File

class ClearPathApp : Application(), Configuration.Provider {

    val database by lazy { AppDatabase.getInstance(this) }
    val cameraRepository by lazy { CameraRepository(database.cameraDao(), this) }
    val routeRepository by lazy { RouteRepository(database.routeDao()) }
    val geocodingCache by lazy { GeocodingCache(database.geocodingCacheDao()) }

    override fun onCreate() {
        super.onCreate()

        // osmdroid: set user agent and configure tile cache directory
        OsmConfiguration.getInstance().apply {
            userAgentValue = "ClearPath/1.0 (https://github.com/clearpath)"
            osmdroidBasePath = File(getExternalFilesDir(null), "osmdroid")
            osmdroidTileCache = File(getExternalFilesDir(null), "osmdroid/tiles")
            tileFileSystemCacheMaxBytes = 500L * 1024 * 1024  // 500 MB cap
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
