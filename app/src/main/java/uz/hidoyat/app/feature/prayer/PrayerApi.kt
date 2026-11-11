package uz.hidoyat.app.feature.prayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

object PrayerApi {

    suspend fun fetchToday(
        regionSlug: String,
        cityName: String
    ): PrayerSchedule = withContext(Dispatchers.IO) {

        require(regionSlug.isNotBlank()) {
            "Hudud tanlanmagan"
        }

        val region = URLEncoder.encode(regionSlug, "UTF-8")
        val url = URL(
            "https://www.namoz-vaqti.uz/index.php" +
                "?region=$region&lang=lotin&period=today&format=json"
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")

            val status = connection.responseCode

            if (status != HttpURLConnection.HTTP_OK) {
                throw IOException("Server xatosi: HTTP $status")
            }

            val response = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val root = JSONObject(response)
            val times = root.getJSONObject("today")
                .getJSONObject("times")

            val schedule = PrayerSchedule(
                city = cityName,
                date = root.optJSONObject("meta")?.optString("date").orEmpty()
                    .ifBlank { LocalDate.now().toString() },
                fajr = times.getString("bomdod"),
                sunrise = times.getString("quyosh"),
                dhuhr = times.getString("peshin"),
                asr = times.getString("asr"),
                maghrib = times.getString("shom"),
                isha = times.getString("xufton"),
                source = "Namoz-vaqti.uz",
                updatedAt = System.currentTimeMillis()
            )

            if (!PrayerValidator.isValidSchedule(schedule)) {
                throw IOException("Namoz vaqtlari ma'lumotlari noto'g'ri")
            }

            schedule
        } finally {
            connection.disconnect()
        }
    }
}
