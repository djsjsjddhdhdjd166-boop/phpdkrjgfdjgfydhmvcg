package com.example.stepwalker

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwalker.data.DatabaseProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WalkState(
    val steps: Int = 0,
    val distanceKm: Double = 0.0,
    val calories: Int = 0,
    val speedKmh: Double = 0.0,
    val lat: Double? = null,
    val lon: Double? = null,
    val route: List<Pair<Double, Double>> = emptyList(),
    val tracking: Boolean = false,
    val gpsAvailable: Boolean = false,
    val stepSensorAvailable: Boolean = false,
    val startedAt: Long? = null,
    val accuracyM: Double? = null,
    val verticalAccuracyM: Double? = null,
    val speedAccuracyMps: Double? = null,
    val bearingAccuracyDeg: Double? = null,
    val satellites: Int? = null
)

class StepViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = DatabaseProvider.get(app).walkDao()
    val state: StateFlow<WalkState> = TrackingStateStore.state
    val history = dao.observeAll()

    fun startTracking() {
        ContextCompat.startForegroundService(
            getApplication(),
            Intent(getApplication(), TrackingService::class.java)
                .setAction(TrackingService.ACTION_START)
        )
    }

    fun stopTracking() {
        getApplication<Application>().startService(
            Intent(getApplication(), TrackingService::class.java)
                .setAction(TrackingService.ACTION_STOP)
        )
    }

    fun reset() {
        getApplication<Application>().startService(
            Intent(getApplication(), TrackingService::class.java)
                .setAction(TrackingService.ACTION_RESET)
        )
    }
}
