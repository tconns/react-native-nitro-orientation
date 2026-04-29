package com.margelo.nitro.orientation

import android.content.Context
import android.content.res.Configuration
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import android.provider.Settings
import androidx.core.content.ContextCompat

@DoNotStrip
class NitroOrientation : HybridNitroOrientationSpec(), NitroOrientationListeners {
  private val reactContext = NitroModules.applicationContext ?: throw Exception("Context is null")

    private var uiOrientationCallback: ((String) -> Unit)? = null
    private var deviceOrientationCallback: ((String) -> Unit)? = null
    private var lockCallback: ((String) -> Unit)? = null

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

    private val orientationListener: OrientationEventListener
    private val configurationReceiver: BroadcastReceiver
    private var isConfigurationChangeReceiverRegistered = false
    private var isLocking = false
    private var lockOrientation = Names.UNKNOWN
    private var lastUiOrientation: String = Names.UNKNOWN
    private var lastDeviceOrientation: String = Names.UNKNOWN

    private var lastDeviceEmitAtMs = 0L
    private var lastUiEmitAtMs = 0L
    private val minEmitIntervalMs = 120L

    private fun now() = SystemClock.uptimeMillis()

    private fun canEmit(lastAt: Long) = now() - lastAt >= minEmitIntervalMs

    private fun sendEvent(event: String, orientation: String) {
      when (event) {
        Events.ORIENTATION_DID_CHANGE -> uiOrientationCallback?.invoke(orientation)
        Events.DEVICE_ORIENTATION_DID_CHANGE -> deviceOrientationCallback?.invoke(orientation)
        Events.LOCK_DID_CHANGE -> lockCallback?.invoke(orientation)
      }
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
      sendEvent(Events.DEVICE_ORIENTATION_DID_CHANGE, orientation)
    }

    private fun notifyLockChange(orientation: String) {
      lockOrientation = orientation
      sendEvent(Events.LOCK_DID_CHANGE, orientation)
    }

    private fun lockTo(requested: Int, orientationName: String) {
      val activity = reactContext.currentActivity ?: return
      activity.requestedOrientation = requested
      isLocking = true
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
      }
    }

    private fun safeUnregisterReceiver() {
      if (!isConfigurationChangeReceiverRegistered) return
      try {
        reactContext.unregisterReceiver(configurationReceiver)
      } catch (_: Exception) {
      } finally {
        isConfigurationChangeReceiverRegistered = false
      }
    }

    private fun getRotation(): Int {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        reactContext.currentActivity?.display?.rotation?.let { return it }
      }
      val wm = reactContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
      @Suppress("DEPRECATION")
      return wm.defaultDisplay.rotation
    }

    private fun currentUiOrientation(): String = when (getRotation()) {
      Surface.ROTATION_0 -> Names.PORTRAIT
      Surface.ROTATION_90 -> Names.LANDSCAPE_LEFT
      Surface.ROTATION_180 -> Names.PORTRAIT_UPSIDE_DOWN
      Surface.ROTATION_270 -> Names.LANDSCAPE_RIGHT
      else -> Names.UNKNOWN
    }

    init {
      orientationListener = object : OrientationEventListener(reactContext, SensorManager.SENSOR_DELAY_UI) {
        override fun onOrientationChanged(degrees: Int) {
          val deviceOrientation = when {
            degrees == ORIENTATION_UNKNOWN -> Names.UNKNOWN
            degrees in 0..5 || degrees in 356..359 -> Names.PORTRAIT
            degrees in 86..94 -> Names.LANDSCAPE_RIGHT
            degrees in 176..184 -> Names.PORTRAIT_UPSIDE_DOWN
            degrees in 266..274 -> Names.LANDSCAPE_LEFT
            else -> lastDeviceOrientation
          }
          if (deviceOrientation != lastDeviceOrientation) {
            notifyDeviceOrientationChange(deviceOrientation)
          }

          val uiOrientation = currentUiOrientation()
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

      configurationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          val uiOrientation = currentUiOrientation()
          notifyUiOrientationChange(uiOrientation, force = true)
        }
      }

      NitroOrientationActivityLifecycle.instance?.registerListeners(this)
    }

    override fun getOrientation(): String = currentUiOrientation()

    override fun getDeviceOrientation(): String = lastDeviceOrientation

    override fun getLockOrientation(): String = lockOrientation

    override fun isLocked(): Boolean = isLocking

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

    override fun unlockAllOrientations() {
      val activity = reactContext.currentActivity ?: return
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
      isLocking = false
      notifyUiOrientationChange(currentUiOrientation(), force = true)
      notifyLockChange(Names.UNKNOWN)
    }

    override fun getAutoRotateState(): Boolean {
      val resolver = reactContext.contentResolver
      return Settings.System.getInt(
        resolver,
        Settings.System.ACCELEROMETER_ROTATION,
        0
      ) == 1
    }

    override fun setChangeListener(listener: (String) -> Unit) {
      uiOrientationCallback = listener
    }

    override fun setDeviceOrientationListener(listener: (String) -> Unit) {
      deviceOrientationCallback = listener
    }

    override fun setLockListener(listener: (String) -> Unit) {
      lockCallback = listener
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
      uiOrientationCallback = null
      deviceOrientationCallback = null
      lockCallback = null
    }
}
