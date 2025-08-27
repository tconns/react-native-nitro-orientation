import { NitroModules } from 'react-native-nitro-modules'
import type { NitroOrientation as NitroOrientationSpec } from './specs/NitroOrientation.nitro'

export const NitroOrientation =
  NitroModules.createHybridObject<NitroOrientationSpec>('NitroOrientation')

export const lockToPortrait = () => {
  NitroOrientation.lockToPortrait()
}

export const lockToPortraitUpsideDown = () => {
  NitroOrientation.lockToPortraitUpsideDown()
}

export const lockToLandscape = () => {
  NitroOrientation.lockToLandscape()
}

export const lockToLandscapeLeft = () => {
  NitroOrientation.lockToLandscapeLeft()
}

export const lockToLandscapeRight = () => {
  NitroOrientation.lockToLandscapeRight()
}

export const unlockAllOrientations = () => {
  NitroOrientation.unlockAllOrientations()
}

export const getOrientation = (): string => {
  return NitroOrientation.getOrientation()
}

export const getDeviceOrientation = (): string => {
  return NitroOrientation.getDeviceOrientation()
}

export const getAutoRotateState = (): boolean => {
  return NitroOrientation.getAutoRotateState()
}
