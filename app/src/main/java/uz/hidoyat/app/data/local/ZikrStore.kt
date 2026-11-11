package uz.hidoyat.app.data.local

import android.content.Context
import java.time.LocalDate

data class ZikrProgress(
    val date: LocalDate,
    val zikrId: String,
    val count: Int,
    val goal: Int,
    val totalToday: Int
)

class ZikrStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("hidoyat_zikr", Context.MODE_PRIVATE)

    fun load(today: LocalDate): ZikrProgress {
        val savedDate = prefs.getString(KEY_DATE, null)
        if (savedDate != today.toString()) {
            return ZikrProgress(
                date = today,
                zikrId = DEFAULT_ZIKR_ID,
                count = 0,
                goal = DEFAULT_GOAL,
                totalToday = 0
            )
        }

        return ZikrProgress(
            date = today,
            zikrId = prefs.getString(KEY_ZIKR_ID, DEFAULT_ZIKR_ID) ?: DEFAULT_ZIKR_ID,
            count = prefs.getInt(KEY_COUNT, 0),
            goal = prefs.getInt(KEY_GOAL, DEFAULT_GOAL),
            totalToday = prefs.getInt(KEY_TOTAL_TODAY, 0)
        )
    }

    fun save(
        today: LocalDate,
        zikrId: String,
        count: Int,
        goal: Int,
        totalToday: Int
    ) {
        prefs.edit()
            .putString(KEY_DATE, today.toString())
            .putString(KEY_ZIKR_ID, zikrId)
            .putInt(KEY_COUNT, count)
            .putInt(KEY_GOAL, goal)
            .putInt(KEY_TOTAL_TODAY, totalToday)
            .apply()
    }

    fun reset(today: LocalDate, zikrId: String, goal: Int) {
        save(
            today = today,
            zikrId = zikrId,
            count = 0,
            goal = goal,
            totalToday = 0
        )
    }

    private companion object {
        const val KEY_DATE = "date"
        const val KEY_ZIKR_ID = "zikr_id"
        const val KEY_COUNT = "count"
        const val KEY_GOAL = "goal"
        const val KEY_TOTAL_TODAY = "total_today"

        const val DEFAULT_ZIKR_ID = "subhanallah"
        const val DEFAULT_GOAL = 33
    }
}
