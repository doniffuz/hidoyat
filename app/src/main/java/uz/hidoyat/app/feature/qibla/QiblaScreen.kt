package uz.hidoyat.app.feature.qibla

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import kotlin.math.roundToInt
import uz.hidoyat.app.R
import uz.hidoyat.app.core.ui.components.HidoyatCard
import uz.hidoyat.app.core.ui.components.HidoyatIcon
import uz.hidoyat.app.core.ui.components.HidoyatIconType
import uz.hidoyat.app.core.ui.components.HidoyatPrimaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSecondaryButton
import uz.hidoyat.app.core.ui.components.HidoyatSectionTitle
import uz.hidoyat.app.core.ui.components.HidoyatShapes
import uz.hidoyat.app.core.ui.theme.HidoyatTheme

private val FramePadding = 14.dp
private val CompassMaxSize = 340.dp
private const val TICK_STEP_DEGREES = 5
private const val TICK_COUNT = 360 / TICK_STEP_DEGREES
private const val MAJOR_TICK_EVERY = 6
private const val MEDIUM_TICK_EVERY = 2
private const val LABEL_STEP_DEGREES = 30
private const val QUARTER_TURN = 90
private const val FULL_TURN = 360

@Composable
fun QiblaScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findHostActivity() }
    val viewModel = remember(activity) { ViewModelProvider(activity)[QiblaViewModel::class.java] }
    val state by viewModel.state.collectAsState()
    val heading = viewModel.heading.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onScreenStarted()
                Lifecycle.Event.ON_STOP -> viewModel.onScreenStopped()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenStopped()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> viewModel.onPermissionResult(result.values.any { it }) }

    QiblaContent(
        state = state,
        heading = heading,
        modifier = modifier,
        onRequestPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onOpenAppSettings = {
            context.openSettings(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        },
        onOpenLocationSettings = {
            context.openSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        onRetry = viewModel::retry
    )
}

private tailrec fun Context.findHostActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> error("QiblaScreen ComponentActivity ichida bo'lishi kerak")
}

private fun Context.openSettings(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // Sozlamalar ekrani mavjud emas: foydalanuvchi qo'lda ochadi.
    }
}

@Composable
private fun QiblaContent(
    state: QiblaUiState,
    heading: State<Float>,
    modifier: Modifier,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HidoyatSectionTitle(
                title = stringResource(R.string.qibla_title),
                subtitle = stringResource(R.string.qibla_subtitle)
            )
            when (state.status) {
                QiblaStatus.LOADING -> StatusCard(
                    title = stringResource(R.string.qibla_loading),
                    message = null,
                    icon = {
                        CircularProgressIndicator(color = HidoyatTheme.colors.gold)
                    }
                )
                QiblaStatus.LOCATION_REQUIRED -> StatusCard(
                    title = stringResource(R.string.qibla_location_required_title),
                    message = stringResource(
                        if (state.permissionDenied) R.string.qibla_permission_denied_message
                        else R.string.qibla_location_required_message
                    ),
                    icon = { QiblaBadgeIcon() }
                ) {
                    HidoyatPrimaryButton(
                        text = stringResource(R.string.qibla_grant_permission),
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.permissionDenied) {
                        HidoyatSecondaryButton(
                            text = stringResource(R.string.qibla_open_settings),
                            onClick = onOpenAppSettings,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                QiblaStatus.SENSOR_UNAVAILABLE -> StatusCard(
                    title = stringResource(R.string.qibla_sensor_unavailable_title),
                    message = stringResource(R.string.qibla_sensor_unavailable_message),
                    icon = { QiblaBadgeIcon() }
                )
                QiblaStatus.ERROR -> StatusCard(
                    title = stringResource(R.string.qibla_error_title),
                    message = stringResource(R.string.qibla_error_message),
                    icon = { QiblaBadgeIcon() }
                ) {
                    HidoyatPrimaryButton(
                        text = stringResource(R.string.qibla_retry),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HidoyatSecondaryButton(
                        text = stringResource(R.string.qibla_open_location_settings),
                        onClick = onOpenLocationSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                QiblaStatus.READY, QiblaStatus.CALIBRATION -> CompassSection(state, heading)
            }
        }
    }
}

@Composable
private fun QiblaBadgeIcon() {
    HidoyatIcon(HidoyatIconType.Qibla, MaterialTheme.colorScheme.primary, size = 44.dp)
}

@Composable
private fun StatusCard(
    title: String,
    message: String?,
    icon: @Composable () -> Unit,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    HidoyatCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(HidoyatTheme.colors.goldContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = HidoyatTheme.colors.onCard,
                textAlign = TextAlign.Center
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HidoyatTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            actions()
        }
    }
}

@Composable
private fun CompassSection(state: QiblaUiState, heading: State<Float>) {
    val directions = stringArrayResource(R.array.qibla_directions)
    val degrees = state.qiblaBearing.roundToInt() % FULL_TURN
    val directionName = directions[state.directionIndex.coerceIn(0, directions.lastIndex)]

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.isAligned) {
        if (state.isAligned) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (state.status == QiblaStatus.CALIBRATION) CalibrationBanner()

        QiblaCompass(
            bearing = state.qiblaBearing,
            heading = heading,
            aligned = state.isAligned,
            description = stringResource(R.string.qibla_compass_description, degrees, directionName),
            modifier = Modifier
                .widthIn(max = CompassMaxSize)
                .fillMaxWidth()
        )

        QiblaInfoCard(state, degrees, directionName)
    }
}

@Composable
private fun CalibrationBanner() {
    val colors = HidoyatTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = HidoyatShapes.Card,
        color = colors.goldContainer,
        contentColor = colors.onCard,
        border = BorderStroke(1.dp, colors.gold)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.qibla_calibration_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.qibla_calibration_message),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QiblaInfoCard(state: QiblaUiState, degrees: Int, directionName: String) {
    val colors = HidoyatTheme.colors
    HidoyatCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.qibla_degree_format, degrees),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.qibla_direction_format, directionName),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onCard
            )
            if (state.isAligned) {
                Text(
                    text = stringResource(R.string.qibla_aligned),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.success,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Text(
                text = stringResource(R.string.qibla_hold_flat),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (state.isLocationAccuracyLow) {
                Text(
                    text = stringResource(R.string.qibla_location_accuracy_low),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center
                )
            }
            state.locationAccuracyMeters?.let { meters ->
                Text(
                    text = stringResource(R.string.qibla_location_accuracy_format, meters.roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun QiblaCompass(
    bearing: Float,
    heading: State<Float>,
    aligned: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        CompassDial(bearing, heading, Modifier.fillMaxSize())
        CompassFrame(aligned, Modifier.fillMaxSize())
        QiblaNeedle(bearing, heading, Modifier.fillMaxSize())
    }
}

/** Aylanuvchi shkala: N/E/S/W, darajalar va Ka'ba belgisi. Sensor qiymati faqat layer'da o'qiladi. */
@Composable
private fun CompassDial(bearing: Float, heading: State<Float>, modifier: Modifier = Modifier) {
    val colors = HidoyatTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val cardinals = stringArrayResource(R.array.qibla_cardinals)
    val cardinalStyle = MaterialTheme.typography.titleLarge.copy(
        color = colors.onHero,
        fontWeight = FontWeight.Bold
    )
    val northStyle = cardinalStyle.copy(color = colors.gold)
    val degreeStyle = MaterialTheme.typography.labelSmall.copy(
        color = colors.onHeroMuted,
        fontSize = 12.sp
    )

    Canvas(modifier.graphicsLayer { rotationZ = -heading.value }) {
        val c = center
        val radius = size.minDimension / 2f - FramePadding.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.heroEnd, colors.heroStart),
                center = c,
                radius = radius
            ),
            radius = radius,
            center = c
        )

        val tickTop = c.y - radius + 3.dp.toPx()
        for (i in 0 until TICK_COUNT) {
            val major = i % MAJOR_TICK_EVERY == 0
            val medium = i % MEDIUM_TICK_EVERY == 0
            val length = when {
                major -> 14.dp.toPx()
                medium -> 9.dp.toPx()
                else -> 5.dp.toPx()
            }
            rotate(i * TICK_STEP_DEGREES.toFloat(), pivot = c) {
                drawLine(
                    color = if (major) colors.onHero else colors.onHeroMuted,
                    start = Offset(c.x, tickTop),
                    end = Offset(c.x, tickTop + length),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        val labelTop = c.y - radius + 38.dp.toPx()
        for (angle in 0 until FULL_TURN step LABEL_STEP_DEGREES) {
            val isCardinal = angle % QUARTER_TURN == 0
            val text = if (isCardinal) cardinals[angle / QUARTER_TURN] else angle.toString()
            val style = when {
                angle == 0 -> northStyle
                isCardinal -> cardinalStyle
                else -> degreeStyle
            }
            val layout = textMeasurer.measure(text, style)
            rotate(angle.toFloat(), pivot = c) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(c.x - layout.size.width / 2f, labelTop)
                )
            }
        }

        rotate(bearing, pivot = c) {
            val marker = Offset(c.x, c.y - radius + 20.dp.toPx())
            val half = 8.dp.toPx()
            drawCircle(colors.gold, radius = 16.dp.toPx(), center = marker)
            drawRoundRect(
                color = colors.onGold,
                topLeft = Offset(marker.x - half, marker.y - half),
                size = Size(half * 2, half * 2),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawRect(
                color = colors.gold,
                topLeft = Offset(marker.x - half, marker.y - half + 4.dp.toPx()),
                size = Size(half * 2, 2.5.dp.toPx())
            )
        }
    }
}

/** Qo'zg'almas halqa, telefon yo'nalishi ko'rsatkichi va Qibla'ga to'g'ri kelganda yorug'lik. */
@Composable
private fun CompassFrame(aligned: Boolean, modifier: Modifier = Modifier) {
    val colors = HidoyatTheme.colors
    val glow by animateFloatAsState(
        targetValue = if (aligned) 1f else 0f,
        label = "qiblaGlow"
    )
    Canvas(modifier) {
        val c = center
        val radius = size.minDimension / 2f - FramePadding.toPx()

        drawCircle(
            color = colors.gold.copy(alpha = 0.35f * glow),
            radius = radius + 3.dp.toPx(),
            center = c,
            style = Stroke(width = 10.dp.toPx())
        )
        drawCircle(
            color = colors.gold,
            radius = radius,
            center = c,
            style = Stroke(width = 3.dp.toPx())
        )

        val pointer = Path().apply {
            moveTo(c.x - 9.dp.toPx(), 0f)
            lineTo(c.x + 9.dp.toPx(), 0f)
            lineTo(c.x, 16.dp.toPx())
            close()
        }
        drawPath(pointer, colors.gold)
    }
}

/** Qibla strelkasi: telefon aylanganda Ka'ba tomonga qarab turadi. */
@Composable
private fun QiblaNeedle(bearing: Float, heading: State<Float>, modifier: Modifier = Modifier) {
    val colors = HidoyatTheme.colors
    Canvas(modifier.graphicsLayer { rotationZ = bearing - heading.value }) {
        val c = center
        val radius = size.minDimension / 2f - FramePadding.toPx()
        val tipY = c.y - (radius - 62.dp.toPx())
        val headLength = 28.dp.toPx()
        val headHalfWidth = 14.dp.toPx()
        val shaftHalfWidth = 3.5.dp.toPx()

        val arrow = Path().apply {
            moveTo(c.x, tipY)
            lineTo(c.x + headHalfWidth, tipY + headLength)
            lineTo(c.x + shaftHalfWidth, tipY + headLength)
            lineTo(c.x + shaftHalfWidth, c.y)
            lineTo(c.x - shaftHalfWidth, c.y)
            lineTo(c.x - shaftHalfWidth, tipY + headLength)
            lineTo(c.x - headHalfWidth, tipY + headLength)
            close()
        }
        drawLine(
            color = colors.onHero.copy(alpha = 0.7f),
            start = c,
            end = Offset(c.x, c.y + 30.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawPath(arrow, colors.gold)
        drawPath(
            arrow,
            colors.heroStart,
            style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
        )
        drawCircle(colors.gold, radius = 11.dp.toPx(), center = c)
        drawCircle(colors.heroStart, radius = 5.dp.toPx(), center = c)
    }
}
