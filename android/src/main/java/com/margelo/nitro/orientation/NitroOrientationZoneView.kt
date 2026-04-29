package com.margelo.nitro.orientation

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class NitroOrientationZoneView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
  var zoneId: String = ""
    set(value) {
      field = value
      if (value.isNotBlank()) {
        NitroOrientationZoneRegistry.register(value, this)
      }
    }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (zoneId.isNotBlank()) {
      NitroOrientationZoneRegistry.register(zoneId, this)
    }
  }

  override fun onDetachedFromWindow() {
    if (zoneId.isNotBlank()) {
      NitroOrientationZoneRegistry.unregister(zoneId, this)
    }
    super.onDetachedFromWindow()
  }
}

object NitroOrientationZoneRegistry {
  private var callback: ((String, NitroOrientationZoneView, Boolean) -> Unit)? = null

  fun setCallback(callback: (String, NitroOrientationZoneView, Boolean) -> Unit) {
    this.callback = callback
  }

  fun register(zoneId: String, view: NitroOrientationZoneView) {
    callback?.invoke(zoneId, view, true)
  }

  fun unregister(zoneId: String, view: NitroOrientationZoneView) {
    callback?.invoke(zoneId, view, false)
  }
}
