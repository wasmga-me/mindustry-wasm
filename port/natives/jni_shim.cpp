#include "jni_shim.h"

const char* jni_error = 0;
int jni_buffer_length = 0;
JNIEnv jni_env;

extern "C" {
// the message stays allocated until the next error; errors are rare
EMSCRIPTEN_KEEPALIVE const char* jni_take_error(){
    static const char* last = 0;
    if(last) free((void*)last);
    last = jni_error;
    jni_error = 0;
    return last;
}

EMSCRIPTEN_KEEPALIVE int jni_buffer_size(){
    return jni_buffer_length;
}
}
