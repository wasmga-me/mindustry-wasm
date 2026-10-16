# Mindustry in the browser

This fork is the web build of [Mindustry](https://github.com/Anuken/Mindustry) that runs on wasmga.me. Play it at **https://wasmga.me/games/mindustry**.

Mindustry v160.7 and its Arc engine are compiled from Java to JavaScript with TeaVM 0.16.0. FreeType and SoLoud, which Arc reaches through JNI, are compiled with Emscripten to `arc-natives.wasm`, and a small generator (`port/natives/jni2wasm.py`) writes both sides of those calls.

## What's here

- Branch `wasmgame` (this one) is upstream commit [`17012d6`](https://github.com/Anuken/Mindustry/commit/17012d61d9e4c3cbd9820d87e81c24495486ac53) with our changes on top as commits, so `git log` and the diff against `17012d6` show everything we changed.
- [`wasmgame/`](../wasmgame) holds the build script and the same changes as `.diff` files against that commit. `build.sh` starts from a fresh upstream checkout, applies them and builds, and its header comment lists each change.

## Building

Needs git, JDK 17 or later, python3 and an activated emsdk 4.0.7. In an empty folder:

```sh
bash /path/to/this/repo/wasmgame/build.sh
```

It clones Mindustry, Arc, rhino, SoLoud and FreeType at pinned commits, applies `mindustry.diff`, `arc.diff` and `backend.diff` (which is the `port/` folder in this branch), and writes `out/`: `mindustry.js`, `arc-natives.js` and `.wasm`, `mindustry-host.js`, and the asset packs `mindustry-core.pak` and `mindustry-music.pak`.

## Running it

`port/host/mindustry-host.js` is the page side: it loads the natives and the asset packs, records input for `WebInput.java` and keeps the files the game writes. On wasmga.me the play page also stores those files as the player's save.

## Licence

Same as upstream: the code is GPL-3.0. See upstream's own licence files in this repository. Our changes are under the same licence as the files they change.
