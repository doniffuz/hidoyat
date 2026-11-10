package uz.hidoyat.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val P = HidoyatPalette

val HidoyatLightColorScheme: ColorScheme = lightColorScheme(
    primary = P.Green800,
    onPrimary = Color.White,
    primaryContainer = P.Green100,
    onPrimaryContainer = P.Green900,
    secondary = P.Gold500,
    onSecondary = P.Green900,
    secondaryContainer = P.Gold100,
    onSecondaryContainer = P.Gold700,
    tertiary = P.Gold700,
    onTertiary = Color.White,
    background = P.Cream100,
    onBackground = P.Ink900,
    surface = P.Cream50,
    onSurface = P.Ink900,
    surfaceVariant = P.Cream200,
    onSurfaceVariant = P.Ink600,
    surfaceTint = P.Green800,
    outline = P.Gold500,
    outlineVariant = P.Cream300,
    error = P.Error,
    onError = Color.White
)

val HidoyatDarkColorScheme: ColorScheme = darkColorScheme(
    primary = P.Gold500,
    onPrimary = P.Night950,
    primaryContainer = P.Green700,
    onPrimaryContainer = P.CreamText,
    secondary = P.Gold400,
    onSecondary = P.Night950,
    secondaryContainer = P.Night600,
    onSecondaryContainer = P.Gold100,
    tertiary = P.Green200,
    onTertiary = P.Night950,
    background = P.Night900,
    onBackground = P.CreamText,
    surface = P.Night900,
    onSurface = P.CreamText,
    surfaceVariant = P.Night700,
    onSurfaceVariant = P.CreamMuted,
    surfaceTint = P.Gold500,
    outline = P.Gold500,
    outlineVariant = P.Night600,
    error = P.ErrorDark,
    onError = P.Night950
)

private val LightExtra = HidoyatExtraColors(
    gold = P.Gold500,
    onGold = P.Green900,
    goldContainer = P.Gold100,
    card = Color.White,
    onCard = P.Ink900,
    cardBorder = P.Cream300,
    textSecondary = P.Ink600,
    success = P.Success,
    heroStart = P.Green800,
    heroEnd = P.Green600,
    onHero = P.Cream50,
    onHeroMuted = P.Green200
)

private val DarkExtra = HidoyatExtraColors(
    gold = P.Gold500,
    onGold = P.Night950,
    goldContainer = P.Night600,
    card = P.Night700,
    onCard = P.CreamText,
    cardBorder = P.Night600,
    textSecondary = P.CreamMuted,
    success = P.SuccessDark,
    heroStart = P.Green700,
    heroEnd = P.Night800,
    onHero = P.CreamText,
    onHeroMuted = P.CreamMuted
)

private val LocalHidoyatColors = staticCompositionLocalOf { LightExtra }

/** Qo'shimcha ranglarga kirish: `HidoyatTheme.colors.gold` */
object HidoyatTheme {
    val colors: HidoyatExtraColors
        @Composable @ReadOnlyComposable get() = LocalHidoyatColors.current
}

@Composable
fun HidoyatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalHidoyatColors provides if (darkTheme) DarkExtra else LightExtra
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) HidoyatDarkColorScheme else HidoyatLightColorScheme,
            typography = HidoyatTypography,
            content = content
        )
    }
}
