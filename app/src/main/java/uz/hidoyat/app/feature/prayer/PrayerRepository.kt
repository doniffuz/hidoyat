package uz.hidoyat.app.feature.prayer

import kotlinx.coroutines.CancellationException

class PrayerRepository {

    /**
     * Avval namoz-vaqti.uz (aniq hudud sluggi bilan), u ishlamasa Aladhan zaxirasi
     * (tanlangan hudud markazi koordinatasi bilan, hisoblangan vaqt).
     */
    suspend fun getToday(
        regionSlug: String,
        city: String,
        latitude: Double,
        longitude: Double
    ): PrayerResult {
        try {
            return PrayerResult(
                PrayerApi.fetchToday(regionSlug, city),
                PrayerDataSource.LOCAL_PRIMARY
            )
        } catch (error: CancellationException) {
            throw error
        } catch (primaryError: Exception) {
            try {
                return PrayerResult(
                    AladhanPrayerApi.fetchToday(latitude, longitude, city),
                    PrayerDataSource.ALADHAN_BACKUP
                )
            } catch (error: CancellationException) {
                throw error
            } catch (backupError: Exception) {
                throw Exception(
                    "Namoz vaqtlarini yuklab bo'lmadi. " +
                        "Internetni tekshiring va qayta urinib ko'ring.",
                    backupError
                )
            }
        }
    }
}
