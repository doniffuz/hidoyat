package uz.hidoyat.app.feature.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PrayerTime(
    val name: String,
    val time: String
)

data class PrayerSchedule(
    val city: String,
    val date: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val source: String,
    val updatedAt: Long
) {
    fun asList(): List<PrayerTime> = listOf(
        PrayerTime("Bomdod", fajr),
        PrayerTime("Quyosh", sunrise),
        PrayerTime("Peshin", dhuhr),
        PrayerTime("Asr", asr),
        PrayerTime("Shom", maghrib),
        PrayerTime("Xufton", isha)
    )
}

enum class PrayerDataSource {
    LOCAL_PRIMARY,
    ALADHAN_BACKUP
}

data class PrayerResult(
    val schedule: PrayerSchedule,
    val source: PrayerDataSource
)

enum class PrayerError {
    LOAD_FAILED
}

enum class LocationHint {
    APPROXIMATE,
    PERMISSION_DENIED,
    UNAVAILABLE,
    OUTSIDE
}

data class PrayerUiState(
    val provinces: List<UzbekRegion> = emptyList(),
    val districts: List<UzbekDistrict> = emptyList(),
    val namozRegions: List<NamozRegion> = emptyList(),
    val selectedProvince: UzbekRegion? = null,
    val selectedDistrict: UzbekDistrict? = null,
    val match: NamozMatch? = null,
    val manualRegion: NamozRegion? = null,
    val schedule: PrayerSchedule? = null,
    val source: PrayerDataSource? = null,
    /** true: ko'rsatilayotgan vaqtlar yangi so'rovdan emas, saqlangan nusxadan. */
    val fromCache: Boolean = false,
    val isLoading: Boolean = false,
    val error: PrayerError? = null,
    val locationHint: LocationHint? = null
)

/** Ma'lumot bugun (qurilma vaqti bilan) saqlanganmi? */
fun isSavedToday(updatedAt: Long): Boolean =
    Instant.ofEpochMilli(updatedAt).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
