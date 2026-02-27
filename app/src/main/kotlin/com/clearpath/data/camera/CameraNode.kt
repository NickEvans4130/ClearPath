package com.clearpath.data.camera

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CameraType { FIXED, PTZ, DOME, ANPR, UNKNOWN }
enum class NodeSource { OSM, USER_TAGGED, USER_ESTIMATED }

@Entity(tableName = "camera_nodes")
data class CameraNode(
    @PrimaryKey
    val id: Long,

    val lat: Double,
    val lon: Double,

    val type: CameraType = CameraType.UNKNOWN,

    // 0–359° if known from OSM tag "direction"
    val direction: Int? = null,

    val operator: String? = null,

    @ColumnInfo(name = "coverage_radius")
    val coverageRadius: Int = 30,

    @ColumnInfo(name = "cone_angle")
    val coneAngle: Int = 90,

    val source: NodeSource = NodeSource.OSM,

    // 0.0–1.0
    val confidence: Float = 0.8f,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),

    val notes: String? = null,
)
