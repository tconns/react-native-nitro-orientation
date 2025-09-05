# react-native-nitro-orientation

Native orientation control for React Native built with Nitro Modules.

## Overview

This module provides native-level orientation detection and locking capabilities for both Android and iOS. It exposes simple JS/TS APIs to read UI and device orientation, lock/unlock orientations, and listen to orientation events emitted from native.

## Features

- Read UI orientation and device (sensor) orientation
- Lock to portrait / portraitUpsideDown / landscapeLeft / landscapeRight
- Unlock to allow all orientations
- Listen to orientation change events with a simple callback API
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
  Orientation,
} from 'react-native-nitro-orientation'

// UI orientation
const ui = getOrientation()

// Device orientation from sensors
const device = getDeviceOrientation()

// Lock / unlock
lockToPortrait()
lockToLandscape()
unlockAllOrientations()

// Listen to orientation changes
const handleOrientationChange = (orientation: 'portrait' | 'landscape') => {
  console.log('Orientation changed to:', orientation)
}

Orientation.addOrientationListener(handleOrientationChange)

// Don't forget to remove the listener when component unmounts
Orientation.removeOrientationListener(handleOrientationChange)
```

## API

### Orientation Control

- `getOrientation(): string` — returns one of: `portrait`, `portraitUpsideDown`, `landscapeLeft`, `landscapeRight`, `unknown`
- `getDeviceOrientation(): string` — orientation from device sensors
- `lockToPortrait(): void`
- `lockToPortraitUpsideDown(): void`
- `lockToLandscape(): void` — sensor-based landscape (Android)
- `lockToLandscapeLeft(): void`
- `lockToLandscapeRight(): void`
- `unlockAllOrientations(): void`
- `getAutoRotateState(): boolean` — (Android) returns whether Auto-Rotate is enabled

### Event Listening

- `Orientation.addOrientationListener(callback)` — add a listener for orientation changes
- `Orientation.removeOrientationListener(callback)` — remove a previously added listener

The callback receives one parameter: `orientation: 'portrait' | 'landscape'`

## Events

The module provides a simple event listening API through the `Orientation` manager:

```ts
import { useEffect } from 'react'
import { Orientation } from 'react-native-nitro-orientation'

useEffect(() => {
  const handleOrientationChange = (orientation: 'portrait' | 'landscape') => {
    console.log('Orientation changed to:', orientation)
  }

  // Add listener
  Orientation.addOrientationListener(handleOrientationChange)

  // Cleanup
  return () => {
    Orientation.removeOrientationListener(handleOrientationChange)
  }
}, [])
```

## Advanced examples

```ts
import {
  lockToLandscapeLeft,
  lockToLandscapeRight,
  lockToLandscape,
  Orientation,
} from 'react-native-nitro-orientation'

// Lock to landscape left
lockToLandscapeLeft()

// Lock to landscape right
lockToLandscapeRight()

// Sensor-based landscape (Android)
lockToLandscape()

// Create a custom hook for orientation changes
const useOrientation = () => {
  const [orientation, setOrientation] = useState<'portrait' | 'landscape'>('portrait')

  useEffect(() => {
    const handleChange = (newOrientation: 'portrait' | 'landscape') => {
      setOrientation(newOrientation)
    }

    Orientation.addOrientationListener(handleChange)

    return () => {
      Orientation.removeOrientationListener(handleChange)
    }
  }, [])

  return orientation
}
```

## Platform Support

### Android

- ✅ Full support

### iOS

- 🚧 In development

## Troubleshooting

- Events not emitted on Android: ensure `NitroOrientationActivityLifecycle` is registered in your `Application` and Nitro runtime provides `applicationContext`.
- On iOS, if `requestGeometryUpdate` does not change UI orientation: verify `Info.plist` and `supportedInterfaceOrientationsFor` implementation.
- Make sure to remove orientation listeners when components unmount to avoid memory leaks.

## Migration / notes

- Orientation strings used by the module: `portrait`, `portraitUpsideDown`, `landscapeLeft`, `landscapeRight`, `unknown`.
- Event callbacks receive simplified orientation values: `'portrait' | 'landscape'`.
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


<a href="https://www.buymeacoffee.com/tconns94" target="_blank">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="200"/>
</a>
