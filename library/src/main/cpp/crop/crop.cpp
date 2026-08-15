#include <jni.h>
#include <vector>

#include "borders.h"

// Mirror of imagedecoder's get_ptr helper – read a long "ptr" field from a Java object.
// Not needed here since we operate on a passed-in byte array, but kept for style parity.

extern "C" JNIEXPORT jintArray JNICALL
Java_com_davemorrissey_labs_subscaleview_CropBorders_findCropBorders(
  JNIEnv* env, jobject /* thiz */,
  jbyteArray jpixels, jint width, jint height)
{
  jsize len = env->GetArrayLength(jpixels);
  jbyte* rgba = env->GetByteArrayElements(jpixels, nullptr);

  // Convert RGBA -> grayscale (single channel) using BT.601 luminance coefficients.
  // borders.cpp expects one byte per pixel.
  std::vector<uint8_t> gray(width * height);
  for (jint i = 0; i < width * height; i++) {
    uint8_t r = (uint8_t)rgba[i * 4 + 0];
    uint8_t g = (uint8_t)rgba[i * 4 + 1];
    uint8_t b = (uint8_t)rgba[i * 4 + 2];
    gray[i] = (uint8_t)((r * 77 + g * 150 + b * 29) >> 8);
  }

  env->ReleaseByteArrayElements(jpixels, rgba, JNI_ABORT);

  Rect rect = findBorders(gray.data(), (uint32_t)width, (uint32_t)height);

  jintArray result = env->NewIntArray(4);
  jint buf[4] = {
    (jint)rect.x,
    (jint)rect.y,
    (jint)rect.width,
    (jint)rect.height,
  };
  env->SetIntArrayRegion(result, 0, 4, buf);
  return result;
}
