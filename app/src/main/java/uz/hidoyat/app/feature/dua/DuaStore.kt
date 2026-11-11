package uz.hidoyat.app.feature.dua

import android.content.Context

class DuaStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "hidoyat_dua",
        Context.MODE_PRIVATE
    )

    fun getFavorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun isFavorite(id: String): Boolean =
        id in getFavorites()

    fun toggleFavorite(id: String): Set<String> {
        val updated = getFavorites().toMutableSet()
        if (!updated.add(id)) {
            updated.remove(id)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
        return updated
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_duas"
    }
}
