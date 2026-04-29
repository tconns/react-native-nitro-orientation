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
  createZone(zoneId: string, options: string): void
  registerZoneHost(zoneId: string, nativeViewTag: number): void
  unregisterZoneHost(zoneId: string, nativeViewTag: number): void
  destroyZone(zoneId: string): void
  setZoneRotation(zoneId: string, angleDeg: number, options: string): void
  setZoneOrientation(zoneId: string, orientation: string): void
  resetZoneRotation(zoneId: string): void
  getZoneSnapshot(zoneId: string): string
  getAllZoneSnapshots(): string
  setZoneListener(listener: (eventJson: string) => void): void
}
