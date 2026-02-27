package com.clearpath.export

import android.content.Context
import com.clearpath.data.camera.CameraNode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

object GeoJsonExporter {

    private val json = Json { prettyPrint = true }

    /**
     * Export user-tagged cameras to a GeoJSON FeatureCollection.
     * Output file is written to app's external exports directory.
     */
    fun exportUserCameras(context: Context, cameras: List<CameraNode>): File {
        val geoJson = buildFeatureCollection(cameras)
        val dir  = File(context.getExternalFilesDir(null), "exports").also { it.mkdirs() }
        val file = File(dir, "clearpath_cameras_${System.currentTimeMillis()}.geojson")
        file.writeText(json.encodeToString(geoJson))
        return file
    }

    private fun buildFeatureCollection(cameras: List<CameraNode>): JsonObject =
        buildJsonObject {
            put("type", "FeatureCollection")
            put("features", buildJsonArray {
                cameras.forEach { cam -> add(cameraToFeature(cam)) }
            })
        }

    private fun cameraToFeature(cam: CameraNode): JsonObject =
        buildJsonObject {
            put("type", "Feature")
            put("geometry", buildJsonObject {
                put("type", "Point")
                put("coordinates", buildJsonArray {
                    add(JsonPrimitive(cam.lon))
                    add(JsonPrimitive(cam.lat))
                })
            })
            put("properties", buildJsonObject {
                put("id", cam.id)
                put("man_made", "surveillance")
                put("surveillance:type", cam.type.name.lowercase())
                cam.direction?.let { put("direction", it) }
                cam.operator?.let  { put("operator", it) }
                cam.notes?.let     { put("note", it) }
                put("source", cam.source.name)
                put("confidence", cam.confidence)
            })
        }

    /**
     * Parse an imported GeoJSON file.
     * Returns the raw string for CameraRepository.parseGeoJson().
     */
    fun readGeoJsonFile(file: File): String = file.readText()
}
