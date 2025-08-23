import { type HybridObject } from 'react-native-nitro-modules'

export interface NitroOrientation
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  getOrientation(): string
  getDeviceOrientation(): string
  lockToPortrait(): void
  lockToPortraitUpsideDown(): void
  lockToLandscape(): void
  lockToLandscapeLeft(): void
  lockToLandscapeRight(): void
  unlockAllOrientations(): void
  getAutoRotateState(): boolean
}
