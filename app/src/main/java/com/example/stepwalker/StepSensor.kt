package com.example.stepwalker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Primary source: hardware TYPE_STEP_COUNTER.
 * Fallback: TYPE_STEP_DETECTOR when a counter is unavailable.
 * The fallback is intentionally not treated as equivalent to the counter.
 */
class StepSensor(
    context: Context,
    private val onSteps: (Int) -> Unit
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetector =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val prefs =
        appContext.getSharedPreferences("step_sensor", Context.MODE_PRIVATE)

    private var baseline = prefs.getFloat("baseline", -1f)
    private var detectorSteps = 0
    private var usingCounter = stepCounter != null

    val isAvailable: Boolean
        get() = stepCounter != null || stepDetector != null

    val sourceName: String
        get() = when {
            stepCounter != null -> "TYPE_STEP_COUNTER"
            stepDetector != null -> "TYPE_STEP_DETECTOR"
            else -> "нет аппаратного шагомера"
        }

    fun beginNewSession() {
        baseline = -1f
        detectorSteps = 0
        prefs.edit().remove("baseline").apply()
    }

    fun start() {
        usingCounter = stepCounter != null
        val sensor = stepCounter ?: stepDetector ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val raw = event.values[0]
                if (baseline < 0f || raw < baseline) {
                    baseline = raw
                    prefs.edit().putFloat("baseline", baseline).apply()
                }
                onSteps((raw - baseline).toInt().coerceAtLeast(0))
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                if (!usingCounter) {
                    detectorSteps++
                    onSteps(detectorSteps)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
