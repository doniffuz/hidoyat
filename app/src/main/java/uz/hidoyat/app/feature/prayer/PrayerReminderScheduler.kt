package uz.hidoyat.app.feature.prayer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import uz.hidoyat.app.R
import uz.hidoyat.app.data.local.PrayerReminderStore

object PrayerReminderScheduler {

    private const val PREFS = "hidoyat_prayer_alarm_times"

    private val prayerNames = mapOf(
        "fajr" to "Bomdod",
        "dhuhr" to "Peshin",
        "asr" to "Asr",
        "maghrib" to "Shom",
        "isha" to "Xufton"
    )

    fun scheduleAll(context: Context, schedule: PrayerSchedule) {
        val app = context.applicationContext
        val settings = PrayerReminderStore(app).load()
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val times = mapOf(
            "fajr" to schedule.fajr,
            "dhuhr" to schedule.dhuhr,
            "asr" to schedule.asr,
            "maghrib" to schedule.maghrib,
            "isha" to schedule.isha
        )

        prefs.edit().apply {
            times.forEach { (id, time) -> putString(id, time) }
            apply()
        }

        times.forEach { (id, time) ->
            if (settingsEnabled(settings, id)) {
                schedulePrayer(app, id, time)
            } else {
                cancelPrayer(app, id)
            }
        }
    }

    fun scheduleNext(context: Context, id: String) {
        val app = context.applicationContext
        if (id !in prayerNames) return

        val settings = PrayerReminderStore(app).load()
        if (!settingsEnabled(settings, id)) return

        val time = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(id, null) ?: return

        schedulePrayer(app, id, time)
    }

    private fun settingsEnabled(
        settings: uz.hidoyat.app.data.local.PrayerReminderSettings,
        id: String
    ): Boolean = when (id) {
        "fajr" -> settings.fajr
        "dhuhr" -> settings.dhuhr
        "asr" -> settings.asr
        "maghrib" -> settings.maghrib
        "isha" -> settings.isha
        else -> false
    }

    private fun schedulePrayer(context: Context, id: String, time: String) {
        val localTime = try {
            LocalTime.parse(time.trim().take(5))
        } catch (_: Exception) {
            return
        }

        var next = LocalDateTime.now().with(localTime)
        if (!next.isAfter(LocalDateTime.now())) {
            next = next.plusDays(1)
        }

        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction("uz.hidoyat.app.PRAYER_$id")
            .putExtra("prayer_id", id)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancelPrayer(context: Context, id: String) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction("uz.hidoyat.app.PRAYER_$id")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAll(context: Context) {
        prayerNames.keys.forEach { cancelPrayer(context, it) }
    }

    internal fun prayerName(id: String): String = prayerNames[id] ?: "Namoz"
}

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("prayer_id") ?: return
        if (id !in PrayerReminderStore.PRAYER_IDS) return

        val app = context.applicationContext
        val settings = PrayerReminderStore(app).load()

        val enabled = when (id) {
            "fajr" -> settings.fajr
            "dhuhr" -> settings.dhuhr
            "asr" -> settings.asr
            "maghrib" -> settings.maghrib
            "isha" -> settings.isha
            else -> false
        }

        if (!enabled) return

        showNotification(app, id, settings.soundEnabled)
        PrayerReminderScheduler.scheduleNext(app, id)
    }

    private fun showNotification(context: Context, id: String, sound: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        val channelId = if (sound) "prayer_reminders_sound" else "prayer_reminders_silent"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (sound) "Namoz eslatmalari" else "Ovozsiz namoz eslatmalari",
                NotificationManager.IMPORTANCE_HIGH
            )
            if (!sound) {
                channel.setSound(null, null)
                channel.enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        val name = PrayerReminderScheduler.prayerName(id)
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("HIDOYAT • $name vaqti kirdi")
            .setContentText("$name namozini ado etishni unutmang.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()

        manager.notify(id.hashCode(), notification)
    }
}
