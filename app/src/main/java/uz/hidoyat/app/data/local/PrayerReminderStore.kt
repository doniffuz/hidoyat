package uz.hidoyat.app.data.local

import android.content.Context

data class PrayerReminderSettings(
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val soundEnabled: Boolean = true
)

class PrayerReminderStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("hidoyat_prayer_reminders", Context.MODE_PRIVATE)

    fun load(): PrayerReminderSettings = PrayerReminderSettings(
        fajr = prefs.getBoolean("fajr", false),
        dhuhr = prefs.getBoolean("dhuhr", false),
        asr = prefs.getBoolean("asr", false),
        maghrib = prefs.getBoolean("maghrib", false),
        isha = prefs.getBoolean("isha", false),
        soundEnabled = prefs.getBoolean("sound_enabled", true)
    )

    fun setPrayerEnabled(id: String, enabled: Boolean) {
        if (id !in PRAYER_IDS) return
        prefs.edit().putBoolean(id, enabled).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    companion object {
        val PRAYER_IDS = setOf("fajr", "dhuhr", "asr", "maghrib", "isha")
    }
}
