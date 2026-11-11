package uz.hidoyat.app.feature.quran

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

enum class QuranLoad { LOADING, NEEDS_DOWNLOAD, DOWNLOADING, READY, ERROR }

enum class QuranFilter { ALL, FAVORITES }

data class QuranUiState(
    val load: QuranLoad = QuranLoad.LOADING,
    val downloadedSurahs: Int = 0,
    val error: QuranErrorKind? = null,
    /** Qayta yuklash kerakligi sababi (buzilgan fayl bo'lsa). */
    val dataWasCorrupt: Boolean = false,
    val data: QuranData? = null,
    val openSurah: Int? = null,
    val initialAyah: Int = 1,
    /** Har safar reader qayta ochilganda o'zgaradi (scroll holatini qayta tiklash uchun). */
    val openToken: Int = 0,
    val query: String = "",
    val results: QuranSearchResults = QuranSearchResults(surahs = QuranCatalog.surahs),
    val filter: QuranFilter = QuranFilter.ALL,
    val favorites: Set<AyahKey> = emptySet(),
    val lastRead: AyahKey? = null,
    /** sura -> eng uzoq yetib borilgan oyat */
    val progress: Map<Int, Int> = emptyMap(),
    val textScale: Float = 1f
) {
    val overallPercent: Int
        get() = ((progress.values.sum() * 100L) / QuranCatalog.AYAH_COUNT).toInt().coerceIn(0, 100)
}

class QuranViewModel(app: Application) : AndroidViewModel(app) {
    private val store = QuranStore(app)
    private val repository = QuranRepository(app)
    val audio = QuranAudioManager(app, store.repeatMode)

    private val _state = MutableStateFlow(
        QuranUiState(
            favorites = store.favorites(),
            lastRead = store.lastRead,
            progress = store.progress(),
            textScale = store.textScale
        )
    )
    val state: StateFlow<QuranUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var tokenCounter = 0

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(load = QuranLoad.LOADING, error = null) }
            when (val local = repository.loadLocal()) {
                is QuranLocal.Ready -> _state.update { it.copy(load = QuranLoad.READY, data = local.data) }
                is QuranLocal.Missing ->
                    _state.update { it.copy(load = QuranLoad.NEEDS_DOWNLOAD, dataWasCorrupt = false) }
                is QuranLocal.Corrupt ->
                    _state.update { it.copy(load = QuranLoad.NEEDS_DOWNLOAD, dataWasCorrupt = true) }
            }
            runSearch()
        }
    }

    fun download() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(load = QuranLoad.DOWNLOADING, downloadedSurahs = 0, error = null) }
            try {
                val data = repository.download { done ->
                    _state.update { it.copy(downloadedSurahs = done) }
                }
                _state.update { it.copy(load = QuranLoad.READY, data = data, dataWasCorrupt = false) }
                runSearch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: QuranException) {
                _state.update { it.copy(load = QuranLoad.ERROR, error = e.kind) }
            } catch (e: Exception) {
                _state.update { it.copy(load = QuranLoad.ERROR, error = QuranErrorKind.UNKNOWN) }
            }
        }
    }

    /** Yuklashni bekor qiladi: ulanish uziladi, mavjud ma'lumotga tegilmaydi, holat "yuklash kerak"ga qaytadi. */
    fun cancelDownload() {
        if (_state.value.load != QuranLoad.DOWNLOADING) return
        loadJob?.cancel()
        _state.update { it.copy(load = QuranLoad.NEEDS_DOWNLOAD, downloadedSurahs = 0, error = null) }
    }

    // ---------- Navigatsiya ----------

    /** Noto'g'ri raqamlar eng yaqin to'g'ri oyatga keltiriladi — crash bo'lmaydi. */
    fun openSurah(surah: Int, ayah: Int = 1) {
        val key = QuranCatalog.clamp(surah, ayah)
        _state.update {
            it.copy(openSurah = key.surah, initialAyah = key.ayah, openToken = ++tokenCounter)
        }
    }

    fun openAyah(key: AyahKey) = openSurah(key.surah, key.ayah)

    /** Bosh sahifadagi "davom ettirish": oxirgi joy bo'lmasa — Al-Fotiha boshidan. */
    fun continueReading() {
        val last = _state.value.lastRead
        if (last != null) openSurah(last.surah, last.ayah) else openSurah(1, 1)
    }

    fun closeReader() {
        _state.update { it.copy(openSurah = null) }
    }

    // ---------- Qidiruv va filtr ----------

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        runSearch()
    }

    fun onFilterChange(filter: QuranFilter) {
        _state.update { it.copy(filter = filter) }
    }

    private fun runSearch() {
        searchJob?.cancel()
        val query = _state.value.query
        val data = _state.value.data
        searchJob = viewModelScope.launch {
            val results = withContext(Dispatchers.Default) { QuranSearch.search(query, data) }
            _state.update { if (it.query == query) it.copy(results = results) else it }
        }
    }

    // ---------- O'qish holati ----------

    /** Ekranda ko'rinib turgan oyat o'zgarganda: oxirgi joy va progress yangilanadi. */
    fun onAyahVisible(key: AyahKey) {
        if (!QuranCatalog.isValid(key.surah, key.ayah)) return
        val current = _state.value
        var progress = current.progress
        if (key.ayah > (progress[key.surah] ?: 0)) {
            store.saveProgress(key.surah, key.ayah)
            progress = progress + (key.surah to key.ayah)
        }
        if (current.lastRead != key) store.lastRead = key
        _state.update { it.copy(lastRead = key, progress = progress) }
    }

    fun toggleFavorite(key: AyahKey) {
        _state.update { s ->
            val updated = if (key in s.favorites) s.favorites - key else s.favorites + key
            store.saveFavorites(updated)
            s.copy(favorites = updated)
        }
    }

    fun changeTextScale(delta: Float) {
        _state.update {
            val scale = (Math.round((it.textScale + delta) * 10f) / 10f)
                .coerceIn(QuranStore.MIN_SCALE, QuranStore.MAX_SCALE)
            store.textScale = scale
            it.copy(textScale = scale)
        }
    }

    // ---------- Audio ----------

    fun playAyah(key: AyahKey) {
        val current = audio.state.value
        when {
            current.current == key && current.status == AudioStatus.PLAYING -> audio.pause()
            current.current == key && (current.status == AudioStatus.PAUSED || current.status == AudioStatus.ERROR) ->
                audio.resume()
            else -> audio.play(key)
        }
    }

    fun cycleRepeat() {
        audio.cycleRepeat()
        store.repeatMode = audio.state.value.repeat
    }

    fun onLeftScreen() {
        audio.pauseForBackground()
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}
