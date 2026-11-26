package com.example.activego

import android.app.Application
import com.example.activego.data.MetricsSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Starts the sensor simulation once, in a scope tied to the process rather than to
 * any Activity, so the watch keeps accumulating activity while the screen is off or
 * the app is in the background - the behaviour the FRD expects from a wearable.
 */
class ActiveGoWearApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        MetricsSimulator.start(applicationScope)
    }
}
