package com.example.activego.data

import android.util.Log
import com.example.activego.ActivitySnapshot
import com.example.activego.CMD_PAUSE
import com.example.activego.CMD_RESUME
import com.example.activego.CMD_START
import com.example.activego.CMD_STOP
import com.example.activego.DATA_PATH_ACTIVITY_TODAY
import com.example.activego.KEY_ACTIVE_MINUTES
import com.example.activego.KEY_CALORIES
import com.example.activego.KEY_DISTANCE_METERS
import com.example.activego.KEY_ELAPSED_SECONDS
import com.example.activego.KEY_HEART_RATE
import com.example.activego.KEY_SESSION_ID
import com.example.activego.KEY_STEPS
import com.example.activego.KEY_UPDATED_AT
import com.example.activego.KEY_WATCH_BATTERY
import com.example.activego.KEY_WORKOUT_CALORIES
import com.example.activego.KEY_WORKOUT_STATE
import com.example.activego.KEY_WORKOUT_TYPE
import com.example.activego.MESSAGE_PATH_WORKOUT_CONTROL
import com.example.activego.WorkoutCommand
import com.example.activego.WorkoutState
import com.example.activego.WorkoutType
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receiving half of both features on the phone.
 *
 * Google Play services delivers here even when the phone app is closed, so the
 * dashboard is already up to date the moment the user opens it.
 */
class PhoneListenerService : WearableListenerService() {

    private val tag = "ActiveGoPhoneSvc"

    // -----------------------------------------------------------------------
    // FEATURE 1 - workout control arriving FROM the watch (MessageClient)
    // -----------------------------------------------------------------------
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != MESSAGE_PATH_WORKOUT_CONTROL) {
            super.onMessageReceived(messageEvent)
            return
        }

        val command = WorkoutCommand.fromBytes(messageEvent.data)
        Log.d(tag, "Command from watch: " + command.encode())

        when (command.action) {
            CMD_START -> PhoneRepository.startWorkout(command.workoutType ?: WorkoutType.RUNNING)
            CMD_PAUSE -> PhoneRepository.pauseWorkout()
            CMD_RESUME -> PhoneRepository.resumeWorkout()
            CMD_STOP -> PhoneRepository.stopWorkout()
            else -> Log.w(tag, "Unknown command: " + command.action)
        }
    }

    // -----------------------------------------------------------------------
    // FEATURE 2 - today's activity arriving FROM the watch (DataClient)
    // -----------------------------------------------------------------------
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            if (item.uri.path != DATA_PATH_ACTIVITY_TODAY) return@forEach

            val map = DataMapItem.fromDataItem(item).dataMap
            val snapshot = ActivitySnapshot(
                steps = map.getInt(KEY_STEPS, 0),
                calories = map.getInt(KEY_CALORIES, 0),
                activeMinutes = map.getInt(KEY_ACTIVE_MINUTES, 0),
                heartRate = map.getInt(KEY_HEART_RATE, 0),
                workoutType = WorkoutType.fromName(map.getString(KEY_WORKOUT_TYPE)),
                workoutState = WorkoutState.fromName(map.getString(KEY_WORKOUT_STATE)),
                elapsedSeconds = map.getInt(KEY_ELAPSED_SECONDS, 0),
                workoutCalories = map.getInt(KEY_WORKOUT_CALORIES, 0),
                distanceMeters = map.getInt(KEY_DISTANCE_METERS, 0),
                watchBattery = map.getInt(KEY_WATCH_BATTERY, 0),
                sessionId = map.getLong(KEY_SESSION_ID, 0L),
                updatedAt = map.getLong(KEY_UPDATED_AT, 0L)
            )

            Log.d(tag, "Snapshot from watch: steps=" + snapshot.steps + " state=" + snapshot.workoutState)
            PhoneRepository.applyActivitySnapshot(snapshot)
        }
        dataEvents.release()
    }
}
