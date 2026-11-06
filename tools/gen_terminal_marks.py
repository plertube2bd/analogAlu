#!/usr/bin/env python3
"""Clear + and - marks on every power source's terminals.

Every polarised device takes its + from the face it points at (the model's north face) and its - from
the face behind. Batteries and converters already had a small dark sign in a coloured ring; it is
redrawn as a bold white sign on a red or blue disc. Generators had no mark at all; they get a red +
badge on the front and a new back face with a blue - badge. Standard library only, and safe to run
again: every mark overwrites the same pixels. The originals are kept beside the script's output as
*_unmarked.png the first time, and marks are always drawn from those.
"""
from __future__ import annotations

import json
import shutil
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "analog_alu"
BLOCK = ASSETS / "textures" / "block"
MODELS = ASSETS / "models" / "block"
ORIGINALS = ROOT / "tools" / "terminal_mark_sources"

POSITIVE = (214, 40, 40, 255)
NEGATIVE = (40, 96, 214, 255)
WHITE = (252, 252, 252, 255)
EDGE = (22, 22, 26, 255)


def read_png(path: Path):
    data = path.read_bytes()
    pos, idat, palette, trns = 8, b"", None, None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, color, _, _, interlace = struct.unpack(">IIBBBBB", body)
        elif kind == b"PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == b"tRNS":
            trns = body
        elif kind == b"IDAT":
            idat += body
    assert interlace == 0 and (depth == 8 or color == 3)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    raw = zlib.decompress(idat)
    stride = width * channels if depth == 8 else (width * depth + 7) // 8
    bpp = channels if depth == 8 else 1
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    pixels = []
    for line in rows:
        if depth < 8:
            per = 8 // depth
            line = bytearray(((line[x // per] >> (8 - depth * (x % per + 1))) & ((1 << depth) - 1)) for x in range(width))
        row = []
        for x in range(width):
            px = line[x * channels:(x + 1) * channels]
            if color == 6:
                row.append(tuple(px))
            elif color == 2:
                row.append((*px, 255))
            elif color == 0:
                row.append((px[0], px[0], px[0], 255))
            elif color == 4:
                row.append((px[0], px[0], px[0], px[1]))
            else:
                r, g, b = palette[px[0]]
                row.append((r, g, b, trns[px[0]] if trns and px[0] < len(trns) else 255))
        pixels.append(row)
    return pixels


def write_png(path: Path, pixels) -> None:
    height, width = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + bytes(v for px in row for v in px) for row in pixels)

    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def source(name: str):
    """The texture as it was before any marks, saved the first time it is marked."""
    ORIGINALS.mkdir(parents=True, exist_ok=True)
    original = ORIGINALS / f"{name}.png"
    if not original.exists():
        shutil.copyfile(BLOCK / f"{name}.png", original)
    return read_png(original)


def fill(pixels, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            pixels[y][x] = color


def centre_sign(pixels, positive: bool) -> None:
    """A disc in the middle of the face with a bold white sign, red for + and blue for -."""
    color = POSITIVE if positive else NEGATIVE
    fill(pixels, 5, 4, 10, 11, color)
    fill(pixels, 4, 5, 11, 10, color)
    fill(pixels, 5, 7, 10, 8, WHITE)
    if positive:
        fill(pixels, 7, 5, 8, 10, WHITE)


def corner_badge(pixels, positive: bool) -> None:
    """A framed square in the top left corner with a white sign."""
    fill(pixels, 1, 1, 7, 7, EDGE)
    fill(pixels, 2, 2, 6, 6, POSITIVE if positive else NEGATIVE)
    fill(pixels, 2, 4, 6, 4, WHITE)
    if positive:
        fill(pixels, 4, 2, 4, 6, WHITE)


def low_badge(pixels, positive: bool, left: int = 5) -> None:
    """A badge in the bottom rows, for the low base of the solar and wind generators."""
    fill(pixels, left, 11, left + 5, 15, EDGE)
    fill(pixels, left + 1, 12, left + 4, 14, POSITIVE if positive else NEGATIVE)
    fill(pixels, left + 1, 13, left + 4, 13, WHITE)
    if positive:
        fill(pixels, left + 2, 12, left + 3, 14, WHITE)


def mark(name: str, draw, positive: bool, out: str | None = None) -> None:
    pixels = source(name)
    draw(pixels, positive)
    write_png(BLOCK / f"{out or name}.png", pixels)


def write_json(path: Path, value) -> None:
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")


def main() -> None:
    for device in ("battery", "rechargeable_battery", "used_battery"):
        mark(f"{device}_front", centre_sign, True)
        mark(f"{device}_back", centre_sign, False)
    for converter in ("step_up_converter", "step_down_transformer"):
        # Input on the front (+) and back (-), output on the top (+) and bottom (-).
        mark(f"{converter}_front", centre_sign, True)
        mark(f"{converter}_back", centre_sign, False)
        mark(f"{converter}_top", centre_sign, True)
        mark(f"{converter}_bottom", centre_sign, False)

    # Coal generator: + on the fire door, a new back face with -.
    mark("coal_generator_front", corner_badge, True)
    mark("coal_generator_side", corner_badge, False, "coal_generator_back")
    model = json.loads((MODELS / "coal_generator.json").read_text(encoding="utf-8"))
    model["textures"]["back"] = "analog_alu:block/coal_generator_back"
    model["elements"][0]["faces"]["south"]["texture"] = "#back"
    write_json(MODELS / "coal_generator.json", model)

    # Diesel generator: it borrowed the oil processor's model; it gets its own, with marked faces.
    mark("oil_processor_front", corner_badge, True, "diesel_generator_front")
    mark("oil_processor_side", corner_badge, False, "diesel_generator_back")
    write_json(MODELS / "diesel_generator.json", {
        "textures": {
            "side": "analog_alu:block/oil_processor_side",
            "front": "analog_alu:block/diesel_generator_front",
            "back": "analog_alu:block/diesel_generator_back",
            "top": "analog_alu:block/oil_processor_top",
            "particle": "analog_alu:block/oil_processor_side",
        },
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": {
                "down": {"texture": "#side"},
                "up": {"texture": "#top"},
                "north": {"texture": "#front"},
                "south": {"texture": "#back"},
                "west": {"texture": "#side"},
                "east": {"texture": "#side"},
            },
        }],
    })

    # Solar and wind generators: the badge sits on the low base, the only part at the terminals.
    for generator in ("solar_generator", "wind_generator"):
        mark(f"{generator}_side", low_badge, True, f"{generator}_positive")
        mark(f"{generator}_side", low_badge, False, f"{generator}_negative")
        model = json.loads((MODELS / f"{generator}.json").read_text(encoding="utf-8"))
        model["textures"]["positive"] = f"analog_alu:block/{generator}_positive"
        model["textures"]["negative"] = f"analog_alu:block/{generator}_negative"
        base = model["elements"][0]
        base["faces"]["north"]["texture"] = "#positive"
        base["faces"]["south"]["texture"] = "#negative"
        write_json(MODELS / f"{generator}.json", model)
    print("terminal marks drawn")


if __name__ == "__main__":
    main()
