package com.example.activego.data

import com.example.activego.WorkoutState
import com.example.activego.WorkoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Stand-in for the physical sensors described in section 3.1 of the ActiveGo
 * Functional Requirements Document (accelerometer, heart rate monitor, GPS).
 *
 * The Wear OS emulator exposes no real sensors, and this practice is an exercise on
 * the Wearable communication APIs rather than on sensor acquisition, so the values
 * are generated locally. Everything downstream of this class - the DataClient
 * payload, the phone's statistics, the goal progress - behaves exactly as it would
 * with real readings, because the rest of the app only ever sees an ActivitySnapshot.
 *
 * Replacing this object with Health Services is the natural next step and requires
 * no change anywhere else in the project.
 */
object MetricsSimulator {

    private const val TICK_MS = 1_000L

    /** Resting heart rate used while no workout is running. */
    private const val RESTING_HR = 72

    private var started = false

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        scope.launch {
            while (true) {
                delay(TICK_MS)
                val snapshot = WearRepository.activity.value

                when (snapshot.workoutState) {
                    WorkoutState.ACTIVE -> {
                        val type = snapshot.workoutType
                        val moving = type.metersPerSecond > 0.0
                        WearRepository.applySensorTick(
                            stepsDelta = when (type) {
                                WorkoutType.RUNNING -> Random.nextInt(2, 4)
                                WorkoutType.WALKING -> Random.nextInt(1, 3)
                                WorkoutType.CYCLING -> 0
                                else -> if (Random.nextInt(100) < 30) 1 else 0
                            },
                            caloriesDelta = if (Random.nextInt(100) < 20) 1 else 0,
                            heartRate = if (moving) Random.nextInt(138, 166) else Random.nextInt(105, 132),
                            metersDelta = type.metersPerSecond * Random.nextDouble(0.85, 1.15),
                            workoutRunning = true
                        )
                    }

                    WorkoutState.PAUSED -> {
                        // Paused: the timer freezes, the heart rate recovers.
                        WearRepository.applySensorTick(
                            stepsDelta = 0,
                            caloriesDelta = 0,
                            heartRate = Random.nextInt(96, 118),
                            metersDelta = 0.0,
                            workoutRunning = false
                        )
                    }

                    WorkoutState.IDLE -> {
                        // Background daily tracking: occasional steps, resting rate.
                        WearRepository.applySensorTick(
                            stepsDelta = if (Random.nextInt(100) < 35) Random.nextInt(1, 3) else 0,
                            caloriesDelta = if (Random.nextInt(100) < 5) 1 else 0,
                            heartRate = RESTING_HR + Random.nextInt(-4, 5),
                            metersDelta = 0.0,
                            workoutRunning = false
                        )
                    }
                }
            }
        }
    }
}
