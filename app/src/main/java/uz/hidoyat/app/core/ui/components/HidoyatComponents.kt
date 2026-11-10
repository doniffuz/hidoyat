package uz.hidoyat.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

object HidoyatShapes {
    val Card = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(16.dp)
}

private val ButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)

/** Bosilganda yengil kichrayish effekti. */
@Composable
private fun pressScale(source: MutableInteractionSource, pressed: Float = 0.97f): Modifier {
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "pressScale"
    )
    return Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun HidoyatCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = HidoyatTheme.colors
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        val source = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.then(pressScale(source, 0.98f)),
            shape = HidoyatShapes.Card,
            color = colors.card,
            contentColor = colors.onCard,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, colors.cardBorder),
            interactionSource = source,
            content = body
        )
    } else {
        Surface(
            modifier = modifier,
            shape = HidoyatShapes.Card,
            color = colors.card,
            contentColor = colors.onCard,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, colors.cardBorder),
            content = body
        )
    }
}

@Composable
fun HidoyatPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).then(pressScale(source)),
        enabled = enabled,
        shape = HidoyatShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = ButtonPadding,
        interactionSource = source
    ) {
        ButtonContent(text, leadingContent)
    }
}

@Composable
fun HidoyatSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    val borderColor =
        if (enabled) MaterialTheme.colorScheme.outline
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).then(pressScale(source)),
        enabled = enabled,
        shape = HidoyatShapes.Button,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        border = BorderStroke(1.5.dp, borderColor),
        contentPadding = ButtonPadding,
        interactionSource = source
    ) {
        ButtonContent(text, leadingContent)
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, leading: (@Composable () -> Unit)?) {
    if (leading != null) {
        leading()
        Box(Modifier.width(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun HidoyatSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(if (subtitle != null) 40.dp else 26.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HidoyatTheme.colors.gold)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = HidoyatTheme.colors.textSecondary
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun HidoyatIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    val colors = HidoyatTheme.colors
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .then(pressScale(source, 0.92f))
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = colors.card,
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, colors.cardBorder),
        interactionSource = source
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun HidoyatDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

/** Qorong'i/yashil fon ustida ishlatiladigan oltin tugma. */
@Composable
fun HidoyatGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = HidoyatTheme.colors
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).then(pressScale(source)),
        enabled = enabled,
        shape = HidoyatShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.gold,
            contentColor = colors.onGold
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        ),
        contentPadding = ButtonPadding,
        interactionSource = source
    ) {
        ButtonContent(text, null)
    }
}
