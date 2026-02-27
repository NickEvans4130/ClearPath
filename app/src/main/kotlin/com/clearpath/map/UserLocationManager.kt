package com.clearpath.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserLocationManager(private val context: Context) {

    /**
     * Returns a cold Flow of [Location] updates.
     * Tries FusedLocationProvider first; falls back to raw GPS for de-Googled devices.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
    fun locationFlow(): Flow<Location> = callbackFlow {
        val fused = tryGetFusedProvider()
        if (fused != null) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { trySend(it) }
                }
            }
            fused.requestLocationUpdates(request, callback, context.mainLooper)
            awaitClose { fused.removeLocationUpdates(callback) }
        } else {
            // Fallback: raw LocationManager GPS provider
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) { trySend(location) }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener)
            awaitClose { lm.removeUpdates(listener) }
        }
    }

    private fun tryGetFusedProvider(): FusedLocationProviderClient? = try {
        LocationServices.getFusedLocationProviderClient(context)
    } catch (e: Exception) {
        null
    }
}
