"""Generate dedicated cube textures for semiconductor blocks and the mass analyzer."""
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu/textures/block"
TRANSPARENT = (0, 0, 0, 0)


def image(base, edge):
    return [[edge if x in (0, 15) or y in (0, 15) else base for x in range(16)] for y in range(16)]


def rect(grid, x1, y1, x2, y2, color):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            grid[y][x] = color


def line(grid, points, color):
    for x, y in points:
        grid[y][x] = color


def save(name, grid):
    os.makedirs(ROOT, exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes((*pixel, 255)) for pixel in row) for row in grid)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    content = b"\x89PNG\r\n\x1a\n"
    content += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    content += chunk(b"IDAT", zlib.compress(raw))
    content += chunk(b"IEND", b"")
    with open(os.path.join(ROOT, name + ".png"), "wb") as image_file:
        image_file.write(content)


# A ceramic package with visible contacts and a central silicon die.
chip_top = image((28, 47, 53), (12, 20, 27))
rect(chip_top, 2, 2, 13, 13, (50, 76, 80))
rect(chip_top, 4, 4, 11, 11, (19, 34, 43))
rect(chip_top, 5, 5, 10, 10, (83, 53, 121))
rect(chip_top, 6, 6, 9, 9, (132, 89, 190))
rect(chip_top, 7, 7, 8, 8, (224, 188, 255))
for y in (3, 12):
    for x in (4, 6, 9, 11):
        rect(chip_top, x, y, x + 1, y, (135, 223, 208))
for x in (3, 12):
    for y in (4, 6, 9, 11):
        rect(chip_top, x, y, x, y + 1, (135, 223, 208))
line(chip_top, [(2, 7), (3, 7), (3, 8), (4, 8)], (82, 197, 181))
line(chip_top, [(11, 7), (12, 7), (12, 8), (13, 8)], (82, 197, 181))
save("semiconductor_block_top", chip_top)

chip_side = image((25, 42, 48), (9, 17, 22))
for y in (3, 12):
    rect(chip_side, 2, y, 13, y, (61, 93, 94))
rect(chip_side, 3, 5, 12, 10, (36, 57, 64))
rect(chip_side, 6, 6, 9, 9, (76, 53, 105))
for x in (3, 5, 10, 12):
    rect(chip_side, x, 2, x, 3, (143, 196, 179))
    rect(chip_side, x, 12, x, 13, (143, 196, 179))
save("semiconductor_block_side", chip_side)

# Gold-plated sealed package, with a continuous gasket around the silicon window.
seal_top = image((96, 69, 26), (35, 25, 14))
rect(seal_top, 2, 2, 13, 13, (180, 137, 45))
rect(seal_top, 3, 3, 12, 12, (230, 190, 82))
rect(seal_top, 4, 4, 11, 11, (57, 63, 72))
rect(seal_top, 5, 5, 10, 10, (33, 39, 49))
rect(seal_top, 6, 6, 9, 9, (120, 78, 162))
rect(seal_top, 7, 7, 8, 8, (209, 166, 244))
for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
    rect(seal_top, x, y, x, y, (248, 218, 125))
save("waterproof_semiconductor_block_top", seal_top)

seal_side = image((113, 79, 24), (37, 26, 13))
rect(seal_side, 2, 2, 13, 4, (219, 174, 62))
rect(seal_side, 2, 5, 13, 11, (175, 127, 39))
rect(seal_side, 2, 12, 13, 13, (238, 199, 85))
rect(seal_side, 4, 6, 11, 10, (47, 48, 57))
for x in (3, 12):
    rect(seal_side, x, 6, x, 10, (248, 220, 133))
save("waterproof_semiconductor_block_side", seal_side)

# Instrument panel with a quartz sample chamber, copper traces, and segmented readout.
analyzer_top = image((46, 47, 54), (20, 23, 30))
rect(analyzer_top, 2, 2, 13, 13, (74, 81, 87))
rect(analyzer_top, 3, 3, 12, 12, (40, 48, 57))
rect(analyzer_top, 4, 4, 11, 11, (17, 27, 38))
rect(analyzer_top, 5, 5, 10, 10, (102, 173, 195))
rect(analyzer_top, 6, 6, 9, 9, (167, 224, 227))
rect(analyzer_top, 7, 7, 8, 8, (239, 250, 221))
for x in (2, 4, 11, 13):
    rect(analyzer_top, x, 2, x, 3, (196, 117, 62))
    rect(analyzer_top, x, 12, x, 13, (196, 117, 62))
rect(analyzer_top, 5, 1, 10, 2, (220, 165, 77))
save("mass_analyzer_top", analyzer_top)

analyzer_side = image((41, 46, 53), (18, 22, 29))
rect(analyzer_side, 2, 2, 13, 4, (94, 103, 105))
rect(analyzer_side, 2, 5, 13, 11, (54, 63, 72))
rect(analyzer_side, 2, 12, 13, 13, (25, 31, 39))
rect(analyzer_side, 5, 6, 10, 10, (27, 55, 66))
rect(analyzer_side, 6, 7, 9, 9, (81, 164, 178))
for x in (3, 12):
    rect(analyzer_side, x, 6, x, 10, (185, 106, 57))
save("mass_analyzer_side", analyzer_side)

analyzer_active = image((43, 53, 62), (19, 27, 35))
rect(analyzer_active, 2, 2, 13, 13, (67, 93, 103))
rect(analyzer_active, 3, 3, 12, 12, (27, 52, 66))
rect(analyzer_active, 4, 4, 11, 11, (12, 37, 52))
rect(analyzer_active, 5, 5, 10, 10, (71, 176, 191))
rect(analyzer_active, 6, 6, 9, 9, (131, 242, 217))
rect(analyzer_active, 7, 7, 8, 8, (231, 255, 197))
for x in (2, 4, 11, 13):
    rect(analyzer_active, x, 2, x, 3, (255, 190, 81))
    rect(analyzer_active, x, 12, x, 13, (255, 190, 81))
rect(analyzer_active, 5, 1, 10, 2, (106, 246, 204))
save("mass_analyzer_active_top", analyzer_active)

analyzer_active_side = image((37, 53, 64), (17, 28, 38))
rect(analyzer_active_side, 2, 2, 13, 4, (87, 147, 147))
rect(analyzer_active_side, 2, 5, 13, 11, (35, 79, 91))
rect(analyzer_active_side, 2, 12, 13, 13, (21, 43, 53))
rect(analyzer_active_side, 5, 6, 10, 10, (22, 69, 78))
rect(analyzer_active_side, 6, 7, 9, 9, (101, 225, 195))
for x in (3, 12):
    rect(analyzer_active_side, x, 6, x, 10, (241, 161, 67))
save("mass_analyzer_active_side", analyzer_active_side)