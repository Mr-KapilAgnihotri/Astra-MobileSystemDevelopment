package com.yourname.trackr.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Wraps the accelerometer and turns raw acceleration samples into step
 * events: magnitude of the (x,y,z) vector, gravity subtracted out, peaks
 * above a threshold count as a step as long as they're outside the debounce
 * window (guards against one physical step producing several peaks).
 *
 * Independently of step detection, it can also report the raw (pre-threshold,
 * pre-debounce) magnitude at a throttled rate via onSample - a second, unrelated
 * output from the same listener, meant for graphing rather than counting.
 */
class StepSensor(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onStepDetected: (() -> Unit)? = null
    private var onSample: ((Float) -> Unit)? = null
    private var lastStepTimestampMs = 0L
    private var lastSampleTimestampMs = 0L

    val isAvailable: Boolean get() = accelerometer != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)
            val nowMs = event.timestamp / 1_000_000L

            // Throttled raw output for graphing, independent of step detection below.
            if (nowMs - lastSampleTimestampMs >= SAMPLE_INTERVAL_MS) {
                lastSampleTimestampMs = nowMs
                onSample?.invoke(magnitude)
            }

            val delta = magnitude - GRAVITY_BASELINE
            if (delta > PEAK_THRESHOLD && nowMs - lastStepTimestampMs > DEBOUNCE_MS) {
                lastStepTimestampMs = nowMs
                onStepDetected?.invoke()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start(onStepDetected: () -> Unit, onSample: ((Float) -> Unit)? = null) {
        this.onStepDetected = onStepDetected
        this.onSample = onSample
        lastStepTimestampMs = 0L
        lastSampleTimestampMs = 0L
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        onStepDetected = null
        onSample = null
    }

    companion object {
        private const val GRAVITY_BASELINE = 9.8
        private const val PEAK_THRESHOLD = 2.0
        private const val DEBOUNCE_MS = 300L
        private const val SAMPLE_INTERVAL_MS = 400L
    }
}
