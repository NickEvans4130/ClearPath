package com.clearpath.overpass

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clearpath.data.AppDatabase
import com.clearpath.data.camera.CameraRepository

class OverpassSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val minLat = inputData.getDouble(KEY_MIN_LAT, 0.0)
        val minLon = inputData.getDouble(KEY_MIN_LON, 0.0)
        val maxLat = inputData.getDouble(KEY_MAX_LAT, 0.0)
        val maxLon = inputData.getDouble(KEY_MAX_LON, 0.0)

        val client = OverpassClient()
        return try {
            val rawJson = client.fetchSurveillance(minLat, minLon, maxLat, maxLon)
            val nodes = OverpassParser.parse(rawJson)

            val db = AppDatabase.getInstance(applicationContext)
            val repo = CameraRepository(db.cameraDao(), applicationContext)
            repo.replaceOsmData(nodes)

            // Save sync timestamp
            applicationContext.getSharedPreferences("clearpath", Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                .apply()

            Result.success(Data.Builder().putInt(KEY_COUNT, nodes.size).build())
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure(
                Data.Builder().putString("error", e.message).build()
            )
        } finally {
            client.close()
        }
    }

    companion object {
        const val KEY_MIN_LAT   = "min_lat"
        const val KEY_MIN_LON   = "min_lon"
        const val KEY_MAX_LAT   = "max_lat"
        const val KEY_MAX_LON   = "max_lon"
        const val KEY_COUNT     = "count"
        const val PREF_LAST_SYNC = "overpass_last_sync"
        const val WORK_TAG      = "overpass_sync"

        fun enqueue(
            context: Context,
            minLat: Double, minLon: Double,
            maxLat: Double, maxLon: Double,
        ): String {
            val request = OneTimeWorkRequestBuilder<OverpassSyncWorker>()
                .setInputData(
                    Data.Builder()
                        .putDouble(KEY_MIN_LAT, minLat)
                        .putDouble(KEY_MIN_LON, minLon)
                        .putDouble(KEY_MAX_LAT, maxLat)
                        .putDouble(KEY_MAX_LON, maxLon)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            return request.id.toString()
        }

        fun lastSyncTime(context: Context): Long =
            context.getSharedPreferences("clearpath", Context.MODE_PRIVATE)
                .getLong(PREF_LAST_SYNC, 0L)
    }
}
