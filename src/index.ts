import { NitroModules } from 'react-native-nitro-modules'
import type { NitroOrientation as NitroOrientationSpec } from './specs/NitroOrientation.nitro'
import EventEmitter from 'eventemitter3'
import type { OrientationValue } from './orientation.types'

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

export const getOrientation = (): OrientationValue => {
  return NitroOrientation.getOrientation() as OrientationValue
}

export const getDeviceOrientation = (): OrientationValue => {
  return NitroOrientation.getDeviceOrientation() as OrientationValue
}

export const getLockOrientation = (): OrientationValue => {
  return NitroOrientation.getLockOrientation() as OrientationValue
}

export const isLocked = (): boolean => {
  return NitroOrientation.isLocked()
}

export const getAutoRotateState = (): boolean => {
  return NitroOrientation.getAutoRotateState()
}

class OrientationManager {
  private emitter = new EventEmitter()

  constructor() {
    NitroOrientation.setChangeListener((orientation) => {
      this.emitter.emit('orientationDidChange', orientation)
      this.emitter.emit('change', orientation)
    })
    NitroOrientation.setDeviceOrientationListener((orientation) => {
      this.emitter.emit('deviceOrientationDidChange', orientation)
    })
    NitroOrientation.setLockListener((orientation) => {
      this.emitter.emit('lockDidChange', orientation)
    })
  }

  addOrientationListener(cb: (o: OrientationValue) => void) {
    this.emitter.on('change', cb as (value: unknown) => void)
  }

  removeOrientationListener(cb: (o: OrientationValue) => void) {
    this.emitter.off('change', cb as (value: unknown) => void)
  }

  addDeviceOrientationListener(cb: (o: OrientationValue) => void) {
    this.emitter.on(
      'deviceOrientationDidChange',
      cb as (value: unknown) => void
    )
  }

  removeDeviceOrientationListener(cb: (o: OrientationValue) => void) {
    this.emitter.off(
      'deviceOrientationDidChange',
      cb as (value: unknown) => void
    )
  }

  addLockListener(cb: (o: OrientationValue) => void) {
    this.emitter.on('lockDidChange', cb as (value: unknown) => void)
  }

  removeLockListener(cb: (o: OrientationValue) => void) {
    this.emitter.off('lockDidChange', cb as (value: unknown) => void)
  }
}

export const Orientation = new OrientationManager()
export type { OrientationValue } from './orientation.types'
