package uz.hidoyat.app.feature.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatActionIcon
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatIconButton
import uz.hidoyat.app.core.ui.components.HidoyatDivider
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

private const val HEADER_ITEMS = 1 // 0-element: sura sarlavhasi; n-oyat indeksi = n

@Composable
fun SurahReaderScreen(
    surah: Int,
    state: QuranUiState,
    data: QuranData,
    audio: AudioState,
    onBack: () -> Unit,
    onAyahVisible: (AyahKey) -> Unit,
    onToggleFavorite: (AyahKey) -> Unit,
    onPlay: (AyahKey) -> Unit,
    onTextScale: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onStopAudio: () -> Unit
) {
    val info = QuranCatalog.surah(surah)
    val ayahs = remember(data, surah) { data.ayahs(surah) }
    if (info == null || ayahs.isEmpty()) {
        // Hech qachon bo'lmasligi kerak (ViewModel raqamni tekshiradi), lekin crash o'rniga ro'yxatga qaytamiz
        SideEffect { onBack() }
        return
    }
    val initial = state.initialAyah.coerceIn(1, ayahs.size)

    // Har safar ochilganda scroll holati qayta quriladi
    key(state.openToken) {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = if (initial <= 1) 0 else initial
        )

        // Ekranda eng yuqorida turgan oyat. Layout hali tayyor bo'lmasa — boshlang'ich oyat qaytariladi.
        val visibleAyah by remember(listState, ayahs.size, initial) {
            derivedStateOf {
                val layout = listState.layoutInfo
                val items = layout.visibleItemsInfo
                when {
                    items.isEmpty() -> initial
                    !listState.canScrollForward && items.any { it.index == ayahs.size } -> ayahs.size
                    else -> {
                        val top = items.firstOrNull {
                            it.index >= HEADER_ITEMS &&
                                it.offset + it.size * 0.6f > layout.viewportStartOffset
                        }
                        (top?.index ?: initial).coerceIn(1, ayahs.size)
                    }
                }
            }
        }
        val latestVisible by rememberUpdatedState(visibleAyah)

        // Barqaror bo'lgach saqlaymiz (tez scrollda har oyat yozilmaydi)
        LaunchedEffect(visibleAyah) {
            delay(600)
            onAyahVisible(AyahKey(surah, visibleAyah))
        }
        // Ekrandan chiqishda oxirgi ko'rilgan oyat darhol saqlanadi
        DisposableEffect(surah) {
            onDispose { onAyahVisible(AyahKey(surah, latestVisible)) }
        }

        // Audio keyingi oyatga o'tganda ko'rinmasa — o'sha oyatga scroll
        val playing = audio.current
        LaunchedEffect(playing, audio.status) {
            if (playing != null && playing.surah == surah &&
                (audio.status == AudioStatus.PLAYING || audio.status == AudioStatus.LOADING)
            ) {
                val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == playing.ayah }
                if (!visible && playing.ayah in 1..ayahs.size) listState.animateScrollToItem(playing.ayah)
            }
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ReaderTopBar(
                title = surahDisplayName(info),
                textScale = state.textScale,
                onBack = onBack,
                onTextScale = onTextScale
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "header") { SurahHeader(info, if (data.showsBasmalaHeader(surah)) data.basmala else null) }
                itemsIndexed(ayahs, key = { _, a -> a.key.id }) { _, ayah ->
                    AyahCard(
                        ayah = ayah,
                        surahName = surahDisplayName(info),
                        scale = state.textScale,
                        favorite = ayah.key in state.favorites,
                        audio = audio,
                        onToggleFavorite = { onToggleFavorite(ayah.key) },
                        onPlay = { onPlay(ayah.key) }
                    )
                }
                item(key = "source") { SourceCard(data.meta) }
            }
            if (audio.current != null && audio.status != AudioStatus.IDLE) {
                PlayerBar(
                    audio = audio,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onCycleRepeat = onCycleRepeat,
                    onClose = onStopAudio
                )
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    textScale: Float,
    onBack: () -> Unit,
    onTextScale: (Float) -> Unit
) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HidoyatIconButton(onClick = onBack, contentDescription = stringResource(R.string.quran_cd_back)) {
            HidoyatActionIcon(HidoyatActionIcon.Back, tint)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.quran_text_size_format, Math.round(textScale * 100f)),
                style = MaterialTheme.typography.labelSmall,
                color = HidoyatTheme.colors.textSecondary
            )
        }
        HidoyatIconButton(
            onClick = { onTextScale(-QuranStore.SCALE_STEP) },
            contentDescription = stringResource(R.string.quran_cd_text_smaller),
            size = 44.dp
        ) { Text("A-", style = MaterialTheme.typography.labelLarge) }
        HidoyatIconButton(
            onClick = { onTextScale(QuranStore.SCALE_STEP) },
            contentDescription = stringResource(R.string.quran_cd_text_larger),
            size = 44.dp
        ) { Text("A+", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun SurahHeader(info: SurahInfo, basmala: String?) {
    val quranFont = rememberQuranFontFamily()
    HidoyatCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = surahDisplayName(info),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (info.revelation == Revelation.MECCAN) R.string.quran_meccan else R.string.quran_medinan
                ) + " · " + stringResource(R.string.quran_ayah_count_format, info.ayahCount),
                style = MaterialTheme.typography.bodySmall,
                color = HidoyatTheme.colors.textSecondary
            )
            // Sarlavha bitta element ichida: ro'yxat indekslari (n-oyat = n) o'zgarmaydi
            if (basmala != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = basmala,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        fontFamily = quranFont,
                        fontSize = 24.sp,
                        lineHeight = 44.sp,
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AyahCard(
    ayah: Ayah,
    surahName: String,
    scale: Float,
    favorite: Boolean,
    audio: AudioState,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val colors = HidoyatTheme.colors
    val n = ayah.key.ayah
    val isCurrent = audio.current == ayah.key && audio.status != AudioStatus.IDLE
    val isPlaying = isCurrent && audio.status == AudioStatus.PLAYING
    val credit = stringResource(R.string.quran_copy_credit, stringResource(R.string.quran_ref_format, surahName, n))
    val copiedMessage = stringResource(R.string.quran_copied)
    val quranFont = rememberQuranFontFamily()

    HidoyatCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberBadge(n)
            Spacer(Modifier.weight(1f))
            HidoyatIconButton(
                onClick = onPlay,
                contentDescription = if (isPlaying) stringResource(R.string.quran_cd_pause)
                else stringResource(R.string.quran_cd_play_ayah_format, n),
                size = 44.dp
            ) {
                HidoyatActionIcon(
                    if (isPlaying) HidoyatActionIcon.Pause else HidoyatActionIcon.Play,
                    if (isCurrent) colors.gold else MaterialTheme.colorScheme.primary,
                    size = 22.dp
                )
            }
            HidoyatIconButton(
                onClick = onToggleFavorite,
                contentDescription = stringResource(
                    if (favorite) R.string.quran_cd_favorite_remove_format else R.string.quran_cd_favorite_add_format, n
                ),
                size = 44.dp
            ) {
                HidoyatActionIcon(
                    if (favorite) HidoyatActionIcon.Heart else HidoyatActionIcon.HeartOutline,
                    if (favorite) colors.gold else MaterialTheme.colorScheme.primary,
                    size = 22.dp
                )
            }
            HidoyatIconButton(
                onClick = {
                    copyAyah(context, ayah, credit, copiedMessage)
                },
                contentDescription = stringResource(R.string.quran_cd_copy_format, n),
                size = 44.dp
            ) {
                HidoyatActionIcon(HidoyatActionIcon.Copy, MaterialTheme.colorScheme.primary, size = 22.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = ayah.arabic,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontFamily = quranFont,
                fontSize = (26f * scale).sp,
                lineHeight = (50f * scale).sp,
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        HidoyatDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            text = ayah.translation,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = (16f * scale).sp,
                lineHeight = (25f * scale).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (ayah.footnote != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.quran_footnote_label) + ": " + ayah.footnote,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontSize = (13f * scale).sp,
                    lineHeight = (19f * scale).sp,
                    fontStyle = FontStyle.Italic
                ),
                color = colors.textSecondary
            )
        }
    }
}

/** Arabcha matn + tarjima + (bo'lsa) izoh + manba. Matn o'zgartirilmaydi. */
private fun copyAyah(context: Context, ayah: Ayah, credit: String, copiedMessage: String) {
    val text = buildString {
        append(ayah.arabic).append("\n\n").append(ayah.translation)
        if (ayah.footnote != null) append("\n\n").append(ayah.footnote)
        append("\n\n").append(credit)
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Qur'on ${ayah.key.id}", text))
    // Android 13+ tizim o'zi tasdiq ko'rsatadi
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun PlayerBar(
    audio: AudioState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClose: () -> Unit
) {
    val colors = HidoyatTheme.colors
    val current = audio.current ?: return
    val tint = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        HidoyatCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.quran_audio_now_format, surahDisplayName(current.surah), current.ayah
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    val statusText = when {
                        audio.status == AudioStatus.LOADING -> stringResource(R.string.quran_audio_loading)
                        audio.status == AudioStatus.ERROR && audio.error == AudioError.NO_INTERNET ->
                            stringResource(R.string.quran_audio_error_no_internet)
                        audio.status == AudioStatus.ERROR -> stringResource(R.string.quran_audio_error_failed)
                        else -> null
                    }
                    if (statusText != null) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (audio.status == AudioStatus.ERROR) MaterialTheme.colorScheme.error
                            else colors.textSecondary
                        )
                    }
                }
                HidoyatIconButton(
                    onClick = onClose,
                    contentDescription = stringResource(R.string.quran_cd_close_player),
                    size = 40.dp
                ) { HidoyatActionIcon(HidoyatActionIcon.Close, tint, size = 18.dp) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HidoyatIconButton(
                    onClick = onCycleRepeat,
                    contentDescription = stringResource(
                        when (audio.repeat) {
                            RepeatMode.OFF -> R.string.quran_cd_repeat_off
                            RepeatMode.AYAH -> R.string.quran_cd_repeat_ayah
                            RepeatMode.SURAH -> R.string.quran_cd_repeat_surah
                        }
                    ),
                    size = 48.dp
                ) {
                    HidoyatActionIcon(
                        if (audio.repeat == RepeatMode.AYAH) HidoyatActionIcon.RepeatOne else HidoyatActionIcon.Repeat,
                        if (audio.repeat == RepeatMode.OFF) colors.textSecondary else colors.gold
                    )
                }
                HidoyatIconButton(
                    onClick = onPrevious,
                    contentDescription = stringResource(R.string.quran_cd_previous),
                    size = 48.dp
                ) { HidoyatActionIcon(HidoyatActionIcon.Previous, tint) }
                val playing = audio.status == AudioStatus.PLAYING || audio.status == AudioStatus.LOADING
                HidoyatIconButton(
                    onClick = onTogglePlayPause,
                    contentDescription = stringResource(
                        if (playing) R.string.quran_cd_pause else R.string.quran_cd_play
                    ),
                    size = 56.dp
                ) {
                    HidoyatActionIcon(
                        if (playing) HidoyatActionIcon.Pause else HidoyatActionIcon.Play,
                        colors.gold,
                        size = 28.dp
                    )
                }
                HidoyatIconButton(
                    onClick = onNext,
                    contentDescription = stringResource(R.string.quran_cd_next),
                    size = 48.dp
                ) { HidoyatActionIcon(HidoyatActionIcon.Next, tint) }
            }
        }
    }
}
