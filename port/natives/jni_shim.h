// Just enough of JNI for the bodies of Arc's natives, compiled for the browser.
#pragma once
#include <emscripten.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef int32_t jint;
typedef intptr_t jlong; // pointers are 32-bit in wasm
typedef int jboolean;
typedef float jfloat;
typedef double jdouble;
typedef int16_t jshort;
typedef int8_t jbyte;
typedef uint16_t jchar;
typedef void* jobject;
typedef void* jclass;
typedef const char* jstring;

extern const char* jni_error;
extern int jni_buffer_length;

struct JNIEnv{
    jclass FindClass(const char*){ return (jclass)1; }
    jint ThrowNew(jclass, const char* message){
        if(!jni_error) jni_error = strdup(message ? message : "native error");
        return 0;
    }
    jstring NewStringUTF(const char* text){ return text; }
    jobject NewDirectByteBuffer(void* data, jlong length){
        jni_buffer_length = (int)length;
        return data;
    }
};

extern JNIEnv jni_env;
