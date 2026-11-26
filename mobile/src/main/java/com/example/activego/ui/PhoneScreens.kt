package com.example.activego.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.activego.ActivitySnapshot
import com.example.activego.Goals
import com.example.activego.R
import com.example.activego.WorkoutRecord
import com.example.activego.WorkoutState
import com.example.activego.WorkoutType
import com.example.activego.ui.theme.ActiveGoColors
import com.example.activego.ui.theme.Geist
import com.example.activego.ui.theme.Outfit
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Text with the exact family / weight / size of a Figma text layer. */
@Composable
private fun FigmaText(
    text: String,
    size: Int,
    color: Color,
    family: FontFamily = Outfit,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        color = color,
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight,
        maxLines = maxLines,
        modifier = modifier
    )
}

/** Body container shared by every frame: 24 dp padding, vertical scroll. */
@Composable
private fun ScreenBody(spacing: Int, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        content()
    }
}

// ===========================================================================
// home-dashboard (Figma 3:981)
// ===========================================================================
@Composable
fun HomeDashboardScreen(
    activity: ActivitySnapshot,
    goals: Goals,
    heartRates: List<Int>,
    onQuickStart: () -> Unit
) {
    ScreenBody(spacing = 24) {
        // header-row
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FigmaText("Welcome back,", 13, ActiveGoColors.Gray, Geist)
                FigmaText("Yair Guzman", 24, ActiveGoColors.White, Outfit, FontWeight.ExtraBold)
            }
            DefaultAvatar(size = 44.dp)
        }

        // progress-card
        FigmaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ConcentricRings(
                    steps = safeProgress(activity.steps, goals.steps),
                    calories = safeProgress(activity.calories, goals.calories),
                    active = safeProgress(activity.activeMinutes, goals.activeMinutes)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RingLegend(
                        ActiveGoColors.Lime, "Steps",
                        formatThousands(activity.steps) + " / " + formatThousands(goals.steps)
                    )
                    RingLegend(
                        ActiveGoColors.Green, "Calories",
                        activity.calories.toString() + " / " + goals.calories + " kcal"
                    )
                    RingLegend(
                        ActiveGoColors.Red, "Active Mins",
                        activity.activeMinutes.toString() + " / " + goals.activeMinutes + " mins"
                    )
                }
            }
        }

        // hr-card
        FigmaCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FigmaIcon(R.drawable.ic_heart_off_20, 20.dp, ActiveGoColors.Red)
                    FigmaText("Heart Rate", 16, ActiveGoColors.White, Outfit, FontWeight.Bold)
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = Outfit, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = ActiveGoColors.White)) {
                            append(activity.heartRate.toString())
                        }
                        withStyle(SpanStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = ActiveGoColors.Gray)) {
                            append(" BPM")
                        }
                    }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                HeartRateSparkline(
                    samples = heartRates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                )
            }
        }

        // quick-start-cta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ActiveGoColors.Lime)
                .clickable(onClick = onQuickStart)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FigmaIcon(R.drawable.ic_play_24, 24.dp, ActiveGoColors.Ink)
            FigmaText(
                "Quick Start Workout", 16, ActiveGoColors.Ink, Outfit, FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            FigmaIcon(R.drawable.ic_arrow_right_20, 20.dp, ActiveGoColors.Ink)
        }
    }
}

@Composable
private fun RingLegend(dot: Color, label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            FigmaText(label, 14, ActiveGoColors.White, Outfit, FontWeight.Bold)
        }
        FigmaText(value, 13, ActiveGoColors.Gray, Geist)
    }
}

// ===========================================================================
// start-workout (Figma 3:1065)
// ===========================================================================
private val TargetTimes = listOf("00:00:00", "00:15:00", "00:30:00", "00:45:00", "01:00:00")
private val TargetDistances = listOf("0.00 miles", "1.00 miles", "2.00 miles", "3.10 miles", "5.00 miles")
private val TargetCalories = listOf("0 kcal", "100 kcal", "200 kcal", "300 kcal", "500 kcal")

@Composable
fun StartWorkoutScreen(
    selected: WorkoutType,
    onSelect: (WorkoutType) -> Unit,
    onStart: () -> Unit
) {
    var timeIndex by remember { mutableIntStateOf(0) }
    var distanceIndex by remember { mutableIntStateOf(0) }
    var caloriesIndex by remember { mutableIntStateOf(0) }

    ScreenBody(spacing = 24) {
        FigmaText("Start Workout", 28, ActiveGoColors.White, Outfit, FontWeight.ExtraBold)

        // grid: two rows of three cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkoutType.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { type ->
                        WorkoutCard(
                            type = type,
                            selected = type == selected,
                            onClick = { onSelect(type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        FigmaText("Set Target Goals (Optional)", 18, ActiveGoColors.White, Outfit, FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TargetRow(R.drawable.ic_clock_20, "Target Time", TargetTimes, timeIndex) {
                timeIndex = (timeIndex + 1) % TargetTimes.size
            }
            TargetRow(R.drawable.ic_map_20, "Target Distance", TargetDistances, distanceIndex) {
                distanceIndex = (distanceIndex + 1) % TargetDistances.size
            }
            TargetRow(R.drawable.ic_flame_20, "Target Calories", TargetCalories, caloriesIndex) {
                caloriesIndex = (caloriesIndex + 1) % TargetCalories.size
            }
        }

        // start-cta
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ActiveGoColors.Lime)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center
        ) {
            FigmaText("Start Session", 16, ActiveGoColors.Ink, Outfit, FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun WorkoutCard(
    type: WorkoutType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = if (selected) ActiveGoColors.Lime else ActiveGoColors.Gray
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) ActiveGoColors.LimeTint else ActiveGoColors.Surface)
            .border(1.dp, if (selected) ActiveGoColors.Lime else ActiveGoColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FigmaIcon(type.icon28(), 28.dp, accent)
        FigmaText(
            type.cardLabel, 14,
            if (selected) ActiveGoColors.Lime else ActiveGoColors.White,
            Outfit, FontWeight.Bold, maxLines = 1
        )
    }
}

@Composable
private fun TargetRow(icon: Int, label: String, values: List<String>, index: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(ActiveGoColors.Surface)
            .border(1.dp, ActiveGoColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FigmaIcon(icon, 20.dp, ActiveGoColors.Gray)
        FigmaText(label, 14, ActiveGoColors.White, Geist, modifier = Modifier.weight(1f))
        FigmaText(values[index], 14, if (index == 0) ActiveGoColors.Dim else ActiveGoColors.White, Geist)
    }
}

// ===========================================================================
// active-workout (Figma 3:1133)
// ===========================================================================

/**
 * Live session frame, also used as the READY frame after "Start Session": the
 * workout begins only when the user presses play, exactly like on the watch.
 *
 * Controls, left to right (same as the watch):
 *  - lime  play  : starts the session (READY) or resumes it (PAUSED)
 *  - blue  pause : pauses a running session
 *  - red   stop  : ends the session, or cancels it while still READY
 * A control that does not apply to the current state is dimmed and inactive.
 */
@Composable
fun ActiveWorkoutScreen(
    activity: ActivitySnapshot,
    ready: Boolean,
    watchConnected: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    val paused = !ready && activity.workoutState == WorkoutState.PAUSED
    val running = !ready && activity.workoutState == WorkoutState.ACTIVE
    val stateLabel = when {
        ready -> " · Ready"
        paused -> " · Paused"
        else -> ""
    }

    ScreenBody(spacing = 32) {
        // header-row
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(ActiveGoColors.LimeTint)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FigmaIcon(R.drawable.ic_play_circle_16, 16.dp, ActiveGoColors.Lime)
                FigmaText(
                    activity.workoutType.watchLabel + stateLabel,
                    13, ActiveGoColors.Lime, Outfit, FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ActiveGoColors.Red)
                )
                FigmaText(
                    if (watchConnected) "Watch Connected" else "Watch Offline",
                    12, ActiveGoColors.Red, Geist, FontWeight.SemiBold
                )
            }
        }

        // timer-block
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FigmaText("ELAPSED TIME", 14, ActiveGoColors.Gray, Geist, FontWeight.Medium)
            FigmaText(
                if (ready) "00:00:00" else activity.elapsedLong(),
                64, ActiveGoColors.White, Outfit, FontWeight.ExtraBold, maxLines = 1
            )
        }

        // metrics-grid
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard("Distance", "%.2f".format(Locale.US, activity.distanceMiles()), "mi", ActiveGoColors.White, Modifier.weight(1f))
                MetricCard("Current Pace", activity.paceText(), "/mi", ActiveGoColors.White, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard("Heart Rate", activity.heartRate.toString(), "bpm", ActiveGoColors.Red, Modifier.weight(1f))
                MetricCard("Active Calories", activity.workoutCalories.toString(), "kcal", ActiveGoColors.Green, Modifier.weight(1f))
            }
        }

        // controls: play / pause / stop
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SessionControl(
                icon = R.drawable.ic_play_32,
                iconSize = 32,
                color = ActiveGoColors.Lime,
                tint = ActiveGoColors.LimeTint,
                enabled = ready || paused,
                onClick = onPlay
            )
            SessionControl(
                icon = R.drawable.ic_pause_32,
                iconSize = 32,
                color = ActiveGoColors.Blue,
                tint = ActiveGoColors.BlueTint,
                enabled = running,
                onClick = onPause
            )
            SessionControl(
                icon = R.drawable.ic_stop_circle_24,
                iconSize = 24,
                color = ActiveGoColors.Red,
                tint = ActiveGoColors.RedTint,
                enabled = true,
                onClick = onStop
            )
        }
    }
}

/** 72 dp round control in the Figma `stop-btn` style: 10 % tint and 2 dp border. */
@Composable
private fun SessionControl(
    icon: Int,
    iconSize: Int,
    color: Color,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(CircleShape)
            .background(tint)
            .border(2.dp, color, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FigmaIcon(icon, iconSize.dp, color)
    }
}

@Composable
private fun MetricCard(label: String, value: String, unit: String, valueColor: Color, modifier: Modifier) {
    FigmaCard(modifier = modifier, spacing = 4.dp) {
        FigmaText(label, 12, ActiveGoColors.Gray, Geist)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = Outfit, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = valueColor)) {
                    append(value)
                }
                withStyle(SpanStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = ActiveGoColors.Gray)) {
                    append(" $unit")
                }
            },
            maxLines = 1
        )
    }
}

// ===========================================================================
// workout-history (Figma 3:1291)
// ===========================================================================
private val HistoryFilters: List<WorkoutType?> = listOf(
    null, WorkoutType.RUNNING, WorkoutType.CYCLING, WorkoutType.STRENGTH,
    WorkoutType.YOGA, WorkoutType.WALKING, WorkoutType.CUSTOM
)

@Composable
fun WorkoutHistoryScreen(history: List<WorkoutRecord>) {
    var filter by remember { mutableStateOf<WorkoutType?>(null) }
    val visible = history.filter { filter == null || it.type == filter }

    ScreenBody(spacing = 20) {
        FigmaText("Workout History", 28, ActiveGoColors.White, Outfit, FontWeight.ExtraBold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilters.forEach { type ->
                val selected = type == filter
                val shape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(if (selected) ActiveGoColors.Lime else ActiveGoColors.Surface)
                        .then(if (selected) Modifier else Modifier.border(1.dp, ActiveGoColors.Border, shape))
                        .clickable { filter = type }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    FigmaText(
                        type?.cardLabel ?: "All", 13,
                        if (selected) ActiveGoColors.Ink else ActiveGoColors.White,
                        Outfit, FontWeight.Bold
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            FigmaCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp, spacing = 4.dp) {
                FigmaText("No workouts yet", 16, ActiveGoColors.White, Outfit, FontWeight.Bold)
                FigmaText(
                    "Finish a session on the watch or the phone and it appears here.",
                    12, ActiveGoColors.Gray, Geist
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visible.forEach { record -> HistoryItem(record) }
            }
        }
    }
}

@Composable
private fun HistoryItem(record: WorkoutRecord) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ActiveGoColors.Surface)
            .border(1.dp, ActiveGoColors.Border, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ActiveGoColors.Track),
            contentAlignment = Alignment.Center
        ) {
            FigmaIcon(record.type.icon22(), 22.dp, ActiveGoColors.Lime)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FigmaText(record.type.sessionTitle, 16, ActiveGoColors.White, Outfit, FontWeight.Bold, maxLines = 1)
            FigmaText(historyDate(record.startedAt), 12, ActiveGoColors.Gray, Geist, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FigmaText(durationText(record.durationSeconds), 14, ActiveGoColors.White, Outfit, FontWeight.Bold)
            FigmaText(record.calories.toString() + " kcal", 12, ActiveGoColors.Green, Geist)
        }
    }
}

private val HistoryTime: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
private val HistoryDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** "Today, 08:30 AM" / "Yesterday, 06:15 PM" / "Oct 24, 07:00 AM", as in the Figma list. */
private fun historyDate(millis: Long): String {
    val zone = ZoneId.systemDefault()
    val moment = Instant.ofEpochMilli(millis).atZone(zone)
    val today = LocalDate.now(zone)
    val time = moment.format(HistoryTime)
    return when (moment.toLocalDate()) {
        today -> "Today, $time"
        today.minusDays(1) -> "Yesterday, $time"
        else -> moment.format(HistoryDay) + ", " + time
    }
}

/** "45 mins" / "1h 15m", as in the Figma list. */
private fun durationText(seconds: Int): String = when {
    seconds < 60 -> "$seconds sec"
    seconds < 3600 -> "${seconds / 60} mins"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}

// ===========================================================================
// fitness-goals (Figma 3:1369)
// ===========================================================================
@Composable
fun FitnessGoalsScreen(
    activity: ActivitySnapshot,
    goals: Goals,
    onSaveGoals: (Goals) -> Unit
) {
    var editing by remember { mutableStateOf(false) }

    ScreenBody(spacing = 24) {
        FigmaText("Active Goals", 28, ActiveGoColors.White, Outfit, FontWeight.ExtraBold)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GoalCard(
                title = "Daily Steps Goal",
                color = ActiveGoColors.Lime,
                value = activity.steps,
                goal = goals.steps,
                currentText = "Current: " + activity.steps + " steps"
            )
            GoalCard(
                title = "Daily Active Time",
                color = ActiveGoColors.Green,
                value = activity.activeMinutes,
                goal = goals.activeMinutes,
                currentText = "Current: " + activity.activeMinutes + " mins"
            )
            GoalCard(
                title = "Daily Calorie Target",
                color = ActiveGoColors.Red,
                value = activity.calories,
                goal = goals.calories,
                currentText = "Current: " + activity.calories + " kcal"
            )
        }

        // fab-container: the FAB opens the goal editor, whose Save pushes to the watch.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(ActiveGoColors.Lime)
                    .clickable { editing = true },
                contentAlignment = Alignment.Center
            ) {
                FigmaIcon(R.drawable.ic_plus_24, 24.dp, ActiveGoColors.Ink)
            }
        }
    }

    if (editing) {
        GoalEditorDialog(
            initial = goals,
            onDismiss = { editing = false },
            onSave = {
                onSaveGoals(it)
                editing = false
            }
        )
    }
}

@Composable
private fun GoalCard(title: String, color: Color, value: Int, goal: Int, currentText: String) {
    FigmaCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FigmaText(title, 16, ActiveGoColors.White, Outfit, FontWeight.Bold, modifier = Modifier.weight(1f))
            FigmaText(percent(value, goal).toString() + "%", 14, color, Outfit, FontWeight.Bold)
        }
        GoalBar(progress = safeProgress(value, goal), color = color)
        Row(modifier = Modifier.fillMaxWidth()) {
            FigmaText(currentText, 12, ActiveGoColors.Gray, Geist, modifier = Modifier.weight(1f))
            FigmaText("Target: $goal", 12, ActiveGoColors.Gray, Geist)
        }
    }
}

/** Goal editor opened by the FAB, styled with the same Figma tokens. */
@Composable
private fun GoalEditorDialog(initial: Goals, onDismiss: () -> Unit, onSave: (Goals) -> Unit) {
    var steps by remember { mutableIntStateOf(initial.steps) }
    var activeMinutes by remember { mutableIntStateOf(initial.activeMinutes) }
    var calories by remember { mutableIntStateOf(initial.calories) }

    Dialog(onDismissRequest = onDismiss) {
        FigmaCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp, spacing = 16.dp) {
            FigmaText("Edit Goals", 22, ActiveGoColors.White, Outfit, FontWeight.ExtraBold)
            GoalStepper("Daily Steps", steps.toString()) { delta -> steps = (steps + delta * 500).coerceAtLeast(500) }
            GoalStepper("Daily Active Time (mins)", activeMinutes.toString()) { delta -> activeMinutes = (activeMinutes + delta * 5).coerceAtLeast(5) }
            GoalStepper("Daily Calories (kcal)", calories.toString()) { delta -> calories = (calories + delta * 50).coerceAtLeast(50) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val shape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(shape)
                        .background(ActiveGoColors.Surface)
                        .border(1.dp, ActiveGoColors.Border, shape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    FigmaText("Cancel", 14, ActiveGoColors.Gray, Outfit, FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(shape)
                        .background(ActiveGoColors.Lime)
                        .clickable {
                            onSave(initial.copy(steps = steps, activeMinutes = activeMinutes, calories = calories))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    FigmaText("Save & Sync", 14, ActiveGoColors.Ink, Outfit, FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun GoalStepper(label: String, value: String, onStep: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FigmaText(label, 12, ActiveGoColors.Gray, Geist)
            FigmaText(value, 24, ActiveGoColors.White, Outfit, FontWeight.ExtraBold)
        }
        StepperButton(R.drawable.ic_minus_22) { onStep(-1) }
        Spacer(Modifier.size(8.dp))
        StepperButton(R.drawable.ic_plus_22) { onStep(1) }
    }
}

@Composable
private fun StepperButton(icon: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(ActiveGoColors.Track)
            .border(1.dp, ActiveGoColors.Border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FigmaIcon(icon, 22.dp, ActiveGoColors.Lime)
    }
}

// ===========================================================================
// settings-device-pairing (Figma 3:1502), reached from the Profile tab
// ===========================================================================
@Composable
fun SettingsScreen(
    watchConnected: Boolean,
    watchBattery: Int,
    lastSyncAt: Long
) {
    var backgroundSync by remember { mutableStateOf(true) }
    var reminders by remember { mutableStateOf(true) }
    var displaySharing by remember { mutableStateOf(false) }

    // Re-evaluate "Synced: x mins ago" while the screen is visible.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    ScreenBody(spacing = 24) {
        FigmaText("Settings", 28, ActiveGoColors.White, Outfit, FontWeight.ExtraBold)
        FigmaText("My Devices", 18, ActiveGoColors.White, Outfit, FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DeviceCard(
                icon = R.drawable.ic_watch_22,
                name = "ActiveGo Watch v3",
                synced = syncedText(lastSyncAt, now),
                status = if (watchConnected) "Connected" else "Disconnected",
                statusColor = if (watchConnected) ActiveGoColors.Green else ActiveGoColors.Red,
                detail = if (lastSyncAt > 0) "$watchBattery%" else "--"
            )
            // The smart display is documented in the FRD but outside this practice.
            DeviceCard(
                icon = R.drawable.ic_tv_22,
                name = "ActiveGo Smart Panel",
                synced = "Not part of Practice 5",
                status = "Not paired",
                statusColor = ActiveGoColors.Gray,
                detail = "--"
            )
        }

        FigmaText("Preferences", 18, ActiveGoColors.White, Outfit, FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PreferenceRow("Background Sync", "Keep device metrics updated in background", backgroundSync) {
                backgroundSync = !backgroundSync
            }
            PreferenceRow("Notification Reminders", "Get alerts on workouts & high heart rate", reminders) {
                reminders = !reminders
            }
            PreferenceRow("Live Display Sharing", "Stream metrics to nearby Smart Panels", displaySharing) {
                displaySharing = !displaySharing
            }
        }
    }
}

@Composable
private fun DeviceCard(icon: Int, name: String, synced: String, status: String, statusColor: Color, detail: String) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ActiveGoColors.Surface)
            .border(1.dp, ActiveGoColors.Border, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ActiveGoColors.Track),
            contentAlignment = Alignment.Center
        ) {
            FigmaIcon(icon, 22.dp, ActiveGoColors.Lime)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FigmaText(name, 15, ActiveGoColors.White, Outfit, FontWeight.Bold, maxLines = 1)
            FigmaText(synced, 12, ActiveGoColors.Gray, Geist, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FigmaText(status, 12, statusColor, Geist, FontWeight.SemiBold)
            FigmaText(detail, 11, ActiveGoColors.Gray, Geist)
        }
    }
}

@Composable
private fun PreferenceRow(title: String, description: String, checked: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FigmaText(title, 15, ActiveGoColors.White, Outfit, FontWeight.Bold)
            FigmaText(description, 12, ActiveGoColors.Gray, Geist)
        }
        FigmaToggle(checked = checked, onToggle = onToggle)
    }
}

private fun syncedText(lastSyncAt: Long, now: Long): String {
    if (lastSyncAt <= 0L) return "Synced: Never"
    val minutes = ((now - lastSyncAt) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "Synced: Just now"
        minutes == 1L -> "Synced: 1 min ago"
        else -> "Synced: $minutes mins ago"
    }
}
