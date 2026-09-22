package com.example.stepwalker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/** Максимальная точность для ходьбы/бега: частые high-accuracy апдейты без батчинга. */
class LocationTracker(
    context: Context,
    private val onLocation: (Location) -> Unit
) {
    private val appContext = context.applicationContext
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(onLocation)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500L
        )
            .setMinUpdateIntervalMillis(250L)
            .setMaxUpdateDelayMillis(0L)
            .setMinUpdateDistanceMeters(0.0f)
            .setWaitForAccurateLocation(true)
            .build()

        client.requestLocationUpdates(request, callback, appContext.mainLooper)
    }

    @SuppressLint("MissingPermission")
    fun primeLastLocation() {
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) onLocation(loc)
        }
    }

    fun stop() {
        client.removeLocationUpdates(callback)
    }
}
