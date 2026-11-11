package uz.hidoyat.app.feature.quran

/**
 * Faqat interfeys uchun belgilar (Qur'on matni emas): sharqiy arab raqamlari va oyat oxiri belgisi.
 * Raqamlar kod nuqtalari orqali hisoblanadi, matn qo'lda yozilmaydi.
 */
object QuranGlyphs {
    /** U+06DD — ARABIC END OF AYAH. */
    const val AYAH_END = '\u06DD'

    private const val ARABIC_INDIC_ZERO = 0x0660

    fun arabicDigits(number: Int): String = buildString {
        number.coerceAtLeast(0).toString().forEach { c ->
            append((ARABIC_INDIC_ZERO + (c - '0')).toChar())
        }
    }

    /** Oyat raqami belgisi: ۝ va uning yonida sharqiy raqam. */
    fun ayahMarker(number: Int): String = "$AYAH_END${arabicDigits(number)}"
}
