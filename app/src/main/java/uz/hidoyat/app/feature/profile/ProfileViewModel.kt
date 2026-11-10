package uz.hidoyat.app.feature.profile

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

data class ProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val avatar: Bitmap? = null,
    val editing: Boolean = false,
    val draftFirstName: String = "",
    val draftLastName: String = "",
    val draftAvatar: Bitmap? = null,
    val avatarChanged: Boolean = false,
    val saving: Boolean = false
) {
    val fullName: String get() = "$firstName $lastName".trim()

    /** Ism bo'sh bo'lsa saqlashga ruxsat yo'q. */
    val canSave: Boolean get() = draftFirstName.isNotBlank() && !saving
}

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProfileStore(app)

    private val _state = MutableStateFlow(
        ProfileUiState(firstName = store.firstName, lastName = store.lastName)
    )
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val avatar = withContext(Dispatchers.IO) { store.loadAvatar() }
            _state.update { it.copy(avatar = avatar) }
        }
    }

    fun startEdit() = _state.update {
        it.copy(
            editing = true,
            draftFirstName = it.firstName,
            draftLastName = it.lastName,
            draftAvatar = it.avatar,
            avatarChanged = false
        )
    }

    fun cancelEdit() = _state.update {
        it.copy(editing = false, draftAvatar = null, avatarChanged = false)
    }

    fun onFirstNameChange(value: String) = _state.update { it.copy(draftFirstName = value.take(MAX_NAME)) }

    fun onLastNameChange(value: String) = _state.update { it.copy(draftLastName = value.take(MAX_NAME)) }

    fun onAvatarPicked(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { AvatarImage.decode(resolver, uri) }
            if (bitmap != null) _state.update { it.copy(draftAvatar = bitmap, avatarChanged = true) }
        }
    }

    fun removeAvatar() = _state.update { it.copy(draftAvatar = null, avatarChanged = true) }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val first = current.draftFirstName.trim()
            val last = current.draftLastName.trim()
            withContext(Dispatchers.IO) {
                store.update(first, last, current.draftAvatar, current.avatarChanged)
            }
            _state.update {
                it.copy(
                    firstName = first,
                    lastName = last,
                    avatar = current.draftAvatar,
                    editing = false,
                    draftAvatar = null,
                    avatarChanged = false,
                    saving = false
                )
            }
            onSaved()
        }
    }

    private companion object {
        const val MAX_NAME = 30
    }
}
