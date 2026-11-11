package uz.hidoyat.app.feature.zikr

import android.app.Application
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.hidoyat.app.data.local.ZikrProgress
import uz.hidoyat.app.data.local.ZikrStore
import java.time.LocalDate

class ZikrViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ZikrStore(app)
    private val _state = MutableStateFlow(store.load(LocalDate.now()))
    val state: StateFlow<ZikrProgress> = _state.asStateFlow()

    fun selectZikr(id: String) {
        val current = _state.value
        val item = ZikrCatalog.find(id)
        update(
            zikrId = item.id,
            count = 0,
            goal = item.recommendedCount,
            totalToday = current.totalToday
        )
    }

    fun setGoal(goal: Int) {
        if (goal !in listOf(33, 99, 100)) return
        val current = _state.value
        update(current.zikrId, current.count, goal, current.totalToday)
    }

    fun increment() {
        val current = _state.value
        val next = current.count + 1
        update(
            current.zikrId,
            next,
            current.goal,
            current.totalToday + 1
        )
        vibrate()
    }

    fun reset() {
        val current = _state.value
        store.reset(LocalDate.now(), current.zikrId, current.goal)
        _state.value = store.load(LocalDate.now())
    }

    private fun update(
        zikrId: String,
        count: Int,
        goal: Int,
        totalToday: Int
    ) {
        val today = LocalDate.now()
        store.save(today, zikrId, count, goal, totalToday)
        _state.value = store.load(today)
    }

    private fun vibrate() {
        val context = getApplication<Application>()
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        25,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {
        }
    }
}
