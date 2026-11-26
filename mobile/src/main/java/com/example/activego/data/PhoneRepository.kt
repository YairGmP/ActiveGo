package com.example.activego.data

import com.example.activego.ActivitySnapshot
import com.example.activego.Goals
import com.example.activego.WorkoutRecord
import com.example.activego.WorkoutState
import com.example.activego.WorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the phone app.
 *
 * Per section 3.2 of the ActiveGo Functional Requirements Document the phone is the
 * central hub: it RECEIVES the activity snapshot produced by the watch, builds the
 * workout history from it (FR-07), and OWNS the goals, which are the only thing it
 * writes back. In a production build this object would be backed by Room; for this
 * practice the state lives in memory, which is enough to demonstrate the data flow.
 */
object PhoneRepository {

    private val _activity = MutableStateFlow(ActivitySnapshot())
    val activity: StateFlow<ActivitySnapshot> = _activity.asStateFlow()

    /** Phone-clock time at which [activity] last changed; drives the live timer. */
    private val _activityAt = MutableStateFlow(System.currentTimeMillis())
    val activityAt: StateFlow<Long> = _activityAt.asStateFlow()

    private val _goals = MutableStateFlow(Goals())
    val goals: StateFlow<Goals> = _goals.asStateFlow()

    private val _history = MutableStateFlow<List<WorkoutRecord>>(emptyList())
    val history: StateFlow<List<WorkoutRecord>> = _history.asStateFlow()

    /** Last heart-rate samples, oldest first, for the dashboard sparkline. */
    private val _heartRates = MutableStateFlow<List<Int>>(emptyList())
    val heartRates: StateFlow<List<Int>> = _heartRates.asStateFlow()

    private val _watchConnected = MutableStateFlow(false)
    val watchConnected: StateFlow<Boolean> = _watchConnected.asStateFlow()

    /** Phone-clock time of the last snapshot received from the watch (0 = never). */
    private val _lastSyncAt = MutableStateFlow(0L)
    val lastSyncAt: StateFlow<Long> = _lastSyncAt.asStateFlow()

    /** Sessions already written to the history, so none is recorded twice. */
    private val recordedSessions = mutableSetOf<Long>()

    /** Latest snapshot of the running session, used to write its history entry. */
    private var lastLiveSnapshot: ActivitySnapshot? = null

    private const val HEART_RATE_SAMPLES = 10

    // -----------------------------------------------------------------------
    // Pending command
    //
    // When the phone issues a command it updates its own state at once
    // (optimistic update). Until the watch confirms it, a periodic snapshot that
    // the watch sent BEFORE receiving the command can still arrive; applying it
    // would undo the change and throw the user back to the previous screen.
    // While a command is pending, such snapshots only refresh the daily totals.
    // -----------------------------------------------------------------------
    private enum class Pending { START, PAUSE, RESUME, STOP }

    private var pending: Pending? = null
    private var pendingSince = 0L
    private var pendingFromSession = 0L

    /** After this long without confirmation the watch's state wins again. */
    private const val PENDING_WINDOW_MS = 8_000L

    private fun markPending(command: Pending) {
        pending = command
        pendingSince = System.currentTimeMillis()
        pendingFromSession = _activity.value.sessionId
    }

    private fun confirms(command: Pending, incoming: ActivitySnapshot): Boolean = when (command) {
        Pending.START -> incoming.workoutState != WorkoutState.IDLE &&
            incoming.sessionId != pendingFromSession
        Pending.PAUSE -> incoming.workoutState == WorkoutState.PAUSED
        Pending.RESUME -> incoming.workoutState == WorkoutState.ACTIVE
        Pending.STOP -> incoming.workoutState == WorkoutState.IDLE
    }

    private fun setActivity(snapshot: ActivitySnapshot) {
        _activity.value = snapshot
        _activityAt.value = System.currentTimeMillis()
    }

    fun setWatchConnected(connected: Boolean) {
        _watchConnected.value = connected
    }

    fun setGoals(goals: Goals) {
        _goals.value = goals.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * Applied when a new data item arrives from the watch over DataClient.
     * The watch is the only writer of activity data, so once no command is pending
     * its snapshot always wins over the phone's optimistic state.
     */
    fun applyActivitySnapshot(incoming: ActivitySnapshot) {
        val now = System.currentTimeMillis()
        _lastSyncAt.value = now

        if (incoming.heartRate > 0) {
            _heartRates.value = (_heartRates.value + incoming.heartRate).takeLast(HEART_RATE_SAMPLES)
        }

        val command = pending
        if (command != null) {
            if (confirms(command, incoming) || now - pendingSince > PENDING_WINDOW_MS) {
                pending = null
            } else {
                // Stale snapshot: keep the workout as the phone set it, refresh the rest.
                val current = _activity.value
                _activity.value = incoming.copy(
                    workoutType = current.workoutType,
                    workoutState = current.workoutState,
                    elapsedSeconds = current.elapsedSeconds,
                    workoutCalories = current.workoutCalories,
                    distanceMeters = current.distanceMeters,
                    sessionId = current.sessionId
                )
                return
            }
        }

        var snapshot = incoming
        val sessionAlreadyClosed = incoming.sessionId in recordedSessions

        if (incoming.workoutState != WorkoutState.IDLE) {
            if (sessionAlreadyClosed) {
                // A late copy of a session that was already stopped: keep it stopped.
                snapshot = incoming.copy(workoutState = WorkoutState.IDLE, elapsedSeconds = 0)
            } else {
                lastLiveSnapshot = incoming
            }
        } else {
            closeSession()
        }
        setActivity(snapshot)
    }

    // -----------------------------------------------------------------------
    // Optimistic updates when a command is issued (from this phone, or relayed
    // from the watch over MessageClient). The next snapshot confirms them.
    // -----------------------------------------------------------------------

    fun startWorkout(type: WorkoutType) {
        markPending(Pending.START)
        setActivity(
            _activity.value.copy(
                workoutType = type,
                workoutState = WorkoutState.ACTIVE,
                elapsedSeconds = 0,
                workoutCalories = 0,
                distanceMeters = 0
            )
        )
    }

    fun pauseWorkout() {
        if (_activity.value.workoutState != WorkoutState.ACTIVE) return
        markPending(Pending.PAUSE)
        setActivity(_activity.value.copy(workoutState = WorkoutState.PAUSED))
    }

    fun resumeWorkout() {
        if (_activity.value.workoutState != WorkoutState.PAUSED) return
        markPending(Pending.RESUME)
        setActivity(_activity.value.copy(workoutState = WorkoutState.ACTIVE))
    }

    fun stopWorkout() {
        markPending(Pending.STOP)
        closeSession()
        setActivity(
            _activity.value.copy(
                workoutState = WorkoutState.IDLE,
                elapsedSeconds = 0,
                workoutCalories = 0,
                distanceMeters = 0
            )
        )
    }

    /** Writes the running session to the history exactly once (FR-07). */
    private fun closeSession() {
        val live = lastLiveSnapshot ?: return
        lastLiveSnapshot = null
        if (live.sessionId == 0L || !recordedSessions.add(live.sessionId)) return

        val record = WorkoutRecord(
            sessionId = live.sessionId,
            type = live.workoutType,
            startedAt = System.currentTimeMillis() - live.elapsedSeconds * 1000L,
            durationSeconds = live.elapsedSeconds,
            calories = live.workoutCalories
        )
        _history.value = listOf(record) + _history.value
    }
}
