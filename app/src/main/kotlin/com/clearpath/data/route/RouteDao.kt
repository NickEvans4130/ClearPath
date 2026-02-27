package com.clearpath.data.route

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: SavedRoute)

    @Query("SELECT * FROM saved_routes ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SavedRoute>>

    @Query("SELECT * FROM saved_routes WHERE alias = :alias ORDER BY created_at DESC")
    fun observeByAlias(alias: String): Flow<List<SavedRoute>>

    @Query("SELECT * FROM saved_routes WHERE alias IS NULL ORDER BY created_at DESC")
    fun observeNoAlias(): Flow<List<SavedRoute>>

    @Query("SELECT * FROM saved_routes WHERE id = :id")
    suspend fun getById(id: String): SavedRoute?

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM saved_routes ORDER BY exposure_score DESC LIMIT 1")
    suspend fun getMostSurveilled(): SavedRoute?

    @Query("SELECT DISTINCT alias FROM saved_routes WHERE alias IS NOT NULL")
    fun observeAliases(): Flow<List<String>>

    @Query("""
        SELECT SUM(
            (SELECT SUM(estimated_exposure_seconds)
             FROM json_each(camera_encounters))
        )
        FROM saved_routes
    """)
    suspend fun getTotalExposureSeconds(): Int?
}
