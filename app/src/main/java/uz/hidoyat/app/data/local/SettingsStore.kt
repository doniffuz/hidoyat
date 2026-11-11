package uz.hidoyat.app.data.local

import android.content.Context

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "hidoyat_settings",
        Context.MODE_PRIVATE
    )

    var themeMode: ThemeMode
        get() = when (prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        set(value) {
            prefs.edit()
                .putString(KEY_THEME, value.name)
                .apply()
        }

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
