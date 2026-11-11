package uz.hidoyat.app.feature.prayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.data.local.PrayerReminderSettings
import uz.hidoyat.app.data.local.PrayerReminderStore

@Composable
fun PrayerReminderSettings(schedule: PrayerSchedule) {
    val context = LocalContext.current
    val store = remember { PrayerReminderStore(context) }
    var settings by remember { mutableStateOf(store.load()) }
    var pendingPrayer by remember { mutableStateOf<String?>(null) }

    fun updatePrayer(id: String, enabled: Boolean) {
        store.setPrayerEnabled(id, enabled)
        settings = store.load()
        if (enabled) {
            PrayerReminderScheduler.scheduleAll(context, schedule)
        } else {
            PrayerReminderScheduler.cancelPrayer(context, id)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val id = pendingPrayer
        pendingPrayer = null
        if (granted && id != null) {
            updatePrayer(id, true)
        }
    }

    fun requestOrEnable(id: String, enabled: Boolean) {
        if (!enabled) {
            updatePrayer(id, false)
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingPrayer = id
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            updatePrayer(id, true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Namoz eslatmalari",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Namoz vaqti kirganda telefonga bildirishnoma keladi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ReminderSwitch("Bomdod", settings.fajr) {
            requestOrEnable("fajr", it)
        }
        ReminderSwitch("Peshin", settings.dhuhr) {
            requestOrEnable("dhuhr", it)
        }
        ReminderSwitch("Asr", settings.asr) {
            requestOrEnable("asr", it)
        }
        ReminderSwitch("Shom", settings.maghrib) {
            requestOrEnable("maghrib", it)
        }
        ReminderSwitch("Xufton", settings.isha) {
            requestOrEnable("isha", it)
        }

        ReminderSwitch("Bildirishnoma ovozi", settings.soundEnabled) {
            store.setSoundEnabled(it)
            settings = store.load()
        }
    }
}

@Composable
private fun ReminderSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    HidoyatCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
