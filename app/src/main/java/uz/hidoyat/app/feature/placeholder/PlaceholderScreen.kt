package uz.hidoyat.app.feature.placeholder

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatIcon
import uz.hidoyat.app.core.ui.components.HidoyatIconType
import uz.hidoyat.app.core.ui.components.HidoyatSectionTitle
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

/** Hali yaratilmagan bo'limlar uchun vaqtinchalik sahifa. */
@Composable
fun PlaceholderScreen(
    @StringRes title: Int,
    icon: HidoyatIconType,
    modifier: Modifier = Modifier
) {
    val colors = HidoyatTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HidoyatSectionTitle(title = stringResource(title))
            HidoyatCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp, vertical = 36.dp
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(colors.goldContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        HidoyatIcon(icon, MaterialTheme.colorScheme.primary, size = 44.dp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onCard,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = CircleShape,
                        color = colors.gold,
                        contentColor = colors.onGold
                    ) {
                        Text(
                            text = stringResource(R.string.placeholder_soon),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.placeholder_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
