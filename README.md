# react-native-nitro-orientation

Native orientation utilities for React Native, powered by Nitro Modules.

`react-native-nitro-orientation` provides APIs to:

- read current UI orientation and device orientation
- lock/unlock orientation modes
- observe UI/device/lock changes through listeners

## Requirements

- React Native `>= 0.76`
- Node.js `>= 18`
- `react-native-nitro-modules` `>= 0.35.x`

## Installation

```bash
npm install react-native-nitro-orientation react-native-nitro-modules
```

or

```bash
yarn add react-native-nitro-orientation react-native-nitro-modules
```

## Platform setup

### Android

1. Ensure activity handles orientation config changes:

```xml
<activity
  android:name=".MainActivity"
  android:configChanges="keyboard|keyboardHidden|orientation|screenSize|smallestScreenSize|uiMode"
  ... />
```

2. Forward configuration changes in `MainActivity`:

```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
  super.onConfigurationChanged(newConfig)
  val intent = Intent("onConfigurationChanged")
  intent.putExtra("newConfig", newConfig)
  sendBroadcast(intent)
}
```

3. Register lifecycle callbacks in `MainApplication`:

```kotlin
registerActivityLifecycleCallbacks(NitroOrientationActivityLifecycle.instance)
```

### iOS

No special permission is required.  
For reliable lock behavior, make sure your app supports the orientations you want in target settings and `Info.plist`.

## Quick usage

```ts
import {
  getOrientation,
  getDeviceOrientation,
  getLockOrientation,
  isLocked,
  lockToPortrait,
  lockToLandscapeLeft,
  unlockAllOrientations,
  Orientation,
} from 'react-native-nitro-orientation'

console.log('UI:', getOrientation())
console.log('Device:', getDeviceOrientation())
console.log('Lock orientation:', getLockOrientation())
console.log('Is locked:', isLocked())

lockToPortrait()
lockToLandscapeLeft()
unlockAllOrientations()

const onUiChange = (value: string) => console.log('UI changed:', value)
Orientation.addOrientationListener(onUiChange)
// cleanup
Orientation.removeOrientationListener(onUiChange)
```

## API

### Query

- `getOrientation(): OrientationValue`
- `getDeviceOrientation(): OrientationValue`
- `getLockOrientation(): OrientationValue`
- `isLocked(): boolean`
- `getAutoRotateState(): boolean`

`OrientationValue`:

- `portrait`
- `portraitUpsideDown`
- `landscapeLeft`
- `landscapeRight`
- `unknown`

### Lock control

- `lockToPortrait()`
- `lockToPortraitUpsideDown()`
- `lockToLandscape()` (sensor-based landscape intent on Android)
- `lockToLandscapeLeft()`
- `lockToLandscapeRight()`
- `unlockAllOrientations()`

### Listeners

`Orientation` manager:

- `addOrientationListener(cb)` / `removeOrientationListener(cb)`
- `addDeviceOrientationListener(cb)` / `removeDeviceOrientationListener(cb)`
- `addLockListener(cb)` / `removeLockListener(cb)`

## Example (React)

```tsx
import React, { useEffect, useState } from 'react'
import { Button, Text, View } from 'react-native'
import {
  Orientation,
  getOrientation,
  getDeviceOrientation,
  getLockOrientation,
  isLocked,
  lockToPortrait,
  lockToLandscapeRight,
  unlockAllOrientations,
} from 'react-native-nitro-orientation'

export function OrientationDemo() {
  const [uiOrientation, setUiOrientation] = useState(getOrientation())
  const [deviceOrientation, setDeviceOrientation] = useState(getDeviceOrientation())
  const [lockOrientation, setLockOrientation] = useState(getLockOrientation())

  useEffect(() => {
    const onUi = (value: string) => setUiOrientation(value)
    const onDevice = (value: string) => setDeviceOrientation(value)
    const onLock = (value: string) => setLockOrientation(value)

    Orientation.addOrientationListener(onUi)
    Orientation.addDeviceOrientationListener(onDevice)
    Orientation.addLockListener(onLock)

    return () => {
      Orientation.removeOrientationListener(onUi)
      Orientation.removeDeviceOrientationListener(onDevice)
      Orientation.removeLockListener(onLock)
    }
  }, [])

  return (
    <View style={{ padding: 16, gap: 8 }}>
      <Text>UI: {uiOrientation}</Text>
      <Text>Device: {deviceOrientation}</Text>
      <Text>Lock: {lockOrientation}</Text>
      <Text>isLocked: {String(isLocked())}</Text>
      <Button title="Lock Portrait" onPress={lockToPortrait} />
      <Button title="Lock Landscape Right" onPress={lockToLandscapeRight} />
      <Button title="Unlock All" onPress={unlockAllOrientations} />
    </View>
  )
}
```

## Platform notes

- **Android**
  - Most complete feature set.
  - Depends on host-app `onConfigurationChanged` forwarding and lifecycle callback registration.

- **iOS**
  - Uses geometry update APIs where available.
  - Actual lock behavior still depends on app-supported orientations.
  - `getAutoRotateState()` reports platform capability as `true` (iOS does not expose an Android-style user toggle API).

## Test checklist

- [ ] UI orientation updates when rotating device/emulator
- [ ] Device orientation listener receives updates
- [ ] Lock listener updates for each lock/unlock action
- [ ] `isLocked()` and `getLockOrientation()` reflect lock state correctly
- [ ] `unlockAllOrientations()` restores free rotation behavior
- [ ] Android flow verified with required MainActivity/MainApplication setup
- [ ] iOS flow verified with supported orientations configured in app target

## Nitro development

When changing `src/specs/*.nitro.ts`:

```bash
npx tsc && npx nitrogen --logLevel="debug"
```

Useful scripts:

- `npm run typecheck`
- `npm run lint`
- `npm run specs`

## License

MIT © [Thành Công](https://github.com/tconns)
