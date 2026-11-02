import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu/textures"


def make(fill):
    return [[fill for _ in range(16)] for _ in range(16)]


def rect(grid, x1, y1, x2, y2, rgb):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                grid[y][x] = (*rgb, 255)


def write(path, grid):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in grid)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    image = b"\x89PNG\r\n\x1a\n"
    image += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    image += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    with open(path, "wb") as output:
        output.write(image)


def texture(name, grid, folder):
    write(os.path.join(ROOT, folder, name + ".png"), grid)


oil = make((16, 21, 19, 255))
for y in range(16):
    for x in range(16):
        if (x * 7 + y * 11) % 17 < 3:
            oil[y][x] = (32, 43, 36, 255)
for x, y in ((2, 4), (3, 4), (10, 8), (11, 8), (7, 13)):
    oil[y][x] = (70, 76, 47, 255)
texture("oil_still", oil, "block")

flow = make((13, 18, 17, 255))
for y in range(16):
    for x in range(16):
        flow[y][x] = ((20 + (x * 3 + y) % 11), (27 + (x + y * 2) % 10), (24 + (x * 2 + y) % 9), 255)
for y in (3, 10):
    for x in range(16):
        flow[y][x] = (60, 67, 43, 255)
texture("oil_flow", flow, "block")

processor_side = make((31, 47, 49, 255))
rect(processor_side, 1, 1, 14, 14, (53, 78, 76))
rect(processor_side, 2, 2, 13, 13, (27, 44, 47))
for y in (3, 6, 9, 12):
    rect(processor_side, 4, y, 11, y, (100, 120, 103))
rect(processor_side, 3, 13, 12, 14, (190, 132, 62))
texture("oil_processor_side", processor_side, "block")

processor_front = make((28, 44, 47, 255))
rect(processor_front, 1, 1, 14, 14, (61, 82, 77))
rect(processor_front, 3, 3, 12, 12, (18, 29, 32))
rect(processor_front, 4, 4, 11, 11, (44, 61, 55))
rect(processor_front, 5, 5, 10, 10, (165, 111, 54))
rect(processor_front, 6, 6, 9, 9, (238, 174, 76))
for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
    processor_front[y][x] = (198, 155, 83, 255)
texture("oil_processor_front", processor_front, "block")

processor_top = make((39, 57, 57, 255))
rect(processor_top, 1, 1, 14, 14, (85, 107, 96))
rect(processor_top, 3, 3, 12, 12, (38, 57, 55))
rect(processor_top, 5, 5, 10, 10, (18, 27, 29))
rect(processor_top, 6, 6, 9, 9, (96, 114, 88))
texture("oil_processor_top", processor_top, "block")

drill_side = make((38, 54, 59, 255))
rect(drill_side, 1, 1, 14, 14, (78, 97, 97))
rect(drill_side, 3, 2, 12, 12, (36, 53, 59))
rect(drill_side, 5, 3, 10, 11, (125, 139, 125))
rect(drill_side, 6, 4, 9, 10, (43, 61, 62))
rect(drill_side, 6, 6, 9, 8, (205, 143, 59))
rect(drill_side, 4, 13, 11, 14, (190, 137, 67))
texture("oil_drill_side", drill_side, "block")

drill_front = make((37, 54, 58, 255))
rect(drill_front, 1, 1, 14, 14, (89, 106, 101))
rect(drill_front, 3, 2, 12, 10, (44, 62, 64))
rect(drill_front, 5, 3, 10, 8, (122, 140, 123))
rect(drill_front, 6, 4, 9, 7, (29, 40, 39))
rect(drill_front, 7, 11, 8, 15, (207, 142, 58))
rect(drill_front, 5, 14, 10, 15, (224, 179, 84))
texture("oil_drill_front", drill_front, "block")

drill_top = make((50, 66, 66, 255))
rect(drill_top, 1, 1, 14, 14, (105, 121, 106))
rect(drill_top, 3, 3, 12, 12, (43, 61, 62))
rect(drill_top, 5, 5, 10, 10, (200, 140, 58))
rect(drill_top, 6, 6, 9, 9, (29, 43, 43))
texture("oil_drill_top", drill_top, "block")

items = {
    "oil_bucket": ("#18201d", "#333d32", "#a77b43", "#dfb459"),
    "solid_oil": ("#161b18", "#303832", "#59604b", "#a39361"),
    "diesel": ("#212c29", "#435449", "#b67b34", "#ebbd5d"),
    "plastic_resin": ("#153b42", "#287b7d", "#82c4ae", "#d8e6bf"),
    "asphalt": ("#202326", "#414649", "#717878", "#a4a89e"),
    "tank_bucket_4": ("#25353a", "#51676a", "#75b4a2", "#d6b66c"),
    "tank_bucket_8": ("#25353a", "#51676a", "#d1a950", "#f2d478"),
    "tank_bucket_16": ("#25353a", "#51676a", "#bd6c39", "#e8a45b"),
}
for name, palette in items.items():
    grid = make((0, 0, 0, 0))
    colors = [tuple(bytes.fromhex(value.removeprefix("#"))) for value in palette]
    rect(grid, 4, 1, 11, 3, colors[2])
    rect(grid, 3, 3, 12, 12, colors[0])
    rect(grid, 4, 4, 11, 11, colors[1])
    rect(grid, 5, 5, 10, 10, colors[0])
    rect(grid, 5, 6, 10, 9, colors[2])
    rect(grid, 6, 6, 9, 8, colors[3])
    for x in (4, 11):
        rect(grid, x, 12, x, 14, colors[2])
    texture(name, grid, "item")