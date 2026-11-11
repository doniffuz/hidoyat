package uz.hidoyat.app.feature.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Qibla yo'nalishini hisoblash. Internet talab qilmaydi. */
object QiblaCalculator {

    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    const val DIRECTION_COUNT = 8

    private const val FULL_CIRCLE = 360.0
    private const val HALF_CIRCLE = 180.0
    private const val DIRECTION_SECTOR = FULL_CIRCLE / DIRECTION_COUNT

    /** Foydalanuvchi nuqtasidan Ka'baga haqiqiy shimolga nisbatan bearing, 0..360 daraja. */
    fun bearingToKaaba(userLatitude: Double, userLongitude: Double): Double {
        val kaabaLat = Math.toRadians(KAABA_LATITUDE)
        val userLat = Math.toRadians(userLatitude)
        val deltaLon = Math.toRadians(KAABA_LONGITUDE - userLongitude)

        val y = sin(deltaLon) * cos(kaabaLat)
        val x = cos(userLat) * sin(kaabaLat) - sin(userLat) * cos(kaabaLat) * cos(deltaLon)

        return normalize360(Math.toDegrees(atan2(y, x)))
    }

    /** Burchakni 0..360 oralig'iga keltiradi. */
    fun normalize360(degrees: Double): Double {
        val result = degrees % FULL_CIRCLE
        return if (result < 0) result + FULL_CIRCLE else result
    }

    /** `target - current` farqi, -180..180 oralig'ida. */
    fun signedDifference(target: Double, current: Double): Double {
        val diff = normalize360(target - current)
        return if (diff > HALF_CIRCLE) diff - FULL_CIRCLE else diff
    }

    /** 8 ta asosiy yo'nalishdan indeks: 0 = shimol, 1 = shimoli-sharq, ... */
    fun directionIndex(bearing: Double): Int =
        ((normalize360(bearing) + DIRECTION_SECTOR / 2) / DIRECTION_SECTOR).toInt() % DIRECTION_COUNT
}
