#!/usr/bin/env python3
"""Music discs: textures, models, sounds.json, jukebox songs, recipes, tags and names.

Standard library only. The audio itself (assets/modernization_world/sounds/records/*.ogg) is made
separately from the source MP3s, mono so that it fades with distance like the vanilla discs:

    ffmpeg -i song.mp3 -vn -map_metadata -1 -ac 1 -ar 44100 -c:a libvorbis -q:a 5 snowman.ogg
"""
from __future__ import annotations

import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
NS = "modernization_world"
ASSETS = RES / "assets" / NS
DATA = RES / "data" / NS

# name: (artist and title, seconds, comparator output, label colour, label highlight, recipe extra item)
DISCS = {
    "snowman": ("Sia - Snowman", 166.0, 13, (150, 214, 245), (236, 248, 255), "minecraft:snow_block"),
    "payphone": ("Maroon 5 - Payphone (feat. Wiz Khalifa)", 233.0, 14, (214, 52, 52), (255, 196, 70), "minecraft:bell"),
}

KO_NAME = "음반"
EN_NAME = "Music Disc"


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def write_png(path: Path, pixels) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = b"".join(b"\x00" + bytes(v for px in row for v in px) for row in pixels)

    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def disc_pixels(label, highlight):
    """A black record with grooves, a coloured label and a hole, lit from the top left."""
    outline = (16, 16, 18, 255)
    vinyl = (36, 36, 40, 255)
    groove = (54, 54, 60, 255)
    shine = (92, 92, 100, 255)
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.6:
                row.append((0, 0, 0, 0))
            elif d > 6.9:
                row.append(outline)
            elif d < 1.0:
                row.append((0, 0, 0, 0))
            elif d < 2.9:
                row.append((*(highlight if x + y < 14 and d > 1.8 else label), 255))
            elif d < 3.4:
                row.append(outline)
            elif int(d * 2) % 3 == 0:
                row.append(groove)
            else:
                row.append(vinyl)
        pixels.append(row)
    for x, y in ((4, 3), (3, 4), (5, 2), (2, 5)):
        pixels[y][x] = shine
    return pixels


def main() -> None:
    sounds = {}
    for name, (title, seconds, comparator, label, highlight, extra) in DISCS.items():
        item = f"music_disc_{name}"
        write_png(ASSETS / "textures" / "item" / f"{item}.png", disc_pixels(label, highlight))
        write_json(ASSETS / "models" / "item" / f"{item}.json", {
            "parent": "minecraft:item/template_music_disc",
            "textures": {"layer0": f"{NS}:item/{item}"},
        })
        write_json(ASSETS / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{item}"}})
        sounds[f"music_disc.{name}"] = {"sounds": [{"name": f"{NS}:records/{name}", "stream": True}]}
        write_json(DATA / "jukebox_song" / f"{name}.json", {
            "comparator_output": comparator,
            "description": {"translate": f"jukebox_song.{NS}.{name}"},
            "length_in_seconds": seconds,
            "sound_event": f"{NS}:music_disc.{name}",
        })
        # Any disc a creeper can drop, plus something that suits the song.
        write_json(DATA / "recipe" / f"{item}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": ["#minecraft:creeper_drop_music_discs", extra],
            "result": {"id": f"{NS}:{item}", "count": 1},
        })
    write_json(ASSETS / "sounds.json", sounds)
    # Creepers killed by skeletons drop these too, like the vanilla discs.
    write_json(RES / "data" / "minecraft" / "tags" / "item" / "creeper_drop_music_discs.json",
               {"replace": False, "values": [f"{NS}:music_disc_{name}" for name in DISCS]})

    for language, disc_name in (("ko_kr", KO_NAME), ("en_us", EN_NAME)):
        entries = {}
        for name, (title, *_rest) in DISCS.items():
            entries[f"item.{NS}.music_disc_{name}"] = disc_name
            entries[f"jukebox_song.{NS}.{name}"] = title
        write_json(ASSETS / "lang" / f"{language}.json", entries)
    print("music discs generated")


if __name__ == "__main__":
    main()
