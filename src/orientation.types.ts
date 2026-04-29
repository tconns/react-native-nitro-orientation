export enum OrientationType {
  PORTRAIT = 'portrait',
  PORTRAIT_UPSIDEDOWN = 'portraitUpsideDown',
  LANDSCAPE_LEFT = 'landscapeLeft',
  LANDSCAPE_RIGHT = 'landscapeRight',
  UNKNOWN = 'unknown',
}

export type OrientationValue =
  | 'portrait'
  | 'portraitUpsideDown'
  | 'landscapeLeft'
  | 'landscapeRight'
  | 'unknown'

export type ZoneEventSource =
  | 'create'
  | 'mount'
  | 'unmount'
  | 'update'
  | 'reset'
  | 'destroy'

export type ZoneAnimationState = 'idle' | 'running' | 'interrupted'

export interface ZoneRotationSnapshot {
  zoneId: string
  angleDeg: number
  orientation: OrientationValue
  attachedViews: number
  sequence: number
  updatedAt: number
}

export interface ZoneRotationEvent {
  source: ZoneEventSource
  snapshot: ZoneRotationSnapshot
  animationState: ZoneAnimationState
}
