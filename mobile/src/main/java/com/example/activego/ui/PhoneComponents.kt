package com.example.activego.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activego.R
import com.example.activego.WorkoutType
import com.example.activego.ui.theme.ActiveGoColors
import com.example.activego.ui.theme.Geist

/** A Figma icon (exported 1:1 as a vector drawable) at its design size and colour. */
@Composable
fun FigmaIcon(@DrawableRes id: Int, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/** Tap target without the Material ripple, so the frames stay pixel-identical. */
@Composable
fun Modifier.plainClick(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** The `#121612` card with a 1 dp `#232A23` border used across every Figma frame. */
@Composable
fun FigmaCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    padding: Dp = 16.dp,
    background: Color = ActiveGoColors.Surface,
    borderColor: Color = ActiveGoColors.Border,
    spacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/**
 * The three concentric rings of `home-dashboard` (Figma 3:997): diameters 140, 110
 * and 80 dp, each with a 12 dp band on a #1C221C track, filled from 12 o'clock
 * clockwise with flat ends.
 */
@Composable
fun ConcentricRings(
    steps: Float,
    calories: Float,
    active: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val unit = size.width / 140f
            val band = 12f * unit
            val rings = listOf(
                Triple(140f, steps, ActiveGoColors.Lime),
                Triple(110f, calories, ActiveGoColors.Green),
                Triple(80f, active, ActiveGoColors.Red)
            )
            rings.forEach { (diameter, progress, color) ->
                val d = diameter * unit
                val inset = (size.width - d) / 2f + band / 2f
                val arcSize = Size(d - band, d - band)
                val stroke = Stroke(width = band, cap = StrokeCap.Butt)
                drawArc(
                    color = ActiveGoColors.Track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke
                )
            }
        }
        FigmaIcon(R.drawable.ic_bolt_24, 24.dp, ActiveGoColors.White)
    }
}

/** Live heart-rate line of the `hr-card` (Figma 3:1031): 2 dp, #FF3B30. */
@Composable
fun HeartRateSparkline(samples: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val top = strokeWidth
        val bottom = size.height - strokeWidth
        val points: List<Offset> = if (samples.size < 2) {
            listOf(Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f))
        } else {
            val min = samples.min().toFloat()
            val max = samples.max().toFloat()
            val range = (max - min).takeIf { it > 0f } ?: 1f
            samples.mapIndexed { i, v ->
                val x = size.width * i / (samples.size - 1)
                val y = bottom - (v - min) / range * (bottom - top)
                Offset(x, y)
            }
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = ActiveGoColors.Red,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Miter)
        )
    }
}

/** Goal progress bar of `fitness-goals`: 8 dp, radius 4, track #1C221C. */
@Composable
fun GoalBar(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ActiveGoColors.Track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .background(color)
        )
    }
}

/** The 48 x 28 toggle of `settings-device-pairing`, drawn from the Figma export. */
@Composable
fun FigmaToggle(checked: Boolean, onToggle: () -> Unit) {
    Canvas(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .plainClick(onToggle)
    ) {
        val unit = size.width / 48f
        if (checked) {
            drawRoundRect(color = ActiveGoColors.Lime, cornerRadius = CornerRadius(14f * unit))
            drawCircle(color = ActiveGoColors.Ink, radius = 12f * unit, center = Offset(34f * unit, 14f * unit))
        } else {
            val half = 0.5f * unit
            drawRoundRect(
                color = ActiveGoColors.Track,
                topLeft = Offset(half, half),
                size = Size(47f * unit, 27f * unit),
                cornerRadius = CornerRadius(13.5f * unit)
            )
            drawRoundRect(
                color = ActiveGoColors.Border,
                topLeft = Offset(half, half),
                size = Size(47f * unit, 27f * unit),
                cornerRadius = CornerRadius(13.5f * unit),
                style = Stroke(width = unit)
            )
            drawCircle(color = ActiveGoColors.Gray, radius = 12f * unit, center = Offset(14f * unit, 14f * unit))
        }
    }
}

/** The five destinations of the Figma bottom navigation. */
enum class PhoneTab(val label: String, @DrawableRes val icon: Int) {
    HOME("Home", R.drawable.ic_home_22),
    WORKOUT("Workout", R.drawable.ic_activity_22),
    HISTORY("History", R.drawable.ic_clock_22),
    GOALS("Goals", R.drawable.ic_target_22),
    PROFILE("Profile", R.drawable.ic_user_22)
}

/**
 * `bottom-nav-container` (Figma 3:1045): #121612 with a 1 dp top border, 64 dp
 * tall, five 64 dp tabs; active tab in lime Geist SemiBold, the rest in grey Geist
 * Medium. Android's own navigation bar takes the place of the iOS home indicator.
 */
@Composable
fun BottomNavigation(current: PhoneTab, onSelect: (PhoneTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ActiveGoColors.Surface)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ActiveGoColors.Border)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhoneTab.entries.forEach { tab ->
                val selected = tab == current
                val color = if (selected) ActiveGoColors.Lime else ActiveGoColors.Gray
                Column(
                    modifier = Modifier
                        .width(64.dp)
                        .plainClick { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FigmaIcon(tab.icon, 22.dp, color)
                    Text(
                        text = tab.label,
                        color = color,
                        fontFamily = Geist,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/** Icon used for each discipline at the 28 dp (grid) Figma size. */
@DrawableRes
fun WorkoutType.icon28(): Int = when (this) {
    WorkoutType.RUNNING -> R.drawable.ic_play_circle_28
    WorkoutType.CYCLING -> R.drawable.ic_bike_28
    WorkoutType.WALKING -> R.drawable.ic_footprints_28
    WorkoutType.STRENGTH -> R.drawable.ic_dumbbell_28
    WorkoutType.YOGA -> R.drawable.ic_circle_x_28
    WorkoutType.CUSTOM -> R.drawable.ic_plus_28
}

/** Icon used for each discipline at the 22 dp (history list) Figma size. */
@DrawableRes
fun WorkoutType.icon22(): Int = when (this) {
    WorkoutType.RUNNING -> R.drawable.ic_play_circle_22
    WorkoutType.CYCLING -> R.drawable.ic_bike_22
    WorkoutType.WALKING -> R.drawable.ic_footprints_22
    WorkoutType.STRENGTH -> R.drawable.ic_dumbbell_22
    WorkoutType.YOGA -> R.drawable.ic_circle_x_22
    WorkoutType.CUSTOM -> R.drawable.ic_plus_22
}

internal fun safeProgress(value: Int, goal: Int): Float =
    if (goal <= 0) 0f else value.toFloat() / goal

internal fun percent(value: Int, goal: Int): Int =
    if (goal <= 0) 0 else ((value.toFloat() / goal) * 100).toInt().coerceIn(0, 999)

internal fun formatThousands(value: Int): String = "%,d".format(java.util.Locale.US, value)

/**
 * Generic profile picture, like the default avatar of messaging apps: a filled
 * head-and-shoulders silhouette on a round background, in the Figma greys.
 */
@Composable
fun DefaultAvatar(size: Dp, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ActiveGoColors.Track)
            .border(1.dp, ActiveGoColors.Border, CircleShape)
    ) {
        val w = this.size.width
        // head
        drawCircle(
            color = ActiveGoColors.Gray,
            radius = w * 0.19f,
            center = Offset(w * 0.5f, w * 0.40f)
        )
        // shoulders: a large circle whose top shows inside the avatar clip
        drawCircle(
            color = ActiveGoColors.Gray,
            radius = w * 0.36f,
            center = Offset(w * 0.5f, w * 1.00f)
        )
    }
}
