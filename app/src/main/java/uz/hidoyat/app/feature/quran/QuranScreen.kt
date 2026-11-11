package uz.hidoyat.app.feature.quran

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatPrimaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSecondaryButton
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

/**
 * Qur'on bo'limi: ma'lumot holati (yuklanmoqda / yuklash kerak / xato) -> suralar ro'yxati -> o'qish ekrani.
 * @param pendingContinue Bosh sahifadagi "Davom ettirish" bosilgan: ma'lumot tayyor bo'lgach oxirgi joy ochiladi.
 */
@Composable
fun QuranScreen(
    pendingContinue: Boolean = false,
    onContinueHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findHostActivity() }
    val viewModel = remember(activity) { ViewModelProvider(activity)[QuranViewModel::class.java] }
    val state by viewModel.state.collectAsState()
    val audio by viewModel.audio.state.collectAsState()

    // Bo'limdan chiqilganda yoki ilova fonga ketganda audio to'xtatiladi (resurs bo'shatilmaydi, joy saqlanadi)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onLeftScreen()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onLeftScreen()
        }
    }

    LaunchedEffect(pendingContinue, state.load) {
        if (!pendingContinue) return@LaunchedEffect
        when (state.load) {
            QuranLoad.LOADING -> Unit
            QuranLoad.READY -> { viewModel.continueReading(); onContinueHandled() }
            else -> onContinueHandled()
        }
    }

    BackHandler(enabled = state.load == QuranLoad.READY && state.openSurah != null) {
        viewModel.closeReader()
    }

    val data = state.data
    when {
        state.load == QuranLoad.READY && data != null -> {
            val open = state.openSurah
            if (open != null) {
                SurahReaderScreen(
                    surah = open,
                    state = state,
                    data = data,
                    audio = audio,
                    onBack = viewModel::closeReader,
                    onAyahVisible = viewModel::onAyahVisible,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onPlay = viewModel::playAyah,
                    onTextScale = viewModel::changeTextScale,
                    onTogglePlayPause = viewModel.audio::togglePlayPause,
                    onNext = viewModel.audio::next,
                    onPrevious = viewModel.audio::previous,
                    onCycleRepeat = viewModel::cycleRepeat,
                    onStopAudio = viewModel.audio::stop
                )
            } else {
                SurahListScreen(
                    state = state,
                    data = data,
                    onQuery = viewModel::onQueryChange,
                    onFilter = viewModel::onFilterChange,
                    onOpenSurah = { viewModel.openSurah(it) },
                    onOpenAyah = viewModel::openAyah,
                    onContinue = viewModel::continueReading
                )
            }
        }
        state.load == QuranLoad.LOADING -> StatusPane {
            CircularProgressIndicator(color = HidoyatTheme.colors.gold)
            Text(
                text = stringResource(R.string.quran_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = HidoyatTheme.colors.textSecondary
            )
        }
        state.load == QuranLoad.DOWNLOADING -> StatusPane {
            Text(
                text = stringResource(R.string.quran_download_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            LinearProgressIndicator(
                progress = state.downloadedSurahs / QuranCatalog.SURAH_COUNT.toFloat(),
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = HidoyatTheme.colors.gold,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = stringResource(
                    R.string.quran_downloading_format,
                    state.downloadedSurahs,
                    QuranCatalog.SURAH_COUNT
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = HidoyatTheme.colors.textSecondary
            )
            HidoyatSecondaryButton(
                text = stringResource(R.string.quran_download_cancel),
                onClick = viewModel::cancelDownload,
                modifier = Modifier.fillMaxWidth()
            )
        }
        state.load == QuranLoad.ERROR -> StatusPane {
            Text(
                text = stringResource(R.string.quran_download_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = quranErrorText(state.error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            HidoyatPrimaryButton(
                text = stringResource(R.string.quran_retry),
                onClick = viewModel::download,
                modifier = Modifier.fillMaxWidth()
            )
        }
        else -> StatusPane { // NEEDS_DOWNLOAD
            Text(
                text = stringResource(R.string.quran_download_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    if (state.dataWasCorrupt) R.string.quran_download_corrupt else R.string.quran_download_body
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = HidoyatTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            HidoyatPrimaryButton(
                text = stringResource(R.string.quran_download_action),
                onClick = viewModel::download,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.dataWasCorrupt) {
                HidoyatSecondaryButton(
                    text = stringResource(R.string.quran_retry),
                    onClick = viewModel::load,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatusPane(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        HidoyatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                content()
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private tailrec fun Context.findHostActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> error("QuranScreen ComponentActivity ichida bo'lishi kerak")
}
