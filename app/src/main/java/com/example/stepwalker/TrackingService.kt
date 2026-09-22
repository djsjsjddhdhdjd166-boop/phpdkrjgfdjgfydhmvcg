package com.example.stepwalker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.app.NotificationManagerCompat
import com.example.stepwalker.data.DatabaseProvider
import com.example.stepwalker.data.WalkEntity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class TrackingService : Service() {
    companion object {
        const val CHANNEL_ID = "stepwalker_tracking"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.stepwalker.START"
        const val ACTION_STOP = "com.example.stepwalker.STOP"
        const val ACTION_RESET = "com.example.stepwalker.RESET"
        private const val PREFS = "tracking_session"
        private const val KEY_ACTIVE = "active"
        private const val KEY_STARTED = "startedAt"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var stepSensor: StepSensor
    private lateinit var locationTracker: LocationTracker
    private lateinit var dao: com.example.stepwalker.data.WalkDao
    private var lastLocation: Location? = null
    private var lastPersistElapsedMs: Long = 0L
    private var currentStartedAt: Long = 0L

    private val persistRunnable = object : Runnable {
        override fun run() {
            persistLightweightState()
            handler.postDelayed(this, 5000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        dao = DatabaseProvider.get(this).walkDao()
        stepSensor = StepSensor(this) { steps ->
            TrackingStateStore.update { s ->
                val gpsDistance = s.distanceKm
                s.copy(
                    steps = steps,
                    distanceKm = if (s.gpsAvailable) gpsDistance else steps * 0.75 / 1000.0,
                    calories = (steps * 0.04).toInt()
                )
            }
            updateNotification()
        }
        locationTracker = LocationTracker(this, ::onLocation)

        getSystemService<NotificationManager>()?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Трекинг прогулки",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)) {
            startTrackingInternal(preserveSession = true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTrackingInternal(save = true)
            ACTION_RESET -> resetInternal()
            else -> startTrackingInternal(preserveSession = false)
        }
        return START_STICKY
    }

    private fun startTrackingInternal(preserveSession: Boolean) {
        if (TrackingStateStore.state.value.tracking) return

        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) {
            stopSelf()
            return
        }

        if (!preserveSession) {
            stepSensor.beginNewSession()
            lastLocation = null
            currentStartedAt = System.currentTimeMillis()
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putLong(KEY_STARTED, currentStartedAt)
                .apply()
            TrackingStateStore.set(
                WalkState(
                    tracking = true,
                    startedAt = currentStartedAt,
                    stepSensorAvailable = stepSensor.isAvailable
                )
            )
        } else {
            currentStartedAt = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(KEY_STARTED, System.currentTimeMillis())
            TrackingStateStore.update { it.copy(tracking = true, startedAt = currentStartedAt, stepSensorAvailable = stepSensor.isAvailable) }
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= 34) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        stepSensor.start()
        locationTracker.start()
        locationTracker.primeLastLocation()

        val settingsClient = LocationServices.getSettingsClient(this)
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    500L
                ).build()
            )
            .setAlwaysShow(true)
            .build()
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnFailureListener {
                // Требование высокого качества обработано в UI.
            }

        handler.removeCallbacks(persistRunnable)
        handler.post(persistRunnable)
    }

    private fun onLocation(loc: Location) {
        if (!loc.hasAccuracy()) return
        if (loc.accuracy > 20f) return
        if (Build.VERSION.SDK_INT >= 31 && loc.isMock) return

        val previous = lastLocation
        if (previous != null) {
            val elapsedSec = if (loc.elapsedRealtimeNanos > 0L && previous.elapsedRealtimeNanos > 0L) {
                (loc.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000.0
            } else {
                (loc.time - previous.time) / 1000.0
            }
            if (elapsedSec > 0.0) {
                val speed = previous.distanceTo(loc) / elapsedSec
                if (speed > 12.0) return
            }

            val moved = previous.distanceTo(loc)
            if (moved < 1.0f && loc.accuracy > previous.accuracy) return
        }

        val current = TrackingStateStore.state.value
        val extraKm = previous?.distanceTo(loc)?.toDouble()?.div(1000.0) ?: 0.0
        val route = (current.route + (loc.latitude to loc.longitude)).takeLast(5000)
        val gpsSpeed = if (loc.hasSpeed() && loc.speed >= 0f) loc.speed * 3.6 else 0.0

        TrackingStateStore.set(
            current.copy(
                lat = loc.latitude,
                lon = loc.longitude,
                distanceKm = current.distanceKm + extraKm,
                speedKmh = gpsSpeed.toDouble(),
                route = route,
                gpsAvailable = true,
                accuracyM = loc.accuracy.toDouble(),
                verticalAccuracyM = if (Build.VERSION.SDK_INT >= 26 && loc.hasVerticalAccuracy()) {
                    loc.verticalAccuracyMeters.toDouble()
                } else {
                    null
                },
                speedAccuracyMps = if (Build.VERSION.SDK_INT >= 26 && loc.hasSpeedAccuracy()) {
                    loc.speedAccuracyMetersPerSecond.toDouble()
                } else {
                    null
                },
                bearingAccuracyDeg = if (Build.VERSION.SDK_INT >= 26 && loc.hasBearingAccuracy()) {
                    loc.bearingAccuracyDegrees.toDouble()
                } else {
                    null
                },
                satellites = if (Build.VERSION.SDK_INT >= 30 && loc.extras != null) {
                    loc.extras.getInt("satellites", 0).takeIf { it > 0 }
                } else {
                    null
                }
            )
        )
        lastLocation = loc
        updateNotification()
    }

    private fun stopTrackingInternal(save: Boolean) {
        if (!TrackingStateStore.state.value.tracking) return
        stepSensor.stop()
        locationTracker.stop()
        handler.removeCallbacks(persistRunnable)

        val state = TrackingStateStore.state.value
        if (save) {
            val started = state.startedAt ?: currentStartedAt
            scope.launch {
                dao.insert(
                    WalkEntity(
                        startedAt = started,
                        endedAt = System.currentTimeMillis(),
                        steps = state.steps,
                        distanceKm = state.distanceKm,
                        calories = state.calories,
                        avgSpeedKmh = if (state.distanceKm > 0.0 && System.currentTimeMillis() > started) {
                            state.distanceKm / ((System.currentTimeMillis() - started) / 3_600_000.0)
                        } else 0.0
                    )
                )
            }
        }

        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply()
        TrackingStateStore.set(state.copy(tracking = false, speedKmh = 0.0))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun resetInternal() {
        if (TrackingStateStore.state.value.tracking) {
            stepSensor.beginNewSession()
            lastLocation = null
            TrackingStateStore.set(
                WalkState(
                    tracking = true,
                    startedAt = currentStartedAt,
                    stepSensorAvailable = stepSensor.isAvailable
                )
            )
        } else {
            stepSensor.beginNewSession()
            TrackingStateStore.set(WalkState(stepSensorAvailable = stepSensor.isAvailable))
        }
    }

    private fun persistLightweightState() {
        lastPersistElapsedMs = System.currentTimeMillis()
        // The foreground service itself is the durable owner of the live session.
        // The step baseline is persisted independently by StepSensor.
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("StepWalker")
        .setContentText(notificationText())
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun notificationText(): String {
        val s = TrackingStateStore.state.value
        val gps = s.accuracyM?.let { "GPS ±%.0f м".format(it) } ?: "GPS"
        return "${s.steps} шагов · %.2f км · %s".format(s.distanceKm, gps)
    }

    private fun updateNotification() {
        if (!TrackingStateStore.state.value.tracking) return
        if (Build.VERSION.SDK_INT >= 33 && !NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        getSystemService<NotificationManager>()?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        handler.removeCallbacks(persistRunnable)
        stepSensor.stop()
        locationTracker.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
