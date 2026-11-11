package uz.hidoyat.app.feature.prayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object PrayerLocation {

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null

        val manager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            try {
                manager.isProviderEnabled(provider)
            } catch (_: Exception) {
                false
            }
        }

        if (providers.isEmpty()) {
            return getLastKnownLocation(context)
        }

        return withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine { continuation ->

                var bestLocation: Location? = getLastKnownLocation(context)
                var finished = false

                lateinit var listener: LocationListener

                fun finish(location: Location?) {
                    if (finished) return
                    finished = true

                    try {
                        manager.removeUpdates(listener)
                    } catch (_: Exception) {
                    }

                    continuation.resume(location ?: bestLocation)
                }

                listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (
                            bestLocation == null ||
                            location.accuracy < bestLocation!!.accuracy
                        ) {
                            bestLocation = location
                        }

                        if (location.accuracy <= 100f) {
                            finish(location)
                        }
                    }
                }

                try {
                    for (provider in providers) {
                        try {
                            manager.requestLocationUpdates(
                                provider,
                                1000L,
                                5f,
                                listener,
                                Looper.getMainLooper()
                            )
                        } catch (_: SecurityException) {
                        }
                    }

                    if (
                        bestLocation != null &&
                        bestLocation!!.accuracy <= 100f
                    ) {
                        finish(bestLocation)
                    }
                } catch (_: Exception) {
                    finish(null)
                }

                continuation.invokeOnCancellation {
                    try {
                        manager.removeUpdates(listener)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    @Suppress("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        if (!hasPermission(context)) return null

        val manager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        val providers = manager.getProviders(true)

        return providers
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: Exception) {
                    null
                }
            }
            .maxByOrNull { it.time }
    }
}
