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
import android.view.View
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

@DoNotStrip
class NitroOrientation : HybridNitroOrientationSpec(), NitroOrientationListeners {
  private data class ZoneState(
    var angleDeg: Double = 0.0,
    var orientation: String = Names.PORTRAIT,
    val attachedViews: MutableSet<Int> = mutableSetOf(),
    var sequence: Long = 0,
    var updatedAtMs: Long = 0,
    var lastEmitAtMs: Long = 0
  )

  private val reactContext = NitroModules.applicationContext ?: throw Exception("Context is null")
    private val hostViewRefs = mutableMapOf<Int, WeakReference<View>>()

    private var uiOrientationCallback: ((String) -> Unit)? = null
    private var deviceOrientationCallback: ((String) -> Unit)? = null
    private var lockCallback: ((String) -> Unit)? = null
    private var zoneCallback: ((String) -> Unit)? = null
    private val zoneStates = mutableMapOf<String, ZoneState>()
    private var zoneSequence = 0L

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
    private var lastStableQuadrant: String = Names.UNKNOWN

    private fun now() = SystemClock.uptimeMillis()

    private fun canEmit(lastAt: Long) = now() - lastAt >= minEmitIntervalMs
    private fun nowEpochMs() = System.currentTimeMillis()

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
      if (orientation == lockOrientation) return
      lockOrientation = orientation
      sendEvent(Events.LOCK_DID_CHANGE, orientation)
    }

    private fun upsertZone(zoneId: String): ZoneState {
      return zoneStates.getOrPut(zoneId) {
        ZoneState(updatedAtMs = nowEpochMs())
      }
    }

    private fun normalizeZoneOrientation(orientation: String): String {
      return when (orientation) {
        Names.PORTRAIT, Names.PORTRAIT_UPSIDE_DOWN, Names.LANDSCAPE_LEFT, Names.LANDSCAPE_RIGHT -> orientation
        else -> Names.UNKNOWN
      }
    }

    private fun angleForZoneOrientation(orientation: String): Double {
      return when (orientation) {
        Names.PORTRAIT -> 0.0
        Names.LANDSCAPE_LEFT -> -90.0
        Names.LANDSCAPE_RIGHT -> 90.0
        Names.PORTRAIT_UPSIDE_DOWN -> 180.0
        else -> 0.0
      }
    }

    private fun findViewByTag(tag: Int): View? {
      hostViewRefs[tag]?.get()?.let { return it }
      return reactContext.currentActivity?.findViewById(tag)
    }

    private fun applyZoneTransform(zoneId: String) {
      val state = zoneStates[zoneId] ?: return
      state.attachedViews.forEach { tag ->
        findViewByTag(tag)?.rotation = state.angleDeg.toFloat()
      }
    }

    private fun zoneSnapshotJson(zoneId: String, state: ZoneState): JSONObject {
      return JSONObject()
        .put("zoneId", zoneId)
        .put("angleDeg", state.angleDeg)
        .put("orientation", state.orientation)
        .put("attachedViews", state.attachedViews.size)
        .put("sequence", state.sequence)
        .put("updatedAt", state.updatedAtMs)
    }

    private fun emitZoneEvent(
      zoneId: String,
      source: String,
      animationState: String = "idle",
      force: Boolean = false
    ) {
      val state = zoneStates[zoneId] ?: return
      if (!force && !canEmit(state.lastEmitAtMs)) return
      state.lastEmitAtMs = now()
      val payload = JSONObject()
        .put("source", source)
        .put("animationState", animationState)
        .put("snapshot", zoneSnapshotJson(zoneId, state))
      zoneCallback?.invoke(payload.toString())
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
      NitroOrientationZoneRegistry.setCallback { zoneId, hostView, isMounted ->
        val viewTag = hostView.id
        if (viewTag == View.NO_ID) return@setCallback
        hostViewRefs[viewTag] = WeakReference(hostView)
        if (isMounted) {
          registerZoneHost(zoneId, viewTag.toDouble())
        } else {
          unregisterZoneHost(zoneId, viewTag.toDouble())
          hostViewRefs.remove(viewTag)
        }
      }
      orientationListener = object : OrientationEventListener(reactContext, SensorManager.SENSOR_DELAY_UI) {
        override fun onOrientationChanged(degrees: Int) {
          val previousQuadrant = lastStableQuadrant
          val deviceOrientation = when {
            degrees == ORIENTATION_UNKNOWN -> Names.UNKNOWN
            degrees <= 20 || degrees >= 340 -> Names.PORTRAIT
            degrees in 70..110 -> Names.LANDSCAPE_RIGHT
            degrees in 160..200 -> Names.PORTRAIT_UPSIDE_DOWN
            degrees in 250..290 -> Names.LANDSCAPE_LEFT
            else -> previousQuadrant
          }
          lastStableQuadrant = deviceOrientation
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
      lockTo(
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        if (currentUiOrientation() == Names.LANDSCAPE_RIGHT) Names.LANDSCAPE_RIGHT else Names.LANDSCAPE_LEFT
      )

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

    override fun createZone(zoneId: String, options: String) {
      if (zoneId.isBlank()) return
      upsertZone(zoneId)
      emitZoneEvent(zoneId, "create", force = true)
    }

    override fun registerZoneHost(zoneId: String, nativeViewTag: Double) {
      if (zoneId.isBlank()) return
      val state = upsertZone(zoneId)
      state.attachedViews.add(nativeViewTag.toInt())
      zoneSequence += 1
      state.sequence = zoneSequence
      state.updatedAtMs = nowEpochMs()
      applyZoneTransform(zoneId)
      emitZoneEvent(zoneId, "mount", force = true)
    }

    override fun unregisterZoneHost(zoneId: String, nativeViewTag: Double) {
      val state = zoneStates[zoneId] ?: return
      state.attachedViews.remove(nativeViewTag.toInt())
      zoneSequence += 1
      state.sequence = zoneSequence
      state.updatedAtMs = nowEpochMs()
      emitZoneEvent(zoneId, "unmount", force = true)
    }

    override fun destroyZone(zoneId: String) {
      val state = zoneStates[zoneId] ?: return
      state.attachedViews.forEach { tag ->
        findViewByTag(tag)?.rotation = 0f
        hostViewRefs.remove(tag)
      }
      zoneStates.remove(zoneId)
    }

    override fun setZoneRotation(zoneId: String, angleDeg: Double, options: String) {
      val state = upsertZone(zoneId)
      if (kotlin.math.abs(state.angleDeg - angleDeg) < 0.1) return
      val parsed = try {
        JSONObject(options)
      } catch (_: Exception) {
        JSONObject()
      }
      val animated = parsed.optBoolean("animated", false)
      state.angleDeg = angleDeg
      zoneSequence += 1
      state.sequence = zoneSequence
      state.updatedAtMs = nowEpochMs()
      applyZoneTransform(zoneId)
      emitZoneEvent(zoneId, "update", animationState = if (animated) "running" else "idle")
    }

    override fun setZoneOrientation(zoneId: String, orientation: String) {
      val state = upsertZone(zoneId)
      state.orientation = normalizeZoneOrientation(orientation)
      state.angleDeg = angleForZoneOrientation(state.orientation)
      zoneSequence += 1
      state.sequence = zoneSequence
      state.updatedAtMs = nowEpochMs()
      applyZoneTransform(zoneId)
      emitZoneEvent(zoneId, "update")
    }

    override fun resetZoneRotation(zoneId: String) {
      val state = upsertZone(zoneId)
      state.angleDeg = 0.0
      state.orientation = Names.PORTRAIT
      zoneSequence += 1
      state.sequence = zoneSequence
      state.updatedAtMs = nowEpochMs()
      applyZoneTransform(zoneId)
      emitZoneEvent(zoneId, "reset", force = true)
    }

    override fun getZoneSnapshot(zoneId: String): String {
      val state = zoneStates[zoneId] ?: return "{}"
      return zoneSnapshotJson(zoneId, state).toString()
    }

    override fun getAllZoneSnapshots(): String {
      val array = JSONArray()
      zoneStates.forEach { (zoneId, state) ->
        array.put(zoneSnapshotJson(zoneId, state))
      }
      return array.toString()
    }

    override fun setZoneListener(listener: (String) -> Unit) {
      zoneCallback = listener
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
      zoneCallback = null
      zoneStates.clear()
      NitroOrientationZoneRegistry.setCallback { _, _, _ -> }
    }
}
