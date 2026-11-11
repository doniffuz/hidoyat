package uz.hidoyat.app.feature.quran

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

enum class RepeatMode { OFF, AYAH, SURAH }

enum class AudioStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

enum class AudioError { NO_INTERNET, FAILED }

data class AudioState(
    val current: AyahKey? = null,
    val status: AudioStatus = AudioStatus.IDLE,
    val repeat: RepeatMode = RepeatMode.OFF,
    val error: AudioError? = null
) {
    val isActive: Boolean get() = current != null && status != AudioStatus.IDLE
}

/**
 * Oyatma-oyat oqimli audio (EveryAyah — Mishary Rashid Alafasy, 128 kbps).
 * Faqat asosiy oqimdan chaqiriladi. Har bir oyat uchun yangi MediaPlayer yaratiladi va eskisi
 * darhol release() qilinadi; eski o'yinchining kechikkan callback'lari [player] solishtirish
 * orqali e'tiborsiz qoldiriladi.
 */
class QuranAudioManager(context: Context, initialRepeat: RepeatMode = RepeatMode.OFF) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _state = MutableStateFlow(AudioState(repeat = initialRepeat))
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var prepared = false
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> { resumeOnFocusGain = false; pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_state.value.status == AudioStatus.PLAYING) { resumeOnFocusGain = true; pause() }
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocusGain) { resumeOnFocusGain = false; resume() }
            else -> Unit
        }
    }

    fun play(key: AyahKey) {
        val valid = QuranCatalog.clamp(key.surah, key.ayah)
        if (!isOnline()) {
            releasePlayer()
            _state.update { it.copy(current = valid, status = AudioStatus.ERROR, error = AudioError.NO_INTERNET) }
            return
        }
        releasePlayer()
        if (!requestFocus()) {
            _state.update { it.copy(current = valid, status = AudioStatus.ERROR, error = AudioError.FAILED) }
            return
        }
        _state.update { it.copy(current = valid, status = AudioStatus.LOADING, error = null) }
        val mp = MediaPlayer()
        player = mp
        prepared = false
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setOnPreparedListener { p ->
                if (p !== player) return@setOnPreparedListener
                prepared = true
                p.start()
                _state.update { it.copy(status = AudioStatus.PLAYING, error = null) }
            }
            mp.setOnCompletionListener { p ->
                if (p === player) onCompleted()
            }
            mp.setOnErrorListener { p, _, _ ->
                if (p === player) fail(if (isOnline()) AudioError.FAILED else AudioError.NO_INTERNET)
                true
            }
            mp.setDataSource(audioUrl(valid))
            mp.prepareAsync()
        } catch (e: Exception) {
            if (mp === player) fail(AudioError.FAILED)
        }
    }

    fun pause() {
        val mp = player ?: return
        if (_state.value.status == AudioStatus.PLAYING && prepared) {
            try { mp.pause() } catch (e: IllegalStateException) { fail(AudioError.FAILED); return }
            _state.update { it.copy(status = AudioStatus.PAUSED) }
        } else if (_state.value.status == AudioStatus.LOADING) {
            // Yuklanayotgan oyatni to'xtatamiz: o'yinchi bo'shatiladi, joy saqlanadi
            releasePlayer()
            _state.update { it.copy(status = AudioStatus.PAUSED) }
        }
    }

    fun resume() {
        val s = _state.value
        val cur = s.current ?: return
        when {
            s.status == AudioStatus.PAUSED && player != null && prepared -> {
                if (!requestFocus()) return
                try { player?.start() } catch (e: IllegalStateException) { play(cur); return }
                _state.update { it.copy(status = AudioStatus.PLAYING, error = null) }
            }
            s.status == AudioStatus.PAUSED || s.status == AudioStatus.ERROR -> play(cur)
        }
    }

    fun togglePlayPause() {
        when (_state.value.status) {
            AudioStatus.PLAYING, AudioStatus.LOADING -> pause()
            AudioStatus.PAUSED, AudioStatus.ERROR -> resume()
            AudioStatus.IDLE -> Unit
        }
    }

    fun next() {
        val cur = _state.value.current ?: return
        val count = QuranCatalog.ayahCount(cur.surah)
        when {
            cur.ayah < count -> play(AyahKey(cur.surah, cur.ayah + 1))
            _state.value.repeat == RepeatMode.SURAH -> play(AyahKey(cur.surah, 1))
            else -> stop() // sura oxiri
        }
    }

    fun previous() {
        val cur = _state.value.current ?: return
        if (cur.ayah > 1) play(AyahKey(cur.surah, cur.ayah - 1)) else play(cur)
    }

    fun cycleRepeat() {
        _state.update {
            it.copy(repeat = when (it.repeat) {
                RepeatMode.OFF -> RepeatMode.AYAH
                RepeatMode.AYAH -> RepeatMode.SURAH
                RepeatMode.SURAH -> RepeatMode.OFF
            })
        }
    }

    /** To'liq to'xtatadi va resurslarni bo'shatadi (joy ham tozalanadi). */
    fun stop() {
        releasePlayer()
        abandonFocus()
        _state.update { it.copy(current = null, status = AudioStatus.IDLE, error = null) }
    }

    /** Ekran/ilova fonga ketganda: ijro to'xtatiladi, lekin joy saqlanadi. */
    fun pauseForBackground() {
        if (_state.value.status == AudioStatus.PLAYING || _state.value.status == AudioStatus.LOADING) pause()
    }

    /** ViewModel o'chganda chaqiriladi. */
    fun release() {
        releasePlayer()
        abandonFocus()
    }

    private fun onCompleted() {
        val cur = _state.value.current ?: return
        when (_state.value.repeat) {
            RepeatMode.AYAH -> play(cur)
            else -> next()
        }
    }

    private fun fail(error: AudioError) {
        releasePlayer()
        _state.update { it.copy(status = AudioStatus.ERROR, error = error) }
    }

    private fun releasePlayer() {
        val mp = player
        player = null
        prepared = false
        if (mp != null) {
            mp.setOnPreparedListener(null)
            mp.setOnCompletionListener(null)
            mp.setOnErrorListener(null)
            try { mp.reset() } catch (e: IllegalStateException) { }
            mp.release()
        }
    }

    private fun requestFocus(): Boolean {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    private fun isOnline(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        const val BASE_URL = "https://everyayah.com/data/Alafasy_128kbps"

        /**
         * EveryAyah fayl nomi: SSSAAA.mp3 (masalan 001001.mp3).
         * Locale.ROOT shart: aks holda arabcha va boshqa locale'larda raqamlar arab-hind raqamlariga aylanishi mumkin.
         */
        fun audioUrl(key: AyahKey): String =
            String.format(Locale.ROOT, "%s/%03d%03d.mp3", BASE_URL, key.surah, key.ayah)
    }
}
