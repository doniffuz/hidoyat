package uz.hidoyat.app.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlinx.coroutines.launch
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatDivider
import uz.hidoyat.app.core.ui.components.HidoyatGoldButton
import uz.hidoyat.app.core.ui.components.HidoyatIcon
import uz.hidoyat.app.core.ui.components.HidoyatIconType
import uz.hidoyat.app.core.ui.components.HidoyatPrimaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSecondaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSectionTitle
import uz.hidoyat.app.core.ui.components.HidoyatShapes
import uz.hidoyat.app.core.ui.theme.HidoyatTheme
import uz.hidoyat.app.feature.quran.AyahKey
import uz.hidoyat.app.feature.dua.DuaCatalog
import uz.hidoyat.app.feature.zikr.ZikrCatalog
import uz.hidoyat.app.feature.quran.surahDisplayName

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleGoal: (DailyGoal) -> Unit,
    onOpenQibla: () -> Unit = {},
    onOpenQuran: () -> Unit = {},
    onContinueQuran: () -> Unit = {},
    onOpenZikr: () -> Unit = {},
    onOpenDua: () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val direction = LocalLayoutDirection.current

    fun soon(@StringRes label: Int) {
        val message = context.getString(R.string.home_soon_format, context.getString(label))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = contentWindowInsets,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = HidoyatShapes.Button,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = inner.calculateStartPadding(direction) + 20.dp,
                end = inner.calculateEndPadding(direction) + 20.dp,
                top = inner.calculateTopPadding() + 16.dp,
                bottom = inner.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { HomeHeader(state) }
            item { NextPrayerCard(onAllTimes = { soon(R.string.home_quick_prayer) }) }
            item { QuranCard(lastRead = state.quranLastRead, onContinue = onContinueQuran) }
            item { QiblaCard(onClick = onOpenQibla) }
            item {
                QuickSections(
                    onPrayer = { soon(R.string.home_quick_prayer) },
                    onQuran = onOpenQuran,
                    onQibla = onOpenQibla,
                    onZikr = onOpenZikr
                )
            }
            item {
                DailyDuaZikr(
                    date = state.date,
                    onDua = onOpenDua,
                    onZikr = onOpenZikr
                )
            }
            item { DailyGoals(state.completedGoals, onToggleGoal) }
        }
    }
}

@Composable
private fun DemoBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = stringResource(R.string.home_demo_badge),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun IconBadge(type: HidoyatIconType, boxSize: Int = 48) {
    Box(
        modifier = Modifier
            .size(boxSize.dp)
            .clip(CircleShape)
            .background(HidoyatTheme.colors.goldContainer),
        contentAlignment = Alignment.Center
    ) {
        HidoyatIcon(type, MaterialTheme.colorScheme.primary, size = (boxSize * 0.55f).dp)
    }
}

@Composable
private fun HomeHeader(state: HomeUiState) {
    val weekdays = stringArrayResource(R.array.home_weekdays)
    val months = stringArrayResource(R.array.home_months)
    val d: LocalDate = state.date
    val dateText = "${weekdays[d.dayOfWeek.value - 1]}, ${d.dayOfMonth}-${months[d.monthValue - 1]} ${d.year}"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(HidoyatIconType.Crescent, boxSize = 40)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.bodyLarge,
            color = HidoyatTheme.colors.textSecondary
        )
        Text(
            text = state.userName,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun NextPrayerCard(onAllTimes: () -> Unit) {
    val colors = HidoyatTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.heroEnd,
        contentColor = colors.onHero,
        shadowElevation = 8.dp
    ) {
        Box(
            Modifier.background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
        ) {
            HidoyatIcon(
                type = HidoyatIconType.Crescent,
                tint = colors.onHero.copy(alpha = 0.07f),
                size = 150.dp,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-8).dp)
            )
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.home_next_prayer),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onHeroMuted
                    )
                    DemoBadge()
                }
                Text(
                    text = stringResource(R.string.home_prayer_name_demo),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onHero
                )
                Text(
                    text = stringResource(R.string.home_prayer_time_demo),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.gold
                )
                Text(
                    text = stringResource(R.string.home_prayer_remaining),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onHero
                )
                Text(
                    text = stringResource(R.string.home_prayer_demo_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onHeroMuted
                )
                Spacer(Modifier.height(10.dp))
                HidoyatGoldButton(
                    text = stringResource(R.string.home_prayer_all_times),
                    onClick = onAllTimes,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QuranCard(lastRead: AyahKey?, onContinue: () -> Unit) {
    HidoyatCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(HidoyatIconType.Quran)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.home_quran_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (lastRead != null) {
                        stringResource(
                            R.string.home_quran_last_read_format,
                            surahDisplayName(lastRead.surah),
                            lastRead.ayah
                        )
                    } else {
                        stringResource(R.string.home_quran_last_read)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = HidoyatTheme.colors.textSecondary
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HidoyatPrimaryButton(
            text = stringResource(R.string.home_quran_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QiblaCard(onClick: () -> Unit) {
    HidoyatCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.qibla_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.qibla_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HidoyatTheme.colors.textSecondary
                )
            }
            IconBadge(HidoyatIconType.Qibla, boxSize = 56)
        }
    }
}

@Composable
private fun QuickSections(
    onPrayer: () -> Unit,
    onQuran: () -> Unit,
    onQibla: () -> Unit,
    onZikr: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HidoyatSectionTitle(title = stringResource(R.string.home_quick_title))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickCard(HidoyatIconType.Crescent, R.string.home_quick_prayer, onPrayer, Modifier.weight(1f))
            QuickCard(HidoyatIconType.Quran, R.string.home_quick_quran, onQuran, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickCard(HidoyatIconType.Qibla, R.string.home_quick_qibla, onQibla, Modifier.weight(1f))
            QuickCard(HidoyatIconType.Zikr, R.string.home_quick_zikr, onZikr, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickCard(
    icon: HidoyatIconType,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HidoyatCard(
        modifier = modifier.heightIn(min = 120.dp),
        onClick = onClick
    ) {
        IconBadge(icon, boxSize = 52)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyDuaZikr(
    date: LocalDate,
    onDua: () -> Unit,
    onZikr: () -> Unit
) {
    val duaIndex = (date.toEpochDay() % DuaCatalog.items.size).toInt()
    val zikrIndex = (date.toEpochDay() % ZikrCatalog.items.size).toInt()

    val dailyDua = DuaCatalog.items[duaIndex]
    val dailyZikr = ZikrCatalog.items[zikrIndex]

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HidoyatSectionTitle(
            title = stringResource(R.string.home_daily_title)
        )

        DailyTextCard(
            title = R.string.home_dua_title,
            arabic = dailyDua.arabic,
            transliteration = dailyDua.transliteration,
            meaning = dailyDua.meaning,
            onDetails = onDua
        )

        DailyTextCard(
            title = R.string.home_zikr_title,
            arabic = dailyZikr.arabic,
            transliteration = dailyZikr.transliteration,
            meaning = dailyZikr.meaning,
            onDetails = onZikr
        )
    }
}

@Composable
private fun DailyTextCard(
    @StringRes title: Int,
    arabic: String,
    transliteration: String,
    meaning: String,
    onDetails: () -> Unit
) {
    HidoyatCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = arabic,
            style = MaterialTheme.typography.headlineMedium,
            color = HidoyatTheme.colors.gold,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = transliteration,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = HidoyatTheme.colors.textSecondary
        )

        Spacer(Modifier.height(16.dp))

        HidoyatSecondaryButton(
            text = stringResource(R.string.home_details),
            onClick = onDetails,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DailyGoals(done: Set<DailyGoal>, onToggle: (DailyGoal) -> Unit) {
    val goals = listOf(
        DailyGoal.Quran to R.string.home_goal_quran,
        DailyGoal.Zikr to R.string.home_goal_zikr,
        DailyGoal.Dua to R.string.home_goal_dua
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HidoyatSectionTitle(title = stringResource(R.string.home_goals_title))
        HidoyatCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Text(
                text = stringResource(R.string.home_goals_progress, done.size, goals.size),
                style = MaterialTheme.typography.labelLarge,
                color = HidoyatTheme.colors.textSecondary
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = done.size / goals.size.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = HidoyatTheme.colors.gold,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(8.dp))
            goals.forEachIndexed { index, (goal, label) ->
                if (index > 0) HidoyatDivider()
                val checked = goal in done
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(
                            value = checked,
                            role = Role.Checkbox,
                            onValueChange = { onToggle(goal) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (checked) HidoyatTheme.colors.textSecondary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


