package com.nitroorientation

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

class NitroOrientationActivityLifecycle private constructor() : ActivityLifecycleCallbacks {
    private var orientationListeners: NitroOrientationListeners? = null
    fun registerListeners(listener: NitroOrientationListeners) {
        orientationListeners = listener
        if (activeCount.get() == 1) {
            orientationListeners!!.start()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed")
        if (activeCount.incrementAndGet() == 1) {
            if (orientationListeners != null) {
                Log.d(TAG, "Start orientation")
                orientationListeners!!.start()
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "onActivityPaused")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(TAG, "onActivityStopped")
        if (activeCount.decrementAndGet() == 0) {
            if (orientationListeners != null) {
                orientationListeners!!.stop()
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.d(TAG, "onActivitySaveInstanceState")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(TAG, "onActivityDestroyed")
        if (activeCount.get() == 0) {
            if (orientationListeners != null) {
                orientationListeners!!.release()
            }
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
