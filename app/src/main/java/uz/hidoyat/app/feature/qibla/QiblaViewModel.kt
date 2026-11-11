package uz.hidoyat.app.feature.qibla

import android.app.Application
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hidoyat.app.feature.prayer.PrayerLocation

enum class QiblaStatus { LOADING, LOCATION_REQUIRED, SENSOR_UNAVAILABLE, CALIBRATION, READY, ERROR }

data class QiblaUiState(
    val status: QiblaStatus = QiblaStatus.LOADING,
    /** Haqiqiy shimolga nisbatan Qibla bearing, 0..360. */
    val qiblaBearing: Float = 0f,
    val directionIndex: Int = 0,
    val locationAccuracyMeters: Float? = null,
    val isLocationAccuracyLow: Boolean = false,
    val isAligned: Boolean = false,
    val permissionDenied: Boolean = false
)

class QiblaViewModel(app: Application) : AndroidViewModel(app) {

    private val sensors = QiblaSensorManager(app)

    private val _state = MutableStateFlow(QiblaUiState())
    val state: StateFlow<QiblaUiState> = _state.asStateFlow()

    /** Haqiqiy shimolga nisbatan telefon yo'nalishi. Tez yangilanadi, shuning uchun alohida oqim. */
    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading.asStateFlow()

    private var sensorJob: Job? = null
    private var locationJob: Job? = null

    private var hasLocation = false
    private var bearing = 0f
    private var declination = 0f
    private var isAligned = false
    private var needsCalibration = false
    private var lowAccuracySince = 0L

    fun onScreenStarted() {
        if (!sensors.isAvailable) {
            _state.update { it.copy(status = QiblaStatus.SENSOR_UNAVAILABLE) }
            return
        }
        if (!hasLocation && !PrayerLocation.hasPermission(getApplication<Application>())) {
            _state.update { it.copy(status = QiblaStatus.LOCATION_REQUIRED) }
            return
        }
        if (!hasLocation) _state.update { it.copy(status = QiblaStatus.LOADING) }
        startSensors()
        refreshLocation()
    }

    fun onScreenStopped() {
        sensorJob?.cancel()
        locationJob?.cancel()
        sensorJob = null
        locationJob = null
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _state.update { it.copy(permissionDenied = false) }
            onScreenStarted()
        } else {
            _state.update { it.copy(status = QiblaStatus.LOCATION_REQUIRED, permissionDenied = true) }
        }
    }

    fun retry() = onScreenStarted()

    private fun startSensors() {
        sensorJob?.cancel()
        sensorJob = viewModelScope.launch {
            sensors.readings()
                .catch { _state.update { it.copy(status = QiblaStatus.SENSOR_UNAVAILABLE) } }
                .collect { reading -> handleReading(reading) }
        }
    }

    private fun refreshLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val app = getApplication<Application>()
            if (!hasLocation) {
                val cached = withContext(Dispatchers.IO) { PrayerLocation.getLastKnownLocation(app) }
                if (cached != null) applyLocation(cached)
            }
            val fresh = PrayerLocation.getCurrentLocation(app)
            if (fresh != null) {
                applyLocation(fresh)
            } else if (!hasLocation) {
                _state.update { it.copy(status = QiblaStatus.ERROR) }
            }
        }
    }

    private fun applyLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        bearing = QiblaCalculator.bearingToKaaba(latitude, longitude).toFloat()
        declination = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        ).declination
        hasLocation = true

        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        _state.update {
            it.copy(
                status = if (needsCalibration) QiblaStatus.CALIBRATION else QiblaStatus.READY,
                qiblaBearing = bearing,
                directionIndex = QiblaCalculator.directionIndex(bearing.toDouble()),
                locationAccuracyMeters = accuracy,
                isLocationAccuracyLow = accuracy != null && accuracy > LOW_ACCURACY_METERS,
                permissionDenied = false
            )
        }
    }

    private fun handleReading(reading: CompassReading) {
        val trueHeading = QiblaCalculator
            .normalize360((reading.azimuthDegrees + declination).toDouble()).toFloat()
        _heading.value = trueHeading

        updateCalibration(reading.accuracy)
        updateAlignment(trueHeading)
    }

    private fun updateCalibration(accuracy: Int) {
        val now = SystemClock.elapsedRealtime()
        val needs = if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            if (lowAccuracySince == 0L) lowAccuracySince = now
            now - lowAccuracySince >= CALIBRATION_DEBOUNCE_MS
        } else {
            lowAccuracySince = 0L
            false
        }
        if (needs == needsCalibration) return
        needsCalibration = needs
        _state.update {
            if (it.status == QiblaStatus.READY || it.status == QiblaStatus.CALIBRATION) {
                it.copy(status = if (needs) QiblaStatus.CALIBRATION else QiblaStatus.READY)
            } else {
                it
            }
        }
    }

    private fun updateAlignment(trueHeading: Float) {
        if (!hasLocation) return
        val diff = abs(QiblaCalculator.signedDifference(bearing.toDouble(), trueHeading.toDouble()))
        val aligned = if (isAligned) diff <= ALIGNED_EXIT_DEGREES else diff <= ALIGNED_ENTER_DEGREES
        if (aligned == isAligned) return
        isAligned = aligned
        _state.update { it.copy(isAligned = aligned) }
    }

    override fun onCleared() {
        onScreenStopped()
        super.onCleared()
    }

    private companion object {
        const val LOW_ACCURACY_METERS = 3_000f
        const val CALIBRATION_DEBOUNCE_MS = 1_500L
        const val ALIGNED_ENTER_DEGREES = 3.0
        const val ALIGNED_EXIT_DEGREES = 6.0
    }
}
