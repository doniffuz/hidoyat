package uz.hidoyat.app.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uz.hidoyat.app.data.local.GoalsStore
import uz.hidoyat.app.data.local.ProfileStore

enum class DailyGoal(val key: String) {
    Quran("quran"),
    Zikr("zikr"),
    Dua("dua")
}

data class HomeUiState(
    val userName: String,
    val date: LocalDate,
    val completedGoals: Set<DailyGoal>
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = GoalsStore(app)
    private val profile = ProfileStore(app)

    private val _state = MutableStateFlow(createState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private fun createState(): HomeUiState {
        val today = LocalDate.now()
        val done = store.load(today)
        return HomeUiState(
            userName = profile.firstName.ifBlank { DEFAULT_USER_NAME },
            date = today,
            completedGoals = DailyGoal.entries.filter { it.key in done }.toSet()
        )
    }

    /** Ilova qaytib ochilganda sana va maqsadlarni yangilaydi. */
    fun refresh() {
        _state.value = createState()
    }

    fun toggleGoal(goal: DailyGoal) {
        _state.update { current ->
            val updated = if (goal in current.completedGoals) {
                current.completedGoals - goal
            } else {
                current.completedGoals + goal
            }
            store.save(current.date, updated.map { it.key }.toSet())
            current.copy(completedGoals = updated)
        }
    }

    private companion object {
        const val DEFAULT_USER_NAME = "Foydalanuvchi"
    }
}
