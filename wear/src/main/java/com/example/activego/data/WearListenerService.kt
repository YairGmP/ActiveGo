package com.example.activego.data

import android.util.Log
import com.example.activego.CMD_PAUSE
import com.example.activego.CMD_RESUME
import com.example.activego.CMD_START
import com.example.activego.CMD_STOP
import com.example.activego.DATA_PATH_GOALS
import com.example.activego.Goals
import com.example.activego.KEY_GOAL_ACTIVE_MINUTES
import com.example.activego.KEY_GOAL_CALORIES
import com.example.activego.KEY_GOAL_STEPS
import com.example.activego.KEY_UPDATED_AT
import com.example.activego.MESSAGE_PATH_WORKOUT_CONTROL
import com.example.activego.WorkoutCommand
import com.example.activego.WorkoutType
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receiving half of both features on the watch.
 *
 * A WearableListenerService is used instead of registering listeners inside the
 * Activity because Google Play services can start it even when the watch app is not
 * in the foreground. That is what makes the offline scenario of FR-05 work: data
 * items that piled up while the devices were apart are delivered as soon as the
 * connection returns, with no screen open.
 */
class WearListenerService : WearableListenerService() {

    private val tag = "ActiveGoWatchSvc"

    // -----------------------------------------------------------------------
    // FEATURE 1 - workout control arriving FROM the phone (MessageClient)
    // -----------------------------------------------------------------------
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != MESSAGE_PATH_WORKOUT_CONTROL) {
            super.onMessageReceived(messageEvent)
            return
        }

        val command = WorkoutCommand.fromBytes(messageEvent.data)
        Log.d(tag, "Command from phone: " + command.encode())

        when (command.action) {
            CMD_START -> {
                WearRepository.startWorkout(command.workoutType ?: WorkoutType.RUNNING)
                Haptics.buzz(this, Haptics.MEDIUM)
            }

            CMD_PAUSE -> {
                WearRepository.pauseWorkout()
                Haptics.buzz(this, Haptics.SHORT)
            }

            CMD_RESUME -> {
                WearRepository.resumeWorkout()
                Haptics.buzz(this, Haptics.SHORT)
            }

            CMD_STOP -> {
                WearRepository.stopWorkout()
                Haptics.buzz(this, Haptics.LONG)
            }

            else -> Log.w(tag, "Unknown command: " + command.action)
        }

        // Echo the new state straight back over DataClient, so the phone's live
        // screen confirms the change without waiting for the next periodic sync.
        Wearable.getDataClient(this).putDataItem(
            ActivityPayload.toRequest(WearRepository.activity.value)
        )
    }

    // -----------------------------------------------------------------------
    // FEATURE 2 - goals arriving FROM the phone (DataClient)
    // -----------------------------------------------------------------------
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            if (item.uri.path != DATA_PATH_GOALS) return@forEach

            val map = DataMapItem.fromDataItem(item).dataMap
            val goals = Goals(
                steps = map.getInt(KEY_GOAL_STEPS, 10_000),
                calories = map.getInt(KEY_GOAL_CALORIES, 750),
                activeMinutes = map.getInt(KEY_GOAL_ACTIVE_MINUTES, 60),
                updatedAt = map.getLong(KEY_UPDATED_AT, 0L)
            )

            Log.d(tag, "Goals from phone: " + goals)
            WearRepository.applyGoals(goals)
            Haptics.buzz(this, Haptics.SHORT)
        }
        dataEvents.release()
    }
}
