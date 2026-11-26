package com.example.activego.presentation

import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.activego.ActivitySnapshot
import com.example.activego.CMD_PAUSE
import com.example.activego.CMD_RESUME
import com.example.activego.CMD_START
import com.example.activego.CMD_STOP
import com.example.activego.MESSAGE_PATH_WORKOUT_CONTROL
import com.example.activego.WorkoutCommand
import com.example.activego.WorkoutState
import com.example.activego.WorkoutType
import com.example.activego.data.ActivityPayload
import com.example.activego.data.Haptics
import com.example.activego.data.WearRepository
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * ActiveGo - watch application.
 *
 * This Activity owns the SENDING half of both features; the receiving half lives in
 * WearListenerService so that it keeps working when the app is not in the
 * foreground.
 *
 *  Feature 1 (MessageClient): [sendWorkoutCommand] pushes start / pause / resume /
 *  stop events to the phone. The local state is updated first so the watch always
 *  feels instantaneous, then the event is broadcast.
 *
 *  Feature 2 (DataClient): [publishActivitySnapshot] writes today's totals and the
 *  live workout state to a replicated data item that the phone observes.
 */
class MainActivity : ComponentActivity() {

    private val tag = "ActiveGoWatch"

    private lateinit var messageClient: MessageClient
    private lateinit var dataClient: DataClient
    private lateinit var nodeClient: NodeClient

    /** How often today's activity is pushed to the phone while the app is visible. */
    private val publishIntervalMs = 3_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messageClient = Wearable.getMessageClient(this)
        dataClient = Wearable.getDataClient(this)
        nodeClient = Wearable.getNodeClient(this)

        observeConnectionAndBattery()
        publishPeriodically()

        setContent {
            MaterialTheme {
                // Figma text layers: no tracking, natural line height (see Palette.kt).
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(fontFamily = Geist, letterSpacing = 0.sp)
                ) {
                WatchApp(
                    onStart = { type -> sendWorkoutCommand(CMD_START, type) },
                    onPause = { sendWorkoutCommand(CMD_PAUSE) },
                    onResume = { sendWorkoutCommand(CMD_RESUME) },
                    onStop = { sendWorkoutCommand(CMD_STOP) }
                )
                }
            }
        }
    }

    // =======================================================================
    // FEATURE 1 - Workout Control Sync (MessageClient)
    // =======================================================================

    /**
     * Applies the command locally and then sends it to every connected phone.
     *
     * MessageClient is fire-and-forget and requires a live connection, which is
     * exactly right for a control event: if the phone is not there, there is nothing
     * meaningful to replay later. Durable state is handled by Feature 2 instead.
     */
    private fun sendWorkoutCommand(action: String, type: WorkoutType? = null) {
        // 1. Local state first, so the watch UI never waits for the radio.
        when (action) {
            CMD_START -> WearRepository.startWorkout(type ?: WorkoutType.RUNNING)
            CMD_PAUSE -> WearRepository.pauseWorkout()
            CMD_RESUME -> WearRepository.resumeWorkout()
            CMD_STOP -> WearRepository.stopWorkout()
        }
        Haptics.buzz(this, if (action == CMD_STOP) Haptics.LONG else Haptics.SHORT)

        // 2. Then broadcast the event to the phone.
        val command = WorkoutCommand(action, type)
        lifecycleScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                WearRepository.setPhoneConnected(nodes.isNotEmpty())
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH_WORKOUT_CONTROL,
                        command.toBytes()
                    ).await()
                    Log.d(tag, "Sent " + command.encode() + " to " + node.displayName)
                }
            } catch (e: Exception) {
                Log.e(tag, "sendWorkoutCommand failed", e)
            }
        }

        // A workout boundary is worth syncing immediately rather than on the next tick.
        publishActivitySnapshot(WearRepository.activity.value)
    }

    // =======================================================================
    // FEATURE 2 - Activity Data Sync (DataClient)
    // =======================================================================

    /**
     * Writes the snapshot to the replicated data store at DATA_PATH_ACTIVITY_TODAY
     * (the DataMap itself is built by [ActivityPayload]).
     *
     * If no phone is reachable the call still succeeds: the item is kept locally and
     * delivered automatically on reconnection, which is how FR-05 is satisfied
     * without writing any buffering code.
     */
    private fun publishActivitySnapshot(snapshot: ActivitySnapshot) {
        lifecycleScope.launch {
            try {
                dataClient.putDataItem(ActivityPayload.toRequest(snapshot)).await()
                Log.d(tag, "Published snapshot: steps=" + snapshot.steps)
            } catch (e: Exception) {
                Log.e(tag, "publishActivitySnapshot failed", e)
            }
        }
    }

    /** Pushes the snapshot on a fixed interval while the app is at least STARTED. */
    private fun publishPeriodically() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    publishActivitySnapshot(WearRepository.activity.value)
                    delay(publishIntervalMs)
                }
            }
        }
    }

    /** Polls NodeClient for the phone link and BatteryManager for the `zap` value. */
    private fun observeConnectionAndBattery() {
        val batteryManager = getSystemService(BatteryManager::class.java)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        ?.takeIf { it in 0..100 }
                        ?.let { WearRepository.setBattery(it) }
                    try {
                        val nodes = nodeClient.connectedNodes.await()
                        WearRepository.setPhoneConnected(nodes.isNotEmpty())
                    } catch (e: Exception) {
                        WearRepository.setPhoneConnected(false)
                    }
                    delay(5_000L)
                }
            }
        }
    }
}

@Composable
private fun WatchApp(
    onStart: (WorkoutType) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val activity by WearRepository.activity.collectAsState()
    val goals by WearRepository.goals.collectAsState()

    var screen by remember { mutableStateOf(WatchScreen.HOME) }

    // Discipline picked on the start list. Picking it only opens the READY frame;
    // the session begins when the user presses play.
    var pendingType by remember { mutableStateOf(WorkoutType.RUNNING) }
    var clock by remember { mutableStateOf(currentClock()) }

    LaunchedEffect(Unit) {
        while (true) {
            clock = currentClock()
            delay(1_000L)
        }
    }

    // A workout running on either device always wins over manual navigation, so a
    // START arriving from the phone brings the watch to the live session by itself.
    LaunchedEffect(activity.workoutState) {
        screen = if (activity.workoutState == WorkoutState.IDLE) {
            if (screen == WatchScreen.ACTIVE) WatchScreen.HOME else screen
        } else {
            WatchScreen.ACTIVE
        }
    }

    // Back (swipe or button) returns to the home frame instead of leaving the app.
    BackHandler(
        enabled = screen == WatchScreen.START ||
            screen == WatchScreen.READY ||
            screen == WatchScreen.SUMMARY
    ) {
        screen = WatchScreen.HOME
    }

    FigmaWatchFrame {
        when (screen) {
            WatchScreen.HOME -> WatchHomeScreen(
                activity = activity,
                goals = goals,
                clock = clock,
                onOpenStart = { screen = WatchScreen.START },
                onOpenSummary = { screen = WatchScreen.SUMMARY }
            )

            WatchScreen.START -> WatchStartWorkoutScreen(
                onStart = { type ->
                    pendingType = type
                    screen = WatchScreen.READY
                }
            )

            // Nothing has been sent yet: play sends START, stop just cancels.
            WatchScreen.READY -> WatchActiveWorkoutScreen(
                activity = activity.copy(workoutType = pendingType),
                ready = true,
                onPlay = { onStart(pendingType) },
                onPause = {},
                onStop = { screen = WatchScreen.HOME }
            )

            WatchScreen.ACTIVE -> WatchActiveWorkoutScreen(
                activity = activity,
                ready = false,
                onPlay = { if (activity.workoutState == WorkoutState.PAUSED) onResume() },
                onPause = { if (activity.workoutState == WorkoutState.ACTIVE) onPause() },
                onStop = onStop
            )

            WatchScreen.SUMMARY -> WatchDailySummaryScreen(
                activity = activity,
                goals = goals,
                onBack = { screen = WatchScreen.HOME }
            )
        }
    }
}

private val ClockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun currentClock(): String = LocalTime.now().format(ClockFormat)
