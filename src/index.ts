import { NitroModules } from 'react-native-nitro-modules'
import type { NitroOrientation as NitroOrientationSpec } from './specs/NitroOrientation.nitro'
import EventEmitter from 'eventemitter3'
import type {
  OrientationValue,
  ZoneAnimationState,
  ZoneRotationEvent,
  ZoneRotationSnapshot,
} from './orientation.types'

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
type ZoneListener = (event: ZoneRotationEvent) => void
export type ZoneRotationOptions = {
  animated?: boolean
  durationMs?: number
  easing?: 'linear' | 'easeInOut' | 'easeOut'
  interruptPolicy?: 'replace' | 'ignore'
}
export type ZoneBenchmarkResult = {
  zoneCount: number
  steps: number
  durationMs: number
  snapshots: ZoneRotationSnapshot[]
}

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

const parseZoneSnapshot = (value: string): ZoneRotationSnapshot | null => {
  try {
    const parsed = JSON.parse(value) as Partial<ZoneRotationSnapshot>
    if (!parsed || typeof parsed.zoneId !== 'string') return null
    return {
      zoneId: parsed.zoneId,
      angleDeg: Number(parsed.angleDeg ?? 0),
      orientation: (parsed.orientation ?? 'unknown') as OrientationValue,
      attachedViews: Number(parsed.attachedViews ?? 0),
      sequence: Number(parsed.sequence ?? 0),
      updatedAt: Number(parsed.updatedAt ?? Date.now()),
    }
  } catch {
    return null
  }
}

const parseZoneEvent = (value: string): ZoneRotationEvent | null => {
  try {
    const parsed = JSON.parse(value) as Partial<ZoneRotationEvent>
    if (!parsed || typeof parsed.source !== 'string' || !parsed.snapshot)
      return null
    const snapshot = parseZoneSnapshot(JSON.stringify(parsed.snapshot))
    if (!snapshot) return null
    return {
      source: parsed.source as ZoneRotationEvent['source'],
      snapshot,
      animationState: (parsed.animationState ?? 'idle') as ZoneAnimationState,
    }
  } catch {
    return null
  }
}

class ZoneRotationManager {
  private emitter = new EventEmitter()
  private snapshots = new Map<string, ZoneRotationSnapshot>()
  private nativeAttached = false

  private attachNativeListener() {
    if (this.nativeAttached) return
    this.nativeAttached = true

    NitroOrientation.setZoneListener((eventJson) => {
      const event = parseZoneEvent(eventJson)
      if (!event) return
      this.snapshots.set(event.snapshot.zoneId, event.snapshot)
      this.emitter.emit(`zone:${event.snapshot.zoneId}`, event)
      this.emitter.emit('all-zones', event)
      this.emitter.emit('snapshot', this.getAllSnapshots())
    })
  }

  createZone(zoneId: string, options: Record<string, unknown> = {}) {
    this.attachNativeListener()
    NitroOrientation.createZone(zoneId, JSON.stringify(options))
  }

  registerHost(zoneId: string, nativeViewTag: number) {
    this.attachNativeListener()
    NitroOrientation.registerZoneHost(zoneId, nativeViewTag)
  }

  unregisterHost(zoneId: string, nativeViewTag: number) {
    this.attachNativeListener()
    NitroOrientation.unregisterZoneHost(zoneId, nativeViewTag)
  }

  destroyZone(zoneId: string) {
    this.attachNativeListener()
    NitroOrientation.destroyZone(zoneId)
    this.snapshots.delete(zoneId)
  }

  setRotation(
    zoneId: string,
    angleDeg: number,
    options: ZoneRotationOptions = {}
  ) {
    this.attachNativeListener()
    NitroOrientation.setZoneRotation(zoneId, angleDeg, JSON.stringify(options))
  }

  setOrientation(zoneId: string, orientation: OrientationValue) {
    this.attachNativeListener()
    NitroOrientation.setZoneOrientation(zoneId, orientation)
  }

  reset(zoneId: string) {
    this.attachNativeListener()
    NitroOrientation.resetZoneRotation(zoneId)
  }

  getSnapshot(zoneId: string): ZoneRotationSnapshot | null {
    const fromCache = this.snapshots.get(zoneId)
    if (fromCache) return fromCache
    const nativeSnapshot = parseZoneSnapshot(
      NitroOrientation.getZoneSnapshot(zoneId)
    )
    if (nativeSnapshot) {
      this.snapshots.set(zoneId, nativeSnapshot)
      return nativeSnapshot
    }
    return null
  }

  getAllSnapshots(): ZoneRotationSnapshot[] {
    try {
      const parsed = JSON.parse(
        NitroOrientation.getAllZoneSnapshots()
      ) as unknown[]
      const normalized = parsed
        .map((item) => parseZoneSnapshot(JSON.stringify(item)))
        .filter(Boolean) as ZoneRotationSnapshot[]
      normalized.forEach((snapshot) =>
        this.snapshots.set(snapshot.zoneId, snapshot)
      )
      return normalized
    } catch {
      return Array.from(this.snapshots.values())
    }
  }

  subscribeZone(zoneId: string, listener: ZoneListener): () => void {
    this.attachNativeListener()
    const key = `zone:${zoneId}`
    this.emitter.on(key, listener as (value: unknown) => void)
    return () => this.emitter.off(key, listener as (value: unknown) => void)
  }

  subscribeAllZones(listener: ZoneListener): () => void {
    this.attachNativeListener()
    this.emitter.on('all-zones', listener as (value: unknown) => void)
    return () =>
      this.emitter.off('all-zones', listener as (value: unknown) => void)
  }

  store(zoneId: string) {
    return {
      subscribe: (onStoreChange: () => void) => {
        const unsubscribe = this.subscribeZone(zoneId, () => onStoreChange())
        return unsubscribe
      },
      getSnapshot: () => this.getSnapshot(zoneId),
    }
  }
}

export const ZoneRotation = new ZoneRotationManager()
export const createZone = (zoneId: string, options?: Record<string, unknown>) =>
  ZoneRotation.createZone(zoneId, options)
export const registerZoneHost = (zoneId: string, nativeViewTag: number) =>
  ZoneRotation.registerHost(zoneId, nativeViewTag)
export const unregisterZoneHost = (zoneId: string, nativeViewTag: number) =>
  ZoneRotation.unregisterHost(zoneId, nativeViewTag)
export const destroyZone = (zoneId: string) => ZoneRotation.destroyZone(zoneId)
export const setZoneRotation = (
  zoneId: string,
  angleDeg: number,
  options?: ZoneRotationOptions
) => ZoneRotation.setRotation(zoneId, angleDeg, options)
export const setZoneOrientation = (
  zoneId: string,
  orientation: OrientationValue
) => ZoneRotation.setOrientation(zoneId, orientation)
export const resetZoneRotation = (zoneId: string) => ZoneRotation.reset(zoneId)
export const getZoneSnapshot = (zoneId: string) =>
  ZoneRotation.getSnapshot(zoneId)
export const getAllZoneSnapshots = () => ZoneRotation.getAllSnapshots()
export const subscribeZone = (zoneId: string, listener: ZoneListener) =>
  ZoneRotation.subscribeZone(zoneId, listener)
export const subscribeAllZones = (listener: ZoneListener) =>
  ZoneRotation.subscribeAllZones(listener)
export const zoneRotationStore = (zoneId: string) => ZoneRotation.store(zoneId)
export const runZoneRotationBenchmark = async (
  zoneIds: string[],
  steps = 24
): Promise<ZoneBenchmarkResult> => {
  const startedAt = Date.now()
  for (let i = 0; i < steps; i += 1) {
    const angle = (i * 360) / steps
    zoneIds.forEach((zoneId) => {
      setZoneRotation(zoneId, angle, { animated: false })
    })
    await new Promise((resolve) => setTimeout(resolve, 16))
  }
  return {
    zoneCount: zoneIds.length,
    steps,
    durationMs: Date.now() - startedAt,
    snapshots: getAllZoneSnapshots(),
  }
}
export type {
  OrientationValue,
  ZoneAnimationState,
  ZoneRotationEvent,
  ZoneRotationSnapshot,
} from './orientation.types'
export { NitroZoneView, useZoneRotation } from './NitroZoneView'
