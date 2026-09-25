package com.yourname.trackr.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Produces a smoothed compass heading in degrees (0-360, 0 = magnetic north).
 *
 * Prefers Sensor.TYPE_ROTATION_VECTOR - a sensor-fusion virtual sensor that combines
 * accelerometer + magnetometer (+ gyroscope where available) internally, far more stable
 * than reading them separately. Falls back to manually combining TYPE_ACCELEROMETER +
 * TYPE_MAGNETIC_FIELD via getRotationMatrix()/getOrientation() only if this device has no
 * rotation-vector sensor.
 */
class CompassSensor(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /** True if this device has a rotation-vector sensor and we're using it; false means the
     *  accelerometer+magnetometer fallback path is active. */
    val usingRotationVector: Boolean = rotationVectorSensor != null

    private var onHeadingChanged: ((Float) -> Unit)? = null
    private var smoothedHeading: Float? = null

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var lastAccel: FloatArray? = null
    private var lastMagnetic: FloatArray? = null

    private val rotationVectorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            emitHeading(orientationAngles[0])
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val fallbackListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> lastAccel = event.values.clone()
                Sensor.TYPE_MAGNETIC_FIELD -> lastMagnetic = event.values.clone()
            }
            val accel = lastAccel
            val magnetic = lastMagnetic
            if (accel != null && magnetic != null) {
                val success = SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnetic)
                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    emitHeading(orientationAngles[0])
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun emitHeading(azimuthRadians: Float) {
        val rawDegrees = (Math.toDegrees(azimuthRadians.toDouble()).toFloat() % 360f + 360f) % 360f
        val previous = smoothedHeading
        val smoothed = if (previous == null) {
            rawDegrees
        } else {
            // Low-pass filter: nudge the smoothed value toward the new reading rather than
            // snapping to it, so the needle doesn't visibly jitter.
            val delta = shortestAngleDelta(rawDegrees, previous)
            val nudged = previous + LOW_PASS_ALPHA * delta
            (nudged % 360f + 360f) % 360f
        }
        smoothedHeading = smoothed
        onHeadingChanged?.invoke(smoothed)
    }

    /** Shortest signed angular difference from `from` to `to`, in the range (-180, 180]. */
    private fun shortestAngleDelta(to: Float, from: Float): Float {
        var delta = (to - from) % 360f
        if (delta < -180f) delta += 360f
        if (delta > 180f) delta -= 360f
        return delta
    }

    fun start(onHeadingChanged: (Float) -> Unit) {
        this.onHeadingChanged = onHeadingChanged
        smoothedHeading = null
        lastAccel = null
        lastMagnetic = null

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(rotationVectorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelerometer?.let { sensorManager.registerListener(fallbackListener, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sensorManager.registerListener(fallbackListener, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(rotationVectorListener)
        sensorManager.unregisterListener(fallbackListener)
        onHeadingChanged = null
    }

    companion object {
        private const val LOW_PASS_ALPHA = 0.15f
    }
}
