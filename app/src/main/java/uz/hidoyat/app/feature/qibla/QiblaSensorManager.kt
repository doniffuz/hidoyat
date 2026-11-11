package uz.hidoyat.app.feature.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/** Kompas o'qishi: magnit shimolga nisbatan azimuth (0..360) va sensor aniqligi. */
data class CompassReading(val azimuthDegrees: Float, val accuracy: Int)

/**
 * Akselerometr va magnit maydon sensorlaridan azimuth hisoblaydi.
 * Sensorlar faqat [readings] oqimi yig'ilayotgan vaqtda ro'yxatdan o'tadi.
 */
class QiblaSensorManager(context: Context) {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val isAvailable: Boolean
        get() = sensorManager != null && accelerometer != null && magnetometer != null

    fun readings(): Flow<CompassReading> = callbackFlow {
        val manager = sensorManager
        val accel = accelerometer
        val magnet = magnetometer
        if (manager == null || accel == null || magnet == null) {
            close(IllegalStateException("Compass sensors unavailable"))
            return@callbackFlow
        }

        val gravity = FloatArray(VECTOR_SIZE)
        val geomagnetic = FloatArray(VECTOR_SIZE)
        val rotation = FloatArray(MATRIX_SIZE)
        val remapped = FloatArray(MATRIX_SIZE)
        val orientation = FloatArray(VECTOR_SIZE)
        var hasGravity = false
        var hasGeomagnetic = false
        var magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
        var lastEmit = 0L
        var smoothed = Float.NaN

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPass(event.values, gravity, hasGravity, ACCELEROMETER_ALPHA)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lowPass(event.values, geomagnetic, hasGeomagnetic, MAGNETIC_ALPHA)
                        hasGeomagnetic = true
                        magneticAccuracy = event.accuracy
                    }
                    else -> return
                }

                val now = SystemClock.elapsedRealtime()
                if (!hasGravity || !hasGeomagnetic || now - lastEmit < MIN_EMIT_INTERVAL_MS) return
                if (!SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) return
                lastEmit = now

                val matrix = remapForDisplay(rotation, remapped)
                SensorManager.getOrientation(matrix, orientation)
                val azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + FULL_CIRCLE) % FULL_CIRCLE)

                smoothed = if (smoothed.isNaN()) {
                    azimuth
                } else {
                    val diff = ((azimuth - smoothed) % FULL_CIRCLE + FULL_CIRCLE + HALF_CIRCLE) % FULL_CIRCLE - HALF_CIRCLE
                    ((smoothed + diff * AZIMUTH_ALPHA) % FULL_CIRCLE + FULL_CIRCLE) % FULL_CIRCLE
                }
                trySend(CompassReading(smoothed, magneticAccuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) magneticAccuracy = accuracy
            }
        }

        val accelOk = manager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        val magnetOk = manager.registerListener(listener, magnet, SensorManager.SENSOR_DELAY_GAME)
        if (!accelOk || !magnetOk) {
            manager.unregisterListener(listener)
            close(IllegalStateException("Compass sensors could not be registered"))
            return@callbackFlow
        }

        awaitClose { manager.unregisterListener(listener) }
    }.buffer(Channel.CONFLATED)

    private fun displayRotation(): Int {
        val displays = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        return displays?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
    }

    private fun remapForDisplay(input: FloatArray, output: FloatArray): FloatArray {
        val (axisX, axisY) = when (displayRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> return input
        }
        return if (SensorManager.remapCoordinateSystem(input, axisX, axisY, output)) output else input
    }

    private fun lowPass(input: FloatArray, output: FloatArray, initialized: Boolean, alpha: Float) {
        for (i in 0 until VECTOR_SIZE) {
            output[i] = if (initialized) output[i] + alpha * (input[i] - output[i]) else input[i]
        }
    }

    private companion object {
        const val VECTOR_SIZE = 3
        const val MATRIX_SIZE = 9
        const val FULL_CIRCLE = 360f
        const val HALF_CIRCLE = 180f
        const val MIN_EMIT_INTERVAL_MS = 33L
        const val ACCELEROMETER_ALPHA = 0.15f
        const val MAGNETIC_ALPHA = 0.2f
        const val AZIMUTH_ALPHA = 0.3f
    }
}
