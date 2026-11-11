package uz.hidoyat.app.feature.prayer

object PrayerValidator {

    private val timePattern = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun isValidTime(value: String): Boolean {
        return timePattern.matches(value)
    }

    fun isValidSchedule(schedule: PrayerSchedule): Boolean {
        val times = listOf(
            schedule.fajr,
            schedule.sunrise,
            schedule.dhuhr,
            schedule.asr,
            schedule.maghrib,
            schedule.isha
        )

        return schedule.city.isNotBlank() &&
            schedule.date.isNotBlank() &&
            schedule.source.isNotBlank() &&
            times.all(::isValidTime)
    }
}
