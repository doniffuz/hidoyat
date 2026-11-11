package uz.hidoyat.app.feature.quran

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatActionIcon
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatIconButton
import uz.hidoyat.app.core.ui.components.HidoyatPrimaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSectionTitle
import uz.hidoyat.app.core.ui.components.HidoyatShapes
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

@Composable
fun SurahListScreen(
    state: QuranUiState,
    data: QuranData,
    onQuery: (String) -> Unit,
    onFilter: (QuranFilter) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenAyah: (AyahKey) -> Unit,
    onContinue: () -> Unit
) {
    val colors = HidoyatTheme.colors
    val searching = state.query.isNotBlank()
    val results = state.results

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            HidoyatSectionTitle(
                title = stringResource(R.string.quran_title),
                subtitle = stringResource(R.string.quran_subtitle)
            )
        }
        item(key = "continue") { ContinueCard(state, onContinue) }
        item(key = "search") { SearchField(state.query, onQuery) }

        if (!searching) {
            item(key = "filters") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onFilter(filter) },
                            label = {
                                Text(
                                    stringResource(
                                        if (filter == QuranFilter.ALL) R.string.quran_filter_all
                                        else R.string.quran_filter_favorites
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.goldContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        when {
            searching -> {
                if (results.jump != null) {
                    item(key = "jump") {
                        val jump = results.jump
                        HidoyatCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenAyah(jump) }) {
                            Text(
                                text = stringResource(
                                    R.string.quran_jump_format, surahDisplayName(jump.surah), jump.ayah
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (results.surahs.isNotEmpty()) {
                    item(key = "h_surahs") { ListHeader(stringResource(R.string.quran_results_surahs)) }
                    items(results.surahs, key = { "s${it.number}" }) { info ->
                        SurahRow(info, state.progress[info.number] ?: 0) { onOpenSurah(info.number) }
                    }
                }
                if (results.ayahs.isNotEmpty()) {
                    item(key = "h_ayahs") { ListHeader(stringResource(R.string.quran_results_ayahs)) }
                    items(results.ayahs, key = { "a${it.key.id}" }) { ayah ->
                        AyahResultRow(ayah) { onOpenAyah(ayah.key) }
                    }
                    if (results.truncated) {
                        item(key = "more") {
                            Text(
                                text = stringResource(R.string.quran_results_more),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                if (results.jump == null && results.surahs.isEmpty() && results.ayahs.isEmpty()) {
                    item(key = "empty") { EmptyText(stringResource(R.string.quran_no_results)) }
                }
            }

            state.filter == QuranFilter.FAVORITES -> {
                val favorites = state.favorites.sortedWith(compareBy({ it.surah }, { it.ayah }))
                if (favorites.isEmpty()) {
                    item(key = "empty_fav") { EmptyText(stringResource(R.string.quran_empty_favorites)) }
                } else {
                    items(favorites, key = { "f${it.id}" }) { key ->
                        val ayah = data.ayah(key)
                        if (ayah != null) AyahResultRow(ayah) { onOpenAyah(key) }
                    }
                }
            }

            else -> items(QuranCatalog.surahs, key = { "s${it.number}" }) { info ->
                SurahRow(info, state.progress[info.number] ?: 0) { onOpenSurah(info.number) }
            }
        }

        item(key = "source") { SourceCard(data.meta) }
    }
}

@Composable
private fun ContinueCard(state: QuranUiState, onContinue: () -> Unit) {
    val last = state.lastRead
    HidoyatCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (last != null) {
                stringResource(R.string.quran_last_read_format, surahDisplayName(last.surah), last.ayah)
            } else {
                stringResource(R.string.quran_no_last_read)
            },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.quran_overall_progress_format, state.overallPercent),
            style = MaterialTheme.typography.bodySmall,
            color = HidoyatTheme.colors.textSecondary
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = state.overallPercent / 100f,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = HidoyatTheme.colors.gold,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(14.dp))
        HidoyatPrimaryButton(
            text = stringResource(if (last != null) R.string.quran_continue else R.string.quran_start),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val colors = HidoyatTheme.colors
    OutlinedTextField(
        value = query,
        onValueChange = { onQuery(it.take(MAX_QUERY_LENGTH)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = HidoyatShapes.Button,
        placeholder = { Text(stringResource(R.string.quran_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            HidoyatActionIcon(HidoyatActionIcon.Search, MaterialTheme.colorScheme.primary, size = 22.dp)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                HidoyatIconButton(
                    onClick = { onQuery("") },
                    contentDescription = stringResource(R.string.quran_cd_clear_search),
                    size = 40.dp
                ) {
                    HidoyatActionIcon(HidoyatActionIcon.Close, MaterialTheme.colorScheme.primary, size = 18.dp)
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.gold,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = colors.card,
            unfocusedContainerColor = colors.card,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun SurahRow(info: SurahInfo, furthest: Int, onClick: () -> Unit) {
    val colors = HidoyatTheme.colors
    val name = surahDisplayName(info)
    val description = stringResource(R.string.quran_cd_surah_format, info.number, name, info.ayahCount)
    HidoyatCard(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NumberBadge(info.number)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        if (info.revelation == Revelation.MECCAN) R.string.quran_meccan else R.string.quran_medinan
                    ) + " · " + stringResource(R.string.quran_ayah_count_format, info.ayahCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        if (furthest > 0) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = (furthest / info.ayahCount.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = colors.gold,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun AyahResultRow(ayah: Ayah, onClick: () -> Unit) {
    HidoyatCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            text = stringResource(R.string.quran_ref_format, surahDisplayName(ayah.key.surah), ayah.key.ayah),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = ayah.translation,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(HidoyatTheme.colors.goldContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ListHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = HidoyatTheme.colors.textSecondary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = HidoyatTheme.colors.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    )
}

@Composable
internal fun SourceCard(meta: QuranMeta) {
    HidoyatCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.quran_source_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.quran_source_text,
                meta.version.ifBlank { QuranRepository.TRANSLATION_VERSION }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = HidoyatTheme.colors.textSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.quran_source_note),
            style = MaterialTheme.typography.bodySmall,
            color = HidoyatTheme.colors.textSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.quran_source_audio),
            style = MaterialTheme.typography.bodySmall,
            color = HidoyatTheme.colors.textSecondary
        )
    }
}

private const val MAX_QUERY_LENGTH = 80
