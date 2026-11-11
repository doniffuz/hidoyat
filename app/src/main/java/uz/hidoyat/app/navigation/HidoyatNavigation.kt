package uz.hidoyat.app.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatIcon
import uz.hidoyat.app.core.ui.components.HidoyatIconType
import uz.hidoyat.app.core.ui.theme.HidoyatTheme
import uz.hidoyat.app.feature.home.DailyGoal
import uz.hidoyat.app.feature.home.HomeScreen
import uz.hidoyat.app.feature.home.HomeUiState
import uz.hidoyat.app.feature.placeholder.PlaceholderScreen
import uz.hidoyat.app.feature.prayer.PrayerScreen
import uz.hidoyat.app.feature.qibla.QiblaScreen
import uz.hidoyat.app.feature.quran.QuranScreen
import uz.hidoyat.app.feature.zikr.ZikrScreen

enum class HidoyatDestination(
    @StringRes val label: Int,
    val icon: HidoyatIconType
) {
    Home(R.string.nav_home, HidoyatIconType.Home),
    Prayer(R.string.nav_prayer, HidoyatIconType.Prayer),
    Quran(R.string.nav_quran, HidoyatIconType.Quran),
    Qibla(R.string.nav_qibla, HidoyatIconType.Qibla),
    Profile(R.string.nav_profile, HidoyatIconType.Profile)
}

/** Asosiy ekran: pastki menyu + tanlangan bo'lim. Tanlov ekran aylantirilganda saqlanadi. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HidoyatMainScreen(
    homeState: HomeUiState,
    onToggleGoal: (DailyGoal) -> Unit,
    profileContent: @Composable () -> Unit,
    /** Bosh sahifa ko'rsatilganda chaqiriladi (masalan, oxirgi o'qilgan Qur'on joyini yangilash uchun). */
    onHomeShown: () -> Unit = {}
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var pendingQuranContinue by remember { mutableStateOf(false) }
    var showZikr by rememberSaveable { mutableStateOf(false) }
    val destinations = HidoyatDestination.entries
    val current = destinations[selected.coerceIn(0, destinations.lastIndex)]

    LaunchedEffect(current) {
        if (current == HidoyatDestination.Home) onHomeShown()
    }

    BackHandler(enabled = showZikr || current != HidoyatDestination.Home) {
        if (showZikr) {
            showZikr = false
        } else {
            selected = HidoyatDestination.Home.ordinal
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!showZikr) {
                HidoyatBottomBar(
                    current = current,
                    onSelect = { selected = it.ordinal }
                )
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (showZikr) {
                ZikrScreen(onBack = { showZikr = false })
            } else when (current) {
                HidoyatDestination.Home -> HomeScreen(
                    state = homeState,
                    onToggleGoal = onToggleGoal,
                    onOpenQibla = { selected = HidoyatDestination.Qibla.ordinal },
                    onOpenQuran = { selected = HidoyatDestination.Quran.ordinal },
                    onContinueQuran = {
                        pendingQuranContinue = true
                        selected = HidoyatDestination.Quran.ordinal
                    },
                    onOpenZikr = { showZikr = true },
                    contentWindowInsets = WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
                HidoyatDestination.Profile -> profileContent()
                HidoyatDestination.Prayer -> PrayerScreen()
                HidoyatDestination.Qibla -> QiblaScreen()
                HidoyatDestination.Quran -> QuranScreen(
                    pendingContinue = pendingQuranContinue,
                    onContinueHandled = { pendingQuranContinue = false }
                )
                else -> PlaceholderScreen(title = current.label, icon = current.icon)
            }
        }
    }
}

@Composable
fun HidoyatBottomBar(
    current: HidoyatDestination,
    onSelect: (HidoyatDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HidoyatTheme.colors
    Surface(
        modifier = modifier,
        color = colors.card,
        contentColor = colors.onCard,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .selectableGroup()
        ) {
            HidoyatDestination.entries.forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    selected = destination == current,
                    onClick = { onSelect(destination) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    destination: HidoyatDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = HidoyatTheme.colors
    val quiet = colors.textSecondary
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else quiet,
        animationSpec = tween(200),
        label = "navContent"
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) colors.goldContainer else colors.card,
        animationSpec = tween(200),
        label = "navPill"
    )
    val accentWidth by animateDpAsState(
        targetValue = if (selected) 28.dp else 0.dp,
        animationSpec = tween(200),
        label = "navAccent"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Oltin aksent chizig'i
        Box(
            Modifier
                .width(accentWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(colors.gold)
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = 56.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            HidoyatIcon(destination.icon, contentColor, size = 22.dp)
        }
        Text(
            text = stringResource(destination.label),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp, start = 2.dp, end = 2.dp)
        )
    }
}
