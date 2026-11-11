package uz.hidoyat.app.feature.quran

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

/**
 * Arabcha Qur'on matni uchun shrift. Shrift fayli ilovaga qo'shiladi (tizim shriftiga ishonilmaydi):
 *   app/src/main/assets/fonts/quran_arabic.ttf
 * Tavsiya: Amiri Quran (SIL OFL 1.1 litsenziyasi — qo'shishdan oldin litsenziya matnini
 * rasmiy manbadan tekshiring va OFL.txt ni shrift bilan birga saqlang).
 *
 * Fayl yo'q yoki buzuq bo'lsa ilova qulamaydi: tizim Serif shrifti ishlatiladi va [isBundled] false bo'ladi.
 * Shrift fayli bu repozitoriyga QO'SHILMAGAN — uni qo'lda qo'shish kerak.
 */
object QuranFont {
    private const val DIR = "fonts"
    private const val FILE = "quran_arabic.ttf"
    const val ASSET_PATH = "$DIR/$FILE"

    private var loaded = false
    private var family: FontFamily? = null

    @Synchronized
    fun family(context: Context): FontFamily {
        if (!loaded) {
            loaded = true
            family = try {
                val assets = context.applicationContext.assets
                if (FILE in assets.list(DIR).orEmpty()) {
                    FontFamily(Typeface.createFromAsset(assets, ASSET_PATH))
                } else null
            } catch (e: Exception) { // fayl buzilgan yoki o'qib bo'lmadi
                null
            }
        }
        return family ?: FontFamily.Serif
    }

    @Synchronized
    fun isBundled(context: Context): Boolean {
        family(context)
        return family != null
    }
}

@Composable
fun rememberQuranFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember(context) { QuranFont.family(context) }
}
