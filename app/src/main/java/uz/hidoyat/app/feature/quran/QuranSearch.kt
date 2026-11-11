package uz.hidoyat.app.feature.quran

data class QuranSearchResults(
    val surahs: List<SurahInfo> = emptyList(),
    val ayahs: List<Ayah> = emptyList(),
    /** "2:255" kabi to'g'ridan-to'g'ri manzil topilgan bo'lsa. */
    val jump: AyahKey? = null,
    val truncated: Boolean = false
)

object QuranSearch {
    private const val MIN_TEXT_QUERY = 3
    private const val MAX_AYAH_RESULTS = 40

    /** Apostrof variantlari va registrni tenglashtiradi (o', o‘, oʻ, o’ hammasi bir xil bo'ladi). */
    fun normalize(text: String): String = buildString(text.length) {
        for (ch in text.lowercase()) {
            when (ch) {
                '\u2018', '\u2019', '\u02BB', '\u02BC', '`', '\u00B4', '\u02B9' -> append('\'')
                '\u2010', '\u2011', '\u2012', '\u2013' -> append('-')
                else -> append(ch)
            }
        }
    }

    private fun compact(text: String): String =
        normalize(text).filter { it.isLetterOrDigit() }

    /** Sura ro'yxatini filtrlaydi: raqam, lotin, kirill va rus nomlari bo'yicha. */
    fun searchSurahs(query: String): List<SurahInfo> {
        val q = compact(query)
        if (q.isEmpty()) return QuranCatalog.surahs
        val number = query.trim().toIntOrNull()
        return QuranCatalog.surahs.filter { s ->
            (number != null && s.number == number) ||
                compact(s.nameUz).contains(q) ||
                compact(s.nameCyrillic).contains(q) ||
                compact(s.nameRu).contains(q)
        }
    }

    /** Suralar, "sura:oyat" manzili va tarjima matni bo'yicha qidiradi. Og'ir ish — fon oqimida chaqiring. */
    fun search(query: String, data: QuranData?): QuranSearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return QuranSearchResults(surahs = QuranCatalog.surahs)
        val surahs = searchSurahs(trimmed)
        val jump = AyahKey.parse(trimmed)
        var ayahs = emptyList<Ayah>()
        var truncated = false
        if (data != null && trimmed.length >= MIN_TEXT_QUERY && jump == null) {
            val q = normalize(trimmed)
            val found = ArrayList<Ayah>()
            outer@ for (info in QuranCatalog.surahs) {
                for (ayah in data.ayahs(info.number)) {
                    if (normalize(ayah.translation).contains(q)) {
                        if (found.size >= MAX_AYAH_RESULTS) { truncated = true; break@outer }
                        found.add(ayah)
                    }
                }
            }
            ayahs = found
        }
        return QuranSearchResults(surahs, ayahs, jump, truncated)
    }
}
