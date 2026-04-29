import type { HybridObject } from 'react-native-nitro-modules'

export interface NitroOrientation extends HybridObject<{
  ios: 'swift'
  android: 'kotlin'
}> {
  getOrientation(): string
  getDeviceOrientation(): string
  getLockOrientation(): string
  isLocked(): boolean
  lockToPortrait(): void
  lockToPortraitUpsideDown(): void
  lockToLandscape(): void
  lockToLandscapeLeft(): void
  lockToLandscapeRight(): void
  unlockAllOrientations(): void
  getAutoRotateState(): boolean
  setChangeListener(listener: (orientation: string) => void): void
  setDeviceOrientationListener(listener: (orientation: string) => void): void
  setLockListener(listener: (orientation: string) => void): void
}
