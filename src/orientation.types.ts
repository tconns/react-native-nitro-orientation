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
