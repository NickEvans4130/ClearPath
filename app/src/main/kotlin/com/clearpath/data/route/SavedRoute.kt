package com.clearpath.data.route

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LatLon(val lat: Double, val lon: Double)

enum class RoutingMode { FOOT, BICYCLE, CAR }

@Serializable
data class CameraEncounter(
    val nodeId: Long,
    val distanceFromRoute: Float,
    val estimatedExposureSeconds: Int,
    val atRoutePosition: Float,     // 0.0–1.0 along route
)

@Entity(tableName = "saved_routes")
@TypeConverters(RouteTypeConverters::class)
data class SavedRoute(
    @PrimaryKey
    val id: String,

    val name: String,

    @Embedded(prefix = "origin_")
    val origin: LatLon,

    @Embedded(prefix = "dest_")
    val destination: LatLon,

    val waypoints: List<LatLon> = emptyList(),

    val polyline: List<LatLon> = emptyList(),

    @ColumnInfo(name = "distance_metres")
    val distanceMetres: Int = 0,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int = 0,

    @ColumnInfo(name = "exposure_score")
    val exposureScore: Float = 0f,

    @ColumnInfo(name = "camera_count")
    val cameraCount: Int = 0,

    @ColumnInfo(name = "camera_encounters")
    val cameraEncounters: List<CameraEncounter> = emptyList(),

    @ColumnInfo(name = "routing_mode")
    val routingMode: RoutingMode = RoutingMode.FOOT,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    val alias: String? = null,
)

class RouteTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun fromLatLonList(value: List<LatLon>): String = json.encodeToString(value)
    @TypeConverter fun toLatLonList(value: String): List<LatLon> = json.decodeFromString(value)

    @TypeConverter fun fromEncounterList(value: List<CameraEncounter>): String = json.encodeToString(value)
    @TypeConverter fun toEncounterList(value: String): List<CameraEncounter> = json.decodeFromString(value)
}
