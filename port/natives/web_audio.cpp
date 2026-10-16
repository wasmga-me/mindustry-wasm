// The page's mute button: SoLoud's global volume (soloud is the instance soloud_jni.cpp makes).
#include <emscripten.h>
#include "soloud.h"

extern SoLoud::Soloud soloud;

extern "C" EMSCRIPTEN_KEEPALIVE void arc_set_muted(int muted){
    soloud.setGlobalVolume(muted ? 0.0f : 1.0f);
}
