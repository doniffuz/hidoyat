package uz.hidoyat.app.feature.quran

import android.content.Context

/**
 * Qur'on holati (faqat telefonda): oxirgi o'qilgan joy, sevimlilar, o'qish progressi, matn hajmi, takrorlash.
 * Saqlangan qiymatlar o'qilganda tekshiriladi: noto'g'ri sura/oyat raqami e'tiborga olinmaydi.
 */
class QuranStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("hidoyat_quran", Context.MODE_PRIVATE)

    var lastRead: AyahKey?
        get() {
            val s = prefs.getInt(KEY_LAST_SURAH, 0)
            val a = prefs.getInt(KEY_LAST_AYAH, 0)
            return if (QuranCatalog.isValid(s, a)) AyahKey(s, a) else null
        }
        set(value) {
            val e = prefs.edit()
            if (value != null && QuranCatalog.isValid(value.surah, value.ayah)) {
                e.putInt(KEY_LAST_SURAH, value.surah).putInt(KEY_LAST_AYAH, value.ayah)
            } else {
                e.remove(KEY_LAST_SURAH).remove(KEY_LAST_AYAH)
            }
            e.apply()
        }

    fun favorites(): Set<AyahKey> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty()
            .mapNotNull { AyahKey.parse(it) }
            .toSet()

    fun saveFavorites(favorites: Set<AyahKey>) {
        prefs.edit().putStringSet(KEY_FAVORITES, HashSet(favorites.map { it.id })).apply()
    }

    /** Har sura uchun eng uzoq yetib borilgan oyat raqami. */
    fun progress(): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        for (info in QuranCatalog.surahs) {
            val v = prefs.getInt(KEY_PROGRESS + info.number, 0)
            if (v in 1..info.ayahCount) out[info.number] = v
        }
        return out
    }

    fun saveProgress(surah: Int, furthestAyah: Int) {
        if (!QuranCatalog.isValid(surah, furthestAyah)) return
        prefs.edit().putInt(KEY_PROGRESS + surah, furthestAyah).apply()
    }

    var textScale: Float
        get() = prefs.getFloat(KEY_TEXT_SCALE, 1f).coerceIn(MIN_SCALE, MAX_SCALE)
        set(value) { prefs.edit().putFloat(KEY_TEXT_SCALE, value.coerceIn(MIN_SCALE, MAX_SCALE)).apply() }

    var repeatMode: RepeatMode
        get() = RepeatMode.entries.firstOrNull { it.name == prefs.getString(KEY_REPEAT, null) } ?: RepeatMode.OFF
        set(value) { prefs.edit().putString(KEY_REPEAT, value.name).apply() }

    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.8f
        const val SCALE_STEP = 0.1f
        private const val KEY_LAST_SURAH = "last_surah"
        private const val KEY_LAST_AYAH = "last_ayah"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PROGRESS = "progress_"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_REPEAT = "repeat_mode"
    }
}
