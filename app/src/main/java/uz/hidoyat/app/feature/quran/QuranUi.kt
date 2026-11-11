package uz.hidoyat.app.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uz.hidoyat.app.R

/** Sura nomini joriy interfeys tiliga qarab tanlaydi: o'zbek lotin / o'zbek kirill / rus. */
@Composable
fun surahDisplayName(info: SurahInfo): String =
    when (stringResource(R.string.quran_name_lang)) {
        "cyr" -> info.nameCyrillic
        "ru" -> info.nameRu
        else -> info.nameUz
    }

@Composable
fun surahDisplayName(number: Int): String =
    QuranCatalog.surah(number)?.let { surahDisplayName(it) } ?: number.toString()

@Composable
fun quranErrorText(kind: QuranErrorKind?): String = stringResource(
    when (kind) {
        QuranErrorKind.NO_INTERNET -> R.string.quran_error_no_internet
        QuranErrorKind.TIMEOUT -> R.string.quran_error_timeout
        QuranErrorKind.SERVER -> R.string.quran_error_server
        QuranErrorKind.BAD_DATA -> R.string.quran_error_bad_data
        QuranErrorKind.STORAGE -> R.string.quran_error_storage
        QuranErrorKind.UNKNOWN, null -> R.string.quran_error_unknown
    }
)
