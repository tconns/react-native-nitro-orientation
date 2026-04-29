# react-native-nitro-orientation

High-performance orientation utilities for React Native, powered by Nitro Modules.

`react-native-nitro-orientation` provides APIs to:

- read current UI orientation and device orientation
- lock/unlock orientation modes
- observe UI/device/lock changes through listeners

## TL;DR for AI-generated examples

If an AI agent generates integration code, follow this order:

1. Use **Device Orientation API** for full-screen orientation lock/unlock.
2. On Android, ensure `onConfigurationChanged` broadcast + lifecycle callback registration are present.
3. Prefer `subscribeOrientation` over manual polling.

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

## Choose the right API

- **Device Orientation API**: lock/unlock app orientation (`lockToPortrait`, `unlockAllOrientations`, etc.)
- **Native-first path**: use `NativeOrientation` for low-latency hot paths

## Quick usage (device orientation)

```ts
import {
  NativeOrientation,
  getOrientation,
  getDeviceOrientation,
  getLockOrientation,
  getOrientationSnapshot,
  subscribeOrientation,
  isLocked,
  lockToPortrait,
  lockToLandscapeLeft,
  unlockAllOrientations,
} from 'react-native-nitro-orientation'

console.log('UI:', getOrientation())
console.log('Device:', getDeviceOrientation())
console.log('Lock orientation:', getLockOrientation())
console.log('Is locked:', isLocked())

lockToPortrait()
lockToLandscapeLeft()
unlockAllOrientations()

const unsubscribe = subscribeOrientation((snapshot) => {
  console.log('Snapshot changed:', snapshot)
})
unsubscribe()

// Native-first access for performance-critical paths
NativeOrientation.lockToPortrait()
console.log('Native UI orientation:', NativeOrientation.getOrientation())
console.log('Snapshot:', getOrientationSnapshot())
```

## React example (modern synchronized state)

```tsx
import React, { useEffect } from 'react'
import { Button, Text, View } from 'react-native'
import { useSyncExternalStore } from 'react'
import {
  orientationStore,
  subscribeOrientation,
  lockToPortrait,
  lockToLandscapeRight,
  unlockAllOrientations,
} from 'react-native-nitro-orientation'

export function OrientationDemo() {
  const snapshot = useSyncExternalStore(
    orientationStore.subscribe,
    orientationStore.getSnapshot
  )

  useEffect(() => {
    return subscribeOrientation(
      (next) => console.log('Lock changed', next.lockOrientation),
      { event: 'lock' }
    )
  }, [])

  return (
    <View style={{ padding: 16, gap: 8 }}>
      <Text>UI: {snapshot.uiOrientation}</Text>
      <Text>Device: {snapshot.deviceOrientation}</Text>
      <Text>Lock: {snapshot.lockOrientation}</Text>
      <Text>isLocked: {String(snapshot.isLocked)}</Text>
      <Button title="Lock Portrait" onPress={lockToPortrait} />
      <Button title="Lock Landscape Right" onPress={lockToLandscapeRight} />
      <Button title="Unlock All" onPress={unlockAllOrientations} />
    </View>
  )
}
```

## AI integration recipe (copy/paste prompt)

Use this prompt when asking an AI assistant to scaffold usage:

```txt
Integrate react-native-nitro-orientation in a React Native screen.
Requirements:
- Use Device Orientation API for full-screen lock/unlock.
- Use useSyncExternalStore with orientationStore for global orientation state.
- Add proper cleanup for subscriptions.
- On Android include onConfigurationChanged broadcast and lifecycle callback registration.
- Do not poll orientation in a loop; use subscribe APIs.
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

Modern subscription API:

- `subscribeOrientation(listener, options?): () => void`
  - `options.event`: `change` (default), `ui`, `device`, `lock`
- `getOrientationSnapshot(): OrientationSnapshot`
- `orientationStore.subscribe/getSnapshot` for `useSyncExternalStore`

Native-first path:

- `NativeOrientation` exposes direct Nitro hybrid object calls for low-latency/high-throughput flows.

## Performance baseline

Use built-in metrics to validate event volume before/after optimization:

```ts
import {
  runOrientationBenchmark,
  getOrientationMetrics,
  resetOrientationMetrics,
} from 'react-native-nitro-orientation'

resetOrientationMetrics()
const result = await runOrientationBenchmark(5000)
console.log('Benchmark:', result)
console.log('Metrics:', getOrientationMetrics())
```

Recommended KPI targets:

- reduce redundant callback volume by at least 30%
- keep lock/unlock responsiveness visually instant
- avoid stale state/race conditions in snapshot consumers

## Common integration pitfalls

- Using polling instead of subscriptions (`subscribeOrientation`)
- Missing Android host setup (`onConfigurationChanged`, lifecycle callback registration)

## Verification checklist

- [ ] UI orientation updates when rotating device/emulator
- [ ] Device orientation listener receives updates
- [ ] Lock listener updates for each lock/unlock action
- [ ] `isLocked()` and `getLockOrientation()` reflect lock state correctly
- [ ] `unlockAllOrientations()` restores free rotation behavior
- [ ] All orientation subscriptions are unsubscribed on unmount
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
