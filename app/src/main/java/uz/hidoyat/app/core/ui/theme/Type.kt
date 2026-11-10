package uz.hidoyat.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Sarlavhalar: nafis serif. Asosiy matn: o'qilishi qulay sans-serif (tizim shriftlari, kutubxonasiz). */
object HidoyatFonts {
    val Heading: FontFamily = FontFamily.Serif
    val Body: FontFamily = FontFamily.SansSerif
}

private fun heading(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = HidoyatFonts.Heading,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

private fun body(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = HidoyatFonts.Body,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

val HidoyatTypography = Typography(
    displayLarge = heading(40, 48, FontWeight.Bold),
    displayMedium = heading(34, 42, FontWeight.Bold),
    displaySmall = heading(30, 38, FontWeight.SemiBold),
    headlineLarge = heading(28, 36, FontWeight.SemiBold),
    headlineMedium = heading(24, 32, FontWeight.SemiBold),
    headlineSmall = heading(20, 28, FontWeight.SemiBold),
    titleLarge = heading(22, 28, FontWeight.SemiBold),
    titleMedium = body(17, 24, FontWeight.SemiBold, 0.1),
    titleSmall = body(15, 22, FontWeight.Medium, 0.1),
    bodyLarge = body(17, 26, FontWeight.Normal, 0.2),
    bodyMedium = body(15, 23, FontWeight.Normal, 0.2),
    bodySmall = body(13, 19, FontWeight.Normal, 0.25),
    labelLarge = body(15, 20, FontWeight.SemiBold, 0.3),
    labelMedium = body(13, 18, FontWeight.Medium, 0.3),
    labelSmall = body(12, 16, FontWeight.Medium, 0.4)
)
