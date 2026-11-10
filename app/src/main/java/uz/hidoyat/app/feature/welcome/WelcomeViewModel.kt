package uz.hidoyat.app.feature.welcome

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hidoyat.app.core.util.AvatarImage
import uz.hidoyat.app.data.local.ProfileStore

data class WelcomeUiState(
    val firstName: String = "",
    val lastName: String = "",
    val avatar: Bitmap? = null,
    val saving: Boolean = false
) {
    /** Ism kiritilmaguncha davom etib bo'lmaydi. */
    val canStart: Boolean get() = firstName.isNotBlank() && !saving
}

class WelcomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProfileStore(app)

    private val _completed = MutableStateFlow(store.isWelcomeCompleted)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun onFirstNameChange(value: String) = _state.update { it.copy(firstName = value.take(MAX_NAME)) }

    fun onLastNameChange(value: String) = _state.update { it.copy(lastName = value.take(MAX_NAME)) }

    fun onAvatarPicked(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { AvatarImage.decode(resolver, uri) }
            if (bitmap != null) _state.update { it.copy(avatar = bitmap) }
        }
    }

    fun removeAvatar() = _state.update { it.copy(avatar = null) }

    /** Ma'lumotlarni saqlab, bosh sahifaga o'tadi. */
    fun start(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canStart) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                store.save(current.firstName.trim(), current.lastName.trim(), current.avatar)
            }
            onSaved()
            _completed.value = true
        }
    }

    private companion object {
        const val MAX_NAME = 30
    }
}
