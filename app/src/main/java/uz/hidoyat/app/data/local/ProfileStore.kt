package uz.hidoyat.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Foydalanuvchi profilini (ism, familiya, avatar) faqat telefonda saqlaydi. */
class ProfileStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("hidoyat_profile", Context.MODE_PRIVATE)
    private val avatarFile get() = File(appContext.filesDir, AVATAR_FILE)

    val firstName: String get() = prefs.getString(KEY_FIRST, "").orEmpty()
    val lastName: String get() = prefs.getString(KEY_LAST, "").orEmpty()

    /** Welcome sahifasi tugatilganmi? */
    val isWelcomeCompleted: Boolean
        get() = prefs.getBoolean(KEY_DONE, false) && firstName.isNotBlank()

    fun loadAvatar(): Bitmap? =
        if (avatarFile.exists()) BitmapFactory.decodeFile(avatarFile.absolutePath) else null

    fun save(firstName: String, lastName: String, avatar: Bitmap?) =
        update(firstName, lastName, avatar, avatarChanged = true)

    /**
     * Profilni yangilaydi. [avatarChanged] false bo'lsa mavjud avatar fayliga tegilmaydi;
     * true bo'lsa yangi rasm yoziladi yoki ([avatar] null bo'lsa) eski fayl o'chiriladi.
     */
    fun update(firstName: String, lastName: String, avatar: Bitmap?, avatarChanged: Boolean) {
        if (avatarChanged) {
            if (avatar != null) writeAvatar(avatar) else avatarFile.delete()
        }
        prefs.edit()
            .putString(KEY_FIRST, firstName)
            .putString(KEY_LAST, lastName)
            .putBoolean(KEY_DONE, true)
            .commit()
    }

    /** Avval vaqtinchalik faylga yozadi, so'ng almashtiradi: yozish uzilsa eski avatar saqlanadi. */
    private fun writeAvatar(avatar: Bitmap) {
        val tmp = File(appContext.filesDir, "$AVATAR_FILE.tmp")
        try {
            tmp.outputStream().use { avatar.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (!tmp.renameTo(avatarFile)) {
                avatarFile.delete()
                tmp.renameTo(avatarFile)
            }
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    private companion object {
        const val KEY_FIRST = "first_name"
        const val KEY_LAST = "last_name"
        const val KEY_DONE = "welcome_done"
        const val AVATAR_FILE = "profile_avatar.jpg"
    }
}
