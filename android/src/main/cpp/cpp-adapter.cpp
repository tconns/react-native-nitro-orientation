#include <fbjni/fbjni.h>
#include <jni.h>

#include "NitroOrientationOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *)
{
  return facebook::jni::initialize(vm, [=]
                                   { margelo::nitro::orientation::initialize(vm); });
}
