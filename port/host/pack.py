#!/usr/bin/env python3
"""Packs Mindustry's assets for the page.

Writes two paks: <out>/mindustry-core.pak (everything the game reads at start) and
<out>/mindustry-music.pak (the music, fetched after the game starts and read as it arrives,
menu music first). Pak format: uint32 LE header length, a JSON header [[path, size, lazy], ...],
then the files back to back. The core pak lists the music as lazy (size 0 there), so the game
sees those files and asks the page for them.
Usage: pack.py <out dir> <dir>... (core/assets, then classpath resources)
"""
import json
import os
import struct
import sys

# sprites/fallback is the atlas for GPUs without 4096px textures; WebGL 2 guarantees 4096
SKIP = ('sprites/fallback/', 'scripts/')
LAZY = 'music/'
FIRST = ['menu', 'land', 'launch', 'game1', 'game2', 'game3']


def write(path, header, files):
    head = json.dumps(header, separators=(',', ':')).encode()
    with open(path, 'wb') as f:
        f.write(struct.pack('<I', len(head)))
        f.write(head)
        for p, size, lazy in header:
            if not lazy:
                with open(files[p], 'rb') as src:
                    data = src.read()
                assert len(data) == size
                f.write(data)
    print(path, len(header), 'files', os.path.getsize(path) // 1024, 'KB')


def main():
    out = sys.argv[1]
    files = {}
    for root in sys.argv[2:]:
        for d, _, names in os.walk(root):
            for n in names:
                files.setdefault(os.path.relpath(os.path.join(d, n), root).replace(os.sep, '/'), os.path.join(d, n))
    paths = sorted(f for f in files if not f.startswith(SKIP))
    music = [p for p in paths if p.startswith(LAZY)]
    name = lambda p: os.path.splitext(os.path.basename(p))[0]
    music.sort(key=lambda p: (FIRST.index(name(p)) if name(p) in FIRST else len(FIRST), p))
    write(os.path.join(out, 'mindustry-core.pak'),
          [[p, os.path.getsize(files[p]), 1 if p.startswith(LAZY) else 0] for p in paths], files)
    write(os.path.join(out, 'mindustry-music.pak'), [[p, os.path.getsize(files[p]), 0] for p in music], files)


if __name__ == '__main__':
    main()
