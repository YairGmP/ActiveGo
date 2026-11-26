package com.example.activego

/**
 * ActiveGo - Communication contract shared by the `mobile` and `wear` modules.
 *
 * This file is intentionally duplicated (byte for byte) in both modules so that
 * neither side can drift from the other. Every path and every DataMap key used by
 * the Wearable APIs lives here and nowhere else: if a string is not in this file,
 * it must not appear in a sendMessage() or putDataItem() call.
 *
 * Two transports are used, each for the kind of payload it is designed for:
 *
 *  - MessageClient -> ephemeral EVENTS (start / pause / resume / stop a workout).
 *    Fire-and-forget, delivered only while both nodes are connected. Perfect for a
 *    button press that has no meaning once the moment has passed.
 *
 *  - DataClient    -> durable STATE (today's activity totals, the user's goals).
 *    The item is persisted in a replicated data store, survives a disconnection and
 *    is delivered automatically when the peer comes back. This is what implements
 *    FR-05 of the ActiveGo Functional Requirements Document.
 */

// ---------------------------------------------------------------------------
// FEATURE 1 - Workout Control Sync (MessageClient)
// ---------------------------------------------------------------------------

/** Bidirectional path for workout control events: watch <-> phone. */
const val MESSAGE_PATH_WORKOUT_CONTROL = "/activego/workout/control"

/** Payload verbs. START is the only one that carries an argument. */
const val CMD_START = "START"
const val CMD_PAUSE = "PAUSE"
const val CMD_RESUME = "RESUME"
const val CMD_STOP = "STOP"

/** Separator between the verb and its argument, e.g. "START:RUNNING". */
const val CMD_SEPARATOR = ":"

// ---------------------------------------------------------------------------
// FEATURE 2 - Activity Data Sync (DataClient)
// ---------------------------------------------------------------------------

/** Watch -> phone. Today's accumulated activity plus the live workout state. */
const val DATA_PATH_ACTIVITY_TODAY = "/activego/activity/today"

/** Phone -> watch. The goals the user configured on the phone (FR-06). */
const val DATA_PATH_GOALS = "/activego/goals"

// DataMap keys for DATA_PATH_ACTIVITY_TODAY
const val KEY_STEPS = "steps"
const val KEY_CALORIES = "calories"
const val KEY_ACTIVE_MINUTES = "active_minutes"
const val KEY_HEART_RATE = "heart_rate"
const val KEY_WORKOUT_TYPE = "workout_type"
const val KEY_WORKOUT_STATE = "workout_state"
const val KEY_ELAPSED_SECONDS = "elapsed_seconds"
const val KEY_WORKOUT_CALORIES = "workout_calories"
const val KEY_DISTANCE_METERS = "distance_meters"
const val KEY_WATCH_BATTERY = "watch_battery"
/** Identifies one workout session (watch clock, ms). Lets the phone record each session exactly once. */
const val KEY_SESSION_ID = "session_id"

// DataMap keys for DATA_PATH_GOALS
const val KEY_GOAL_STEPS = "goal_steps"
const val KEY_GOAL_CALORIES = "goal_calories"
const val KEY_GOAL_ACTIVE_MINUTES = "goal_active_minutes"

/**
 * Present in EVERY DataMap written by this app, and it is not decorative.
 *
 * DataClient de-duplicates: if a new DataMap is byte-for-byte identical to the one
 * already stored at the same path, the item is considered unchanged and
 * onDataChanged() never fires on the peer. Stamping each write with the current
 * time guarantees that every update is seen as a real change and propagates.
 */
const val KEY_UPDATED_AT = "updated_at"

// ---------------------------------------------------------------------------
// Team (UTRM - Smart Devices Development)
// ---------------------------------------------------------------------------

const val TEAM_MEMBER_ONE = "Yair Gamaliel Guzman Perez"
const val TEAM_MEMBER_TWO = "Adriel Solano"
const val TEAM_LABEL = "Guzman Perez / Solano - TI-91"
