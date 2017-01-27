/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class VisualizerJNI */

#ifndef _Included_VisualizerJNI
#define _Included_VisualizerJNI
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     VisualizerJNI
 * Method:    nsetup
 * Signature: (Lprocessing/core/PApplet;)V
 */
JNIEXPORT void JNICALL Java_VisualizerJNI_nsetup
  (JNIEnv *, jobject, jobject);

/*
 * Class:     VisualizerJNI
 * Method:    nstart
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_VisualizerJNI_nstart
  (JNIEnv *, jobject);

/*
 * Class:     VisualizerJNI
 * Method:    nstop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_VisualizerJNI_nstop
  (JNIEnv *, jobject);

/*
 * Class:     VisualizerJNI
 * Method:    nupdate
 * Signature: (Lprocessing/core/PApplet;LPeople;)V
 */
JNIEXPORT void JNICALL Java_VisualizerJNI_nupdate
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     VisualizerJNI
 * Method:    ndraw
 * Signature: (LTracker;Lprocessing/core/PGraphics;LPeople;)V
 */
JNIEXPORT void JNICALL Java_VisualizerJNI_ndraw
  (JNIEnv *, jobject, jobject, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif