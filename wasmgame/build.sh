#!/bin/bash
# Builds Mindustry v160.7 for the browser, into ./out:
#   mindustry.js        the game and its Arc engine, compiled from Java with TeaVM 0.16.0
#   arc-natives.js/.wasm  FreeType and SoLoud with Arc's JNI code, compiled with emsdk 4.0.7
#   mindustry-host.js   the page side (input, files, music)
#   mindustry-core.pak, mindustry-music.pak   the game's assets (served from R2)
# mindustry.diff and arc.diff change the game and the engine; backend.diff adds the
# browser backend (port/web), the native build (port/natives) and the page side (port/host).
# Needs git, JDK 17 or later, python3 and an activated emsdk 4.0.7. Run in an empty folder.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
work=$PWD

clone(){ # url dir commit
    git clone -q "$1" "$2"
    git -C "$2" checkout -q "$3"
}
clone https://github.com/Anuken/Mindustry Mindustry 17012d61d9e4c3cbd9820d87e81c24495486ac53
clone https://github.com/Anuken/Arc Arc 7445105cd2683daf5b9ac7033ebb07265d3e814d
clone https://github.com/Anuken/rhino rhino 32395f942976a6d1d21ec9877664554cd5762306
clone https://github.com/Anuken/soloud soloud eb83070e62e9edd3acc46cebbe345803c842620b
clone https://github.com/freetype/freetype freetype 0a0221a1347e2f1e07c395263540026e9a0aa7c7
git -C Mindustry apply "$here/mindustry.diff"
git -C Arc apply "$here/arc.diff"
git apply "$here/backend.diff"

# Arc's natives: C++ for emscripten, and Java classes that call it (mindustry.web.emu)
emu=$work/port/web/src/main/java/mindustry/web/emu
(cd port/natives
python3 jni2wasm.py "$work/Arc/arc-core/src/arc/audio/Soloud.java" --package mindustry.web.emu \
    --java-out "$emu/Soloud.java" --cpp-out soloud_jni.cpp \
    --override 'Soloud.wavLoadFile=byte[] data = WebNatives.read(path); return wavLoadBytes(data, data.length);' \
    --override 'Soloud.streamLoadFile=byte[] data = WebNatives.read(path); return streamLoadBytes(data, data.length);'
python3 jni2wasm.py "$work/Arc/extensions/freetype/src/arc/freetype/FreeType.java" --package mindustry.web.emu \
    --java-out "$emu/FreeType.java" --cpp-out freetype_jni.cpp --keep Library.newMemoryFace.data
sed -i 's#^\( *\)new SharedLibraryLoader().load("arc-freetype");#\1//natives come from arc-natives.wasm#' "$emu/FreeType.java"
SOLOUD=$work/soloud FREETYPE=$work/freetype bash build-natives.sh)

# The game's jars (Arc and rhino build alongside), then the building lookup Block.initBuilding uses
(cd Mindustry && ./gradlew core:jar -Pbuildversion=160.7)
java port/tools/GenBuildTypes.java port/web/src/main/java/mindustry/web/WebBuildTypes.java \
    Mindustry/core/build/libs/core-release.jar Mindustry/core/build/libs/core-release.jar \
    Arc/arc-core/build/libs/arc-core-1.0.jar Arc/extensions/*/build/libs/*.jar rhino/build/libs/rhino-1.7.13-SNAPSHOT.jar

cp -r Mindustry/gradlew Mindustry/gradle port/web/
(cd port/web && SRC=$work ./gradlew generateJavaScript)

mkdir -p out/fx
unzip -q Arc/extensions/fx/build/libs/fx-1.0.jar 'vfxshaders/*' -d out/fx
python3 port/host/pack.py out Mindustry/core/assets out/fx
rm -r out/fx
cp port/web/build/generated/teavm/js/mindustry.js port/natives/out/arc-natives.js port/natives/out/arc-natives.wasm port/host/mindustry-host.js out/
ls -l out
