package com.clearpath.data.camera

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class CameraRepository(
    private val dao: CameraDao,
    private val context: Context,
) {

    fun observeAll(): Flow<List<CameraNode>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    fun observeUserTagged(): Flow<List<CameraNode>> = dao.observeUserTagged()

    suspend fun getInBounds(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
    ): List<CameraNode> = dao.getInBounds(minLat, maxLat, minLon, maxLon)

    suspend fun getAll(): List<CameraNode> = dao.getAll()

    suspend fun insert(camera: CameraNode) = dao.insert(camera)

    suspend fun update(camera: CameraNode) = dao.update(camera)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun replaceOsmData(cameras: List<CameraNode>) {
        dao.deleteAllOsm()
        dao.insertAll(cameras)
    }

    /** Seed from bundled assets/bundled/uk_cameras_snapshot.geojson on first launch. */
    suspend fun seedBundledDataIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countOsmCameras() > 0) return@withContext
        try {
            val json = context.assets.open("bundled/uk_cameras_snapshot.geojson")
                .bufferedReader().readText()
            val nodes = parseGeoJson(json)
            dao.insertAll(nodes)
        } catch (e: Exception) {
            // Asset may not exist in development — silently skip
        }
    }

    // ── GeoJSON parsing ───────────────────────────────────────────────────────

    fun parseGeoJson(geoJson: String): List<CameraNode> {
        val root = Json.parseToJsonElement(geoJson).jsonObject
        val features = root["features"]?.jsonArray ?: return emptyList()
        return features.mapNotNull { element ->
            try {
                val feature = element.jsonObject
                val geometry = feature["geometry"]?.jsonObject ?: return@mapNotNull null
                val coords = geometry["coordinates"]?.jsonArray ?: return@mapNotNull null
                val lon = coords[0].jsonPrimitive.double
                val lat = coords[1].jsonPrimitive.double
                val props = feature["properties"]?.jsonObject ?: JsonObject(emptyMap())

                val rawId = props["id"]?.jsonPrimitive?.longOrNull
                    ?: props["osm_id"]?.jsonPrimitive?.longOrNull
                    ?: (lat * 1_000_000 + lon * 1_000_000).toLong()

                val type = parseCameraType(props)
                val direction = props["direction"]?.jsonPrimitive?.intOrNull
                val operator = props["operator"]?.jsonPrimitive?.content

                CameraNode(
                    id = rawId,
                    lat = lat,
                    lon = lon,
                    type = type,
                    direction = direction,
                    operator = operator,
                    source = NodeSource.OSM,
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseCameraType(props: JsonObject): CameraType {
        val surveillanceType = props["surveillance:type"]?.jsonPrimitive?.content
            ?: props["camera:type"]?.jsonPrimitive?.content
        val manMade = props["man_made"]?.jsonPrimitive?.content
        return when {
            surveillanceType?.contains("ANPR", ignoreCase = true) == true -> CameraType.ANPR
            surveillanceType?.contains("ALPR", ignoreCase = true) == true -> CameraType.ANPR
            surveillanceType?.contains("anpr", ignoreCase = true) == true -> CameraType.ANPR
            surveillanceType == "dome" -> CameraType.DOME
            surveillanceType == "ptz" -> CameraType.PTZ
            surveillanceType == "fixed" -> CameraType.FIXED
            manMade == "camera" -> CameraType.FIXED
            else -> CameraType.UNKNOWN
        }
    }
}
