package uz.hidoyat.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class HidoyatIconType { Crescent, Quran, Qibla, Zikr, Home, Prayer, Profile }

/** Kutubxonasiz, Canvas'da chizilgan ikonalar (24x24 koordinata tizimida). */
@Composable
fun HidoyatIcon(
    type: HidoyatIconType,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            when (type) {
                HidoyatIconType.Crescent -> crescent(tint)
                HidoyatIconType.Quran -> book(tint)
                HidoyatIconType.Qibla -> compass(tint)
                HidoyatIconType.Zikr -> beads(tint)
                HidoyatIconType.Home -> house(tint)
                HidoyatIconType.Prayer -> mosque(tint)
                HidoyatIconType.Profile -> person(tint)
            }
        }
    }
}

private fun circlePath(cx: Float, cy: Float, r: Float) =
    Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

private fun DrawScope.crescent(tint: Color) {
    val moon = Path.combine(
        PathOperation.Difference,
        circlePath(11f, 12.5f, 8.5f),
        circlePath(15.2f, 10.2f, 7f)
    )
    drawPath(moon, tint)
    drawPath(circlePath(18.5f, 6f, 1.6f), tint)
}

private fun DrawScope.book(tint: Color) {
    val left = Path().apply {
        moveTo(3f, 5.5f); lineTo(11f, 7f); lineTo(11f, 19.5f); lineTo(3f, 18f); close()
    }
    val right = Path().apply {
        moveTo(21f, 5.5f); lineTo(13f, 7f); lineTo(13f, 19.5f); lineTo(21f, 18f); close()
    }
    drawPath(left, tint)
    drawPath(right, tint)
}

private fun DrawScope.compass(tint: Color) {
    drawCircle(tint, radius = 9f, center = Offset(12f, 12f), style = Stroke(width = 1.8f))
    val north = Path().apply {
        moveTo(12f, 4.8f); lineTo(14.4f, 12f); lineTo(9.6f, 12f); close()
    }
    val south = Path().apply {
        moveTo(12f, 19.2f); lineTo(14.4f, 12f); lineTo(9.6f, 12f); close()
    }
    drawPath(north, tint)
    drawPath(south, tint.copy(alpha = 0.4f))
    drawCircle(tint, radius = 1.1f, center = Offset(12f, 12f))
}

private fun DrawScope.beads(tint: Color) {
    val count = 10
    for (i in 0 until count) {
        val a = 2.0 * PI * i / count - PI / 2
        drawCircle(
            tint,
            radius = 1.8f,
            center = Offset(12f + 7.5f * cos(a).toFloat(), 11f + 7.5f * sin(a).toFloat())
        )
    }
    drawCircle(tint.copy(alpha = 0.5f), radius = 1.5f, center = Offset(12f, 21.6f))
}

private fun DrawScope.house(tint: Color) {
    val path = Path().apply {
        moveTo(3f, 11f); lineTo(12f, 3.5f); lineTo(21f, 11f); lineTo(19f, 11f)
        lineTo(19f, 20f); lineTo(14f, 20f); lineTo(14f, 14f); lineTo(10f, 14f)
        lineTo(10f, 20f); lineTo(5f, 20f); lineTo(5f, 11f); close()
    }
    drawPath(path, tint)
}

private fun DrawScope.mosque(tint: Color) {
    val dome = Path().apply {
        moveTo(5f, 16f)
        cubicTo(5f, 10f, 8.5f, 6.5f, 12f, 6f)
        cubicTo(15.5f, 6.5f, 19f, 10f, 19f, 16f)
        close()
    }
    drawPath(dome, tint)
    drawLine(tint, Offset(12f, 6f), Offset(12f, 3f), strokeWidth = 1.6f)
    drawRect(tint, topLeft = Offset(4f, 17.2f), size = androidx.compose.ui.geometry.Size(16f, 3f))
}

private fun DrawScope.person(tint: Color) {
    drawPath(circlePath(12f, 8f, 4f), tint)
    val body = Path().apply {
        moveTo(4f, 20f)
        cubicTo(4f, 15f, 8f, 13.5f, 12f, 13.5f)
        cubicTo(16f, 13.5f, 20f, 15f, 20f, 20f)
        close()
    }
    drawPath(body, tint)
}

/** Qur'on va audio boshqaruvi uchun Canvas asosidagi ikonalar. */
enum class HidoyatActionIcon {
    Search, Close, Back, Pause, Play, Heart, HeartOutline,
    Copy, Repeat, RepeatOne, Previous, Next
}

@Composable
fun HidoyatActionIcon(
    type: HidoyatActionIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            val stroke = Stroke(width = 2f)
            when (type) {
                HidoyatActionIcon.Search -> {
                    drawCircle(tint, 7f, Offset(10f, 10f), style = stroke)
                    drawLine(tint, Offset(15f, 15f), Offset(21f, 21f), 2f)
                }
                HidoyatActionIcon.Close -> {
                    drawLine(tint, Offset(5f, 5f), Offset(19f, 19f), 2f)
                    drawLine(tint, Offset(19f, 5f), Offset(5f, 19f), 2f)
                }
                HidoyatActionIcon.Back -> {
                    drawLine(tint, Offset(19f, 12f), Offset(5f, 12f), 2f)
                    drawLine(tint, Offset(5f, 12f), Offset(12f, 5f), 2f)
                    drawLine(tint, Offset(5f, 12f), Offset(12f, 19f), 2f)
                }
                HidoyatActionIcon.Pause -> {
                    drawLine(tint, Offset(8f, 5f), Offset(8f, 19f), 3f)
                    drawLine(tint, Offset(16f, 5f), Offset(16f, 19f), 3f)
                }
                HidoyatActionIcon.Play -> {
                    val p = Path().apply {
                        moveTo(7f, 4f)
                        lineTo(20f, 12f)
                        lineTo(7f, 20f)
                        close()
                    }
                    drawPath(p, tint)
                }
                HidoyatActionIcon.Heart,
                HidoyatActionIcon.HeartOutline -> {
                    val p = Path().apply {
                        moveTo(12f, 21f)
                        cubicTo(10f, 19f, 3f, 13f, 3f, 8.5f)
                        cubicTo(3f, 3f, 10f, 2f, 12f, 8f)
                        cubicTo(14f, 2f, 21f, 3f, 21f, 8.5f)
                        cubicTo(21f, 13f, 14f, 19f, 12f, 21f)
                        close()
                    }
                    if (type == HidoyatActionIcon.Heart) {
                        drawPath(p, tint)
                    } else {
                        drawPath(p, tint, style = stroke)
                    }
                }
                HidoyatActionIcon.Copy -> {
                    drawRect(
                        tint, Offset(8f, 4f),
                        androidx.compose.ui.geometry.Size(12f, 14f),
                        style = stroke
                    )
                    drawRect(
                        tint, Offset(4f, 8f),
                        androidx.compose.ui.geometry.Size(12f, 14f),
                        style = stroke
                    )
                }
                HidoyatActionIcon.Repeat,
                HidoyatActionIcon.RepeatOne -> {
                    drawLine(tint, Offset(4f, 8f), Offset(19f, 8f), 2f)
                    drawLine(tint, Offset(19f, 8f), Offset(16f, 5f), 2f)
                    drawLine(tint, Offset(19f, 8f), Offset(16f, 11f), 2f)
                    drawLine(tint, Offset(20f, 16f), Offset(5f, 16f), 2f)
                    drawLine(tint, Offset(5f, 16f), Offset(8f, 13f), 2f)
                    drawLine(tint, Offset(5f, 16f), Offset(8f, 19f), 2f)
                }
                HidoyatActionIcon.Previous -> {
                    drawLine(tint, Offset(5f, 5f), Offset(5f, 19f), 2f)
                    val p = Path().apply {
                        moveTo(19f, 5f)
                        lineTo(8f, 12f)
                        lineTo(19f, 19f)
                        close()
                    }
                    drawPath(p, tint)
                }
                HidoyatActionIcon.Next -> {
                    drawLine(tint, Offset(19f, 5f), Offset(19f, 19f), 2f)
                    val p = Path().apply {
                        moveTo(5f, 5f)
                        lineTo(16f, 12f)
                        lineTo(5f, 19f)
                        close()
                    }
                    drawPath(p, tint)
                }
            }
        }
    }
}
