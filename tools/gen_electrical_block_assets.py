"""Generate crisp pixel textures for electrical blocks and copper cable."""
import os
import struct
import zlib

ROOT = "src/main/resources/assets/analog_alu/textures/block"


def rgb(hex_color):
    return tuple(bytes.fromhex(hex_color.removeprefix("#"))) + (255,)


def canvas(base):
    return [[base for _ in range(16)] for _ in range(16)]


def rect(image, x1, y1, x2, y2, value):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            image[y][x] = value


def points(image, coords, value):
    for x, y in coords:
        image[y][x] = value


def write_png(name, image):
    os.makedirs(ROOT, exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in image)

    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))

    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw))
    data += chunk(b"IEND", b"")
    with open(os.path.join(ROOT, f"{name}.png"), "wb") as image_file:
        image_file.write(data)


def cable():
    image = canvas(rgb("#18252b"))
    rect(image, 0, 1, 4, 14, rgb("#263940"))
    rect(image, 11, 1, 15, 14, rgb("#263940"))
    rect(image, 5, 0, 10, 15, rgb("#874b29"))
    rect(image, 6, 0, 9, 15, rgb("#c87837"))
    rect(image, 7, 0, 7, 15, rgb("#f0bf68"))
    for y in (1, 6, 7, 12, 13):
        points(image, [(5, y), (10, y)], rgb("#e5a34d"))
    for y in (3, 10):
        rect(image, 0, y, 4, y, rgb("#344b51"))
        rect(image, 11, y, 15, y, rgb("#344b51"))
    return image


def cable_glow():
    image = canvas(rgb("#10242b"))
    rect(image, 0, 1, 4, 14, rgb("#28505a"))
    rect(image, 11, 1, 15, 14, rgb("#28505a"))
    rect(image, 5, 0, 10, 15, rgb("#c6672f"))
    rect(image, 6, 0, 9, 15, rgb("#ff9b3d"))
    rect(image, 7, 0, 8, 15, rgb("#fff0a0"))
    for y in (1, 6, 7, 12, 13):
        points(image, [(5, y), (10, y)], rgb("#ffd66f"))
    for y in (3, 10):
        rect(image, 0, y, 4, y, rgb("#3a7378"))
        rect(image, 11, y, 15, y, rgb("#3a7378"))
    return image


def cable_lock():
    image = canvas(rgb("#15262c"))
    rect(image, 2, 2, 13, 13, rgb("#344a4d"))
    rect(image, 3, 3, 12, 12, rgb("#202f32"))
    rect(image, 5, 4, 10, 9, rgb("#e8b958"))
    rect(image, 6, 5, 9, 8, rgb("#263940"))
    rect(image, 4, 8, 11, 12, rgb("#e8b958"))
    rect(image, 5, 9, 10, 11, rgb("#f6d87a"))
    rect(image, 7, 9, 8, 10, rgb("#704d2a"))
    points(image, [(2, 2), (13, 2), (2, 13), (13, 13)], rgb("#f4d887"))
    return image


def battery_front():
    image = canvas(rgb("#17242b"))
    rect(image, 1, 1, 14, 14, rgb("#31464d"))
    rect(image, 2, 2, 13, 13, rgb("#466168"))
    rect(image, 3, 3, 12, 12, rgb("#20343b"))
    rect(image, 4, 4, 11, 11, rgb("#17272d"))
    rect(image, 5, 5, 10, 10, rgb("#29464d"))
    rect(image, 6, 6, 9, 9, rgb("#1b3037"))
    rect(image, 7, 6, 8, 9, rgb("#d8a85b"))
    points(image, [(5, 7), (6, 7), (9, 8), (10, 8)], rgb("#f1cb79"))
    rect(image, 3, 12, 12, 12, rgb("#c77d43"))
    points(image, [(2, 2), (13, 2), (2, 13), (13, 13)], rgb("#e1bd77"))
    return image


def battery_side():
    image = canvas(rgb("#1a2a30"))
    rect(image, 1, 1, 14, 14, rgb("#3d555b"))
    rect(image, 2, 2, 13, 13, rgb("#263a40"))
    rect(image, 3, 3, 12, 12, rgb("#1c2b31"))
    for x in (4, 6, 9, 11):
        rect(image, x, 4, x, 11, rgb("#668083"))
        rect(image, x + 1, 4, x + 1, 11, rgb("#344d53"))
    rect(image, 3, 3, 12, 3, rgb("#b47a47"))
    rect(image, 3, 12, 12, 12, rgb("#b47a47"))
    points(image, [(2, 2), (13, 2), (2, 13), (13, 13)], rgb("#e1bd77"))
    return image


def battery_top():
    image = canvas(rgb("#1b2b31"))
    rect(image, 1, 1, 14, 14, rgb("#536a6c"))
    rect(image, 2, 2, 13, 13, rgb("#344b51"))
    rect(image, 3, 3, 12, 12, rgb("#263a40"))
    rect(image, 4, 5, 6, 10, rgb("#c7773e"))
    rect(image, 9, 5, 11, 10, rgb("#c7773e"))
    rect(image, 5, 6, 5, 9, rgb("#f0c66d"))
    rect(image, 10, 6, 10, 9, rgb("#f0c66d"))
    points(image, [(2, 2), (13, 2), (2, 13), (13, 13)], rgb("#e1bd77"))
    return image


def switch(front_active):
    image = canvas(rgb("#17242b"))
    rect(image, 1, 1, 14, 14, rgb("#344b51"))
    rect(image, 2, 2, 13, 13, rgb("#20343b"))
    rect(image, 3, 3, 12, 12, rgb("#17272d"))
    rect(image, 4, 5, 11, 11, rgb("#49636a"))
    rect(image, 5, 6, 10, 10, rgb("#253b42"))
    rect(image, 6, 7, 9, 9, rgb("#c78345"))
    if front_active:
        rect(image, 7, 4, 8, 6, rgb("#67d6c1"))
        rect(image, 7, 5, 8, 8, rgb("#b9f1ce"))
    else:
        rect(image, 7, 8, 8, 11, rgb("#d99b58"))
        rect(image, 7, 10, 8, 11, rgb("#f0c66d"))
    points(image, [(2, 2), (13, 2), (2, 13), (13, 13)], rgb("#e1bd77"))
    return image


for texture, image in {
    "copper_cable": cable(),
    "copper_cable_glow": cable_glow(),
    "copper_cable_lock": cable_lock(),
    "battery_front": battery_front(),
    "battery_side": battery_side(),
    "battery_top": battery_top(),
    "circuit_switch_front_off": switch(False),
    "circuit_switch_front_on": switch(True),
    "circuit_switch_side": battery_side(),
    "circuit_switch_top": battery_top(),
}.items():
    write_png(texture, image)