package uz.hidoyat.app.feature.quran

import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** QuranEnc API'dan kelgan bitta oyat (o'zgartirilmagan). */
data class RemoteAyah(
    val ayah: Int,
    val arabic: String,
    val translation: String,
    val footnote: String?
)

/**
 * QuranEnc.com — tarjima kaliti `uzbek_sadiq` (Muhammad Sodiq).
 * Har bir sura alohida so'raladi va oyatlar soni katalogga qarshi tekshiriladi.
 *
 * Bekor qilish: korutina bekor qilinsa, bloklovchi HTTP ulanish darhol uziladi
 * (aks holda o'qish [TIMEOUT_MS] gacha davom etardi).
 */
class QuranRemoteSource {

    /** @throws QuranException tarmoq, server yoki ma'lumot xatosi bo'lsa */
    suspend fun fetchSurah(surah: Int): List<RemoteAyah> {
        val expected = QuranCatalog.surah(surah)?.ayahCount
            ?: throw QuranException(QuranErrorKind.BAD_DATA, "Noto'g'ri sura: $surah")
        val url = URL(String.format(Locale.ROOT, API_URL, TRANSLATION_KEY, surah))
        val body = withContext(Dispatchers.IO) {
            coroutineScope {
                val active = AtomicReference<HttpURLConnection?>(null)
                // Bekor qilinganda (yoki tugaganda) ulanishni uzadi
                val watchdog = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        active.get()?.disconnect()
                    }
                }
                try {
                    download(url, active) { !isActive }
                } catch (e: QuranException) {
                    ensureActive() // bekor qilish tufayli uzilgan bo'lsa — xato emas, CancellationException
                    throw e
                } finally {
                    watchdog.cancel()
                }
            }
        }
        return parse(body, surah, expected)
    }

    private fun download(
        url: URL,
        holder: AtomicReference<HttpURLConnection?>,
        cancelled: () -> Boolean
    ): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            holder.set(connection)
            if (cancelled()) throw IOException("bekor qilindi") // watchdog allaqachon ishlab bo'lgan bo'lishi mumkin
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw QuranException(QuranErrorKind.SERVER, "Server javobi: HTTP $code")
            }
            return connection.inputStream.use { readLimited(InputStreamReader(it, Charsets.UTF_8)) }
        } catch (e: QuranException) {
            throw e
        } catch (e: UnknownHostException) {
            throw QuranException(QuranErrorKind.NO_INTERNET, "Internet yo'q", e)
        } catch (e: SocketTimeoutException) {
            throw QuranException(QuranErrorKind.TIMEOUT, "Kutish vaqti tugadi", e)
        } catch (e: SSLException) {
            throw QuranException(QuranErrorKind.SERVER, "Xavfsiz ulanish xatosi", e)
        } catch (e: IOException) {
            // ConnectException va boshqa tarmoq uzilishlari
            throw QuranException(QuranErrorKind.NO_INTERNET, "Tarmoq xatosi", e)
        } finally {
            holder.set(null)
            connection?.disconnect()
        }
    }

    /** Javob hajmini cheklaydi: buzuq/zararli server xotirani to'ldira olmasin. */
    private fun readLimited(reader: InputStreamReader): String {
        val sb = StringBuilder()
        val buf = CharArray(8192)
        while (true) {
            val n = reader.read(buf)
            if (n < 0) break
            sb.append(buf, 0, n)
            if (sb.length > MAX_RESPONSE_CHARS) {
                throw QuranException(QuranErrorKind.BAD_DATA, "Javob juda katta")
            }
        }
        return sb.toString()
    }

    private fun parse(body: String, surah: Int, expected: Int): List<RemoteAyah> {
        try {
            val array = JSONObject(body).getJSONArray("result")
            val byAyah = HashMap<Int, RemoteAyah>(expected * 2)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                // Eslatma: Android'da JSONObject.NULL uchun optString/getString "null" qaytaradi — shuning uchun isNull tekshiriladi
                val itemSurah = if (item.isNull("sura")) null else item.optString("sura").trim().toIntOrNull()
                if (itemSurah != null && itemSurah != surah) bad("$surah-sura javobida boshqa sura: $itemSurah")
                val ayah = requiredText(item, "aya", surah).trim().toIntOrNull() ?: bad("Oyat raqami noto'g'ri")
                if (ayah !in 1..expected) bad("Diapazondan tashqari oyat $surah:$ayah")
                val arabic = requiredText(item, "arabic_text", surah, ayah).trim()
                val translation = requiredText(item, "translation", surah, ayah).trim()
                if (arabic.isEmpty() || translation.isEmpty()) bad("Bo'sh matn $surah:$ayah")
                // footnotes ixtiyoriy: yo'q yoki null bo'lsa — izoh yo'q (literal "null" saqlanmaydi)
                val note = if (item.isNull("footnotes")) null else item.getString("footnotes").trim().ifEmpty { null }
                if (byAyah.put(ayah, RemoteAyah(ayah, arabic, translation, note)) != null) {
                    bad("Takrorlangan oyat $surah:$ayah")
                }
            }
            if (byAyah.size != expected) bad("$surah-sura to'liq kelmadi: ${byAyah.size} / $expected")
            return (1..expected).map { byAyah.getValue(it) }
        } catch (e: JSONException) {
            throw QuranException(QuranErrorKind.BAD_DATA, "JSON xato: ${e.message}", e)
        }
    }

    private fun requiredText(item: JSONObject, key: String, surah: Int, ayah: Int? = null): String {
        if (!item.has(key) || item.isNull(key)) bad("'$key' yo'q: $surah:${ayah ?: "?"}")
        return item.getString(key)
    }

    private fun bad(message: String): Nothing =
        throw QuranException(QuranErrorKind.BAD_DATA, message)

    companion object {
        const val TRANSLATION_KEY = "uzbek_sadiq"
        const val API_URL = "https://quranenc.com/api/v1/translation/sura/%s/%d"
        const val USER_AGENT = "HIDOYAT-QuranApp/1.0"
        private const val TIMEOUT_MS = 20_000
        private const val MAX_RESPONSE_CHARS = 8_000_000
    }
}
