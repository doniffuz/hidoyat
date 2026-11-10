package uz.hidoyat.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/** Yagona palitra: barcha ranglar faqat shu yerda e'lon qilinadi. */
object HidoyatPalette {
    // Yashil
    val Green900 = Color(0xFF062A1F)
    val Green800 = Color(0xFF0B3D2E)
    val Green700 = Color(0xFF14543F)
    val Green600 = Color(0xFF1B6B4F)
    val Green200 = Color(0xFFB7D9C8)
    val Green100 = Color(0xFFDCEEE4)

    // Oltin
    val Gold700 = Color(0xFF8A6D1D)
    val Gold500 = Color(0xFFC9A84C)
    val Gold400 = Color(0xFFD9BC6A)
    val Gold100 = Color(0xFFF3E6BF)

    // Krem / oq
    val Cream50 = Color(0xFFFFFCF5)
    val Cream100 = Color(0xFFF8F3E6)
    val Cream200 = Color(0xFFEFE7D3)
    val Cream300 = Color(0xFFE3D9BF)
    val CreamText = Color(0xFFEDE6D3)
    val CreamMuted = Color(0xFFB9B3A0)

    // Qora-yashil (dark)
    val Night950 = Color(0xFF04140F)
    val Night900 = Color(0xFF071C15)
    val Night800 = Color(0xFF0D2B21)
    val Night700 = Color(0xFF143A2D)
    val Night600 = Color(0xFF1D4A3A)

    // Matn
    val Ink900 = Color(0xFF10201A)
    val Ink600 = Color(0xFF4A5B53)

    // Holat
    val Error = Color(0xFFB3261E)
    val ErrorDark = Color(0xFFF2B8B5)
    val Success = Color(0xFF2E7D4F)
    val SuccessDark = Color(0xFF7FCB9C)
}

/** Material 3 sxemasiga kirmaydigan qo'shimcha ranglar. */
data class HidoyatExtraColors(
    val gold: Color,
    val onGold: Color,
    val goldContainer: Color,
    val card: Color,
    val onCard: Color,
    val cardBorder: Color,
    val textSecondary: Color,
    val success: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color
)
