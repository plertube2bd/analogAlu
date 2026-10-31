"""Generate icons only for the added silicon refining intermediates."""
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu/textures/item"
TRANSPARENT = (0, 0, 0, 0)
PALETTES = {
    "silicon_purification_charge": ((33, 54, 68), (82, 147, 161), (226, 205, 132)),
    "purified_silicon": ((25, 55, 74), (53, 147, 176), (173, 239, 239)),
    "seeded_silicon": ((45, 50, 84), (100, 91, 174), (230, 208, 255)),
    "monocrystalline_silicon": ((34, 54, 79), (80, 148, 193), (213, 242, 255)),
    "wafer_cutting_wire": ((47, 57, 67), (155, 174, 178), (244, 191, 99)),
}
PATTERNS = {
    "silicon_purification_charge": ["................", ".....######.....", "....#......#....", "...#...##...#...", "...#..####..#...", "...#..#  #..#...", "...#..#  #..#...", "...#..####..#...", "...#...##...#...", "....#......#....", ".....######.....", "................"],
    "purified_silicon": ["................", ".......##.......", "......####......", ".....##..##.....", "....##.##.##....", "...##..##..##...", "..##...##...##..", "..##...##...##..", "...##..##..##...", "....##.##.##....", ".....######.....", "......####......", ".......##.......", "................"],
    "seeded_silicon": ["................", ".......##.......", "......####......", ".....##..##.....", "....##.##.##....", "...##..####..##..", "..##...####...##.", "..##....##....##.", "...##...##...##..", "....##..##..##...", ".....##.##.##....", "......######.....", ".......####......", "................"],
    "monocrystalline_silicon": ["................", ".......##.......", "......####......", ".....######.....", "....########....", "...####..####...", "..####.##.####..", "..####.##.####..", "...####..####...", "....########....", ".....######.....", "......####......", ".......##.......", "................"],
    "wafer_cutting_wire": ["................", ".....######.....", "...##......##...", "..#..........#..", ".#............#.", ".#...######...#.", "#...##....##...#", "#...#......#...#", "#...##....##...#", ".#...######...#.", ".#............#.", "..#..........#..", "...##......##...", ".....######.....", "................", "................"],
}


def write_png(path, grid):
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in grid)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as image:
        image.write(png)


os.makedirs(ROOT, exist_ok=True)
for name, (dark, mid, light) in PALETTES.items():
    grid = [[TRANSPARENT for _ in range(16)] for _ in range(16)]
    for y, row in enumerate(PATTERNS[name]):
        for x, pixel in enumerate(row):
            if pixel == ".":
                continue
            color = dark if x in (2, 13) or y in (2, 13) else mid
            if pixel == "#":
                color = light
            grid[y][x] = (*color, 255)
    write_png(os.path.join(ROOT, f"{name}.png"), grid)