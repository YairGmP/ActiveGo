package com.example.activego.data

import com.example.activego.ActivitySnapshot
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
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest

/**
 * Encodes an [ActivitySnapshot] as the DataClient request for
 * DATA_PATH_ACTIVITY_TODAY. Shared by MainActivity (periodic sync) and
 * WearListenerService (immediate sync after a command from the phone), so the
 * wire format is written in exactly one place.
 *
 *  - KEY_UPDATED_AT is always stamped, because DataClient drops a write whose
 *    DataMap is identical to the stored one and the peer would never be notified.
 *  - setUrgent() asks Play services to sync now instead of batching.
 */
object ActivityPayload {

    fun toRequest(snapshot: ActivitySnapshot): PutDataRequest =
        PutDataMapRequest.create(DATA_PATH_ACTIVITY_TODAY).apply {
            dataMap.putInt(KEY_STEPS, snapshot.steps)
            dataMap.putInt(KEY_CALORIES, snapshot.calories)
            dataMap.putInt(KEY_ACTIVE_MINUTES, snapshot.activeMinutes)
            dataMap.putInt(KEY_HEART_RATE, snapshot.heartRate)
            dataMap.putInt(KEY_ELAPSED_SECONDS, snapshot.elapsedSeconds)
            dataMap.putInt(KEY_WORKOUT_CALORIES, snapshot.workoutCalories)
            dataMap.putInt(KEY_DISTANCE_METERS, snapshot.distanceMeters)
            dataMap.putInt(KEY_WATCH_BATTERY, snapshot.watchBattery)
            dataMap.putLong(KEY_SESSION_ID, snapshot.sessionId)
            dataMap.putString(KEY_WORKOUT_TYPE, snapshot.workoutType.name)
            dataMap.putString(KEY_WORKOUT_STATE, snapshot.workoutState.name)
            dataMap.putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
}
