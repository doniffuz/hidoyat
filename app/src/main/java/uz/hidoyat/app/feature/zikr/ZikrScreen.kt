package uz.hidoyat.app.feature.zikr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatSectionTitle
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

@Composable
fun ZikrScreen(
    onBack: () -> Unit,
    zikrViewModel: ZikrViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by zikrViewModel.state.collectAsStateWithLifecycle()
    val zikr = ZikrCatalog.find(state.zikrId)
    val colors = HidoyatTheme.colors
    val progress = (state.count.toFloat() / state.goal.coerceAtLeast(1)).coerceIn(0f, 1f)

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
                TextButton(onClick = onBack) {
                    Text("‹  Orqaga")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "TASBEH",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.gold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::reset) {
                    Text("Tozalash")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                HidoyatSectionTitle(
                    title = "Zikr va tasbeh",
                    subtitle = "Qalbingizni zikr bilan xotirjam qiling"
                )
            }

            item {
                HidoyatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = zikr.arabic,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                lineHeight = 48.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = zikr.transliteration,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = colors.gold
                        )
                        Text(
                            text = zikr.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(33, 99, 100).forEach { goal ->
                        val selected = state.goal == goal
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { zikrViewModel.setGoal(goal) },
                            color = if (selected) colors.goldContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) colors.gold else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = "$goal",
                                modifier = Modifier.padding(vertical = 14.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(colors.card),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = colors.gold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.count.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/ ${state.goal}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.count >= state.goal) "Maqsad bajarildi" else "Zikrlar soni",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.gold
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::increment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "ZIKR QO‘SHISH  +",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            item {
                HidoyatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Bugungi jami zikrlar", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.totalToday.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                HidoyatSectionTitle(title = "Zikrni tanlang")
            }

            items(ZikrCatalog.items.size) { index ->
                val item = ZikrCatalog.items[index]
                val selected = item.id == state.zikrId
                HidoyatCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { zikrViewModel.selectZikr(item.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.transliteration, style = MaterialTheme.typography.titleMedium)
                            Text(
                                item.meaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        if (selected) {
                            Text("✓", color = colors.gold, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}
