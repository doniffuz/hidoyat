package uz.hidoyat.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Asosiy ranglar: to'q yashil, oltin, oq, yumshoq krem (2-bosqichda kengaytiriladi)
val HidoyatGreen = Color(0xFF0B3D2E)
val HidoyatGreenLight = Color(0xFF1B6B4F)
val HidoyatGold = Color(0xFFD4AF37)
val HidoyatCream = Color(0xFFF7F1E3)
val HidoyatDark = Color(0xFF07241B)

private val LightColors = lightColorScheme(
    primary = HidoyatGreen,
    onPrimary = Color.White,
    secondary = HidoyatGold,
    onSecondary = HidoyatDark,
    background = HidoyatCream,
    onBackground = HidoyatDark,
    surface = Color.White,
    onSurface = HidoyatDark
)

private val DarkColors = darkColorScheme(
    primary = HidoyatGold,
    onPrimary = HidoyatDark,
    secondary = HidoyatGreenLight,
    onSecondary = Color.White,
    background = HidoyatDark,
    onBackground = HidoyatCream,
    surface = HidoyatGreen,
    onSurface = HidoyatCream
)

@Composable
fun HidoyatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
