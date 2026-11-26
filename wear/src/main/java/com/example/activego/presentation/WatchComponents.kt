package com.example.activego.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

/** Size of every watch frame in the Figma file. */
val FigmaWatchSize = 198.dp

/**
 * Lays the content out on the exact 198 x 198 canvas of the Figma frames and then
 * scales it to the real screen, so every measurement in the design (paddings, font
 * sizes, ring thickness) keeps its proportion on any round watch.
 * Pointer input is transformed with the layer, so taps still land correctly.
 */
@Composable
fun FigmaWatchFrame(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background),
        contentAlignment = Alignment.Center
    ) {
        val scale = minOf(maxWidth / FigmaWatchSize, maxHeight / FigmaWatchSize)
        Box(
            modifier = Modifier
                .requiredSize(FigmaWatchSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(Palette.Background)
                .border(2.dp, Palette.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

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

/**
 * The `progress-ring` of `watch-home`: a 186 dp ring whose band is 5 % of the
 * radius (innerRadius = 0.95 in Figma), track #1C221C, fill starting at 12 o'clock
 * and sweeping clockwise with flat ends, exactly like the Figma arc.
 */
@Composable
fun WatchProgressRing(progress: Float, diameter: Dp = 186.dp) {
    Canvas(modifier = Modifier.size(diameter)) {
        val band = size.minDimension / 2f * 0.05f
        val inset = band / 2f
        val arcSize = Size(size.width - band, size.height - band)
        val stroke = Stroke(width = band, cap = StrokeCap.Butt)
        drawArc(
            color = Palette.Track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Palette.Lime,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
    }
}

/** One labelled bar of `watch-daily-summary`. */
@Composable
fun SummaryBar(
    label: String,
    value: String,
    goal: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Palette.Gray,
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Palette.White, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 11.sp)) {
                        append("$value ")
                    }
                    withStyle(SpanStyle(color = Palette.Gray, fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 9.sp)) {
                        append("/ $goal")
                    }
                }
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Palette.Border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(color)
            )
        }
    }
}

internal fun safeProgress(value: Int, goal: Int): Float =
    if (goal <= 0) 0f else value.toFloat() / goal

/** 8450 -> "8.4k", 10000 -> "10k", 480 -> "480" (format used by the Figma summary). */
internal fun compactNumber(value: Int): String {
    if (value < 1000) return value.toString()
    val tenths = value / 100
    return if (tenths % 10 == 0) "${tenths / 10}k" else "${tenths / 10}.${tenths % 10}k"
}

internal fun formatThousands(value: Int): String = "%,d".format(java.util.Locale.US, value)
