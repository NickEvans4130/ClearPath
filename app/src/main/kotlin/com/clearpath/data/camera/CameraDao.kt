package com.clearpath.data.camera

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cameras: List<CameraNode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(camera: CameraNode)

    @Update
    suspend fun update(camera: CameraNode)

    @Query("SELECT * FROM camera_nodes")
    fun observeAll(): Flow<List<CameraNode>>

    @Query("SELECT * FROM camera_nodes")
    suspend fun getAll(): List<CameraNode>

    /** Spatial bounding-box query — fast pre-filter before radius check. */
    @Query("""
        SELECT * FROM camera_nodes
        WHERE lat BETWEEN :minLat AND :maxLat
          AND lon BETWEEN :minLon AND :maxLon
    """)
    suspend fun getInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<CameraNode>

    @Query("SELECT * FROM camera_nodes WHERE source = 'USER_TAGGED' OR source = 'USER_ESTIMATED'")
    fun observeUserTagged(): Flow<List<CameraNode>>

    @Query("SELECT COUNT(*) FROM camera_nodes")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM camera_nodes WHERE source = 'OSM'")
    suspend fun countOsmCameras(): Int

    @Query("DELETE FROM camera_nodes WHERE source = 'OSM'")
    suspend fun deleteAllOsm()

    @Query("DELETE FROM camera_nodes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM camera_nodes WHERE id = :id")
    suspend fun getById(id: Long): CameraNode?
}
