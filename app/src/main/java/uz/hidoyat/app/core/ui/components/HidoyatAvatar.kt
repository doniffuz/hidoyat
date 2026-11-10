package uz.hidoyat.app.core.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

/** Dumaloq avatar: rasm bo'lsa rasm, yo'q bo'lsa ismning bosh harfi yoki standart ikona. */
@Composable
fun HidoyatAvatar(
    bitmap: Bitmap?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp
) {
    val colors = HidoyatTheme.colors
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.goldContainer)
            .border(2.dp, colors.gold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = name.trim().take(1).uppercase()
            if (initial.isNotEmpty()) {
                Text(
                    text = initial,
                    style = if (size >= 96.dp) MaterialTheme.typography.displaySmall
                    else MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                HidoyatIcon(HidoyatIconType.Profile, MaterialTheme.colorScheme.primary, size = size * 0.42f)
            }
        }
    }
}
