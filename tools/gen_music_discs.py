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
    # A shooter's anthem: a disc and a 9mm round.
    "die_for_you": ("VALORANT - Die For You (feat. Grabbitz)", 219.0, 9, (255, 70, 85), (236, 232, 225), "analog_alu:bullet_9mm"),
    # C418, made nostalgic: a disc and a grass block.
    "mice_on_venus": ("C418 - Mice On Venus (EMBRR ver.)", 157.0, 10, (96, 168, 72), (170, 220, 120), "minecraft:grass_block"),
    # Somewhere to stay: a disc and any bed.
    "stay_at_your_house": ("Rosa Walton - I Really Want to Stay at Your House", 247.0, 11, (250, 228, 40), (60, 220, 230), "#minecraft:beds"),
    # A song to dance to: a disc and a note block.
    "hai_yorokonde": ("こっちのけんと - はいよろこんで", 162.0, 12, (240, 140, 40), (255, 220, 150), "minecraft:note_block"),
    # A circus: a disc and a cake.
    "digital_circus": ("Carameii - The Amazing Digital Circus", 195.0, 15, (200, 40, 60), (60, 90, 210), "minecraft:cake"),
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
        # Any music disc at all (the c:music_discs convention tag: every vanilla disc and other mods'),
        # plus something that suits the song.
        write_json(DATA / "recipe" / f"{item}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": ["#c:music_discs", extra],
            "result": {"id": f"{NS}:{item}", "count": 1},
        })
    write_json(ASSETS / "sounds.json", sounds)
    # They are music discs like any other, for this mod's recipes and other mods alike.
    write_json(RES / "data" / "c" / "tags" / "item" / "music_discs.json",
               {"replace": False, "values": [f"{NS}:music_disc_{name}" for name in DISCS]})
    # Creepers killed by skeletons drop these too, like the vanilla discs.
    write_json(RES / "data" / "minecraft" / "tags" / "item" / "creeper_drop_music_discs.json",
               {"replace": False, "values": [f"{NS}:music_disc_{name}" for name in DISCS]})

    for language, disc_name in (("ko_kr", KO_NAME), ("en_us", EN_NAME)):
        entries = {}
        for name, (title, *_rest) in DISCS.items():
            entries[f"item.{NS}.music_disc_{name}"] = disc_name
            entries[f"jukebox_song.{NS}.{name}"] = title
        # Added to what is there, since other features of this namespace have names in the same file.
        path = ASSETS / "lang" / f"{language}.json"
        existing = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        existing.update(entries)
        write_json(path, existing)
    print("music discs generated")


if __name__ == "__main__":
    main()
