"""Generate crisp 16x16 pixel textures for the electrical generators."""
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu/textures/block"
TRANSPARENT = (0, 0, 0, 0)


def color(value):
    value = value.removeprefix("#")
    return tuple(bytes.fromhex(value)) + (255,)


def make_grid(fill=TRANSPARENT):
    return [[fill for _ in range(16)] for _ in range(16)]


def rect(grid, x1, y1, x2, y2, value):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                grid[y][x] = value


def pixel(grid, x, y, value):
    if 0 <= x < 16 and 0 <= y < 16:
        grid[y][x] = value


def write_png(name, grid):
    path = os.path.join(ROOT, f"{name}.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in grid)

    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as image:
        image.write(png)


def coal_front():
    grid = make_grid(color("#17242b"))
    rect(grid, 1, 1, 14, 14, color("#243840"))
    rect(grid, 2, 2, 13, 13, color("#49616a"))
    rect(grid, 3, 3, 12, 12, color("#18262d"))
    rect(grid, 4, 5, 11, 12, color("#10191e"))
    rect(grid, 5, 6, 10, 11, color("#733d25"))
    rect(grid, 6, 7, 9, 10, color("#e77732"))
    rect(grid, 7, 8, 8, 10, color("#ffd06b"))
    for x in (4, 7, 10):
        rect(grid, x, 5, x, 12, color("#839394"))
    rect(grid, 5, 4, 10, 4, color("#b77a48"))
    rect(grid, 6, 2, 9, 3, color("#b77a48"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixel(grid, x, y, color("#e3b46b"))
    return grid


def coal_side():
    grid = make_grid(color("#1b2b31"))
    rect(grid, 1, 1, 14, 14, color("#344a51"))
    rect(grid, 2, 2, 13, 13, color("#263a41"))
    rect(grid, 3, 3, 12, 12, color("#1b292e"))
    rect(grid, 3, 4, 5, 11, color("#b87543"))
    rect(grid, 4, 5, 4, 10, color("#d69a58"))
    for y in (4, 6, 8, 10, 12):
        rect(grid, 7, y, 12, y, color("#7d9291"))
    rect(grid, 8, 5, 11, 5, color("#b8c2ad"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixel(grid, x, y, color("#e2af62"))
    return grid


def coal_top():
    grid = make_grid(color("#263a40"))
    rect(grid, 1, 1, 14, 14, color("#60777a"))
    rect(grid, 2, 2, 13, 13, color("#334a50"))
    rect(grid, 4, 3, 11, 12, color("#18262b"))
    rect(grid, 5, 4, 10, 11, color("#795032"))
    rect(grid, 6, 5, 9, 10, color("#20272a"))
    rect(grid, 7, 6, 8, 9, color("#c87837"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixel(grid, x, y, color("#e2af62"))
    return grid


def wind_side():
    grid = make_grid(color("#18272d"))
    rect(grid, 1, 1, 14, 14, color("#405860"))
    rect(grid, 2, 2, 13, 13, color("#263b42"))
    rect(grid, 3, 3, 12, 12, color("#1a2a30"))
    rect(grid, 4, 4, 11, 5, color("#6f8585"))
    rect(grid, 4, 7, 11, 11, color("#243a41"))
    for x in (5, 7, 9, 11):
        rect(grid, x, 7, x, 11, color("#8ca6a1"))
    rect(grid, 4, 12, 11, 12, color("#d08b52"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixel(grid, x, y, color("#72d5c7"))
    return grid


def wind_top():
    grid = make_grid(color("#1c3037"))
    rect(grid, 1, 1, 14, 14, color("#6c8281"))
    rect(grid, 2, 2, 13, 13, color("#324b52"))
    rect(grid, 4, 4, 11, 11, color("#1b2b31"))
    rect(grid, 6, 6, 9, 9, color("#79d7c6"))
    pixel(grid, 7, 7, color("#d8f2cd"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixel(grid, x, y, color("#e3b36b"))
    return grid


def wind_rotor():
    grid = make_grid()
    dark = color("#263e46")
    steel = color("#a6c2b9")
    cyan = color("#63d9ca")
    rect(grid, 7, 1, 8, 14, dark)
    rect(grid, 7, 1, 8, 5, steel)
    rect(grid, 7, 11, 8, 14, steel)
    rect(grid, 1, 7, 14, 8, dark)
    rect(grid, 1, 7, 5, 8, steel)
    rect(grid, 11, 7, 14, 8, steel)
    rect(grid, 7, 1, 8, 2, cyan)
    rect(grid, 13, 7, 14, 8, cyan)
    rect(grid, 6, 6, 9, 9, color("#e0b56e"))
    rect(grid, 7, 7, 8, 8, color("#f4e3a8"))
    return grid


def solar_side():
    grid = make_grid(color("#172830"))
    rect(grid, 1, 1, 14, 14, color("#667d7e"))
    rect(grid, 2, 2, 13, 13, color("#344b51"))
    rect(grid, 3, 4, 12, 11, color("#24363d"))
    rect(grid, 4, 5, 11, 6, color("#4d6870"))
    rect(grid, 4, 9, 11, 10, color("#1b292e"))
    rect(grid, 5, 7, 10, 8, color("#d99b58"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixel(grid, x, y, color("#e3b36b"))
    return grid


def solar_panel():
    grid = make_grid(color("#152c49"))
    rect(grid, 0, 0, 15, 1, color("#cf9b59"))
    rect(grid, 0, 14, 15, 15, color("#cf9b59"))
    rect(grid, 0, 2, 1, 13, color("#cf9b59"))
    rect(grid, 14, 2, 15, 13, color("#cf9b59"))
    for x in (5, 10):
        rect(grid, x, 2, x, 13, color("#56a9c5"))
    for y in (5, 9):
        rect(grid, 2, y, 13, y, color("#56a9c5"))
    rect(grid, 2, 2, 4, 4, color("#244e78"))
    rect(grid, 6, 2, 9, 4, color("#1d416b"))
    rect(grid, 11, 2, 13, 4, color("#244e78"))
    rect(grid, 2, 6, 4, 8, color("#1d416b"))
    rect(grid, 6, 6, 9, 8, color("#2b5b86"))
    rect(grid, 11, 6, 13, 8, color("#1d416b"))
    rect(grid, 2, 10, 4, 13, color("#244e78"))
    rect(grid, 6, 10, 9, 13, color("#1d416b"))
    rect(grid, 11, 10, 13, 13, color("#244e78"))
    pixel(grid, 7, 7, color("#f3d37e"))
    return grid


for texture_name, texture in {
    "coal_generator_front": coal_front(),
    "coal_generator_side": coal_side(),
    "coal_generator_top": coal_top(),
    "wind_generator_side": wind_side(),
    "wind_generator_top": wind_top(),
    "wind_generator_rotor": wind_rotor(),
    "solar_generator_side": solar_side(),
    "solar_generator_panel": solar_panel(),
}.items():
    write_png(texture_name, texture)