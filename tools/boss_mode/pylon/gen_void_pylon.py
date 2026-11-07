"""Builds the Void Pylon's GeckoLib assets (the Void Sovereign's shield crystals): a plain cube model (no Blender source -
GeckoLib bakes cube bones itself), a looping spin/bob animation and a 64x64 texture with its glowmask.

Run:  python tools/pylon/gen_void_pylon.py
"""
import json
import math
import os
import random
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "boss_mode")


def write_png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            raw.extend(bytes(max(0, min(255, int(c))) for c in pixels[y][x]))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def textures():
    rng = random.Random(9)
    size = 64
    albedo = [[[0, 0, 0, 255] for _ in range(size)] for _ in range(size)]
    glow = [[[0, 0, 0, 0] for _ in range(size)] for _ in range(size)]
    for y in range(size):
        for x in range(size):
            t = 1.0 - (y % 32) / 31.0
            facet = (x % 8 == 0) or ((x + y) % 11 == 0)
            base = [18 + 30 * t, 6 + 8 * t, 30 + 40 * t]
            if facet:
                base = [200, 40, 110]
            n = rng.uniform(-6, 6)
            albedo[y][x] = [base[0] + n, base[1] + n * 0.5, base[2] + n, 255]
            if facet:
                glow[y][x] = [255, 60, 140, 255]
            elif rng.random() < 0.05:
                glow[y][x] = [255, 120, 190, 200]
                albedo[y][x] = [230, 90, 160, 255]
    return albedo, glow


def geo():
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.void_pylon", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 3, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "crystal", "parent": "root", "pivot": [0, 24, 0], "rotation": [0, 45, 0], "cubes": [
                {"origin": [-4, 12, -4], "size": [8, 24, 8], "uv": [0, 0]},
                {"origin": [-2.5, 36, -2.5], "size": [5, 6, 5], "uv": [32, 0]},
                {"origin": [-2.5, 6, -2.5], "size": [5, 6, 5], "uv": [32, 11]},
                {"origin": [-1, 42, -1], "size": [2, 3, 2], "uv": [52, 0]},
            ]},
            {"name": "ring", "parent": "root", "pivot": [0, 24, 0], "cubes": [
                {"origin": [8, 23, -1], "size": [2, 2, 2], "uv": [32, 22]},
                {"origin": [-10, 23, -1], "size": [2, 2, 2], "uv": [40, 22]},
                {"origin": [-1, 23, 8], "size": [2, 2, 2], "uv": [48, 22]},
                {"origin": [-1, 23, -10], "size": [2, 2, 2], "uv": [56, 22]},
            ]},
        ]}]}


def animation():
    def keys(points):
        return {("%.1f" % t): {"vector": v, "easing": "linear"} for t, v in points}

    return {"format_version": "1.8.0", "animations": {"animation.void_pylon.idle": {
        "loop": True, "animation_length": 4.0, "bones": {
            "crystal": {"rotation": keys([(0, [0, 0, 0]), (1, [0, 90, 0]), (2, [0, 180, 0]), (3, [0, 270, 0]), (4, [0, 360, 0])]),
                        "position": keys([(0, [0, 0, 0]), (1, [0, 1.5, 0]), (2, [0, 0, 0]), (3, [0, -1.5, 0]), (4, [0, 0, 0])])},
            "ring": {"rotation": keys([(0, [0, 0, 0]), (1, [0, -90, 0]), (2, [0, -180, 0]), (3, [0, -270, 0]), (4, [0, -360, 0])]),
                     "position": keys([(0, [0, -1, 0]), (2, [0, 1, 0]), (4, [0, -1, 0])])},
        }}}}


def main():
    albedo, glow = textures()
    write_png(os.path.join(ASSETS, "textures", "entity", "void_pylon.png"), albedo)
    write_png(os.path.join(ASSETS, "textures", "entity", "void_pylon_glowmask.png"), glow)
    with open(os.path.join(ASSETS, "geckolib", "models", "entity", "void_pylon.geo.json"), "w", encoding="utf-8") as f:
        json.dump(geo(), f, indent=1)
    with open(os.path.join(ASSETS, "geckolib", "animations", "entity", "void_pylon.animation.json"), "w", encoding="utf-8") as f:
        json.dump(animation(), f, indent=1)
    print("void pylon assets written")


if __name__ == "__main__":
    main()
