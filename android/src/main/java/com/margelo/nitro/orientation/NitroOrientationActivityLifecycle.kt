package com.margelo.nitro.orientation

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

class NitroOrientationActivityLifecycle private constructor() : ActivityLifecycleCallbacks {
    private var orientationListeners: NitroOrientationListeners? = null
    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
    fun registerListeners(listener: NitroOrientationListeners) {
        orientationListeners = listener
        if (activeCount.get() == 1) {
            orientationListeners?.start()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        debugLog("onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        debugLog("onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        debugLog("onActivityResumed")
        if (activeCount.incrementAndGet() == 1) {
            debugLog("Start orientation")
            orientationListeners?.start()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        debugLog("onActivityPaused")
    }

    override fun onActivityStopped(activity: Activity) {
        debugLog("onActivityStopped")
        if (activeCount.decrementAndGet() == 0) {
            orientationListeners?.stop()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        debugLog("onActivitySaveInstanceState")
    }

    override fun onActivityDestroyed(activity: Activity) {
        debugLog("onActivityDestroyed")
        if (activeCount.get() == 0) {
            orientationListeners?.release()
        }
    }

    companion object {
        private const val TAG = "NitroOrientationModule"
        private val activeCount = AtomicInteger(0)
        var instance: NitroOrientationActivityLifecycle? = null
            get() {
                if (field == null) {
                    field = NitroOrientationActivityLifecycle()
                }
                return field
            }
            private set
    }
}