package uz.hidoyat.app.feature.dua

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DuaUiState(
    val query: String = "",
    val category: DuaCategory? = null,
    val favoritesOnly: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val selectedDua: DuaItem? = null
) {
    val visibleDuas: List<DuaItem>
        get() {
            var result = DuaCatalog.items

            category?.let { selected ->
                result = result.filter { it.category == selected }
            }

            if (favoritesOnly) {
                result = result.filter { it.id in favoriteIds }
            }

            val text = query.trim()
            if (text.isNotEmpty()) {
                result = result.filter {
                    it.title.contains(text, true) ||
                        it.transliteration.contains(text, true) ||
                        it.meaning.contains(text, true)
                }
            }

            return result
        }
}

class DuaViewModel(app: Application) : AndroidViewModel(app) {
    private val store = DuaStore(app)

    private val _state = MutableStateFlow(
        DuaUiState(favoriteIds = store.getFavorites())
    )
    val state: StateFlow<DuaUiState> = _state.asStateFlow()

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun selectCategory(category: DuaCategory?) {
        _state.value = _state.value.copy(
            category = category,
            favoritesOnly = false
        )
    }

    fun showFavorites() {
        _state.value = _state.value.copy(
            category = null,
            favoritesOnly = true
        )
    }

    fun showAll() {
        _state.value = _state.value.copy(
            category = null,
            favoritesOnly = false
        )
    }

    fun toggleFavorite(id: String) {
        val updated = store.toggleFavorite(id)
        _state.value = _state.value.copy(favoriteIds = updated)
    }

    fun openDua(dua: DuaItem) {
        _state.value = _state.value.copy(selectedDua = dua)
    }

    fun closeDua() {
        _state.value = _state.value.copy(selectedDua = null)
    }
}
