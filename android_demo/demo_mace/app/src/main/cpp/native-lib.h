//
// Created by lchad on 2017/7/18.
//

#ifndef OPENCVCOMPILE_NATIVE_LIB_H
#define OPENCVCOMPILE_NATIVE_LIB_H

#include <jni.h>
extern "C" {

    JNIEXPORT jlong JNICALL
    Java_com_jupiter_facedetection_DetectionTracker_nativeCreateObject(JNIEnv *env, jclass type,
                                                                jstring cascadeName_,
                                                                jint minFaceSize);

    JNIEXPORT void JNICALL
    Java_com_jupiter_facedetection_DetectionTracker_nativeDestroyObject(JNIEnv *env, jclass type, jlong thiz);

    JNIEXPORT void JNICALL
    Java_com_jupiter_facedetection_DetectionTracker_nativeStart(JNIEnv *env, jclass type, jlong thiz);
    JNIEXPORT void JNICALL
    Java_com_jupiter_facedetection_DetectionTracker_nativeStop(JNIEnv *env, jclass type, jlong thiz);

    JNIEXPORT void JNICALL
    Java_com_jupiter_facedetection_DetectionTracker_nativeSetFaceSize(JNIEnv *env, jclass type, jlong thiz,
    jint size);

    JNIEXPORT void JNICALL
    Java_com_jupiter_facedetection_DetectionTracker_nativeDetect(JNIEnv *env, jclass type, jlong thiz,
    jlong inputImage, jlong faces);

};
#endif //OPENCVCOMPILE_NATIVE_LIB_H
