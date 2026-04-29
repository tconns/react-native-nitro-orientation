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

type OrientationEventType = 'ui' | 'device' | 'lock' | 'change'

export interface OrientationSnapshot {
  uiOrientation: OrientationValue
  deviceOrientation: OrientationValue
  lockOrientation: OrientationValue
  isLocked: boolean
  autoRotate: boolean
  sequence: number
  updatedAt: number
}

export interface OrientationMetrics {
  nativeUiEvents: number
  nativeDeviceEvents: number
  nativeLockEvents: number
  jsChangeEvents: number
  jsSubscriberNotifications: number
  uiDeduped: number
  deviceDeduped: number
  lockDeduped: number
}

export interface OrientationBenchmarkResult {
  durationMs: number
  eventDelta: OrientationMetrics
  finalSnapshot: OrientationSnapshot
}

type ListenerOptions = {
  event?: OrientationEventType
}

type SnapshotListener = (snapshot: OrientationSnapshot) => void

class OrientationManager {
  private emitter = new EventEmitter()
  private nativeAttached = false
  private sequence = 0
  private snapshot: OrientationSnapshot = {
    uiOrientation: getOrientation(),
    deviceOrientation: getDeviceOrientation(),
    lockOrientation: getLockOrientation(),
    isLocked: isLocked(),
    autoRotate: getAutoRotateState(),
    sequence: 0,
    updatedAt: Date.now(),
  }
  private metrics: OrientationMetrics = {
    nativeUiEvents: 0,
    nativeDeviceEvents: 0,
    nativeLockEvents: 0,
    jsChangeEvents: 0,
    jsSubscriberNotifications: 0,
    uiDeduped: 0,
    deviceDeduped: 0,
    lockDeduped: 0,
  }

  constructor() {
    this.attachNativeListeners()
  }

  private attachNativeListeners() {
    if (this.nativeAttached) return
    this.nativeAttached = true

    NitroOrientation.setChangeListener((orientation) => {
      this.metrics.nativeUiEvents += 1
      if (this.snapshot.uiOrientation === orientation) {
        this.metrics.uiDeduped += 1
        return
      }
      this.sequence += 1
      this.metrics.jsChangeEvents += 1
      this.snapshot.uiOrientation = orientation as OrientationValue
      this.snapshot.sequence = this.sequence
      this.snapshot.updatedAt = Date.now()
      this.emitter.emit('orientationDidChange', orientation)
      this.emitter.emit('change', orientation)
      this.metrics.jsSubscriberNotifications +=
        this.emitter.listenerCount('snapshot')
      this.emitter.emit('snapshot', this.getSnapshot())
    })
    NitroOrientation.setDeviceOrientationListener((orientation) => {
      this.metrics.nativeDeviceEvents += 1
      if (this.snapshot.deviceOrientation === orientation) {
        this.metrics.deviceDeduped += 1
        return
      }
      this.sequence += 1
      this.snapshot.deviceOrientation = orientation as OrientationValue
      this.snapshot.sequence = this.sequence
      this.snapshot.updatedAt = Date.now()
      this.emitter.emit('deviceOrientationDidChange', orientation)
      this.metrics.jsSubscriberNotifications +=
        this.emitter.listenerCount('snapshot')
      this.emitter.emit('snapshot', this.getSnapshot())
    })
    NitroOrientation.setLockListener((orientation) => {
      this.metrics.nativeLockEvents += 1
      if (this.snapshot.lockOrientation === orientation) {
        this.metrics.lockDeduped += 1
        return
      }
      this.sequence += 1
      this.snapshot.lockOrientation = orientation as OrientationValue
      this.snapshot.isLocked = orientation !== 'unknown'
      this.snapshot.sequence = this.sequence
      this.snapshot.updatedAt = Date.now()
      this.emitter.emit('lockDidChange', orientation)
      this.metrics.jsSubscriberNotifications +=
        this.emitter.listenerCount('snapshot')
      this.emitter.emit('snapshot', this.getSnapshot())
    })
  }

  addOrientationListener(cb: (o: OrientationValue) => void) {
    this.attachNativeListeners()
    this.emitter.on('change', cb as (value: unknown) => void)
    return () => this.removeOrientationListener(cb)
  }

  removeOrientationListener(cb: (o: OrientationValue) => void) {
    this.emitter.off('change', cb as (value: unknown) => void)
  }

  addDeviceOrientationListener(cb: (o: OrientationValue) => void) {
    this.attachNativeListeners()
    this.emitter.on(
      'deviceOrientationDidChange',
      cb as (value: unknown) => void
    )
    return () => this.removeDeviceOrientationListener(cb)
  }

  removeDeviceOrientationListener(cb: (o: OrientationValue) => void) {
    this.emitter.off(
      'deviceOrientationDidChange',
      cb as (value: unknown) => void
    )
  }

  addLockListener(cb: (o: OrientationValue) => void) {
    this.attachNativeListeners()
    this.emitter.on('lockDidChange', cb as (value: unknown) => void)
    return () => this.removeLockListener(cb)
  }

  removeLockListener(cb: (o: OrientationValue) => void) {
    this.emitter.off('lockDidChange', cb as (value: unknown) => void)
  }

  getSnapshot(): OrientationSnapshot {
    return {
      ...this.snapshot,
      // keep in sync with direct native state queries for native-first reads
      autoRotate: getAutoRotateState(),
      isLocked: isLocked(),
    }
  }

  subscribe(
    listener: SnapshotListener,
    options: ListenerOptions = {}
  ): () => void {
    this.attachNativeListeners()
    const event = options.event ?? 'change'

    if (event === 'change') {
      const onSnapshot = () => listener(this.getSnapshot())
      this.emitter.on('snapshot', onSnapshot as (value: unknown) => void)
      listener(this.getSnapshot())
      return () =>
        this.emitter.off('snapshot', onSnapshot as (value: unknown) => void)
    }

    const nativeEvent =
      event === 'ui'
        ? 'orientationDidChange'
        : event === 'device'
          ? 'deviceOrientationDidChange'
          : 'lockDidChange'

    const wrapped = () => listener(this.getSnapshot())
    this.emitter.on(nativeEvent, wrapped as (value: unknown) => void)
    listener(this.getSnapshot())
    return () =>
      this.emitter.off(nativeEvent, wrapped as (value: unknown) => void)
  }

  getMetrics(): OrientationMetrics {
    return { ...this.metrics }
  }

  resetMetrics() {
    this.metrics = {
      nativeUiEvents: 0,
      nativeDeviceEvents: 0,
      nativeLockEvents: 0,
      jsChangeEvents: 0,
      jsSubscriberNotifications: 0,
      uiDeduped: 0,
      deviceDeduped: 0,
      lockDeduped: 0,
    }
  }
}

export const Orientation = new OrientationManager()
export const NativeOrientation = NitroOrientation
export const getOrientationSnapshot = () => Orientation.getSnapshot()
export const subscribeOrientation = (
  listener: SnapshotListener,
  options?: ListenerOptions
) => Orientation.subscribe(listener, options)
export const orientationStore = {
  subscribe: (onStoreChange: () => void) =>
    subscribeOrientation(() => onStoreChange()),
  getSnapshot: getOrientationSnapshot,
}
export const getOrientationMetrics = () => Orientation.getMetrics()
export const resetOrientationMetrics = () => Orientation.resetMetrics()
export const runOrientationBenchmark = async (
  durationMs = 5000
): Promise<OrientationBenchmarkResult> => {
  const startedAt = Date.now()
  resetOrientationMetrics()
  return new Promise((resolve) => {
    setTimeout(() => {
      const endedAt = Date.now()
      resolve({
        durationMs: endedAt - startedAt,
        eventDelta: getOrientationMetrics(),
        finalSnapshot: getOrientationSnapshot(),
      })
    }, durationMs)
  })
}
export type { OrientationValue } from './orientation.types'
