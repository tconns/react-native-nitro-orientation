package com.nitroorientation;

import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.TurboReactPackage;
import com.margelo.nitro.core.HybridObject;
import com.margelo.nitro.nitroorientation.NitroOrientationOnLoad;

import java.util.HashMap;
import java.util.function.Supplier;

public class NitroOrientationPackage extends TurboReactPackage {
  @Nullable
  @Override
  public NativeModule getModule(@NonNull String name, @NonNull ReactApplicationContext reactContext) {
    return null;
  }

  @NonNull
  @Override
  public ReactModuleInfoProvider getReactModuleInfoProvider() {
    return HashMap::new;
  }

  static {
    NitroOrientationOnLoad.initializeNative();
  }
}
