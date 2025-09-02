import { NitroModules } from 'react-native-nitro-modules'
import type { NitroOrientation as NitroOrientationSpec } from './specs/NitroOrientation.nitro'
import EventEmitter from 'eventemitter3'

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

class OrientationManager {
  private emitter = new EventEmitter()

  constructor() {
    NitroOrientation.setChangeListener((o) => {
      this.emitter.emit('change', o)
    })
  }

  addOrientationListener(cb: (o: 'portrait' | 'landscape') => void) {
    this.emitter.on('change', cb)
  }

  removeOrientationListener(cb: (o: 'portrait' | 'landscape') => void) {
    this.emitter.off('change', cb)
  }
}

export const Orientation = new OrientationManager()
