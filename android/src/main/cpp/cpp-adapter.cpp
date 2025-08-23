#include <jni.h>
#include "NitroOrientationOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return margelo::nitro::nitroorientation::initialize(vm);
}
