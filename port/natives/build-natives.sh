#!/bin/bash
# Builds arc-natives.js/.wasm: FreeType and SoLoud with Arc's JNI bodies (see jni2wasm.py).
# Needs emsdk 4.0.7 activated, SOLOUD and FREETYPE pointing at the source checkouts,
# and soloud_jni.cpp/freetype_jni.cpp generated next to this script.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
out=${OUT:-$here/out}
obj=$out/obj
mkdir -p "$obj"

ft=$FREETYPE
sl=$SOLOUD
ftflags="-O2 -DFT2_BUILD_LIBRARY -DFT_CONFIG_MODULES_H=<ftmodule_arc.h> -I$here -I$ft/include"
slflags="-O2 -DWITH_MINIAUDIO -DSOLOUD_MAX_VOICE_COUNT=100 -I$sl/include -I$sl/src/audiosource/wav -I$sl/src/backend/miniaudio"

objs=()
compile(){ # compiler flags src
    local o="$obj/$(echo "$3" | md5sum | cut -c1-12)-$(basename "$3").o"
    if [ ! -f "$o" ] || [ "$3" -nt "$o" ]; then $1 $2 -c "$3" -o "$o"; fi
    objs+=("$o")
}

for f in base/ftsystem.c base/ftinit.c base/ftdebug.c base/ftbase.c base/ftbbox.c base/ftglyph.c base/ftbitmap.c \
         base/ftstroke.c base/ftmm.c truetype/truetype.c sfnt/sfnt.c smooth/smooth.c autofit/autofit.c gzip/ftgzip.c psnames/psnames.c; do
    compile emcc "$ftflags" "$ft/src/$f"
done
for f in "$sl"/src/core/*.cpp "$sl"/src/audiosource/wav/*.cpp "$sl"/src/filter/*.cpp "$sl"/src/backend/miniaudio/*.cpp; do
    compile em++ "$slflags" "$f"
done
compile emcc "$slflags" "$sl/src/audiosource/wav/stb_vorbis.c"
compile em++ "-O2 -I$here" "$here/jni_shim.cpp"
compile em++ "$ftflags -I$here" "$here/freetype_jni.cpp"
compile em++ "$slflags -I$here" "$here/soloud_jni.cpp"
compile em++ "$slflags" "$here/web_audio.cpp"

em++ -O2 "${objs[@]}" -o "$out/arc-natives.js" \
    -sMODULARIZE -sEXPORT_NAME=createArcNatives -sENVIRONMENT=web \
    -sALLOW_MEMORY_GROWTH -sINITIAL_MEMORY=64MB -sSTACK_SIZE=1MB \
    -sEXPORTED_FUNCTIONS=_malloc,_free \
    -sEXPORTED_RUNTIME_METHODS=HEAP8,HEAPU8,HEAP16,HEAPU16,HEAP32,HEAPF32,HEAPF64,stringToNewUTF8,UTF8ToString
ls -l "$out"/arc-natives.*
