package com.example.activego

/**
 * ActiveGo - Data model shared by the `mobile` and `wear` modules.
 * Duplicated byte for byte in both modules, exactly like WearableConstants.kt.
 *
 * Everything that travels over the Wearable APIs is encoded from, and decoded into,
 * one of these types. Keeping the (de)serialization next to the model means the
 * wire format is defined in a single place.
 */

/**
 * Workout disciplines. The labels come straight from the Figma file:
 *  - [cardLabel]    phone `start-workout` grid and the watch `RUNNING` badge
 *  - [watchLabel]   watch `watch-start-workout` list
 *  - [sessionTitle] phone `workout-history` list
 * [metersPerSecond] drives the simulated distance (0 for stationary workouts).
 */
enum class WorkoutType(
    val cardLabel: String,
    val watchLabel: String,
    val sessionTitle: String,
    val metersPerSecond: Double
) {
    RUNNING("Running", "Outdoor Run", "Outdoor Run", 2.75),
    CYCLING("Cycling", "Cycling", "Outdoor Bike", 6.0),
    WALKING("Walking", "Walking", "Outdoor Walk", 1.4),
    STRENGTH("Strength", "Strength", "Strength Training", 0.0),
    YOGA("Yoga", "Yoga", "Yoga Flow", 0.0),
    CUSTOM("Custom", "Custom", "Custom Session", 0.0);

    companion object {
        fun fromName(name: String?): WorkoutType =
            entries.firstOrNull { it.name == name } ?: RUNNING
    }
}

/** Lifecycle of a workout session. Mirrored on both devices by Feature 1. */
enum class WorkoutState {
    IDLE,
    ACTIVE,
    PAUSED;

    companion object {
        fun fromName(name: String?): WorkoutState =
            entries.firstOrNull { it.name == name } ?: IDLE
    }
}

/**
 * A control event sent over MessageClient.
 *
 * Wire format: the verb, optionally followed by ":" and an argument.
 *   "START:RUNNING", "PAUSE", "RESUME", "STOP"
 *
 * The payload is deliberately a short UTF-8 string instead of a serialized object:
 * MessageClient caps the payload at 100 KB and these events must stay cheap.
 */
data class WorkoutCommand(
    val action: String,
    val workoutType: WorkoutType? = null
) {
    fun encode(): String =
        if (workoutType != null) action + CMD_SEPARATOR + workoutType.name else action

    fun toBytes(): ByteArray = encode().toByteArray(Charsets.UTF_8)

    companion object {
        fun decode(raw: String): WorkoutCommand {
            val parts = raw.split(CMD_SEPARATOR)
            val action = parts.firstOrNull().orEmpty()
            val type = parts.getOrNull(1)?.let { WorkoutType.fromName(it) }
            return WorkoutCommand(action, type)
        }

        fun fromBytes(bytes: ByteArray): WorkoutCommand =
            decode(String(bytes, Charsets.UTF_8))
    }
}

/**
 * Everything the watch measures, as of [updatedAt].
 *
 * The watch is the only device with sensors, so it is the single writer of this
 * object; the phone only ever reads it (FRD sections 3.1 and 3.2).
 *
 * Daily totals ([steps], [calories], [activeMinutes]) keep growing all day, while
 * the workout fields ([elapsedSeconds], [workoutCalories], [distanceMeters]) reset
 * at the start of every session.
 */
data class ActivitySnapshot(
    val steps: Int = 0,
    val calories: Int = 0,
    val activeMinutes: Int = 0,
    val heartRate: Int = 0,
    val workoutType: WorkoutType = WorkoutType.RUNNING,
    val workoutState: WorkoutState = WorkoutState.IDLE,
    val elapsedSeconds: Int = 0,
    val workoutCalories: Int = 0,
    val distanceMeters: Int = 0,
    val watchBattery: Int = 0,
    val sessionId: Long = 0L,
    val updatedAt: Long = 0L
) {
    /** mm:ss, used by the watch (`32:14`). */
    fun elapsedShort(): String {
        val totalMinutes = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        return "%02d:%02d".format(totalMinutes, s)
    }

    /** hh:mm:ss, used by the phone (`00:32:14`). */
    fun elapsedLong(): String {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    /** Distance in miles, the unit used by the Figma `active-workout` frame. */
    fun distanceMiles(): Double = distanceMeters / METERS_PER_MILE

    /** Pace as minutes'seconds" per mile, e.g. 9'45". */
    fun paceText(): String {
        val miles = distanceMiles()
        if (miles < 0.01 || elapsedSeconds <= 0) return "--'--\""
        val secondsPerMile = (elapsedSeconds / miles).toInt().coerceAtMost(99 * 60 + 59)
        return "%d'%02d\"".format(secondsPerMile / 60, secondsPerMile % 60)
    }

    companion object {
        const val METERS_PER_MILE = 1609.344
    }
}

/** Daily targets. Created and edited on the phone only (FR-06), read on the watch. */
data class Goals(
    val steps: Int = 10_000,
    val calories: Int = 750,
    val activeMinutes: Int = 60,
    val updatedAt: Long = 0L
)

/** One finished session, as listed on the phone's `workout-history` screen (FR-07). */
data class WorkoutRecord(
    val sessionId: Long,
    val type: WorkoutType,
    val startedAt: Long,
    val durationSeconds: Int,
    val calories: Int
)
