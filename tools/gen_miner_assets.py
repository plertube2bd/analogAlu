"""Generate crisp 16x16 pixel textures for the electric mining machine."""
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu/textures/block"


def color(value):
    return tuple(bytes.fromhex(value.removeprefix("#"))) + (255,)


def image(base):
    return [[base for _ in range(16)] for _ in range(16)]


def rect(pixels, x1, y1, x2, y2, value):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            pixels[y][x] = value


def save(name, pixels):
    os.makedirs(ROOT, exist_ok=True)
    scanlines = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(scanlines))
    png += chunk(b"IEND", b"")
    with open(os.path.join(ROOT, f"{name}.png"), "wb") as output:
        output.write(png)


def front():
    pixels = image(color("#17242b"))
    rect(pixels, 1, 1, 14, 14, color("#3c5157"))
    rect(pixels, 2, 2, 13, 13, color("#263940"))
    rect(pixels, 3, 3, 12, 12, color("#18272d"))
    rect(pixels, 4, 4, 11, 11, color("#b87842"))
    rect(pixels, 5, 5, 10, 10, color("#354c50"))
    rect(pixels, 6, 6, 9, 9, color("#202f33"))
    rect(pixels, 7, 5, 8, 10, color("#d9934b"))
    rect(pixels, 5, 7, 10, 8, color("#d9934b"))
    rect(pixels, 7, 7, 8, 8, color("#f1d27b"))
    rect(pixels, 2, 12, 13, 13, color("#ad693e"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixels[y][x] = color("#e4bf79")
    return pixels


def side():
    pixels = image(color("#18262c"))
    rect(pixels, 1, 1, 14, 14, color("#344a50"))
    rect(pixels, 2, 2, 13, 13, color("#263940"))
    rect(pixels, 3, 3, 12, 12, color("#1b2b30"))
    for y in (4, 7, 10):
        rect(pixels, 4, y, 11, y, color("#738582"))
        rect(pixels, 5, y + 1, 10, y + 1, color("#304348"))
    rect(pixels, 2, 6, 4, 9, color("#b87540"))
    rect(pixels, 3, 7, 4, 8, color("#f0ca70"))
    rect(pixels, 11, 6, 13, 9, color("#b87540"))
    rect(pixels, 11, 7, 12, 8, color("#f0ca70"))
    rect(pixels, 4, 12, 11, 12, color("#bd7940"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixels[y][x] = color("#e4bf79")
    return pixels


def top():
    pixels = image(color("#1a2a30"))
    rect(pixels, 1, 1, 14, 14, color("#53686a"))
    rect(pixels, 2, 2, 13, 13, color("#344a50"))
    rect(pixels, 3, 3, 12, 12, color("#202f34"))
    rect(pixels, 4, 4, 11, 11, color("#b87540"))
    rect(pixels, 5, 5, 10, 10, color("#273b40"))
    rect(pixels, 6, 6, 9, 9, color("#66817e"))
    rect(pixels, 7, 7, 8, 8, color("#d8b969"))
    rect(pixels, 4, 5, 6, 7, color("#b87540"))
    rect(pixels, 9, 8, 11, 10, color("#b87540"))
    pixels[6][5] = color("#f0ca70")
    pixels[9][10] = color("#f0ca70")
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixels[y][x] = color("#e4bf79")
    return pixels


for texture_name, pixels in {
    "miner_front": front(),
    "miner_side": side(),
    "miner_top": top(),
}.items():
    save(texture_name, pixels)