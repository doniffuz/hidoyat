package uz.hidoyat.app.feature.prayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.hidoyat.app.data.local.PrayerReminderStore

class PrayerBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext
        val store = PrayerReminderStore(app)
        val settings = store.load()

        val prefs = app.getSharedPreferences(
            "hidoyat_prayer_alarm_times",
            Context.MODE_PRIVATE
        )

        val enabled = mapOf(
            "fajr" to settings.fajr,
            "dhuhr" to settings.dhuhr,
            "asr" to settings.asr,
            "maghrib" to settings.maghrib,
            "isha" to settings.isha
        )

        enabled.forEach { (id, isEnabled) ->
            if (isEnabled && prefs.contains(id)) {
                PrayerReminderScheduler.scheduleNext(app, id)
            }
        }
    }
}
