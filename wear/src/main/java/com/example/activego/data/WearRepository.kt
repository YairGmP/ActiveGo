package com.example.activego.data

import com.example.activego.ActivitySnapshot
import com.example.activego.Goals
import com.example.activego.WorkoutState
import com.example.activego.WorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the watch app, held in memory for the lifetime of the
 * process. Both the UI (MainActivity) and the background receiver
 * (WearListenerService) run in the same process, so a plain object is enough to
 * share state between them without a database or a bound service.
 *
 * The watch OWNS the activity snapshot (it is the device with the sensors) and only
 * CONSUMES the goals, which are authored on the phone.
 */
object WearRepository {

    /**
     * The simulated day starts in progress, like the Figma mock-ups (around 7,800
     * steps, 470 kcal and 26 active minutes), so the rings and bars show realistic
     * proportions from the first launch. From here on every value grows only from
     * the simulated sensor ticks.
     */
    private const val BASELINE_STEPS = 7_800
    private const val BASELINE_CALORIES = 470
    private const val BASELINE_ACTIVE_MINUTES = 26

    private val _activity = MutableStateFlow(
        ActivitySnapshot(
            steps = BASELINE_STEPS,
            calories = BASELINE_CALORIES,
            activeMinutes = BASELINE_ACTIVE_MINUTES,
            heartRate = 72
        )
    )
    val activity: StateFlow<ActivitySnapshot> = _activity.asStateFlow()

    private val _goals = MutableStateFlow(Goals())
    val goals: StateFlow<Goals> = _goals.asStateFlow()

    /** True while NodeClient reports at least one connected phone. */
    private val _phoneConnected = MutableStateFlow(false)
    val phoneConnected: StateFlow<Boolean> = _phoneConnected.asStateFlow()

    /** Backing counter so active minutes advance one second at a time. */
    private var activeSeconds: Int = BASELINE_ACTIVE_MINUTES * 60

    /** Sub-meter remainder so slow disciplines still accumulate distance. */
    private var distanceRemainder: Double = 0.0

    fun setPhoneConnected(connected: Boolean) {
        _phoneConnected.value = connected
    }

    fun setBattery(percent: Int) {
        if (_activity.value.watchBattery != percent) {
            _activity.value = _activity.value.copy(watchBattery = percent)
        }
    }

    /** Applied when the phone pushes new goals over DataClient. */
    fun applyGoals(goals: Goals) {
        _goals.value = goals
    }

    // -----------------------------------------------------------------------
    // Workout lifecycle. Called both by the local UI and by commands arriving
    // from the phone, so the two devices converge on the same state.
    // -----------------------------------------------------------------------

    fun startWorkout(type: WorkoutType) {
        val now = System.currentTimeMillis()
        distanceRemainder = 0.0
        _activity.value = _activity.value.copy(
            workoutType = type,
            workoutState = WorkoutState.ACTIVE,
            elapsedSeconds = 0,
            workoutCalories = 0,
            distanceMeters = 0,
            sessionId = now,
            updatedAt = now
        )
    }

    fun pauseWorkout() {
        if (_activity.value.workoutState != WorkoutState.ACTIVE) return
        _activity.value = _activity.value.copy(
            workoutState = WorkoutState.PAUSED,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun resumeWorkout() {
        if (_activity.value.workoutState != WorkoutState.PAUSED) return
        _activity.value = _activity.value.copy(
            workoutState = WorkoutState.ACTIVE,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Ends the session. The session id is kept so the phone can tell which session
     * just finished; the workout counters are reset for the next one.
     */
    fun stopWorkout() {
        if (_activity.value.workoutState == WorkoutState.IDLE) return
        _activity.value = _activity.value.copy(
            workoutState = WorkoutState.IDLE,
            elapsedSeconds = 0,
            workoutCalories = 0,
            distanceMeters = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    /** Called once per second by [MetricsSimulator] to advance the sensor values. */
    fun applySensorTick(
        stepsDelta: Int,
        caloriesDelta: Int,
        heartRate: Int,
        metersDelta: Double,
        workoutRunning: Boolean
    ) {
        val current = _activity.value
        if (workoutRunning) activeSeconds += 1

        distanceRemainder += metersDelta
        val wholeMeters = distanceRemainder.toInt()
        distanceRemainder -= wholeMeters

        _activity.value = current.copy(
            steps = current.steps + stepsDelta,
            calories = current.calories + caloriesDelta,
            activeMinutes = activeSeconds / 60,
            heartRate = heartRate,
            elapsedSeconds = current.elapsedSeconds + if (workoutRunning) 1 else 0,
            workoutCalories = current.workoutCalories + if (workoutRunning) caloriesDelta else 0,
            distanceMeters = current.distanceMeters + if (workoutRunning) wholeMeters else 0,
            updatedAt = System.currentTimeMillis()
        )
    }
}
