package uz.hidoyat.app.feature.dua

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatSectionTitle
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

@Composable
fun DuaScreen(
    onBack: () -> Unit,
    duaViewModel: DuaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by duaViewModel.state.collectAsStateWithLifecycle()
    val colors = HidoyatTheme.colors
    val selectedDua = state.selectedDua

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (selectedDua != null) duaViewModel.closeDua()
                        else onBack()
                    }
                ) {
                    Text("‹ Orqaga")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "DUOLAR",
                    color = colors.gold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = duaViewModel::showFavorites) {
                    Text("♥")
                }
            }
        }
    ) { padding ->
        if (selectedDua != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    HidoyatSectionTitle(title = selectedDua.title)
                }
                item {
                    HidoyatCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedDua.arabic,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 28.sp,
                                    lineHeight = 48.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            HorizontalDivider()
                            Text(
                                selectedDua.transliteration,
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.gold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                selectedDua.meaning,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Manba: ${selectedDua.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { duaViewModel.toggleFavorite(selectedDua.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (selectedDua.id in state.favoriteIds)
                                "♥ Sevimlilardan olib tashlash"
                            else "♡ Sevimlilarga qo‘shish"
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HidoyatSectionTitle(
                        title = "Kundalik duolar",
                        subtitle = "Har kuningizni duo bilan bezang"
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = duaViewModel::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Duo qidirish...") }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !state.favoritesOnly && state.category == null,
                            onClick = duaViewModel::showAll,
                            label = { Text("Barchasi") }
                        )
                        FilterChip(
                            selected = state.favoritesOnly,
                            onClick = duaViewModel::showFavorites,
                            label = { Text("♥ Sevimlilar") }
                        )
                    }
                }
                item {
                    HidoyatSectionTitle(title = "Kategoriyalar")
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DuaCategory.values().forEach { category ->
                            FilterChip(
                                selected = state.category == category,
                                onClick = {
                                    duaViewModel.selectCategory(
                                        if (state.category == category) null else category
                                    )
                                },
                                label = { Text(category.title) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    HidoyatSectionTitle(title = "Duolar")
                }
                if (state.visibleDuas.isEmpty()) {
                    item {
                        Text(
                            "Bu bo‘limda hozircha duo topilmadi.",
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            textAlign = TextAlign.Center,
                            color = colors.textSecondary
                        )
                    }
                } else {
                    items(state.visibleDuas, key = { it.id }) { dua ->
                        HidoyatCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { duaViewModel.openDua(dua) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        dua.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        dua.transliteration,
                                        color = colors.gold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        dua.meaning,
                                        color = colors.textSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                }
                                Text(
                                    if (dua.id in state.favoriteIds) "♥" else "♡",
                                    color = colors.gold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.clickable {
                                        duaViewModel.toggleFavorite(dua.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
