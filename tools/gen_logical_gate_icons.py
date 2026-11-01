"""Generate high-contrast inventory icons for the logical gate blocks."""
import json
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu"
GLYPHS = {
    "O": ("111", "101", "101", "101", "111"),
    "R": ("110", "101", "110", "101", "101"),
    "A": ("010", "101", "111", "101", "101"),
    "N": ("101", "111", "111", "111", "101"),
    "D": ("110", "101", "101", "101", "110"),
    "X": ("101", "101", "010", "101", "101"),
}
GATES = {
    "logical_or": ("OR", (238, 112, 78)),
    "logical_and": ("AND", (70, 174, 132)),
    "logical_xor": ("XOR", (72, 142, 218)),
}


def write_png(path, pixels):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    with open(path, "wb") as image:
        image.write(png)


def make_icon(label, accent):
    pixels = [[(31, 39, 43, 255) for _ in range(16)] for _ in range(16)]
    for x in range(16):
        pixels[0][x] = pixels[15][x] = (13, 19, 22, 255)
    for y in range(16):
        pixels[y][0] = pixels[y][15] = (13, 19, 22, 255)
    for y in range(2, 14):
        for x in range(2, 14):
            pixels[y][x] = (45, 54, 57, 255)

    width = len(label) * 4 - 1
    origin_x = (16 - width) // 2
    for index, character in enumerate(label):
        for row, pattern in enumerate(GLYPHS[character]):
            for column, bit in enumerate(pattern):
                if bit == "1":
                    pixels[5 + row][origin_x + index * 4 + column] = (*accent, 255)

    for x in (4, 5, 10, 11):
        pixels[3][x] = (235, 78, 70, 255)
    for x in (7, 8):
        pixels[12][x] = accent + (255,)
    return pixels


for name, (label, color) in GATES.items():
    write_png(f"{ROOT}/textures/item/{name}.png", make_icon(label, color))
    model = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"analog_alu:item/{name}"},
    }
    with open(f"{ROOT}/models/item/{name}_icon.json", "w", encoding="utf-8") as output:
        json.dump(model, output, indent=2)
        output.write("\n")