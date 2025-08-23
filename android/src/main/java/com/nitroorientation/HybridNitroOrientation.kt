package com.nitroorientation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.margelo.nitro.NitroModules
import com.margelo.nitro.nitroorientation.HybridNitroOrientationSpec
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class HybridNitroOrientation : HybridNitroOrientationSpec(), NitroOrientationListeners {

    private val reactContext = NitroModules.applicationContext ?: throw Exception("Context is null")

    // ---- Constants (tên event/chuỗi orientation dùng lại nhiều) ----
    private object Events {
        const val ORIENTATION_DID_CHANGE = "orientationDidChange"
        const val DEVICE_ORIENTATION_DID_CHANGE = "deviceOrientationDidChange"
        const val LOCK_DID_CHANGE = "lockDidChange"
    }
    private object Names {
        const val UNKNOWN = "unknown"
        const val PORTRAIT = "portrait"
        const val PORTRAIT_UPSIDE_DOWN = "portraitUpsideDown"
        const val LANDSCAPE_LEFT = "landscapeLeft"
        const val LANDSCAPE_RIGHT = "landscapeRight"
    }

    // ---- State ----
    private val orientationListener: OrientationEventListener
    private val configurationReceiver: BroadcastReceiver
    private var isConfigurationChangeReceiverRegistered = false
    private var isLocking = false
    private var lastUiOrientation: String = Names.UNKNOWN
    private var lastDeviceOrientation: String = Names.UNKNOWN

    // throttle để tránh spam event khi sensor rung nhanh
    private var lastDeviceEmitAtMs = 0L
    private var lastUiEmitAtMs = 0L
    private val minEmitIntervalMs = 120L

    // ---- Helpers ----
    private fun now() = SystemClock.uptimeMillis()

    private fun canEmit(lastAt: Long) = now() - lastAt >= minEmitIntervalMs

    private fun sendEvent(event: String, orientation: String) {
        if (!reactContext.hasActiveReactInstance()) return
        val params = Arguments.createMap().apply { putString("orientation", orientation) }
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(event, params)
    }

    private fun notifyUiOrientationChange(orientation: String, force: Boolean = false) {
        if (orientation == lastUiOrientation && !force) return
        if (!force && !canEmit(lastUiEmitAtMs)) return
        lastUiOrientation = orientation
        lastUiEmitAtMs = now()
        sendEvent(Events.ORIENTATION_DID_CHANGE, orientation)
    }

    private fun notifyDeviceOrientationChange(orientation: String) {
        if (orientation == lastDeviceOrientation) return
        if (!canEmit(lastDeviceEmitAtMs)) return
        lastDeviceOrientation = orientation
        lastDeviceEmitAtMs = now()
        // giữ nguyên key "deviceOrientation" để backward-compat? Trước đây bạn set "deviceOrientation" trong map khác.
        // Tuy nhiên các event trước dùng "orientation" key. Để thống nhất, tiếp tục dùng "orientation".
        sendEvent(Events.DEVICE_ORIENTATION_DID_CHANGE, orientation)
    }

    private fun notifyLockChange(orientation: String) {
        sendEvent(Events.LOCK_DID_CHANGE, orientation)
    }

    private fun lockTo(requested: Int, orientationName: String) {
        val activity = reactContext.currentActivity ?: return
        activity.requestedOrientation = requested
        isLocking = true
        // force gửi UI orientation ngay để JS đồng bộ
        notifyUiOrientationChange(orientationName, force = true)
        notifyLockChange(orientationName)
    }

    private fun safeRegisterReceiver() {
        if (isConfigurationChangeReceiverRegistered) return
        val intentFilter = IntentFilter("onConfigurationChanged")
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                reactContext.registerReceiver(configurationReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                ContextCompat.registerReceiver(
                    reactContext,
                    configurationReceiver,
                    intentFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            isConfigurationChangeReceiverRegistered = true
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun safeUnregisterReceiver() {
        if (!isConfigurationChangeReceiverRegistered) return
        try {
            reactContext.unregisterReceiver(configurationReceiver)
        } catch (_: Exception) {
            // ignore
        } finally {
            isConfigurationChangeReceiverRegistered = false
        }
    }

    // Lấy rotation an toàn theo API
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getRotation(): Int {
        // Ưu tiên activity.display trên API 30+
        reactContext.currentActivity?.display?.rotation?.let { return it }
        // Fallback WindowManager
        val wm = reactContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        return wm.defaultDisplay.rotation
    }

    private val currentUiOrientation: String
        @RequiresApi(Build.VERSION_CODES.R)
        get() = when (getRotation()) {
            Surface.ROTATION_0 -> Names.PORTRAIT
            Surface.ROTATION_90 -> Names.LANDSCAPE_LEFT
            Surface.ROTATION_180 -> Names.PORTRAIT_UPSIDE_DOWN
            Surface.ROTATION_270 -> Names.LANDSCAPE_RIGHT
            else -> Names.UNKNOWN
        }

    init {
        // --- OrientationEventListener (cảm biến) ---
        orientationListener = object : OrientationEventListener(reactContext, SensorManager.SENSOR_DELAY_UI) {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onOrientationChanged(degrees: Int) {
                val deviceOrientation = when {
                    degrees == ORIENTATION_UNKNOWN -> Names.UNKNOWN
                    degrees in 0..5 || degrees in 356..359 -> Names.PORTRAIT
                    degrees in 86..94 -> Names.LANDSCAPE_RIGHT
                    degrees in 176..184 -> Names.PORTRAIT_UPSIDE_DOWN
                    degrees in 266..274 -> Names.LANDSCAPE_LEFT
                    else -> lastDeviceOrientation // không thay đổi nếu chưa vào “khe” hợp lệ
                }
                if (deviceOrientation != lastDeviceOrientation) {
                    notifyDeviceOrientationChange(deviceOrientation)
                }

                val uiOrientation = currentUiOrientation
                if (uiOrientation != lastUiOrientation) {
                    notifyUiOrientationChange(uiOrientation)
                }
            }
        }

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        } else {
            orientationListener.disable()
        }

        // --- BroadcastReceiver cho onConfigurationChanged (theo thiết kế của bạn) ---
        configurationReceiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(context: Context?, intent: Intent?) {
                val uiOrientation = currentUiOrientation
                // cập nhật và emit ngay vì đây là configuration thay đổi thực sự của UI
                notifyUiOrientationChange(uiOrientation, force = true)
            }
        }

        // Đăng ký lifecycle listener
        NitroOrientationActivityLifecycle.instance?.registerListeners(this)
    }

    // ---- Public API (giữ nguyên signature) ----

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getOrientation(): String = currentUiOrientation

    override fun getDeviceOrientation(): String = lastDeviceOrientation

    override fun lockToPortrait() =
        lockTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Names.PORTRAIT)

    override fun lockToPortraitUpsideDown() =
        lockTo(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, Names.PORTRAIT_UPSIDE_DOWN)

    override fun lockToLandscape() =
        lockTo(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, Names.LANDSCAPE_LEFT)

    override fun lockToLandscapeLeft() =
        lockTo(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Names.LANDSCAPE_LEFT)

    override fun lockToLandscapeRight() =
        lockTo(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, Names.LANDSCAPE_RIGHT)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun unlockAllOrientations() {
        val activity = reactContext.currentActivity ?: return
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        isLocking = false

        // Khi unlock, báo UI orientation hiện tại và lockDidChange = "unknown"
        notifyUiOrientationChange(currentUiOrientation, force = true)
        notifyLockChange(Names.UNKNOWN)
    }

    override fun getAutoRotateState(): Boolean {
        val resolver = reactContext.contentResolver
        val isAutoRotateEnabled = Settings.System.getInt(
            resolver,
            Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 1
        return isAutoRotateEnabled
    }

    override fun start() {
        orientationListener.enable()
        safeRegisterReceiver()
    }

    override fun stop() {
        orientationListener.disable()
        safeUnregisterReceiver()
    }

    override fun release() {
        orientationListener.disable()
        safeUnregisterReceiver()
    }
}
