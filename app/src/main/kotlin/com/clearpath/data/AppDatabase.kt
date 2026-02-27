package com.clearpath.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clearpath.data.camera.CameraDao
import com.clearpath.data.camera.CameraNode
import com.clearpath.data.route.RouteDao
import com.clearpath.data.route.RouteTypeConverters
import com.clearpath.data.route.SavedRoute
import com.clearpath.geocoding.GeocodingCacheDao
import com.clearpath.geocoding.GeocodingResult

@Database(
    entities = [
        CameraNode::class,
        SavedRoute::class,
        GeocodingResult::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RouteTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cameraDao(): CameraDao
    abstract fun routeDao(): RouteDao
    abstract fun geocodingCacheDao(): GeocodingCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clearpath.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
