"""Generate pixel-art icons for the silicon manufacturing progression."""
import json
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu"


def write_png(path, grid):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in grid)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as image:
        image.write(png)


def make_icon(name, palette, motif):
    transparent = (0, 0, 0, 0)
    grid = [[transparent for _ in range(16)] for _ in range(16)]
    dark, mid, light, glow = palette

    def put(x, y, color):
        if 0 <= x < 16 and 0 <= y < 16:
            grid[y][x] = (*color, 255)

    def wafer(color, edge):
        for y in range(3, 13):
            for x in range(2, 14):
                put(x, y, edge if x in (2, 13) or y in (3, 12) else color)
        for x, y in ((4, 2), (5, 2), (10, 13), (11, 13)):
            put(x, y, edge)
        for x, y in ((4, 5), (11, 5), (4, 10), (11, 10)):
            put(x, y, glow)

    if motif == "crystal":
        rows = [
            (5, 10), (4, 11), (3, 12), (2, 13), (2, 13), (3, 12),
            (4, 11), (5, 10), (6, 9), (7, 8), (8, 7), (9, 6),
        ]
        for y, (left, right) in enumerate(rows, 2):
            for x in range(left, right + 1):
                put(x, y, dark if x in (left, right) else mid)
        for x, y in ((7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (5, 7), (6, 7)):
            put(x, y, light)
    elif motif == "gas":
        for cx, cy, radius in ((5, 7, 3), (9, 6, 4), (7, 10, 3), (11, 9, 2)):
            for y in range(2, 14):
                for x in range(2, 14):
                    if (x - cx) ** 2 + (y - cy) ** 2 <= radius ** 2:
                        put(x, y, mid)
        for x, y in ((6, 5), (10, 4), (8, 8), (4, 9), (11, 11)):
            put(x, y, glow)
    else:
        wafer(mid, dark)
        if motif == "wafer":
            for x in range(5, 11):
                put(x, 7, light)
            for y in range(5, 11):
                put(7, y, light)
        elif motif == "quartz":
            for i in range(4):
                put(5 + i, 5 + i, light)
                put(10 - i, 5 + i, glow)
            put(7, 8, light)
            put(8, 8, light)
        elif motif == "implanted":
            for x, y in ((5, 4), (6, 5), (7, 6), (8, 7), (9, 8), (10, 9), (11, 10),
                         (10, 4), (9, 5), (8, 6), (7, 7), (6, 8), (5, 9), (4, 10)):
                put(x, y, glow)
            put(7, 8, light)
            put(8, 8, light)
        elif motif == "selected":
            for y in range(5, 11):
                put(7, y, light)
                put(8, y, glow)
            for x in range(5, 11):
                put(x, 7, glow)
                put(x, 8, light)
        elif motif == "focused":
            for x, y in ((7, 4), (8, 4), (6, 5), (9, 5), (5, 6), (10, 6), (4, 7), (11, 7),
                         (4, 8), (11, 8), (5, 9), (10, 9), (6, 10), (9, 10), (7, 11), (8, 11)):
                put(x, y, glow)
            put(7, 7, light)
            put(8, 7, light)
            put(7, 8, light)
            put(8, 8, light)
        elif motif == "inactive":
            for x, y in ((6, 6), (7, 6), (8, 6), (9, 6), (5, 7), (10, 7), (5, 8), (10, 8),
                         (6, 9), (7, 9), (8, 9), (9, 9)):
                put(x, y, glow)
            put(7, 7, light)
            put(8, 8, light)
        elif motif == "chip":
            for x in range(5, 11):
                put(x, 4, light)
                put(x, 11, light)
            for y in range(5, 11):
                put(4, y, light)
                put(11, y, light)
            for x, y in ((6, 6), (9, 6), (7, 7), (8, 7), (6, 9), (9, 9), (7, 10), (8, 10)):
                put(x, y, glow)

    write_png(f"{ROOT}/textures/item/{name}.png", grid)
    model = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"analog_alu:item/{name}"},
    }
    os.makedirs(f"{ROOT}/models/item", exist_ok=True)
    with open(f"{ROOT}/models/item/{name}.json", "w", encoding="utf-8") as model_file:
        json.dump(model, model_file, indent=2)

    item_definition = {"model": {"type": "minecraft:model", "model": f"analog_alu:item/{name}"}}
    with open(f"{ROOT}/items/{name}.json", "w", encoding="utf-8") as item_file:
        json.dump(item_definition, item_file, indent=2)


ITEMS = {
    "silicon_boule": ((35, 57, 78), (72, 131, 167), (178, 225, 238), (106, 225, 228), "crystal"),
    "silicon": ((39, 68, 77), (63, 155, 158), (192, 236, 220), (102, 235, 196), "crystal"),
    "silicon_wafer": ((38, 67, 83), (67, 140, 157), (169, 219, 218), (108, 236, 210), "wafer"),
    "quartz_treated_wafer": ((61, 58, 84), (127, 111, 157), (224, 214, 235), (178, 150, 236), "quartz"),
    "ion_implanted_silicon": ((50, 42, 84), (110, 75, 171), (214, 195, 247), (122, 220, 247), "implanted"),
    "strong_ion_gas": ((39, 82, 91), (60, 169, 161), (190, 246, 220), (115, 255, 202), "gas"),
    "mass_selected_silicon": ((52, 63, 80), (82, 127, 161), (224, 203, 121), (255, 218, 91), "selected"),
    "accelerated_focused_silicon": ((68, 43, 74), (155, 71, 145), (252, 205, 133), (255, 237, 129), "focused"),
    "inactive_doped_wafer": ((73, 48, 73), (153, 85, 133), (226, 182, 198), (239, 117, 170), "inactive"),
    "semiconductor": ((46, 35, 77), (109, 66, 163), (223, 188, 255), (190, 112, 255), "chip"),
}


for item_name, (dark, mid, light, glow, motif) in ITEMS.items():
    make_icon(item_name, (dark, mid, light, glow), motif)