package com.example.activego.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.example.activego.ActivitySnapshot
import com.example.activego.Goals
import com.example.activego.R
import com.example.activego.WorkoutState
import com.example.activego.WorkoutType

/**
 * Watch destinations, one per frame of the Figma "Smartwatch" page. READY reuses
 * the `watch-active-workout` frame before the session starts: the workout only
 * begins when the user presses play.
 */
enum class WatchScreen { HOME, START, READY, ACTIVE, SUMMARY }

/** Tap target without the Material ripple, so the frames stay pixel-identical. */
@Composable
private fun Modifier.plainClick(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}

// ---------------------------------------------------------------------------
// watch-home (Figma 27:3)
// ---------------------------------------------------------------------------
@Composable
fun WatchHomeScreen(
    activity: ActivitySnapshot,
    goals: Goals,
    clock: String,
    onOpenStart: () -> Unit,
    onOpenSummary: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WatchProgressRing(progress = safeProgress(activity.steps, goals.steps))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = clock,
                color = Palette.Gray,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            // Tapping the step count opens the workout picker (FR-01: two taps).
            Column(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .plainClick(onOpenStart),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatThousands(activity.steps),
                    color = Palette.White,
                    fontFamily = Outfit,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 28.sp
                )
                Text(
                    text = "STEPS",
                    color = Palette.Lime,
                    fontFamily = Geist,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            }

            // Tapping the metrics row opens the daily summary.
            Row(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .plainClick(onOpenSummary),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FigmaIcon(R.drawable.ic_heart_off_11, 11.dp, Palette.Red)
                    Text(
                        text = activity.heartRate.toString(),
                        color = Palette.White,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(10.dp)
                        .background(Palette.Border)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FigmaIcon(R.drawable.ic_zap_11, 11.dp, Palette.Lime)
                    Text(
                        text = activity.watchBattery.toString() + "%",
                        color = Palette.White,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// watch-start-workout (Figma 27:23)
// ---------------------------------------------------------------------------
private val WatchWorkouts = listOf(
    WorkoutType.RUNNING to R.drawable.ic_play_circle_14,
    WorkoutType.CYCLING to R.drawable.ic_bike_14,
    WorkoutType.STRENGTH to R.drawable.ic_dumbbell_14
)

@Composable
fun WatchStartWorkoutScreen(onStart: (WorkoutType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "START WORKOUT",
            color = Palette.Gray,
            fontFamily = Outfit,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WatchWorkouts.forEachIndexed { index, (type, icon) ->
                val highlighted = index == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (highlighted) Palette.LimeTint else Palette.Surface)
                        .border(1.dp, if (highlighted) Palette.Lime else Palette.Border, RoundedCornerShape(16.dp))
                        .clickable { onStart(type) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FigmaIcon(icon, 14.dp, if (highlighted) Palette.Lime else Palette.Gray)
                    Text(
                        text = type.watchLabel,
                        color = if (highlighted) Palette.Lime else Palette.White,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// watch-active-workout (Figma 27:41)
// ---------------------------------------------------------------------------

/**
 * Live session frame, also used as the READY frame before the session starts.
 *
 * Three controls, left to right:
 *  - lime  play  : starts the session (READY) or resumes it (PAUSED)
 *  - blue  pause : pauses a running session
 *  - red   stop  : ends the session, or cancels it while still READY
 * A control that does not apply to the current state is dimmed and inactive.
 */
@Composable
fun WatchActiveWorkoutScreen(
    activity: ActivitySnapshot,
    ready: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    val paused = !ready && activity.workoutState == WorkoutState.PAUSED
    val running = !ready && activity.workoutState == WorkoutState.ACTIVE

    val badgeColor = if (running) Palette.Lime else Palette.Gray
    val badgeText = when {
        ready -> "READY"
        paused -> "PAUSED"
        else -> activity.workoutType.cardLabel.uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Text(
                text = badgeText,
                color = badgeColor,
                fontFamily = Geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        }

        Text(
            text = if (ready) "00:00" else activity.elapsedShort(),
            color = Palette.White,
            fontFamily = Outfit,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 32.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FigmaIcon(R.drawable.ic_heart_12, 12.dp, Palette.Red)
                Text(
                    text = activity.heartRate.toString(),
                    color = Palette.Red,
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FigmaIcon(R.drawable.ic_flame_12, 12.dp, Palette.Lime)
                Text(
                    text = if (ready) "0" else activity.workoutCalories.toString(),
                    color = Palette.White,
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WatchControl(
                icon = R.drawable.ic_play_12,
                color = Palette.Lime,
                tint = Palette.LimeTint,
                enabled = ready || paused,
                onClick = onPlay
            )
            WatchControl(
                icon = R.drawable.ic_pause_12,
                color = Palette.Blue,
                tint = Palette.BlueTint,
                enabled = running,
                onClick = onPause
            )
            WatchControl(
                icon = R.drawable.ic_stop_circle_12,
                color = Palette.Red,
                tint = Palette.RedTint,
                enabled = true,
                onClick = onStop
            )
        }
    }
}

/** 32 dp round control with the Figma style: 10 % tint, 1 dp border, 12 dp icon. */
@Composable
private fun WatchControl(
    icon: Int,
    color: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(tint)
            .border(1.dp, color, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FigmaIcon(icon, 12.dp, color)
    }
}

// ---------------------------------------------------------------------------
// watch-daily-summary (Figma 27:66)
// ---------------------------------------------------------------------------
@Composable
fun WatchDailySummaryScreen(
    activity: ActivitySnapshot,
    goals: Goals,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .plainClick(onBack)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "DAILY SUMMARY",
            color = Palette.Gray,
            fontFamily = Outfit,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SummaryBar(
                label = "Steps",
                value = compactNumber(activity.steps),
                goal = compactNumber(goals.steps),
                progress = safeProgress(activity.steps, goals.steps),
                color = Palette.Lime
            )
            SummaryBar(
                label = "Active",
                value = activity.activeMinutes.toString() + "m",
                goal = goals.activeMinutes.toString() + "m",
                progress = safeProgress(activity.activeMinutes, goals.activeMinutes),
                color = Palette.Red
            )
            SummaryBar(
                label = "Energy",
                value = activity.calories.toString(),
                goal = goals.calories.toString(),
                progress = safeProgress(activity.calories, goals.calories),
                color = Palette.Green
            )
        }
    }
}
