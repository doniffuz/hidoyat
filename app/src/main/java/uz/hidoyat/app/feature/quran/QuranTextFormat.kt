package uz.hidoyat.app.feature.quran

/**
 * Fayl formati (tools/quran_data.py bilan bir xil). Har qatorda:  sura|oyat|matn
 * - "#" bilan boshlangan qatorlar: sarlavha (# kalit: qiymat) yoki izoh
 * - matn ichida "\\" -> "\\\\", yangi qator -> "\\n", "|" belgisi oddiy qoladi (3-bo'lakdan keyin kesilmaydi)
 * Matn o'zgartirilmaydi: faqat saqlash uchun escape qilinadi va o'qishda aynan qaytariladi.
 */
object QuranTextFormat {

    class ParsedFile(val meta: Map<String, String>, val entries: Map<AyahKey, String>)

    fun escape(text: String): String =
        text.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n")

    fun unescape(text: String): String {
        if (!text.contains('\\')) return text
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2; continue }
                    '\\' -> { sb.append('\\'); i += 2; continue }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /**
     * @param allowPartial true bo'lsa (izohlar fayli) yetishmayotgan oyatlar xato emas.
     * Noto'g'ri qator, diapazondan tashqari raqam, bo'sh matn yoki takror bo'lsa [QuranException] (BAD_DATA).
     */
    fun parse(content: String, name: String, allowPartial: Boolean = false): ParsedFile {
        val meta = LinkedHashMap<String, String>()
        val entries = LinkedHashMap<AyahKey, String>()
        var lineNo = 0
        for (raw in content.removePrefix("\uFEFF").lineSequence()) {
            lineNo++
            val line = raw.trimEnd('\r')
            if (line.isBlank()) continue
            if (line.startsWith("#")) {
                val body = line.removePrefix("#").trim()
                val idx = body.indexOf(':')
                if (idx > 0) meta[body.substring(0, idx).trim()] = body.substring(idx + 1).trim()
                continue
            }
            val parts = line.split('|', limit = 3)
            if (parts.size != 3) bad("$name:$lineNo noto'g'ri format")
            val s = parts[0].trim().toIntOrNull() ?: bad("$name:$lineNo sura raqami noto'g'ri")
            val a = parts[1].trim().toIntOrNull() ?: bad("$name:$lineNo oyat raqami noto'g'ri")
            if (!QuranCatalog.isValid(s, a)) bad("$name:$lineNo diapazondan tashqari $s:$a")
            val text = unescape(parts[2].trim())
            if (text.isBlank()) bad("$name:$lineNo bo'sh matn $s:$a")
            val key = AyahKey(s, a)
            if (entries.put(key, text) != null) bad("$name:$lineNo takrorlangan $s:$a")
        }
        if (!allowPartial && entries.size != QuranCatalog.AYAH_COUNT) {
            bad("$name to'liq emas: ${entries.size} / ${QuranCatalog.AYAH_COUNT} oyat")
        }
        return ParsedFile(meta, entries)
    }

    /** Arabcha va tarjima fayllarini bir-biriga mosligini tekshirib, [QuranData] quradi. */
    fun build(arabic: ParsedFile, translation: ParsedFile, notes: ParsedFile?): QuranData {
        val bySurah = ArrayList<List<Ayah>>(QuranCatalog.SURAH_COUNT)
        for (info in QuranCatalog.surahs) {
            val list = ArrayList<Ayah>(info.ayahCount)
            for (a in 1..info.ayahCount) {
                val key = AyahKey(info.number, a)
                val ar = arabic.entries[key] ?: bad("arabcha matnda yo'q: ${key.id}")
                val tr = translation.entries[key] ?: bad("tarjimada yo'q: ${key.id}")
                list.add(Ayah(key, ar, tr, notes?.entries?.get(key)))
            }
            bySurah.add(list)
        }
        val m = translation.meta
        val meta = QuranMeta(
            source = m["source"] ?: "QuranEnc.com",
            version = m["version"] ?: "",
            translator = m["translator"] ?: "",
            url = m["url"] ?: "https://quranenc.com"
        )
        return QuranData(bySurah, meta)
    }

    private fun bad(message: String): Nothing =
        throw QuranException(QuranErrorKind.BAD_DATA, message)
}
