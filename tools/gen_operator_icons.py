"""Generate readable inventory icons for the ALU operator blocks."""
import json
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu"
GLYPHS = {
    "A": ("010", "101", "111", "101", "101"),
    "D": ("110", "101", "101", "101", "110"),
    "E": ("111", "100", "110", "100", "111"),
    "H": ("101", "101", "111", "101", "101"),
    "I": ("111", "010", "010", "010", "111"),
    "L": ("100", "100", "100", "100", "111"),
    "N": ("101", "111", "111", "111", "101"),
    "O": ("111", "101", "101", "101", "111"),
    "R": ("110", "101", "110", "101", "101"),
    "S": ("111", "100", "111", "001", "111"),
    "T": ("111", "010", "010", "010", "010"),
    "X": ("101", "101", "010", "101", "101"),
    "+": ("000", "010", "111", "010", "000"),
    "-": ("000", "000", "111", "000", "000"),
    "<": ("001", "010", "100", "010", "001"),
    ">": ("100", "010", "001", "010", "100"),
    "~": ("000", "010", "101", "000", "000"),
    "!": ("010", "010", "010", "000", "010"),
}
ICONS = {
    "adder": ("+", (72, 194, 104)),
    "subtractor": ("-", (88, 142, 230)),
    "extender": (">", (239, 157, 65)),
    "shift_right": (">>", (185, 104, 221)),
    "shift_left": ("<<", (66, 197, 211)),
    "bitwise_not": ("~", (232, 107, 167)),
    "logical_not": ("!", (221, 146, 86)),
    "and_gate": ("AND", (232, 190, 68)),
    "or_gate": ("OR", (231, 91, 83)),
    "xor_gate": ("XOR", (76, 195, 141)),
}


def write_png(path, pixels):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    result = b"\x89PNG\r\n\x1a\n"
    result += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    result += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    with open(path, "wb") as output:
        output.write(result)


def make_icon(label, accent):
    pixels = [[(29, 37, 41, 255) for _ in range(16)] for _ in range(16)]
    for y in range(1, 15):
        for x in range(1, 15):
            pixels[y][x] = (48, 58, 61, 255)
    for x in range(16):
        pixels[0][x] = pixels[15][x] = (12, 18, 21, 255)
    for y in range(16):
        pixels[y][0] = pixels[y][15] = (12, 18, 21, 255)

    text_width = len(label) * 4 - 1
    origin_x = (16 - text_width) // 2
    origin_y = 5 if len(label) == 1 else 6
    for letter_index, character in enumerate(label):
        for row_index, row in enumerate(GLYPHS[character]):
            for column_index, pixel in enumerate(row):
                if pixel == "1":
                    x = origin_x + letter_index * 4 + column_index
                    y = origin_y + row_index
                    pixels[y][x] = (*accent, 255)

    for x in range(5, 11):
        pixels[3][x] = (*accent, 255)
    for x in (4, 11):
        pixels[12][x] = (*accent, 255)
    return pixels


for name, (label, accent) in ICONS.items():
    write_png(f"{ROOT}/textures/item/{name}.png", make_icon(label, accent))
    model = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"analog_alu:item/{name}"},
    }
    with open(f"{ROOT}/models/item/{name}_icon.json", "w", encoding="utf-8") as output:
        json.dump(model, output, indent=2)
        output.write("\n")