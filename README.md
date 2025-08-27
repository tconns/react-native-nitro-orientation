# react-native-nitro-orientation

Native orientation control for React Native built with Nitro Modules.

## Overview

This module provides native-level orientation detection and locking capabilities for both Android and iOS. It exposes simple JS/TS APIs to read UI and device orientation, lock/unlock orientations, and listen to orientation events emitted from native.

## Features

- Read UI orientation and device (sensor) orientation
- Lock to portrait / portraitUpsideDown / landscapeLeft / landscapeRight
- Unlock to allow all orientations
- Emit events when orientation or lock state changes
- Built with Nitro Modules for native performance and autolinking support

## Requirements

- React Native >= 0.76
- Node >= 18
- `react-native-nitro-modules` must be installed (Nitro runtime)

## Installation

```bash
npm install react-native-nitro-orientation react-native-nitro-modules
# or
yarn add react-native-nitro-orientation react-native-nitro-modules
```

## Configuration

### iOS

Add the following to your project's AppDelegate.mm:

```diff

+#import <NitroOrientation/NitroOrientation.h>

@implementation AppDelegate

// ...

+- (UIInterfaceOrientationMask)application:(UIApplication *)application supportedInterfaceOrientationsForWindow:(UIWindow *)window {
+  return [NitroOrientation getOrientation];
+}

@end
```

### Android

Add following to android/app/src/main/AndroidManifest.xml

```diff
      <activity
        ....
+       android:configChanges="keyboard|keyboardHidden|orientation|screenSize"
        android:windowSoftInputMode="adjustResize">

          ....

      </activity>

```

Implement onConfigurationChanged method (in MainActivity.kt)

```kotlin

import android.content.Intent
import android.content.res.Configuration

// ...


class MainActivity : ReactActivity() {
//...
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val intent = Intent("onConfigurationChanged")
        intent.putExtra("newConfig", newConfig)
        this.sendBroadcast(intent)
    }
}
```

Add following to MainApplication.kt

```kotlin

import com.margelo.nitro.orientation.NitroOrientationActivityLifecycle
//...

class MainApplication : Application(), ReactApplication {
  override fun onCreate() {
    //...
    registerActivityLifecycleCallbacks(NitroOrientationActivityLifecycle.instance)
  }
}
```

## Quick usage (JS/TS)

```ts
import {
  getOrientation,
  getDeviceOrientation,
  lockToPortrait,
  lockToLandscape,
  unlockAllOrientations,
} from 'react-native-nitro-orientation'

// UI orientation
const ui = getOrientation()

// Device orientation from sensors
const device = getDeviceOrientation()

// Lock / unlock
lockToPortrait()
lockToLandscape()
unlockAllOrientations()
```

## API

- `getOrientation(): string` — returns one of: `portrait`, `portraitUpsideDown`, `landscapeLeft`, `landscapeRight`, `unknown`
- `getDeviceOrientation(): string` — orientation from device sensors
- `lockToPortrait(): void`
- `lockToPortraitUpsideDown(): void`
- `lockToLandscape(): void` — sensor-based landscape (Android)
- `lockToLandscapeLeft(): void`
- `lockToLandscapeRight(): void`
- `unlockAllOrientations(): void`
- `getAutoRotateState(): boolean` — (Android) returns whether Auto-Rotate is enabled

## Events

The module emits events via `DeviceEventEmitter` (payload: `{ orientation: string }`):

- `orientationDidChange` — UI orientation changed
- `deviceOrientationDidChange` — device sensor orientation changed
- `lockDidChange` — lock state changed (will emit `unknown` when unlocked)

Example listener:

```ts
import { useEffect } from 'react'
import { DeviceEventEmitter } from 'react-native'

useEffect(() => {
  const handler = (payload: { orientation: string }) => {
    console.log('orientation event', payload.orientation)
  }

  const sub = DeviceEventEmitter.addListener('orientationDidChange', handler)

  return () => sub.remove()
}, [])
```

## Advanced examples

```ts
import {
  lockToLandscapeLeft,
  lockToLandscapeRight,
  lockToLandscape,
} from 'react-native-nitro-orientation'

// Lock to landscape left
lockToLandscapeLeft()

// Lock to landscape right
lockToLandscapeRight()

// Sensor-based landscape (Android)
lockToLandscape()
```

## Platform Support

### Android

- ✅ Full support

### iOS

- 🚧 In development

## Troubleshooting

- Events not emitted on Android: ensure `NitroOrientationActivityLifecycle` is registered in your `Application` and Nitro runtime provides `applicationContext`.
- On iOS, if `requestGeometryUpdate` does not change UI orientation: verify `Info.plist` and `supportedInterfaceOrientationsFor` implementation.

## Migration / notes

- Orientation strings used by the module: `portrait`, `portraitUpsideDown`, `landscapeLeft`, `landscapeRight`, `unknown`.
- When updating spec files in `src/specs/*.nitro.ts`, regenerate Nitro artifacts:

```bash
npx nitro-codegen
```

## Contributing

See `CONTRIBUTING.md` for contribution workflow. Run `npx nitro-codegen` after editing spec files.

## Project structure

- `android/` — native Android (Kotlin)
- `ios/` — native iOS (Swift / ObjC bridge)
- `src/` — TypeScript exports
- `nitrogen/` — generated Nitro artifacts

## Acknowledgements

Special thanks to the following open-source projects which inspired and supported the development of this library:

- [mrousavy/nitro](https://github.com/mrousavy/nitro) – for the Nitro Modules architecture and tooling
- [react-native-orientation-locker](https://github.com/wonday/react-native-orientation-locker)
- [react-native-neo-orientation](https://github.com/duguyihou/react-native-neo-orientation)

## License

MIT © [Thành Công](https://github.com/tconns)
