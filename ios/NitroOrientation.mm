//
//  NitroOrientation.mm
//  Pods
//
//  Created by TcoND on 23/8/25.
//

#import "React/RCTBridgeModule.h"

@interface

RCT_EXTERN_MODULE(NitroOrientation, NSObject)

RCT_EXTERN_METHOD(lockToLandscape)
RCT_EXTERN_METHOD(lockToPortrait)
RCT_EXTERN_METHOD(lockToPortraitUpsideDown)
RCT_EXTERN_METHOD(lockToLandscapeRight)
RCT_EXTERN_METHOD(lockToLandscapeLeft)
RCT_EXTERN_METHOD(unlockAllOrientations)

@end