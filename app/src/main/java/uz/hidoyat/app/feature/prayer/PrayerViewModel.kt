package uz.hidoyat.app.feature.prayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrayerViewModel(app: Application) : AndroidViewModel(app) {

    private val regions = UzbekRegionsRepository(app)
    private val repository = PrayerRepository()
    private val cache = PrayerCache(app)

    private val _state = MutableStateFlow(
        PrayerUiState(
            provinces = regions.getRegions(),
            namozRegions = regions.getNamozRegions()
        )
    )
    val state: StateFlow<PrayerUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    private data class Target(
        val key: String,
        val slug: String,
        val label: String,
        val latitude: Double,
        val longitude: Double
    )

    init {
        val saved = cache.loadSelection()
        val province = saved.provinceId?.let(regions::findRegion)
        if (province != null) {
            val district = saved.districtId
                ?.let(regions::findDistrict)
                ?.takeIf { it.parentId == province.id }
            val manual = saved.manualSlug?.let(regions::findNamozRegion)
            applySelection(province, district, manual)
        }
    }

    fun selectProvince(id: String) {
        val province = regions.findRegion(id) ?: return
        if (province.id == _state.value.selectedProvince?.id) return
        applySelection(province, null, null)
        persistSelection()
    }

    fun selectDistrict(id: String) {
        val province = _state.value.selectedProvince ?: return
        val district = regions.findDistrict(id)?.takeIf { it.parentId == province.id } ?: return
        applySelection(province, district, null)
        persistSelection()
        load()
    }

    fun selectManualRegion(slug: String) {
        val state = _state.value
        val province = state.selectedProvince ?: return
        val district = state.selectedDistrict ?: return
        val region = regions.findNamozRegion(slug) ?: return
        applySelection(province, district, region)
        persistSelection()
        load()
    }

    fun refresh() = load()

    /** Ekran ochilganda: ma'lumot yo'q, saqlangan nusxa yoki bugundan eski bo'lsa yangilaydi. */
    fun onScreenShown() {
        val s = _state.value
        if (targetOf(s) == null || s.isLoading) return
        val schedule = s.schedule
        if (schedule == null || s.fromCache || s.error != null || !isSavedToday(schedule.updatedAt)) {
            load()
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) locate() else _state.update { it.copy(locationHint = LocationHint.PERMISSION_DENIED) }
    }

    /** Oxirgi ma'lum joylashuvga eng yaqin district markazini tanlaydi (taxminiy). */
    fun locate() {
        viewModelScope.launch {
            val location = withContext(Dispatchers.IO) {
                PrayerLocation.getLastKnownLocation(getApplication<Application>())
            }
            if (location == null) {
                _state.update { it.copy(locationHint = LocationHint.UNAVAILABLE) }
                return@launch
            }
            val district = regions.nearestDistrict(location.latitude, location.longitude)
            val province = district?.let { regions.findRegion(it.parentId) }
            if (district == null || province == null) {
                _state.update { it.copy(locationHint = LocationHint.OUTSIDE) }
                return@launch
            }
            applySelection(province, district, null)
            _state.update { it.copy(locationHint = LocationHint.APPROXIMATE) }
            persistSelection()
            load()
        }
    }

    private fun applySelection(
        province: UzbekRegion,
        district: UzbekDistrict?,
        manual: NamozRegion?
    ) {
        loadJob?.cancel()
        _state.update {
            it.copy(
                selectedProvince = province,
                districts = regions.getDistricts(province.id),
                selectedDistrict = district,
                match = district?.let { d -> regions.matchFor(d.id) },
                manualRegion = manual,
                schedule = null,
                source = null,
                fromCache = false,
                isLoading = false,
                error = null,
                locationHint = null
            )
        }
        showCached()
    }

    private fun persistSelection() {
        val s = _state.value
        cache.saveSelection(
            PrayerSelection(s.selectedProvince?.id, s.selectedDistrict?.id, s.manualRegion?.slug)
        )
    }

    private fun targetOf(s: PrayerUiState): Target? {
        val district = s.selectedDistrict ?: return null
        val slug = (s.match as? NamozMatch.Exact)?.region?.slug ?: s.manualRegion?.slug ?: return null
        return Target("${district.id}|$slug", slug, district.name, district.latitude, district.longitude)
    }

    private fun showCached() {
        val target = targetOf(_state.value) ?: return
        val cached = cache.load(target.key) ?: return
        _state.update {
            it.copy(schedule = cached.schedule, source = cached.source, fromCache = true)
        }
        PrayerReminderScheduler.scheduleAll(
            getApplication<Application>(),
            cached.schedule
        )
    }

    private fun load() {
        val target = targetOf(_state.value) ?: return
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getToday(
                    target.slug, target.label, target.latitude, target.longitude
                )
                cache.save(target.key, result)
                PrayerReminderScheduler.scheduleAll(
                    getApplication<Application>(),
                    result.schedule
                )
                _state.update {
                    if (targetOf(it)?.key != target.key) it
                    else it.copy(
                        schedule = result.schedule,
                        source = result.source,
                        fromCache = false,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    when {
                        targetOf(it)?.key != target.key -> it
                        it.schedule != null -> it.copy(isLoading = false, fromCache = true)
                        else -> it.copy(isLoading = false, error = PrayerError.LOAD_FAILED)
                    }
                }
            }
        }
    }
}
