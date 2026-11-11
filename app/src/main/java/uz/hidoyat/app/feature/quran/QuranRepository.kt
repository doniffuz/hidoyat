package uz.hidoyat.app.feature.quran

import android.content.Context
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

sealed interface QuranLocal {
    data class Ready(val data: QuranData) : QuranLocal
    /** Ilova ichida ham, yuklangan ham ma'lumot yo'q. */
    data object Missing : QuranLocal
    /** Fayl bor, lekin buzilgan/to'liq emas. */
    data class Corrupt(val reason: String) : QuranLocal
}

/**
 * Offline-first: 1) ilova ichidagi assets/quran, 2) filesDir/quran (ilova ichida yuklangan).
 * Yuklash vaqtida hamma narsa avval vaqtinchalik papkaga yoziladi, to'liq tekshiriladi va faqat
 * shundan keyin yagona rename bilan almashtiriladi — qisman fayl hech qachon "tayyor" bo'lmaydi.
 */
class QuranRepository(context: Context) {
    private val appContext = context.applicationContext
    private val remote = QuranRemoteSource()

    private val finalDir get() = File(appContext.filesDir, DIR)
    private val tempDir get() = File(appContext.filesDir, "$DIR.tmp")
    private val oldDir get() = File(appContext.filesDir, "$DIR.old")

    suspend fun loadLocal(): QuranLocal = withContext(Dispatchers.IO) {
        cleanupLeftovers()
        var corrupt: String? = null
        try {
            val assetNames = appContext.assets.list(DIR)?.toSet().orEmpty()
            if (FILE_AR in assetNames && FILE_UZ in assetNames) {
                // Izohlar (footnotes) fayli majburiy: u bo'lmasa izohlar jimgina yo'qolardi
                if (FILE_NOTES !in assetNames) throw QuranException(QuranErrorKind.BAD_DATA, "$FILE_NOTES yo'q")
                return@withContext QuranLocal.Ready(
                    build(
                        read { appContext.assets.open("$DIR/$FILE_AR").bufferedReader(Charsets.UTF_8).readText() },
                        read { appContext.assets.open("$DIR/$FILE_UZ").bufferedReader(Charsets.UTF_8).readText() },
                        read { appContext.assets.open("$DIR/$FILE_NOTES").bufferedReader(Charsets.UTF_8).readText() }
                    )
                )
            }
        } catch (e: QuranException) {
            corrupt = "assets: ${e.message}"
        } catch (e: IOException) {
            corrupt = "assets: ${e.message}"
        }

        val ar = File(finalDir, FILE_AR)
        val uz = File(finalDir, FILE_UZ)
        val notesFile = File(finalDir, FILE_NOTES)
        if (ar.isFile && uz.isFile) {
            try {
                if (!notesFile.isFile) throw QuranException(QuranErrorKind.BAD_DATA, "$FILE_NOTES yo'q")
                return@withContext QuranLocal.Ready(
                    build(ar.readText(Charsets.UTF_8), uz.readText(Charsets.UTF_8), notesFile.readText(Charsets.UTF_8))
                )
            } catch (e: QuranException) {
                finalDir.deleteRecursively() // buzilgan yuklama — qayta yuklash uchun o'chiriladi
                return@withContext QuranLocal.Corrupt(e.message ?: "buzilgan")
            } catch (e: IOException) {
                return@withContext QuranLocal.Corrupt(e.message ?: "o'qib bo'lmadi")
            }
        }
        if (corrupt != null) QuranLocal.Corrupt(corrupt) else QuranLocal.Missing
    }

    /**
     * 114 suraning hammasini yuklaydi. [onProgress] — tugatilgan suralar soni.
     * Muvaffaqiyatsiz yoki bekor qilinsa, mavjud ma'lumot tegilmaydi.
     */
    suspend fun download(onProgress: (Int) -> Unit): QuranData = withContext(Dispatchers.IO) {
        val ar = StringBuilder()
        val uz = StringBuilder()
        val notes = StringBuilder()
        for (surah in 1..QuranCatalog.SURAH_COUNT) {
            ensureActive()
            val ayahs = fetchWithRetry(surah)
            for (a in ayahs) {
                ar.append(surah).append('|').append(a.ayah).append('|').append(QuranTextFormat.escape(a.arabic)).append('\n')
                uz.append(surah).append('|').append(a.ayah).append('|').append(QuranTextFormat.escape(a.translation)).append('\n')
                a.footnote?.let {
                    notes.append(surah).append('|').append(a.ayah).append('|').append(QuranTextFormat.escape(it)).append('\n')
                }
            }
            onProgress(surah)
        }
        val arText = header("Arabic text") + ar
        val uzText = header("Translation: Muhammad Sodiq") + uz
        val notesText = header("Footnotes") + notes
        // Yozishdan oldin xotirada to'liq tekshiramiz
        val data = build(arText, uzText, notesText)
        ensureActive()
        try {
            tempDir.deleteRecursively()
            if (!tempDir.mkdirs()) throw IOException("Papka yaratilmadi")
            File(tempDir, FILE_AR).writeText(arText, Charsets.UTF_8)
            File(tempDir, FILE_UZ).writeText(uzText, Charsets.UTF_8)
            File(tempDir, FILE_NOTES).writeText(notesText, Charsets.UTF_8)
            // Diskdan qayta o'qib tekshiramiz
            build(
                File(tempDir, FILE_AR).readText(Charsets.UTF_8),
                File(tempDir, FILE_UZ).readText(Charsets.UTF_8),
                File(tempDir, FILE_NOTES).readText(Charsets.UTF_8)
            )
            oldDir.deleteRecursively()
            if (finalDir.exists() && !finalDir.renameTo(oldDir)) throw IOException("Eski papka almashtirilmadi")
            if (!tempDir.renameTo(finalDir)) {
                if (oldDir.exists()) oldDir.renameTo(finalDir)
                throw IOException("Yangi papka almashtirilmadi")
            }
            oldDir.deleteRecursively()
        } catch (e: IOException) {
            throw QuranException(QuranErrorKind.STORAGE, "Saqlashda xato: ${e.message}", e)
        } finally {
            // Muvaffaqiyatli almashtirishdan keyin tempDir yo'q; xato yoki bekor qilishda qoldiq o'chiriladi
            tempDir.deleteRecursively()
        }
        data
    }

    private suspend fun fetchWithRetry(surah: Int): List<RemoteAyah> {
        var last: QuranException? = null // fetchSurah endi suspend: bekor qilinganda ulanish darhol uziladi
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return remote.fetchSurah(surah)
            } catch (e: CancellationException) {
                throw e
            } catch (e: QuranException) {
                last = e
                // Ma'lumot buzuq bo'lsa yoki internet butunlay yo'q bo'lsa qayta urinish foydasiz
                if (e.kind == QuranErrorKind.BAD_DATA || e.kind == QuranErrorKind.NO_INTERNET) throw e
                if (attempt < MAX_ATTEMPTS - 1) delay(700L * (attempt + 1))
            }
        }
        throw last ?: QuranException(QuranErrorKind.UNKNOWN, "Noma'lum xato")
    }

    private fun header(kind: String): String =
        "# source: QuranEnc.com\n" +
            "# translation_key: ${QuranRemoteSource.TRANSLATION_KEY}\n" +
            "# translator: Muhammad Sodiq\n" +
            "# version: $TRANSLATION_VERSION\n" +
            "# url: https://quranenc.com\n" +
            "# content: $kind\n"

    private fun build(arabic: String, translation: String, notes: String?): QuranData =
        QuranTextFormat.build(
            QuranTextFormat.parse(arabic, FILE_AR),
            QuranTextFormat.parse(translation, FILE_UZ),
            notes?.let { QuranTextFormat.parse(it, FILE_NOTES, allowPartial = true) }
        )

    private inline fun read(block: () -> String): String = block()

    /** Yarim yo'lda to'xtagan yuklashdan qolgan vaqtinchalik papkalarni tozalaydi. */
    private fun cleanupLeftovers() {
        tempDir.deleteRecursively()
        if (!finalDir.exists() && oldDir.exists()) oldDir.renameTo(finalDir) else oldDir.deleteRecursively()
    }

    companion object {
        const val DIR = "quran"
        const val FILE_AR = "quran_ar.txt"
        const val FILE_UZ = "quran_uz.txt"
        const val FILE_NOTES = "quran_uz_notes.txt"
        const val TRANSLATION_VERSION = "1.0.1"
        private const val MAX_ATTEMPTS = 3
    }
}
