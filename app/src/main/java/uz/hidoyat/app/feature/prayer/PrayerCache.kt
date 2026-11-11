package uz.hidoyat.app.feature.prayer

import android.content.Context
import org.json.JSONObject

data class PrayerSelection(
    val provinceId: String?,
    val districtId: String?,
    val manualSlug: String?
)

/** Oxirgi muvaffaqiyatli vaqtlar va oxirgi tanlangan hudud (faqat telefonda). */
class PrayerCache(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("hidoyat_prayer", Context.MODE_PRIVATE)

    fun save(key: String, result: PrayerResult) {
        val s = result.schedule
        val json = JSONObject()
            .put("city", s.city).put("date", s.date)
            .put("fajr", s.fajr).put("sunrise", s.sunrise).put("dhuhr", s.dhuhr)
            .put("asr", s.asr).put("maghrib", s.maghrib).put("isha", s.isha)
            .put("source", s.source).put("updatedAt", s.updatedAt)
            .put("origin", result.source.name)
        prefs.edit().putString(PREFIX + key, json.toString()).apply()
    }

    fun load(key: String): PrayerResult? {
        val raw = prefs.getString(PREFIX + key, null) ?: return null
        return try {
            val j = JSONObject(raw)
            val schedule = PrayerSchedule(
                city = j.getString("city"),
                date = j.getString("date"),
                fajr = j.getString("fajr"),
                sunrise = j.getString("sunrise"),
                dhuhr = j.getString("dhuhr"),
                asr = j.getString("asr"),
                maghrib = j.getString("maghrib"),
                isha = j.getString("isha"),
                source = j.getString("source"),
                updatedAt = j.getLong("updatedAt")
            )
            if (!PrayerValidator.isValidSchedule(schedule)) return null
            PrayerResult(schedule, PrayerDataSource.valueOf(j.getString("origin")))
        } catch (e: Exception) {
            null
        }
    }

    fun saveSelection(selection: PrayerSelection) {
        prefs.edit()
            .putString(KEY_PROVINCE, selection.provinceId)
            .putString(KEY_DISTRICT, selection.districtId)
            .putString(KEY_MANUAL, selection.manualSlug)
            .apply()
    }

    fun loadSelection() = PrayerSelection(
        provinceId = prefs.getString(KEY_PROVINCE, null),
        districtId = prefs.getString(KEY_DISTRICT, null),
        manualSlug = prefs.getString(KEY_MANUAL, null)
    )

    private companion object {
        const val PREFIX = "cache:"
        const val KEY_PROVINCE = "sel_province"
        const val KEY_DISTRICT = "sel_district"
        const val KEY_MANUAL = "sel_manual"
    }
}
