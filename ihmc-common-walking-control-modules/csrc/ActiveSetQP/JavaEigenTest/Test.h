/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class Test */

#ifndef _Included_Test
#define _Included_Test
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     Test
 * Method:    inverse
 * Signature: ([D[D[DII)V
 */
JNIEXPORT void JNICALL Java_Test_inverse
  (JNIEnv *, jclass, jdoubleArray, jdoubleArray, jdoubleArray, jint, jint);

JNIEXPORT void inverse(double* m1, double* m2,double* r, int nRow, int nCol);

#ifdef __cplusplus
}
#endif
#endif