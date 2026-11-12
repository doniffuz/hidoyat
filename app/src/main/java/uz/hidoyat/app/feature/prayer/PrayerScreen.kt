package uz.hidoyat.app.feature.prayer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatPrimaryButton
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

@Composable
fun PrayerScreen(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> viewModel.onLocationPermissionResult(result.values.any { it }) }

    PrayerContent(
        state = state,
        modifier = modifier,
        onProvince = viewModel::selectProvince,
        onDistrict = viewModel::selectDistrict,
        onManualRegion = viewModel::selectManualRegion,
        onRefresh = viewModel::refresh,
        onLocate = {
            if (PrayerLocation.hasPermission(context)) {
                viewModel.locate()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    )
}


@Composable
private fun PrayerContent(
    state: PrayerUiState,
    modifier: Modifier,
    onProvince: (String) -> Unit,
    onDistrict: (String) -> Unit,
    onManualRegion: (String) -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit
) {
    val colors = HidoyatTheme.colors
    val canLoad = state.selectedDistrict != null &&
        (state.match is NamozMatch.Exact || state.manualRegion != null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.prayer_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onCard
            )

            HidoyatCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectorField(
                        label = stringResource(R.string.prayer_province),
                        value = state.selectedProvince?.name,
                        placeholder = stringResource(R.string.prayer_select_province),
                        enabled = true,
                        options = state.provinces.map { it.id to it.name },
                        onSelect = onProvince
                    )
                    SelectorField(
                        label = stringResource(R.string.prayer_district),
                        value = state.selectedDistrict?.name,
                        placeholder = stringResource(
                            if (state.selectedProvince == null) R.string.prayer_select_province_first
                            else R.string.prayer_select_district
                        ),
                        enabled = state.selectedProvince != null,
                        options = state.districts.map { it.id to it.name },
                        onSelect = onDistrict
                    )
                    TextButton(
                        onClick = onLocate,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(stringResource(R.string.prayer_use_location))
                    }
                    state.locationHint?.let { hint ->
                        Text(
                            text = stringResource(
                                when (hint) {
                                    LocationHint.APPROXIMATE -> R.string.prayer_loc_approx
                                    LocationHint.PERMISSION_DENIED -> R.string.prayer_loc_denied
                                    LocationHint.UNAVAILABLE -> R.string.prayer_loc_unavailable
                                    LocationHint.OUTSIDE -> R.string.prayer_loc_outside
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (state.selectedDistrict == null) {
                Text(
                    text = stringResource(R.string.prayer_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }

            if (state.match is NamozMatch.Unresolved) {
                HidoyatCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.prayer_unresolved_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onCard
                        )
                        Text(
                            text = stringResource(R.string.prayer_unresolved_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        SelectorField(
                            label = stringResource(R.string.prayer_manual_region),
                            value = state.manualRegion?.name,
                            placeholder = stringResource(R.string.prayer_manual_select),
                            enabled = true,
                            options = state.namozRegions.map { it.slug to it.name },
                            onSelect = onManualRegion
                        )
                    }
                }
            }

            if (canLoad) {
                HidoyatPrimaryButton(
                    text = stringResource(R.string.prayer_refresh),
                    onClick = onRefresh,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = colors.gold
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.prayer_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            if (state.error != null) {
                HidoyatCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prayer_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            state.schedule?.let { schedule ->
                ScheduleSection(state, schedule)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ScheduleSection(state: PrayerUiState, schedule: PrayerSchedule) {
    val colors = HidoyatTheme.colors
    val names = listOf(
        R.string.prayer_fajr, R.string.prayer_sunrise, R.string.prayer_dhuhr,
        R.string.prayer_asr, R.string.prayer_maghrib, R.string.prayer_isha
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = schedule.city,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onCard
        )
        val province = state.selectedProvince?.name
        Text(
            text = listOfNotNull(province, schedule.date.takeIf { it.isNotBlank() })
                .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        if (state.fromCache && !state.isLoading) {
            val note = if (isSavedToday(schedule.updatedAt)) {
                stringResource(R.string.prayer_cache_today)
            } else {
                stringResource(
                    R.string.prayer_cache_stale,
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(schedule.updatedAt))
                )
            }
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        schedule.asList().forEachIndexed { index, prayer ->
            HidoyatCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(names[index]),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onCard,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = prayer.time,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.gold
                    )
                }
            }
        }

        PrayerReminderSettings(schedule)

        val backup = state.source == PrayerDataSource.ALADHAN_BACKUP
        Text(
            text = stringResource(
                if (backup) R.string.prayer_source_backup else R.string.prayer_source_primary
            ),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        if (backup) {
            Text(
                text = stringResource(R.string.prayer_source_backup_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SelectorField(
    label: String,
    value: String?,
    placeholder: String,
    enabled: Boolean,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    val colors = HidoyatTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.cardBorder)
            ) {
                Text(
                    text = value ?: placeholder,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (value != null) colors.onCard else colors.textSecondary
                )
                Text(text = "▾", color = colors.gold)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onSelect(id)
                        }
                    )
                }
            }
        }
    }
}
