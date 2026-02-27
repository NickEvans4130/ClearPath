package com.clearpath.data.tiles

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class Region(
    val id: String,
    val displayName: String,
    val mbtilesUrl: String,
    val estimatedSizeMb: Int,
    val boundingBox: BoundingBox,
)

data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
)

object RegionCatalogue {
    // Tile mirror — use a reliable public CDN or self-hosted mirror.
    // These are illustrative URLs pointing to a typical mbtiles CDN pattern.
    private const val BASE = "https://download.maptiles.openstreetmap.fr/mbtiles"

    val regions = listOf(
        Region(
            id = "london",
            displayName = "London",
            mbtilesUrl = "$BASE/london.mbtiles",
            estimatedSizeMb = 350,
            boundingBox = BoundingBox(51.28, 51.69, -0.51, 0.33),
        ),
        Region(
            id = "manchester",
            displayName = "Manchester",
            mbtilesUrl = "$BASE/manchester.mbtiles",
            estimatedSizeMb = 120,
            boundingBox = BoundingBox(53.35, 53.62, -2.38, -1.96),
        ),
        Region(
            id = "birmingham",
            displayName = "Birmingham",
            mbtilesUrl = "$BASE/birmingham.mbtiles",
            estimatedSizeMb = 140,
            boundingBox = BoundingBox(52.38, 52.58, -2.05, -1.72),
        ),
        Region(
            id = "edinburgh",
            displayName = "Edinburgh",
            mbtilesUrl = "$BASE/edinburgh.mbtiles",
            estimatedSizeMb = 80,
            boundingBox = BoundingBox(55.88, 56.00, -3.36, -3.09),
        ),
        Region(
            id = "bristol",
            displayName = "Bristol",
            mbtilesUrl = "$BASE/bristol.mbtiles",
            estimatedSizeMb = 60,
            boundingBox = BoundingBox(51.39, 51.54, -2.72, -2.49),
        ),
        Region(
            id = "leeds",
            displayName = "Leeds",
            mbtilesUrl = "$BASE/leeds.mbtiles",
            estimatedSizeMb = 90,
            boundingBox = BoundingBox(53.74, 53.88, -1.67, -1.43),
        ),
    )

    fun findById(id: String): Region? = regions.firstOrNull { it.id == id }
}

class TileDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val regionId = inputData.getString(KEY_REGION_ID) ?: return Result.failure()
        val region = RegionCatalogue.findById(regionId) ?: return Result.failure()

        val outFile = File(applicationContext.getExternalFilesDir("tiles"), "${regionId}.mbtiles")
        if (outFile.exists()) outFile.delete()

        return try {
            val client = HttpClient()
            client.prepareGet(region.mbtilesUrl) {
                onDownload { bytesSent, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        val progress = (bytesSent * 100 / contentLength).toInt()
                        setProgressAsync(Data.Builder().putInt(KEY_PROGRESS, progress).build())
                    }
                }
            }.execute { response ->
                withContext(Dispatchers.IO) {
                    val channel = response.bodyAsChannel()
                    outFile.outputStream().use { out ->
                        val buffer = ByteArray(8192)
                        while (!channel.isClosedForRead) {
                            val read = channel.readAvailable(buffer)
                            if (read > 0) out.write(buffer, 0, read)
                        }
                    }
                }
            }
            client.close()
            Result.success(Data.Builder().putString(KEY_OUTPUT_FILE, outFile.absolutePath).build())
        } catch (e: Exception) {
            outFile.delete()
            Result.failure(Data.Builder().putString("error", e.message).build())
        }
    }

    companion object {
        const val KEY_REGION_ID   = "region_id"
        const val KEY_PROGRESS    = "progress"
        const val KEY_OUTPUT_FILE = "output_file"
        const val WORK_TAG        = "tile_download"

        fun enqueue(context: Context, regionId: String): String {
            val request = OneTimeWorkRequestBuilder<TileDownloadWorker>()
                .setInputData(Data.Builder().putString(KEY_REGION_ID, regionId).build())
                .addTag(WORK_TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            return request.id.toString()
        }

        fun observe(context: Context, workId: String) =
            WorkManager.getInstance(context).getWorkInfoByIdLiveData(
                java.util.UUID.fromString(workId)
            )
    }
}
