package com.clearpath.overpass

import com.clearpath.data.camera.CameraNode
import com.clearpath.data.camera.CameraType
import com.clearpath.data.camera.NodeSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList(),
)

@Serializable
data class OverpassElement(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val tags: Map<String, String> = emptyMap(),
)

object OverpassParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawJson: String): List<CameraNode> {
        val response = json.decodeFromString<OverpassResponse>(rawJson)
        return response.elements.map { element ->
            val tags = element.tags
            CameraNode(
                id             = element.id,
                lat            = element.lat,
                lon            = element.lon,
                type           = resolveCameraType(tags),
                direction      = tags["direction"]?.toIntOrNull(),
                operator       = tags["operator"],
                coverageRadius = resolveCoverageRadius(tags),
                coneAngle      = 90,
                source         = NodeSource.OSM,
                confidence     = 0.8f,
                notes          = buildNotes(tags),
            )
        }
    }

    private fun resolveCameraType(tags: Map<String, String>): CameraType {
        val surveillanceType = tags["surveillance:type"]?.lowercase()
        val cameraType       = tags["camera:type"]?.lowercase()
        val combined = surveillanceType ?: cameraType
        return when {
            combined == null            -> CameraType.UNKNOWN
            combined.contains("anpr")   -> CameraType.ANPR
            combined.contains("alpr")   -> CameraType.ANPR
            combined == "ptz"           -> CameraType.PTZ
            combined == "dome"          -> CameraType.DOME
            combined == "fixed"         -> CameraType.FIXED
            combined == "outdoor"       -> CameraType.FIXED
            combined == "indoor"        -> CameraType.FIXED
            tags["man_made"] == "camera"-> CameraType.FIXED
            else                        -> CameraType.UNKNOWN
        }
    }

    private fun resolveCoverageRadius(tags: Map<String, String>): Int {
        // ANPR cameras typically have longer effective range
        val type = tags["surveillance:type"]?.lowercase() ?: tags["camera:type"]?.lowercase()
        return when {
            type?.contains("anpr") == true -> 50
            type == "ptz"                  -> 40
            else                           -> 30
        }
    }

    private fun buildNotes(tags: Map<String, String>): String? {
        val zone = tags["surveillance:zone"]
        val note = tags["note"]
        return listOfNotNull(zone?.let { "Zone: $it" }, note).joinToString("; ").ifBlank { null }
    }
}
