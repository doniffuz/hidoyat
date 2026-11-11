package uz.hidoyat.app.feature.quran

enum class Revelation { MECCAN, MEDINAN }

/** Sura haqida tuzilma ma'lumot (matn emas). */
data class SurahInfo(
    val number: Int,
    val nameUz: String,
    val nameRu: String,
    val ayahCount: Int,
    val revelation: Revelation
) {
    val nameCyrillic: String get() = UzbekScript.toCyrillic(nameUz)
}

/** Oyat manzili: sura va oyat raqami (1 dan boshlanadi). */
data class AyahKey(val surah: Int, val ayah: Int) {
    val id: String get() = "$surah:$ayah"

    companion object {
        /** "2:255" ko'rinishini o'qiydi; noto'g'ri yoki diapazondan tashqari bo'lsa null. */
        fun parse(raw: String): AyahKey? {
            val parts = raw.trim().split(':')
            if (parts.size != 2) return null
            val s = parts[0].trim().toIntOrNull() ?: return null
            val a = parts[1].trim().toIntOrNull() ?: return null
            return if (QuranCatalog.isValid(s, a)) AyahKey(s, a) else null
        }
    }
}

/** Bitta oyat: arabcha matn, tarjima va (bo'lsa) manbadagi izoh. Hammasi manbadan o'zgartirilmagan holda. */
data class Ayah(
    val key: AyahKey,
    val arabic: String,
    val translation: String,
    val footnote: String?
)

/** Fayl sarlavhasidagi manba ma'lumotlari (QuranEnc qayta nashr talablari uchun saqlanadi). */
data class QuranMeta(
    val source: String,
    val version: String,
    val translator: String,
    val url: String
)

/** Xotiradagi to'liq Qur'on ma'lumoti: 114 sura, 6236 oyat. */
class QuranData(
    private val bySurah: List<List<Ayah>>,
    val meta: QuranMeta
) {
    fun ayahs(surah: Int): List<Ayah> = bySurah.getOrNull(surah - 1).orEmpty()

    fun ayah(key: AyahKey): Ayah? = bySurah.getOrNull(key.surah - 1)?.getOrNull(key.ayah - 1)

    val totalAyahs: Int get() = bySurah.sumOf { it.size }

    /** Basmala matni: Al-Fotiha 1-oyatining arabcha matni, manbadan o'zgartirilmagan holda (qo'lda yozilmaydi). */
    val basmala: String? get() = bySurah.getOrNull(0)?.getOrNull(0)?.arabic

    /**
     * Sura boshida alohida basmala sarlavhasi ko'rsatiladimi.
     * - Al-Fotiha: basmala 1-oyatning o'zi (takror ko'rsatilmaydi); At-Tavba: basmala yo'q.
     * - Manba 1-oyat matniga basmalani allaqachon qo'shib yuborgan bo'lsa — takrorlanmaydi.
     */
    fun showsBasmalaHeader(surah: Int): Boolean {
        if (surah == 1 || surah == 9 || surah !in 1..QuranCatalog.SURAH_COUNT) return false
        val basmala = basmala?.filterNot { it.isWhitespace() } ?: return false
        val first = bySurah.getOrNull(surah - 1)?.firstOrNull()?.arabic?.filterNot { it.isWhitespace() } ?: return false
        return !first.startsWith(basmala)
    }
}

enum class QuranErrorKind { NO_INTERNET, TIMEOUT, SERVER, BAD_DATA, STORAGE, UNKNOWN }

class QuranException(val kind: QuranErrorKind, message: String, cause: Throwable? = null) :
    Exception(message, cause)
