package uz.hidoyat.app.feature.quran

/**
 * Sura NOMLARINI (faqat nomlarni, Qur'on matnini emas) o'zbek lotinidan kirillga o'giradi.
 * Tarjima matni hech qachon o'girilmaydi — manba matni o'zgarishsiz ko'rsatiladi.
 */
object UzbekScript {
    private val digraphs = listOf(
        "o'" to "ў", "g'" to "ғ", "sh" to "ш", "ch" to "ч", "ng" to "нг",
        "yo" to "ё", "yu" to "ю", "ya" to "я"
    )
    private val single = mapOf(
        'a' to "а", 'b' to "б", 'd' to "д", 'e' to "э", 'f' to "ф", 'g' to "г", 'h' to "ҳ",
        'i' to "и", 'j' to "ж", 'k' to "к", 'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о",
        'p' to "п", 'q' to "қ", 'r' to "р", 's' to "с", 't' to "т", 'u' to "у", 'v' to "в",
        'x' to "х", 'y' to "й", 'z' to "з", '\'' to "ъ"
    )

    fun toCyrillic(latin: String): String {
        val out = StringBuilder()
        var i = 0
        var wordStart = true
        while (i < latin.length) {
            val rest = latin.substring(i)
            val lower = rest.lowercase()
            val di = digraphs.firstOrNull { lower.startsWith(it.first) }
            val piece: String
            val consumed: Int
            if (di != null) {
                piece = di.second
                consumed = di.first.length
            } else {
                val c = lower[0]
                piece = single[c] ?: c.toString()
                consumed = 1
            }
            val isLetter = rest[0].isLetter()
            out.append(if (isLetter && wordStart) piece.replaceFirstChar { it.uppercase() } else piece)
            wordStart = !isLetter && rest[0] != '\''
            i += consumed
        }
        return out.toString()
    }
}
