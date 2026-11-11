package uz.hidoyat.app.feature.prayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
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
                } catch (e: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time }
    }

    private const val PROVIDER_TIMEOUT_MS = 8_000L

    /**
     * Joriy joylashuvni so'raydi: avval GPS (aniq ruxsat bo'lsa), keyin tarmoq provayderi.
     * Hech biri javob bermasa, oxirgi ma'lum joylashuv qaytariladi.
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val providers = buildList {
            if (hasFine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { provider ->
            try {
                manager.isProviderEnabled(provider)
            } catch (e: SecurityException) {
                false
            }
        }

        for (provider in providers) {
            val location = requestSingleLocation(context, manager, provider)
            if (location != null) return location
        }
        return getLastKnownLocation(context)
    }

    @Suppress("MissingPermission")
    private suspend fun requestSingleLocation(
        context: Context,
        manager: LocationManager,
        provider: String
    ): Location? = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
        suspendCancellableCoroutine<Location?> { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (e: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (e: IllegalArgumentException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
