package uz.hidoyat.app.data.local

import android.content.Context
import java.time.LocalDate

/** Kunlik maqsadlar holatini SharedPreferences'da saqlaydi. Har yangi kunda holat tozalanadi. */
class GoalsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("hidoyat_goals", Context.MODE_PRIVATE)

    fun load(today: LocalDate): Set<String> {
        if (prefs.getString(KEY_DATE, null) != today.toString()) return emptySet()
        return prefs.getStringSet(KEY_DONE, emptySet()).orEmpty().toSet()
    }

    fun save(today: LocalDate, done: Set<String>) {
        prefs.edit()
            .putString(KEY_DATE, today.toString())
            .putStringSet(KEY_DONE, HashSet(done))
            .apply()
    }

    private companion object {
        const val KEY_DATE = "date"
        const val KEY_DONE = "done"
    }
}
