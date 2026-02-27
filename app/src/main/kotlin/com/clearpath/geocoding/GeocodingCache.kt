package com.clearpath.geocoding

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "geocoding_cache")
data class GeocodingResult(
    @PrimaryKey
    val query: String,
    val lat: Double,
    val lon: Double,
    val displayName: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Dao
interface GeocodingCacheDao {
    @Query("SELECT * FROM geocoding_cache WHERE query = :query LIMIT 1")
    suspend fun get(query: String): GeocodingResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: GeocodingResult)
}

class GeocodingCache(private val dao: GeocodingCacheDao) {
    suspend fun get(query: String): GeocodingResult? = dao.get(query)
    suspend fun put(result: GeocodingResult) = dao.insert(result)
}
