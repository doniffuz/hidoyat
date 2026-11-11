package uz.hidoyat.app.feature.prayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Zaxira manba: Aladhan hisoblangan vaqtlari (MWL, Hanafiy asr). */
object AladhanPrayerApi {

    private fun today(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

    suspend fun fetchToday(city: String): PrayerSchedule {
        require(city.isNotBlank()) {
            "Shahar nomi kiritilmagan"
        }
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val encodedCountry = URLEncoder.encode("Uzbekistan", "UTF-8")
        return request(
            "https://api.aladhan.com/v1/timingsByCity/${today()}" +
                "?city=$encodedCity&country=$encodedCountry&method=3&school=1",
            city
        )
    }

    suspend fun fetchToday(latitude: Double, longitude: Double, label: String): PrayerSchedule {
        require(label.isNotBlank()) {
            "Hudud nomi kiritilmagan"
        }
        val lat = String.format(Locale.US, "%.4f", latitude)
        val lon = String.format(Locale.US, "%.4f", longitude)
        return request(
            "https://api.aladhan.com/v1/timings/${today()}" +
                "?latitude=$lat&longitude=$lon&method=3&school=1" +
                "&timezonestring=Asia/Tashkent",
            label
        )
    }

    private suspend fun request(address: String, label: String): PrayerSchedule =
        withContext(Dispatchers.IO) {
            val connection = URL(address).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Aladhan server xatosi")
                }

                val response = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val root = JSONObject(response)

                if (root.optInt("code") != 200) {
                    throw IOException("Aladhan ma'lumot qaytarmadi")
                }

                val times = root.getJSONObject("data").getJSONObject("timings")

                fun time(key: String): String {
                    return times.getString(key).substringBefore(" ")
                }

                val schedule = PrayerSchedule(
                    city = label,
                    date = LocalDate.now().toString(),
                    fajr = time("Fajr"),
                    sunrise = time("Sunrise"),
                    dhuhr = time("Dhuhr"),
                    asr = time("Asr"),
                    maghrib = time("Maghrib"),
                    isha = time("Isha"),
                    source = "Aladhan (MWL, Hanafi)",
                    updatedAt = System.currentTimeMillis()
                )

                if (!PrayerValidator.isValidSchedule(schedule)) {
                    throw IOException("Aladhan vaqtlari noto'g'ri")
                }

                schedule
            } finally {
                connection.disconnect()
            }
        }
}
