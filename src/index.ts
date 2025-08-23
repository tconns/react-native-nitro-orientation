import { NitroModules } from 'react-native-nitro-modules'
import type { NitroOrientation as NitroOrientationSpec } from './specs/nitro-orientation.nitro'
// import { DeviceEventEmitter } from 'react-native'

// const run = () => {
//   const sub1 = DeviceEventEmitter.addListener('orientationDidChange', event => {
//     console.log('UI Orientation changed:', event.orientation);
//   });

//   const sub2 = DeviceEventEmitter.addListener(
//     'deviceOrientationDidChange',
//     event => {
//       console.log('Device Orientation changed:', event.orientation);
//     },
//   );

//   const sub3 = DeviceEventEmitter.addListener('lockDidChange', event => {
//     console.log('Lock state changed:', event.orientation);
//   });
// };

// run();

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
