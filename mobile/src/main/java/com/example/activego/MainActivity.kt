package com.example.activego

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.activego.data.PhoneRepository
import com.example.activego.ui.ActiveWorkoutScreen
import com.example.activego.ui.BottomNavigation
import com.example.activego.ui.FitnessGoalsScreen
import com.example.activego.ui.HomeDashboardScreen
import com.example.activego.ui.PhoneTab
import com.example.activego.ui.SettingsScreen
import com.example.activego.ui.StartWorkoutScreen
import com.example.activego.ui.WorkoutHistoryScreen
import com.example.activego.ui.theme.ActiveGoColors
import com.example.activego.ui.theme.ActiveGoTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ActiveGo - phone application (the central hub of section 3.2 of the FRD).
 *
 * Like its counterpart on the watch, this Activity owns the SENDING half of both
 * features while PhoneListenerService owns the receiving half.
 *
 *  Feature 1 (MessageClient): [sendWorkoutCommand] mirrors the watch logic in the
 *  opposite direction, so a workout can be driven from either device.
 *
 *  Feature 2 (DataClient): [publishGoals] writes the goals the user edited here, and
 *  the watch picks them up through its own listener service.
 */
class MainActivity : ComponentActivity() {

    private val tag = "ActiveGoPhone"

    private lateinit var messageClient: MessageClient
    private lateinit var dataClient: DataClient
    private lateinit var nodeClient: NodeClient

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark, transparent system bars so the Figma background runs edge to edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        messageClient = Wearable.getMessageClient(this)
        dataClient = Wearable.getDataClient(this)
        nodeClient = Wearable.getNodeClient(this)

        observeWatchConnection()

        setContent {
            ActiveGoTheme {
                PhoneApp(
                    onStart = { type -> sendWorkoutCommand(CMD_START, type) },
                    onPause = { sendWorkoutCommand(CMD_PAUSE) },
                    onResume = { sendWorkoutCommand(CMD_RESUME) },
                    onStop = { sendWorkoutCommand(CMD_STOP) },
                    onSaveGoals = { goals -> publishGoals(goals) }
                )
            }
        }
    }

    // =======================================================================
    // FEATURE 1 - Workout Control Sync (MessageClient)
    // =======================================================================

    /**
     * Mirror image of the watch implementation: update local state, then broadcast
     * the event to every connected node on the shared control path.
     */
    private fun sendWorkoutCommand(action: String, type: WorkoutType? = null) {
        when (action) {
            CMD_START -> PhoneRepository.startWorkout(type ?: WorkoutType.RUNNING)
            CMD_PAUSE -> PhoneRepository.pauseWorkout()
            CMD_RESUME -> PhoneRepository.resumeWorkout()
            CMD_STOP -> PhoneRepository.stopWorkout()
        }

        val command = WorkoutCommand(action, type)
        lifecycleScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                PhoneRepository.setWatchConnected(nodes.isNotEmpty())
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
    }

    // =======================================================================
    // FEATURE 2 - Goal Sync (DataClient)
    // =======================================================================

    /**
     * Stores the goals locally and publishes them so the watch can show progress
     * against them.
     *
     * The phone is the only writer of this path, which is what section 4.1 of the FRD
     * means by moving all complex configuration to the phone: the watch only reads.
     */
    private fun publishGoals(goals: Goals) {
        PhoneRepository.setGoals(goals)

        val request = PutDataMapRequest.create(DATA_PATH_GOALS).apply {
            dataMap.putInt(KEY_GOAL_STEPS, goals.steps)
            dataMap.putInt(KEY_GOAL_CALORIES, goals.calories)
            dataMap.putInt(KEY_GOAL_ACTIVE_MINUTES, goals.activeMinutes)
            // Same reason as on the watch: without a fresh timestamp an identical
            // DataMap is discarded and the watch is never notified.
            dataMap.putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        lifecycleScope.launch {
            try {
                dataClient.putDataItem(request).await()
                Log.d(tag, "Goals sent: " + goals)
            } catch (e: Exception) {
                Log.e(tag, "publishGoals failed", e)
            }
        }
    }

    /** Polls NodeClient so the Profile and live screens can show the watch link. */
    private fun observeWatchConnection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    try {
                        val nodes = nodeClient.connectedNodes.await()
                        PhoneRepository.setWatchConnected(nodes.isNotEmpty())
                    } catch (e: Exception) {
                        PhoneRepository.setWatchConnected(false)
                    }
                    delay(5_000L)
                }
            }
        }
    }
}

@Composable
private fun PhoneApp(
    onStart: (WorkoutType) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSaveGoals: (Goals) -> Unit
) {
    val activity by PhoneRepository.activity.collectAsState()
    val goals by PhoneRepository.goals.collectAsState()
    val history by PhoneRepository.history.collectAsState()
    val heartRates by PhoneRepository.heartRates.collectAsState()
    val watchConnected by PhoneRepository.watchConnected.collectAsState()
    val lastSyncAt by PhoneRepository.lastSyncAt.collectAsState()
    val activityAt by PhoneRepository.activityAt.collectAsState()

    var tab by remember { mutableStateOf(PhoneTab.HOME) }
    var selectedType by remember { mutableStateOf(WorkoutType.RUNNING) }

    // "Start Session" only opens the READY frame; play sends START (same as the watch).
    var phoneReady by remember { mutableStateOf(false) }

    // A workout started on the watch pulls the phone to the live screen by itself:
    // the cross-device state change the practice asks to demonstrate.
    val isLive = activity.workoutState != WorkoutState.IDLE
    LaunchedEffect(isLive) {
        if (isLive) {
            tab = PhoneTab.WORKOUT
            phoneReady = false
        }
    }

    // The watch reports the elapsed time every few seconds; between two reports the
    // phone keeps the timer moving on its own clock (at most 5 s ahead), so the big
    // counter ticks every second like the watch instead of jumping.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activity.workoutState) {
        while (activity.workoutState == WorkoutState.ACTIVE) {
            now = System.currentTimeMillis()
            delay(250L)
        }
    }
    val liveActivity = if (activity.workoutState == WorkoutState.ACTIVE) {
        val ahead = ((now - activityAt) / 1000L).toInt().coerceIn(0, 5)
        activity.copy(elapsedSeconds = activity.elapsedSeconds + ahead)
    } else {
        activity
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ActiveGoColors.Background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            when (tab) {
                PhoneTab.HOME -> HomeDashboardScreen(
                    activity = activity,
                    goals = goals,
                    heartRates = heartRates,
                    onQuickStart = { tab = PhoneTab.WORKOUT }
                )

                // The Workout tab shows the live session while a workout runs on
                // either device, the READY frame after "Start Session", and the
                // picker otherwise (Figma 3:1133 / 3:1065).
                PhoneTab.WORKOUT -> when {
                    activity.workoutState != WorkoutState.IDLE -> ActiveWorkoutScreen(
                        activity = liveActivity,
                        ready = false,
                        watchConnected = watchConnected,
                        onPlay = { if (activity.workoutState == WorkoutState.PAUSED) onResume() },
                        onPause = { if (activity.workoutState == WorkoutState.ACTIVE) onPause() },
                        onStop = onStop
                    )

                    phoneReady -> ActiveWorkoutScreen(
                        activity = ActivitySnapshot(
                            heartRate = activity.heartRate,
                            workoutType = selectedType
                        ),
                        ready = true,
                        watchConnected = watchConnected,
                        onPlay = {
                            phoneReady = false
                            onStart(selectedType)
                        },
                        onPause = {},
                        onStop = { phoneReady = false }
                    )

                    else -> StartWorkoutScreen(
                        selected = selectedType,
                        onSelect = { selectedType = it },
                        onStart = { phoneReady = true }
                    )
                }

                PhoneTab.HISTORY -> WorkoutHistoryScreen(history = history)

                PhoneTab.GOALS -> FitnessGoalsScreen(
                    activity = activity,
                    goals = goals,
                    onSaveGoals = onSaveGoals
                )

                PhoneTab.PROFILE -> SettingsScreen(
                    watchConnected = watchConnected,
                    watchBattery = activity.watchBattery,
                    lastSyncAt = lastSyncAt
                )
            }
        }

        BottomNavigation(current = tab, onSelect = { tab = it })
    }
}
