#!/usr/bin/env python3
"""Generates the 1.6.0 factory/rail resources and the tram models. Standard library only.

Run from anywhere; everything is written under src/main. Re-run after editing a table below.
"""
from __future__ import annotations

import json
import math
import struct
import zlib
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / "redstone_transportation"
DATA = RES / "data" / "redstone_transportation"
JAVA = ROOT / "src" / "main" / "java" / "com" / "example" / "redstonetransportation"
NS = "redstone_transportation"

MACHINES = [
    "tree_cutter", "auto_planter", "growth_booster", "crop_harvester",
    "quarry_drill", "item_collector",
]
STATUSES = ["off", "ready", "working", "blocked"]
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
CONTROLLER_MODES = ["stopped", "running", "signal"]
EXISTING_BLOCKS = [
    "vehicle_controller", "alu_train_controller", "redstone_motor", "redstone_motor_2",
    "redstone_motor_3", "redstone_motor_4", "power_cell", "vehicle_station", "signal_relay",
]
CORES = ["elevator_core", "ship_helm", "airship_helm"]
FURNACE_KINDS = ["upgraded_furnace", "upgraded_blast_furnace", "upgraded_smoker"]
FURNACE_TIERS = [1, 2, 3]
FURNACES = [f"{kind}_{tier}" for kind in FURNACE_KINDS for tier in FURNACE_TIERS]
# Not a factory machine, but it wears the same arrow and status colours.
LEVELER = "land_leveler"
NEW_BLOCKS = MACHINES + [LEVELER, "factory_controller", "conveyor_belt", "drive_rail"] + CORES + [
    "lift_balloon", "elevator_call", "route_beacon"] + FURNACES


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


# --------------------------------------------------------------------------- pixels

class Canvas:
    def __init__(self, width: int, height: int, fill=(0, 0, 0, 0)):
        self.width = width
        self.height = height
        self.pixels = [[fill for _ in range(width)] for _ in range(height)]

    def set(self, x: int, y: int, color) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y][x] = color if len(color) == 4 else (*color, 255)

    def rect(self, x0: int, y0: int, x1: int, y1: int, color) -> None:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, color)

    def frame(self, x0: int, y0: int, x1: int, y1: int, color) -> None:
        for x in range(x0, x1 + 1):
            self.set(x, y0, color)
            self.set(x, y1, color)
        for y in range(y0, y1 + 1):
            self.set(x0, y, color)
            self.set(x1, y, color)

    def stamp(self, rows: list[str], palette: dict, ox: int = 0, oy: int = 0) -> None:
        for y, row in enumerate(rows):
            for x, symbol in enumerate(row):
                if symbol in palette:
                    self.set(ox + x, oy + y, palette[symbol])

    def paste(self, other: "Canvas", ox: int, oy: int) -> None:
        for y in range(other.height):
            for x in range(other.width):
                self.set(ox + x, oy + y, other.pixels[y][x])

    def save(self, path: Path) -> None:
        raw = bytearray()
        for row in self.pixels:
            raw.append(0)
            for pixel in row:
                raw.extend(pixel)

        def chunk(kind: bytes, body: bytes) -> bytes:
            return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(
            b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b"")
        )


# The steel-and-rivets plate shared with the motors and controllers.
STEEL = (61, 68, 79)
STEEL_DARK = (43, 47, 56)
STEEL_LIGHT = (83, 92, 104)
RIVET = (171, 177, 184)
OUTLINE = (16, 19, 26)


def plate() -> Canvas:
    canvas = Canvas(16, 16, (*STEEL, 255))
    canvas.frame(0, 0, 15, 15, STEEL_DARK)
    canvas.rect(1, 1, 14, 1, STEEL_LIGHT)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        canvas.set(x, y, RIVET)
    return canvas


def top_arrow(fill, shine) -> Canvas:
    canvas = plate()
    arrow = [
        ".......##.......",
        "......#oo#......",
        ".....#osso#.....",
        "....#oossoo#....",
        "...#oooooooo#...",
        "..#oooooooooo#..",
        ".####oooooo####.",
        "....#oooooo#....",
        "....#oooooo#....",
        "....#oooooo#....",
        "....#oooooo#....",
        "....#oooooo#....",
        "....#oooooo#....",
        "....########....",
    ]

    canvas.stamp(arrow, {"#": OUTLINE, "o": fill, "s": shine}, 0, 1)
    return canvas


STATUS_COLORS = {
    "off": ((96, 102, 112), (126, 132, 140)),
    "ready": ((238, 196, 62), (255, 230, 130)),
    "working": ((64, 232, 120), (160, 255, 190)),
    "blocked": ((232, 62, 58), (255, 138, 130)),
}


def machine_side() -> Canvas:
    canvas = plate()
    canvas.rect(0, 7, 15, 8, (168, 96, 52))
    canvas.rect(0, 7, 15, 7, (208, 132, 76))
    canvas.rect(5, 5, 10, 10, STEEL_DARK)
    canvas.rect(6, 6, 9, 9, (58, 196, 200))
    canvas.rect(6, 6, 7, 7, (140, 232, 232))
    return canvas


def machine_bottom() -> Canvas:
    canvas = plate()
    canvas.rect(4, 4, 11, 11, STEEL_DARK)
    return canvas


def icon_front(rows: list[str], palette: dict, ground) -> Canvas:
    canvas = plate()
    canvas.rect(2, 2, 13, 13, ground)
    canvas.frame(2, 2, 13, 13, OUTLINE)
    canvas.stamp(rows, palette, 2, 2)
    return canvas


FRONT_ICONS = {
    "tree_cutter": (
        (66, 44, 30),
        [
            "............",
            ".....ss.....",
            "...s.SS.s...",
            "...SSSSSS...",
            "..SSSddSSSs.",
            ".sSSdkkdSS..",
            "..SSdkkdSSs.",
            ".sSSSddSSS..",
            "...SSSSSS...",
            "...s.SS.s...",
            ".....ss.....",
            "............",
        ],
        {"S": (196, 204, 212), "s": (232, 238, 242), "d": (116, 124, 134), "k": (30, 32, 38)},
    ),
    "auto_planter": (
        (58, 42, 30),
        [
            "............",
            ".......gg...",
            "......gGGg..",
            "..gg..gGg...",
            ".gGGg.gg....",
            "..gGGgg.....",
            "....gGg.....",
            ".....G......",
            ".....G......",
            "..bbbbbbbb..",
            "...bBBBBb...",
            "............",
        ],
        {"g": (86, 196, 82), "G": (44, 142, 56), "b": (120, 82, 54), "B": (86, 58, 38)},
    ),
    "growth_booster": (
        (38, 56, 44),
        [
            "............",
            ".....gg.....",
            "....gGGg....",
            "...gGGGGg...",
            "..gGGGGGGg..",
            ".....GG.....",
            "..w..GG..w..",
            ".www.GG.www.",
            "..w..GG..w..",
            "....wGGw....",
            "...www.www..",
            "............",
        ],
        {"g": (130, 232, 130), "G": (60, 184, 86), "w": (236, 232, 214)},
    ),
    "crop_harvester": (
        (70, 56, 28),
        [
            "............",
            "..mmmmmmm...",
            ".mMMMMMMMm..",
            ".......mMm..",
            "...y...yMm..",
            "..yyy.yy.m..",
            "...y.yyy....",
            "..yyy.y.....",
            "...y..y.....",
            "...y..y.....",
            "...y..y.....",
            "............",
        ],
        {"m": (150, 160, 170), "M": (214, 222, 228), "y": (236, 196, 74)},
    ),
    "quarry_drill": (
        (44, 48, 58),
        [
            "............",
            "....ssss....",
            "....sSSs....",
            "...ssSSss...",
            "...sSSSSs...",
            "....sSSs....",
            "....cSSc....",
            "....cCCc....",
            ".....cc.....",
            ".....cC.....",
            ".....Cc.....",
            "............",
        ],
        {"s": (104, 112, 122), "S": (176, 184, 192), "c": (64, 212, 222), "C": (176, 248, 250)},
    ),
    "land_leveler": (
        (58, 74, 92),
        [
            "............",
            "......yyyy..",
            "......yWWy..",
            "s..yyyyyyyy.",
            "sS.yYYYYYYy.",
            "sS.yYYYYYYy.",
            "sSyyyyyyyyy.",
            "sS.tttttttt.",
            "s..tTtTtTtt.",
            "....tttttt..",
            "............",
            "............",
        ],
        {"y": (236, 170, 40), "Y": (255, 214, 96), "W": (168, 222, 240), "s": (120, 128, 138),
         "S": (196, 204, 212), "t": (30, 32, 38), "T": (96, 100, 108)},
    ),
    "item_collector": (
        (54, 46, 62),
        [
            "............",
            "..rrr..bbb..",
            "..rRr..bBb..",
            "..rRr..bBb..",
            "..rRr..bBb..",
            "..rRr..bBb..",
            "..rRrrrbBb..",
            "...rRRRBb...",
            "....rrbb....",
            "............",
            "..w..w...w..",
            "............",
        ],
        {"r": (214, 60, 56), "R": (168, 34, 36), "b": (66, 122, 226), "B": (42, 84, 178), "w": (232, 226, 150)},
    ),
}


def controller_face(mode: str) -> Canvas:
    colors = {"stopped": (214, 58, 54), "running": (64, 232, 120), "signal": (70, 196, 246)}
    canvas = plate()
    canvas.rect(3, 3, 12, 12, OUTLINE)
    canvas.rect(4, 4, 11, 11, (24, 30, 38))
    color = colors[mode]
    if mode == "stopped":
        canvas.rect(5, 5, 10, 10, color)
    elif mode == "running":
        canvas.stamp(["c.....", "ccc...", "cccccc", "cccccc", "ccc...", "c....."], {"c": color}, 5, 5)
    else:
        canvas.stamp(["c..c.c", "c..c.c", "cc.c.c", "cccc.c", "cccccc", "cccccc"], {"c": color}, 5, 5)
    return canvas


def controller_side() -> Canvas:
    canvas = plate()
    canvas.rect(0, 7, 15, 8, (168, 96, 52))
    canvas.rect(6, 6, 9, 9, STEEL_DARK)
    return canvas


def conveyor_frame(offset: int, moving: bool) -> Canvas:
    canvas = Canvas(16, 16, (40, 42, 48, 255))
    canvas.rect(0, 0, 1, 15, (116, 122, 130))
    canvas.rect(14, 0, 15, 15, (116, 122, 130))
    canvas.rect(0, 0, 0, 15, (78, 84, 92))
    canvas.rect(15, 0, 15, 15, (78, 84, 92))
    stripe = (238, 180, 52) if moving else (150, 112, 44)
    for row in range(0, 16, 8):
        y0 = (row - offset) % 16
        for dx in range(6):
            canvas.set(7 - dx, (y0 + dx) % 16, stripe)
            canvas.set(8 + dx, (y0 + dx) % 16, stripe)
            canvas.set(7 - dx, (y0 + dx + 1) % 16, stripe)
            canvas.set(8 + dx, (y0 + dx + 1) % 16, stripe)
    return canvas


def conveyor_curve(offset: int, moving: bool) -> Canvas:
    """The straight belt bent round the top left corner: in from the west, out to the north."""
    straight = conveyor_frame(offset, moving)
    canvas = Canvas(16, 16, (78, 84, 92, 255))
    for y in range(16):
        for x in range(16):
            radius = math.hypot(x + 0.5, y + 0.5)
            if radius < 16:
                along = math.atan2(y + 0.5, x + 0.5) / (math.pi / 2) * 16
                canvas.pixels[y][x] = straight.pixels[min(int(along), 15)][int(radius)]
    return canvas


def conveyor_both(offset: int, moving: bool) -> Canvas:
    """Two bends meeting down the middle: in from both sides, out to the north."""
    bend = conveyor_curve(offset, moving)
    canvas = Canvas(16, 16)
    for y in range(16):
        for x in range(8):
            canvas.pixels[y][x] = bend.pixels[y][x]
            canvas.pixels[y][15 - x] = bend.pixels[y][x]
    return canvas


def conveyor_side() -> Canvas:
    canvas = Canvas(16, 16, (86, 92, 100, 255))
    canvas.rect(0, 0, 15, 0, (120, 126, 134))
    canvas.rect(0, 3, 15, 3, (52, 56, 64))
    for x in (2, 7, 12):
        canvas.rect(x, 1, x + 1, 2, (40, 42, 48))
    return canvas


def drive_rail_corner(core) -> Canvas:
    """A quarter circle joining the south and east edges, matching the straight rail's gauge."""
    canvas = Canvas(16, 16)
    tie = (84, 62, 44)
    steel = (152, 160, 170)
    for y in range(16):
        for x in range(16):
            dx = 16.0 - (x + 0.5)
            dy = 16.0 - (y + 0.5)
            distance = math.hypot(dx, dy)
            sector = int(math.degrees(math.atan2(dy, dx)) // 11.25)
            if 2.0 < distance < 14.5 and sector % 2 == 0:
                canvas.set(x, y, tie)
            if abs(distance - 8.0) < 1.0:
                canvas.set(x, y, core)
            if abs(distance - 12.5) < 0.72 or abs(distance - 3.5) < 0.72:
                canvas.set(x, y, steel)
    return canvas


def drive_rail(lit: bool, curved: bool, arrow: bool) -> Canvas:
    core = (255, 72, 56) if lit else (112, 40, 38)
    if curved:
        return drive_rail_corner(core)

    canvas = Canvas(16, 16)
    tie = (84, 62, 44)
    tie_light = (112, 84, 58)
    steel = (152, 160, 170)
    steel_dark = (98, 104, 114)
    for y in (1, 5, 9, 13):
        canvas.rect(1, y, 14, y + 1, tie)
        canvas.rect(1, y, 14, y, tie_light)
    canvas.rect(3, 0, 3, 15, steel)
    canvas.rect(12, 0, 12, 15, steel)
    canvas.rect(4, 0, 4, 15, steel_dark)
    canvas.rect(11, 0, 11, 15, steel_dark)
    if arrow:
        chevron = ["....##....", "...####...", "..######..", ".##.##.##.", "....##....", "....##...."]
        canvas.stamp(chevron, {"#": core}, 3, 1)
        canvas.stamp(chevron, {"#": core}, 3, 9)
    else:
        canvas.rect(7, 0, 8, 15, core)
    return canvas

CORE_COLORS = {
    "elevator_core": ((238, 196, 62), (255, 230, 130)),
    "ship_helm": ((70, 150, 226), (160, 214, 255)),
    "airship_helm": ((226, 92, 72), (255, 170, 150)),
}
CORE_ICONS = {
    "elevator_core": (
        [
            "....##....",
            "...####...",
            "..######..",
            "....##....",
            "....##....",
            "....##....",
            "....##....",
            "..######..",
            "...####...",
            "....##....",
        ],
        {"#": (238, 196, 62)},
    ),
    "ship_helm": (
        [
            "...####...",
            "..#....#..",
            ".#..##..#.",
            "#..#..#..#",
            "#.#.oo.#.#",
            "#.#.oo.#.#",
            "#..#..#..#",
            ".#..##..#.",
            "..#....#..",
            "...####...",
        ],
        {"#": (176, 118, 66), "o": (70, 150, 226)},
    ),
    "airship_helm": (
        [
            "..######..",
            ".########.",
            "##########",
            "##########",
            ".########.",
            "..######..",
            "...l..l...",
            "...l..l...",
            "...oooo...",
            "...oooo...",
        ],
        {"#": (226, 92, 72), "l": (171, 177, 184), "o": (176, 118, 66)},
    ),
}


def core_side(name: str) -> Canvas:
    canvas = plate()
    canvas.stamp(*CORE_ICONS[name], 3, 3)
    return canvas


def balloon_cloth() -> Canvas:
    canvas = Canvas(16, 16, (236, 228, 208, 255))
    for x in range(16):
        if x % 8 >= 6:
            canvas.rect(x, 0, x, 15, (206, 72, 58))
    canvas.frame(0, 0, 15, 15, (196, 186, 164))
    return canvas


def call_panel(lit: bool) -> Canvas:
    canvas = plate()
    canvas.rect(4, 4, 11, 11, STEEL_DARK)
    canvas.rect(5, 5, 10, 10, (64, 232, 120) if lit else (118, 124, 132))
    canvas.rect(5, 5, 10, 5, (160, 255, 190) if lit else (150, 156, 164))
    return canvas


def route_beacon_face(lit: bool) -> Canvas:
    canvas = plate()
    canvas.stamp([
        "....##....",
        "...#oo#...",
        "..#oooo#..",
        ".#oo##oo#.",
        "..#oooo#..",
        "...#oo#...",
        "...#oo#...",
        "....##....",
        "..######..",
        "..######..",
    ], {"#": OUTLINE, "o": (232, 62, 58) if not lit else (64, 232, 120)}, 3, 3)
    return canvas


FURNACE_BODIES = {
    "upgraded_furnace": ((128, 128, 128), (92, 92, 92), (164, 164, 164)),
    "upgraded_blast_furnace": ((84, 84, 92), (56, 56, 64), (118, 118, 128)),
    "upgraded_smoker": ((104, 84, 66), (72, 56, 44), (140, 116, 92)),
}
FURNACE_TRIMS = {1: (206, 212, 218), 2: (238, 190, 58), 3: (78, 226, 216)}


def furnace_body(kind: str, tier: int) -> Canvas:
    base, dark, light = FURNACE_BODIES[kind]
    canvas = Canvas(16, 16, (*base, 255))
    for x, y in ((3, 4), (9, 2), (12, 7), (5, 10), (10, 12), (2, 13)):
        canvas.rect(x, y, x + 1, y, dark)
        canvas.set(x + 1, y + 1, light)
    canvas.frame(0, 0, 15, 15, FURNACE_TRIMS[tier])
    canvas.frame(1, 1, 14, 14, dark)
    return canvas


def furnace_front(kind: str, tier: int, lit: bool) -> Canvas:
    canvas = furnace_body(kind, tier)
    canvas.rect(3, 6, 12, 12, OUTLINE)
    canvas.rect(4, 7, 11, 11, (232, 120, 32) if lit else (30, 30, 34))
    if lit:
        canvas.rect(5, 9, 10, 11, (255, 216, 96))
        canvas.rect(7, 8, 8, 8, (255, 216, 96))
    canvas.rect(4, 3, 11, 4, FURNACE_TRIMS[tier])
    for pip in range(tier):
        canvas.set(6 + pip * 2, 3, OUTLINE)
    return canvas


def furnace_top(kind: str, tier: int) -> Canvas:
    canvas = furnace_body(kind, tier)
    canvas.rect(5, 5, 10, 10, FURNACE_TRIMS[tier])
    canvas.rect(6, 6, 9, 9, OUTLINE)
    return canvas


def tool_icon(rows: list[str], palette: dict) -> Canvas:
    canvas = Canvas(16, 16)
    canvas.stamp(rows, palette)
    return canvas


TRACK_LAYER_ICON = (
    [
        "................",
        "...........rr...",
        "..........rRRr..",
        ".........rRRr...",
        "........iIIr....",
        ".......iIIi.....",
        "......iIIi......",
        ".....iIIi.......",
        "....iIIi..t..t..",
        "...wWWi..tttttt.",
        "..wWWw....t..t..",
        ".wWWw....tttttt.",
        "..ww......t..t..",
        ".........tttttt.",
        "..........t..t..",
        "................",
    ],
    {
        "r": (168, 34, 36), "R": (232, 72, 60), "i": (130, 138, 148), "I": (206, 212, 218),
        "w": (92, 62, 40), "W": (138, 96, 62), "t": (176, 184, 192),
    },
)
TRAM_ICON = (
    [
        "................",
        "......k..k......",
        ".......kk.......",
        "..wwwwwwwwwwww..",
        ".wWWWWWWWWWWWWw.",
        ".wgGggGggGggGgw.",
        ".wgggggggggggGw.",
        ".wgggggggggggGw.",
        ".wWWWWWWWWWWWWw.",
        ".nnnnnnnnnnnnnn.",
        ".rrrrrrrrrrrrrr.",
        ".nnnnnnnnnnnnnn.",
        "..ddd......ddd..",
        ".d.o.d....d.o.d.",
        "..ddd......ddd..",
        "................",
    ],
    {
        "w": (196, 204, 208), "W": (236, 240, 238), "g": (64, 142, 160), "G": (150, 226, 232),
        "n": (34, 56, 74), "r": (232, 62, 52), "d": (28, 30, 36), "o": (136, 142, 150), "k": (70, 74, 82),
    },
)
MARKER_ICON = (
    [
        "................",
        "..........cccc..",
        "..........c..c..",
        "..........c.....",
        ".........w......",
        "........wW......",
        ".......wWw......",
        "......wWw.......",
        ".....wWw........",
        "....wWw.........",
        "...wWw..........",
        "..wWw...........",
        ".wWw.....c..c...",
        ".ww......c..c...",
        ".........cccc...",
        "................",
    ],
    {"w": (92, 62, 40), "W": (138, 96, 62), "c": (226, 150, 88)},
)
CARGO_ICON = (
    [
        "................",
        "................",
        "................",
        "..oooooooooooo..",
        ".oOoOoOoOoOoOoO.",
        ".oOoOoOoOoOoOoO.",
        ".oOoOoOoOoOoOoO.",
        ".oWWWWWWWWWWWWO.",
        ".oOoOoOoOoOoOoO.",
        ".oOoOoOoOoOoOoO.",
        ".nnnnnnnnnnnnnn.",
        ".nrrrrrrrrrrrrn.",
        "..ddd......ddd..",
        ".d.o.d....d.o.d.",
        "..ddd......ddd..",
        "................",
    ],
    {
        "o": (196, 92, 40), "O": (232, 124, 56), "W": (240, 236, 224),
        "n": (34, 56, 74), "r": (232, 62, 52), "d": (28, 30, 36),
    },
)


def cargo_tram_gui() -> Canvas:
    """The vanilla container look, with room for four rows of eight."""
    edge, highlight, body, shadow = (0, 0, 0), (255, 255, 255), (198, 198, 198), (85, 85, 85)
    width, height = 176, 186
    gui = Canvas(256, 256)
    gui.rect(2, 0, width - 3, height - 1, edge)
    gui.rect(0, 2, width - 1, height - 3, edge)
    gui.rect(1, 1, width - 2, height - 2, edge)
    gui.rect(2, 2, width - 3, height - 3, body)
    gui.rect(2, 1, width - 4, 2, highlight)
    gui.rect(1, 2, 2, height - 4, highlight)
    gui.rect(3, height - 3, width - 3, height - 2, shadow)
    gui.rect(width - 3, 3, width - 2, height - 3, shadow)
    gui.set(1, 1, edge)
    gui.set(width - 2, height - 2, edge)

    def slot(x: int, y: int) -> None:
        gui.rect(x, y, x + 17, y + 17, (139, 139, 139))
        gui.rect(x, y, x + 16, y, (55, 55, 55))
        gui.rect(x, y, x, y + 16, (55, 55, 55))
        gui.rect(x + 1, y + 17, x + 17, y + 17, highlight)
        gui.rect(x + 17, y + 1, x + 17, y + 17, highlight)

    for row in range(4):
        for column in range(8):
            slot(16 + column * 18, 17 + row * 18)
    for column in range(9):
        for row in range(3):
            slot(7 + column * 18, 102 + row * 18)
        slot(7 + column * 18, 160)
    return gui


def land_leveler_gui() -> Canvas:
    """The vanilla container look: three rows of storage, a fuel slot with its gauge, and room for two buttons."""
    edge, highlight, body, shadow = (0, 0, 0), (255, 255, 255), (198, 198, 198), (85, 85, 85)
    width, height = 176, 202
    gui = Canvas(256, 256)
    gui.rect(2, 0, width - 3, height - 1, edge)
    gui.rect(0, 2, width - 1, height - 3, edge)
    gui.rect(1, 1, width - 2, height - 2, edge)
    gui.rect(2, 2, width - 3, height - 3, body)
    gui.rect(2, 1, width - 4, 2, highlight)
    gui.rect(1, 2, 2, height - 4, highlight)
    gui.rect(3, height - 3, width - 3, height - 2, shadow)
    gui.rect(width - 3, 3, width - 2, height - 3, shadow)
    gui.set(1, 1, edge)
    gui.set(width - 2, height - 2, edge)

    def slot(x: int, y: int) -> None:
        gui.rect(x, y, x + 17, y + 17, (139, 139, 139))
        gui.rect(x, y, x + 16, y, (55, 55, 55))
        gui.rect(x, y, x, y + 16, (55, 55, 55))
        gui.rect(x + 1, y + 17, x + 17, y + 17, highlight)
        gui.rect(x + 17, y + 1, x + 17, y + 17, highlight)

    for row in range(3):
        for column in range(9):
            slot(7 + column * 18, 17 + row * 18)
    slot(7, 75)
    slot(35, 75)
    slot(53, 75)
    ghost = (122, 122, 122)
    # Faint marks in the empty slots: a flame for fuel, courses of bricks for the paving block.
    gui.stamp([
        "......#.......",
        ".....##.......",
        ".....###......",
        "....####..#...",
        "...######.##..",
        "...#########..",
        "..##########..",
        "..###....###..",
        "..##......##..",
        "...##....##...",
        "....######....",
    ], {"#": ghost}, 9, 78)
    for row, y in enumerate(range(79, 92, 3)):
        gui.rect(38, y, 49, y, ghost)
        for x in range(39 + (row % 2) * 3, 49, 6):
            gui.rect(x, y + 1, x, y + 2, ghost)
    # A flint and steel for the lighter slot.
    gui.stamp([
        "........##....",
        ".......#..#...",
        "......#....#..",
        "......#....#..",
        ".......#..#...",
        "...##...##....",
        "..####........",
        ".######.......",
        ".#####........",
        "..###.........",
    ], {"#": ghost}, 55, 79)
    # The fuel gauge's well, filled in by the screen.
    gui.rect(27, 75, 32, 92, (55, 55, 55))
    gui.rect(28, 76, 31, 91, (40, 34, 30))
    gui.rect(28, 92, 32, 92, highlight)
    gui.rect(32, 76, 32, 92, highlight)
    for column in range(9):
        for row in range(3):
            slot(7 + column * 18, 119 + row * 18)
        slot(7 + column * 18, 177)
    return gui


def write_block_textures() -> None:
    out = ASSETS / "textures" / "block"
    machine_side().save(out / "machine_side.png")
    machine_bottom().save(out / "machine_bottom.png")
    for status in STATUSES:
        fill, shine = STATUS_COLORS[status]
        top_arrow(fill, shine).save(out / f"machine_top_{status}.png")
    for machine, (ground, rows, palette) in FRONT_ICONS.items():
        icon_front(rows, palette, ground).save(out / f"{machine}_front.png")
    for mode in CONTROLLER_MODES:
        controller_face(mode).save(out / f"factory_controller_{mode}.png")
    controller_side().save(out / "factory_controller_side.png")

    frames = Canvas(16, 16 * 8)
    for frame in range(8):
        frames.paste(conveyor_frame(frame, True), 0, frame * 16)
    frames.save(out / "conveyor_belt.png")
    write_json(out / "conveyor_belt.png.mcmeta", {"animation": {"frametime": 2}})
    conveyor_frame(0, False).save(out / "conveyor_belt_stopped.png")
    curve_frames = Canvas(16, 16 * 8)
    for frame in range(8):
        curve_frames.paste(conveyor_curve(frame, True), 0, frame * 16)
    curve_frames.save(out / "conveyor_belt_curve.png")
    write_json(out / "conveyor_belt_curve.png.mcmeta", {"animation": {"frametime": 2}})
    conveyor_curve(0, False).save(out / "conveyor_belt_curve_stopped.png")
    both_frames = Canvas(16, 16 * 8)
    for frame in range(8):
        both_frames.paste(conveyor_both(frame, True), 0, frame * 16)
    both_frames.save(out / "conveyor_belt_both.png")
    write_json(out / "conveyor_belt_both.png.mcmeta", {"animation": {"frametime": 2}})
    conveyor_both(0, False).save(out / "conveyor_belt_both_stopped.png")
    conveyor_side().save(out / "conveyor_belt_side.png")

    for core in CORES:
        core_side(core).save(out / f"{core}_side.png")
        top_arrow(*CORE_COLORS[core]).save(out / f"{core}_top.png")
    balloon_cloth().save(out / "lift_balloon.png")
    route_beacon_face(False).save(out / "route_beacon.png")
    route_beacon_face(True).save(out / "route_beacon_docked.png")
    for kind in FURNACE_KINDS:
        for tier in FURNACE_TIERS:
            furnace_body(kind, tier).save(out / f"{kind}_{tier}_side.png")
            furnace_top(kind, tier).save(out / f"{kind}_{tier}_top.png")
            furnace_front(kind, tier, False).save(out / f"{kind}_{tier}_front.png")
            furnace_front(kind, tier, True).save(out / f"{kind}_{tier}_front_on.png")
    call_panel(False).save(out / "elevator_call.png")
    call_panel(True).save(out / "elevator_call_on.png")

    for suffix, lit in (("", False), ("_on", True)):
        drive_rail(lit, False, True).save(out / f"drive_rail{suffix}.png")
        drive_rail(lit, False, False).save(out / f"drive_rail_plain{suffix}.png")
        drive_rail(lit, True, False).save(out / f"drive_rail_corner{suffix}.png")


def write_item_textures() -> None:
    out = ASSETS / "textures" / "item"
    tool_icon(*TRACK_LAYER_ICON).save(out / "track_layer.png")
    tool_icon(*TRAM_ICON).save(out / "redstone_tram.png")
    tool_icon(*CARGO_ICON).save(out / "cargo_tram.png")
    tool_icon(*MARKER_ICON).save(out / "structure_marker.png")
    cargo_tram_gui().save(ASSETS / "textures" / "gui" / "container" / "cargo_tram.png")
    land_leveler_gui().save(ASSETS / "textures" / "gui" / "container" / "land_leveler.png")


# --------------------------------------------------------------------------- block JSON

def model_id(name: str) -> str:
    return f"{NS}:block/{name}"


def texture_id(name: str) -> str:
    return f"{NS}:block/{name}"


def write_item_definition(name: str, model: str) -> None:
    write_json(ASSETS / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": model}})


def write_flat_item(name: str, texture: str) -> None:
    write_json(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": texture},
    })
    write_item_definition(name, f"{NS}:item/{name}")


def write_machine(machine: str) -> None:
    for status in STATUSES:
        write_json(ASSETS / "models" / "block" / f"{machine}_{status}.json", {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": texture_id("machine_side"),
                "down": texture_id("machine_bottom"),
                "up": texture_id(f"machine_top_{status}"),
                "north": texture_id(f"{machine}_front"),
                "south": texture_id("machine_side"),
                "east": texture_id("machine_side"),
                "west": texture_id("machine_side"),
            },
        })

    variants = {}
    for facing, rotation in FACINGS.items():
        for status in STATUSES:
            variant = {"model": model_id(f"{machine}_{status}")}
            if rotation:
                variant["y"] = rotation
            variants[f"facing={facing},status={status}"] = variant
    write_json(ASSETS / "blockstates" / f"{machine}.json", {"variants": variants})
    write_item_definition(machine, model_id(f"{machine}_ready"))


def write_controller() -> None:
    variants = {}
    for mode in CONTROLLER_MODES:
        write_json(ASSETS / "models" / "block" / f"factory_controller_{mode}.json", {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": texture_id("factory_controller_side"),
                "down": texture_id("machine_bottom"),
                "up": texture_id(f"factory_controller_{mode}"),
                "north": texture_id(f"factory_controller_{mode}"),
                "south": texture_id(f"factory_controller_{mode}"),
                "east": texture_id("factory_controller_side"),
                "west": texture_id("factory_controller_side"),
            },
        })
        variants[f"mode={mode}"] = {"model": model_id(f"factory_controller_{mode}")}
    write_json(ASSETS / "blockstates" / "factory_controller.json", {"variants": variants})
    write_item_definition("factory_controller", model_id("factory_controller_running"))


def write_structure_core(core: str) -> None:
    write_json(ASSETS / "models" / "block" / f"{core}.json", {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": texture_id(f"{core}_side"),
            "down": texture_id("machine_bottom"),
            "up": texture_id(f"{core}_top"),
            "north": texture_id(f"{core}_side"),
            "south": texture_id(f"{core}_side"),
            "east": texture_id(f"{core}_side"),
            "west": texture_id(f"{core}_side"),
        },
    })
    variants = {}
    for facing, rotation in FACINGS.items():
        variant = {"model": model_id(core)}
        if rotation:
            variant["y"] = rotation
        variants[f"facing={facing}"] = variant
    write_json(ASSETS / "blockstates" / f"{core}.json", {"variants": variants})
    write_item_definition(core, model_id(core))


def write_balloon() -> None:
    write_json(ASSETS / "models" / "block" / "lift_balloon.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": texture_id("lift_balloon")},
    })
    write_json(ASSETS / "blockstates" / "lift_balloon.json", {"variants": {"": {"model": model_id("lift_balloon")}}})
    write_item_definition("lift_balloon", model_id("lift_balloon"))


def write_elevator_call() -> None:
    variants = {}
    for powered, name in (("false", "elevator_call"), ("true", "elevator_call_on")):
        write_json(ASSETS / "models" / "block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": texture_id(name)},
        })
        variants[f"powered={powered}"] = {"model": model_id(name)}
    write_json(ASSETS / "blockstates" / "elevator_call.json", {"variants": variants})
    write_item_definition("elevator_call", model_id("elevator_call"))


def write_route_beacon() -> None:
    variants = {}
    for docked, name in (("false", "route_beacon"), ("true", "route_beacon_docked")):
        write_json(ASSETS / "models" / "block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": texture_id(name)},
        })
        for powered in ("false", "true"):
            variants[f"docked={docked},powered={powered}"] = {"model": model_id(name)}
    write_json(ASSETS / "blockstates" / "route_beacon.json", {"variants": variants})
    write_item_definition("route_beacon", model_id("route_beacon"))


def write_furnace(name: str) -> None:
    for suffix, front in (("", f"{name}_front"), ("_on", f"{name}_front_on")):
        write_json(ASSETS / "models" / "block" / f"{name}{suffix}.json", {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": texture_id(f"{name}_side"),
                "down": texture_id(f"{name}_top"),
                "up": texture_id(f"{name}_top"),
                "north": texture_id(front),
                "south": texture_id(f"{name}_side"),
                "east": texture_id(f"{name}_side"),
                "west": texture_id(f"{name}_side"),
            },
        })
    variants = {}
    for facing, rotation in FACINGS.items():
        for lit, suffix in (("false", ""), ("true", "_on")):
            for powered in ("false", "true"):
                variant = {"model": model_id(f"{name}{suffix}")}
                if rotation:
                    variant["y"] = rotation
                variants[f"facing={facing},lit={lit},powered={powered}"] = variant
    write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})
    write_item_definition(name, model_id(name))


def write_conveyor() -> None:
    def textures(top: str):
        return {"particle": "#side", "top": texture_id(top), "side": texture_id("conveyor_belt_side")}

    def flat_model(top: str, mirrored: bool = False):
        side = {"uv": [0, 12, 16, 16], "texture": "#side"}
        return {
            "textures": textures(top),
            "elements": [{
                "from": [0, 0, 0],
                "to": [16, 4, 16],
                "faces": {
                    "up": {"uv": [16, 0, 0, 16] if mirrored else [0, 0, 16, 16], "texture": "#top"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"},
                    "north": side, "south": side, "east": side, "west": side,
                },
            }],
        }

    def ramp_model(top: str, high_end_north: bool):
        """A tilted slab, set the way a raised rail is, on stepped legs. The arrows always run north."""
        edge = {"uv": [0, 12, 16, 15], "texture": "#side"}
        leg = {"uv": [0, 8, 8, 16], "texture": "#side"}
        elements = [{
            "from": [0.02, 9, 0],
            "to": [15.98, 12, 16],
            "rotation": {"origin": [8, 12, 8], "axis": "x", "angle": 45 if high_end_north else -45, "rescale": True},
            "faces": {
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "north": edge, "south": edge, "east": edge, "west": edge,
            },
        }]
        for z0, z1, height in ((0.5, 4, 10), (4, 8, 6), (8, 12, 2)):
            near, far = (z0, z1) if high_end_north else (16 - z1, 16 - z0)
            elements.append({
                "from": [1, 0, near],
                "to": [15, height, far],
                "faces": {face: leg for face in ("up", "north", "south", "east", "west")}
                | {"down": {**leg, "cullface": "down"}},
            })
        return {"textures": textures(top), "elements": elements}

    block_models = ASSETS / "models" / "block"
    variants = {}
    for suffix, top_suffix in (("", ""), ("_stopped", "_stopped")):
        write_json(block_models / f"conveyor_belt{suffix}.json", flat_model(f"conveyor_belt{top_suffix}"))
        write_json(block_models / f"conveyor_belt_left{suffix}.json", flat_model(f"conveyor_belt_curve{top_suffix}"))
        write_json(block_models / f"conveyor_belt_right{suffix}.json", flat_model(f"conveyor_belt_curve{top_suffix}", True))
        write_json(block_models / f"conveyor_belt_both{suffix}.json", flat_model(f"conveyor_belt_both{top_suffix}"))
        write_json(block_models / f"conveyor_belt_up{suffix}.json", ramp_model(f"conveyor_belt{top_suffix}", True))
        write_json(block_models / f"conveyor_belt_down{suffix}.json", ramp_model(f"conveyor_belt{top_suffix}", False))

    for facing, rotation in FACINGS.items():
        for powered, suffix in (("false", ""), ("true", "_stopped")):
            for shape, model in (("straight", "conveyor_belt"), ("left", "conveyor_belt_left"), ("right", "conveyor_belt_right"),
                                 ("both", "conveyor_belt_both"), ("up", "conveyor_belt_up"), ("down", "conveyor_belt_down")):
                variant = {"model": model_id(model + suffix)}
                if rotation:
                    variant["y"] = rotation
                variants[f"facing={facing},powered={powered},shape={shape}"] = variant
    write_json(ASSETS / "blockstates" / "conveyor_belt.json", {"variants": variants})
    write_item_definition("conveyor_belt", model_id("conveyor_belt"))


def write_drive_rail() -> None:
    for suffix in ("", "_on"):
        models = {
            f"drive_rail{suffix}": ("minecraft:block/rail_flat", f"drive_rail{suffix}"),
            f"drive_rail_corner{suffix}": ("minecraft:block/rail_curved", f"drive_rail_corner{suffix}"),
            f"drive_rail_raised_ne{suffix}": ("minecraft:block/template_rail_raised_ne", f"drive_rail_plain{suffix}"),
            f"drive_rail_raised_sw{suffix}": ("minecraft:block/template_rail_raised_sw", f"drive_rail_plain{suffix}"),
        }
        for name, (parent, texture) in models.items():
            write_json(ASSETS / "models" / "block" / f"{name}.json", {"parent": parent, "textures": {"rail": texture_id(texture)}})

    # The straight texture's arrow points north, so a heading is a model rotation.
    straight = {"north_south": {"false": 180, "true": 0}, "east_west": {"false": 90, "true": 270}}
    sloped = {
        "ascending_north": ("drive_rail_raised_ne", 0), "ascending_south": ("drive_rail_raised_sw", 0),
        "ascending_east": ("drive_rail_raised_ne", 90), "ascending_west": ("drive_rail_raised_sw", 90),
    }
    curved = {"south_east": 0, "south_west": 90, "north_west": 180, "north_east": 270}

    def add(variants, shape, powered, reversed_, base, rotation):
        variant = {"model": model_id(base + ("_on" if powered == "true" else ""))}
        if rotation:
            variant["y"] = rotation
        variants[f"powered={powered},reversed={reversed_},shape={shape}"] = variant

    variants = {}
    for powered in ("false", "true"):
        for reversed_ in ("false", "true"):
            for shape, rotations in straight.items():
                add(variants, shape, powered, reversed_, "drive_rail", rotations[reversed_])
            for shape, (base, rotation) in sloped.items():
                add(variants, shape, powered, reversed_, base, rotation)
            for shape, rotation in curved.items():
                add(variants, shape, powered, reversed_, "drive_rail_corner", rotation)
    write_json(ASSETS / "blockstates" / "drive_rail.json", {"variants": variants})
    write_flat_item("drive_rail", texture_id("drive_rail_on"))


# Every advancement: path, parent, icon, how it is earned, frame, then Korean and English title and description.
# "item:a,b" is earned by holding any of those items, "all:a,b" by having held each of them,
# "ride:x" by riding entity x, and "event" by the code calling ModAdvancements.
ADVANCEMENTS = [
    ("root", None, "redstone_motor", "join", "task",
     "레드스톤 운송", "레드스톤으로 움직이는 철도와 공장의 세계",
     "Redstone Transportation", "Railways and factories that run on redstone"),

    ("rail/motor", "root", "redstone_motor", "item:redstone_motor", "task",
     "첫 번째 모터", "레드스톤 전차 모터를 만드세요", "First Motor", "Make a Redstone Tram Motor"),
    ("rail/drive_rail", "rail/motor", "drive_rail", "item:drive_rail", "task",
     "레일이 곧 엔진", "동력 선로를 만드세요", "The Rail Is the Engine", "Make a Drive Rail"),
    ("rail/track_layer", "rail/drive_rail", "track_layer", "item:track_layer", "task",
     "두 번만 클릭하세요", "선로 부설기를 만드세요", "Just Two Clicks", "Make a Track Layer"),
    ("rail/tram", "rail/motor", "redstone_tram", "ride:redstone_tram", "task",
     "승차 완료", "레드스톤 전차에 타세요", "All Aboard", "Ride a Redstone Tram"),
    ("rail/train", "rail/tram", "tram_coupler", "event", "task",
     "열차 편성", "전차 연결기로 카트 두 대를 연결하세요", "Making Up a Train", "Couple two carts with a Tram Coupler"),
    ("rail/cargo", "rail/train", "cargo_tram", "item:cargo_tram", "task",
     "화물 수송", "화물 전차를 만드세요", "Freight Service", "Make a Cargo Tram"),
    ("rail/alu", "rail/tram", "alu_train_controller", "item:alu_train_controller", "task",
     "아날로그 운전대", "ALU 전차 제어기를 만드세요", "Analog Controls", "Make an ALU Train Controller"),
    ("rail/motor_4", "rail/motor", "redstone_motor_4", "item:redstone_motor_4", "goal",
     "최고 출력", "레드스톤 전차 모터 IV를 만드세요", "Full Power", "Make a Redstone Tram Motor IV"),
    ("rail/full_speed", "rail/motor_4", "redstone_motor_4", "event", "challenge",
     "시속 360킬로미터", "카트를 타고 모터 IV로 초속 100미터에 도달하세요", "360 km/h", "Ride a cart up to 100 m/s on Motor IV"),

    ("factory/tree_cutter", "root", "tree_cutter", "item:tree_cutter", "task",
     "나무꾼의 은퇴", "벌목기를 만드세요", "The Lumberjack Retires", "Make a Tree Cutter"),
    ("factory/conveyor", "factory/tree_cutter", "conveyor_belt", "item:conveyor_belt", "task",
     "끝없는 벨트", "컨베이어 벨트를 만드세요", "Endless Belt", "Make a Conveyor Belt"),
    ("factory/controller", "factory/tree_cutter", "factory_controller", "event", "goal",
     "공장장", "공장 제어기에 기계 3대 이상을 연결하세요", "Plant Manager", "Link three or more machines to a Factory Controller"),
    ("factory/quarry", "factory/controller", "quarry_drill", "item:quarry_drill", "task",
     "땅 밑으로", "채굴 드릴을 만드세요", "Going Down", "Make a Quarry Drill"),
    ("factory/leveler", "factory/quarry", LEVELER, f"item:{LEVELER}", "task",
     "산을 옮기는 자", "평지화 기계를 만드세요", "Mountain Mover", "Make a Land Leveler"),

    ("furnace/first", "root", "upgraded_furnace_1", "item:upgraded_furnace_1,upgraded_blast_furnace_1,upgraded_smoker_1", "task",
     "더 뜨겁게", "강화 화로, 용광로, 훈연기 중 하나를 만드세요", "Hotter", "Make an upgraded furnace, blast furnace or smoker"),
    ("furnace/third", "furnace/first", "upgraded_furnace_3", "item:upgraded_furnace_3,upgraded_blast_furnace_3,upgraded_smoker_3", "goal",
     "불꽃의 정점", "III단계 강화 블록을 만드세요", "Peak Flame", "Make a tier III upgraded cooker"),
    ("furnace/all", "furnace/third", "upgraded_blast_furnace_3", "all:upgraded_furnace_3,upgraded_blast_furnace_3,upgraded_smoker_3", "challenge",
     "화덕의 주인", "III단계 화로, 용광로, 훈연기를 모두 만드세요", "Master of the Hearth", "Make all three tier III cookers"),

    ("structure/first", "root", "elevator_core", "event", "task",
     "통째로 움직이기", "구조물을 조립하세요", "The Whole Thing Moves", "Assemble a moving structure"),
    ("structure/elevator", "structure/first", "elevator_core", "event", "task",
     "올라갑니다", "엘리베이터를 조립하세요", "Going Up", "Assemble an elevator"),
    ("structure/ship", "structure/first", "ship_helm", "event", "task",
     "진수식", "배를 조립하세요", "Launch Day", "Assemble a ship"),
    ("structure/airship", "structure/first", "airship_helm", "event", "goal",
     "하늘을 나는 집", "비행선을 조립하세요", "House in the Sky", "Assemble an airship"),
    ("structure/route", "structure/ship", "route_beacon", "event", "goal",
     "자동 항해", "배나 비행선이 항로 표지에 도착하게 하세요", "On Autopilot", "Bring a ship or airship in to a Route Beacon"),
    ("structure/repairs", "structure/first", "structure_marker", "event", "task",
     "항해 중 수리", "움직이는 구조물에 블록을 놓으세요", "Repairs at Sea", "Place a block on a moving structure"),
    ("structure/giant", "structure/airship", "lift_balloon", "event", "challenge",
     "거대 건조물", "블록 500개 이상으로 구조물을 조립하세요", "Colossal", "Assemble a structure of 500 blocks or more"),
]
# Only reachable with Analog ALU, whose recipe this block needs: without it the advancement does not exist.
ALU_ADVANCEMENTS = {"rail/alu"}


def advancement_key(path: str) -> str:
    return f"advancements.{NS}.{path.replace('/', '.')}"


def advancement_criteria(earned: str) -> tuple[dict, list]:
    kind, _, value = earned.partition(":")
    items = [f"{NS}:{item}" for item in value.split(",")] if value else []
    if kind == "join":
        return {"join": {"trigger": "minecraft:tick"}}, [["join"]]
    if kind == "event":
        return {"done": {"trigger": "minecraft:impossible"}}, [["done"]]
    if kind == "ride":
        return {"ride": {
            "trigger": "minecraft:started_riding",
            "conditions": {"player": {
                "type": "minecraft:entity_properties",
                "entity": "this",
                "predicate": {"minecraft:vehicle": {"minecraft:entity_type": f"{NS}:{value}"}},
            }},
        }}, [["ride"]]
    if kind == "item":
        return {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": items}]}}}, [["has_item"]]
    criteria = {
        item.split(":")[1]: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}
        for item in items
    }
    return criteria, [[name] for name in criteria]


def write_advancements() -> None:
    ko = {}
    en = {}
    for path, parent, icon, earned, frame, ko_title, ko_text, en_title, en_text in ADVANCEMENTS:
        key = advancement_key(path)
        ko[f"{key}.title"], ko[f"{key}.description"] = ko_title, ko_text
        en[f"{key}.title"], en[f"{key}.description"] = en_title, en_text

        criteria, requirements = advancement_criteria(earned)
        display = {
            "title": {"translate": f"{key}.title"},
            "description": {"translate": f"{key}.description"},
            "icon": {"id": f"{NS}:{icon}"},
            "frame": frame,
        }
        data = {"criteria": criteria, "requirements": requirements, "display": display}
        if parent:
            data["parent"] = f"{NS}:{parent}"
        else:
            display["background"] = "minecraft:gui/advancements/backgrounds/stone"
            display["show_toast"] = False
            display["announce_to_chat"] = False
        if frame == "challenge":
            data["rewards"] = {"experience": 100}
        if path in ALU_ADVANCEMENTS:
            data["fabric:load_conditions"] = [{"condition": "fabric:all_mods_loaded", "values": ["analog_alu"]}]
        write_json(DATA / "advancement" / f"{path}.json", data)

    for name, entries in (("ko_kr", ko), ("en_us", en)):
        lang = ASSETS / "lang" / f"{name}.json"
        language = json.loads(lang.read_text(encoding="utf-8"))
        language.update(entries)
        lang.write_text(json.dumps(language, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def write_block_data() -> None:
    for block in EXISTING_BLOCKS + NEW_BLOCKS:
        write_json(DATA / "loot_table" / "blocks" / f"{block}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{block}"}],
            }],
        })

    # Ground a structure's block search stops at. Everything else joined to a core moves with it.
    write_json(DATA / "tags" / "block" / "structure_terrain.json", {
        "replace": False,
        "values": [
            "#minecraft:dirt", "#minecraft:grass_blocks", "#minecraft:sand", "#minecraft:base_stone_overworld",
            "#minecraft:base_stone_nether", "#minecraft:terracotta", "#minecraft:snow", "#minecraft:ice",
            "minecraft:gravel", "minecraft:clay", "minecraft:mud", "minecraft:moss_block",
        ],
    })

    tags = RES / "data" / "minecraft" / "tags"
    rail = {"replace": False, "values": [f"{NS}:drive_rail"]}
    # What a land leveler may fill holes with and, when told to, throw away past a few stacks.
    write_json(DATA / "tags" / "item" / "leveler_rubble.json", {
        "replace": False,
        "values": [
            "minecraft:cobblestone", "minecraft:cobbled_deepslate", "minecraft:dirt", "minecraft:coarse_dirt",
            "minecraft:andesite", "minecraft:diorite", "minecraft:granite", "minecraft:tuff", "minecraft:calcite",
            "minecraft:netherrack", "minecraft:sandstone", "minecraft:red_sandstone", "minecraft:gravel",
            "minecraft:sand", "minecraft:red_sand", "minecraft:stone", "minecraft:deepslate", "minecraft:blackstone",
            "minecraft:basalt", "minecraft:smooth_basalt", "minecraft:end_stone", "minecraft:soul_soil",
            "minecraft:rooted_dirt", "minecraft:mud", "minecraft:terracotta",
            *[f"minecraft:{color}_terracotta" for color in (
                "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")],
        ],
    })
    write_json(tags / "block" / "rails.json", rail)
    write_json(tags / "item" / "rails.json", rail)
    # Analog ALU's blocks share this tag file in the merged mod: keep every entry that is not ours.
    pickaxe = tags / "block" / "mineable" / "pickaxe.json"
    others = []
    if pickaxe.exists():
        others = [v for v in json.loads(pickaxe.read_text(encoding="utf-8"))["values"] if not v.startswith(f"{NS}:")]
    write_json(pickaxe, {
        "replace": False,
        "values": others + [f"{NS}:{block}" for block in EXISTING_BLOCKS + NEW_BLOCKS],
    })


# --------------------------------------------------------------------------- recipes

# The automation machines. With Analog ALU, its semiconductor takes the redstone's place and a craft makes
# a batch; without ALU there is no way to get one, so the plain recipe stands in for it.
RECIPES = {
    "tree_cutter": (["IAI", "RMR", "ICI"], {"A": "minecraft:iron_axe", "C": "minecraft:chest"}),
    "auto_planter": (["IWI", "RMR", "IDI"], {"W": "minecraft:wheat_seeds", "D": "minecraft:dispenser"}),
    "growth_booster": (["IBI", "RMR", "IDI"], {"B": "minecraft:bone_block", "D": "minecraft:dispenser"}),
    "crop_harvester": (["IHI", "RMR", "ICI"], {"H": "minecraft:iron_hoe", "C": "minecraft:chest"}),
    "quarry_drill": (["IPI", "RMR", "ICI"], {"P": "minecraft:diamond_pickaxe", "C": "minecraft:chest"}),
    "item_collector": (["IHI", "RMR", "ICI"], {"H": "minecraft:hopper", "C": "minecraft:chest"}),
    "factory_controller": (["IRI", "PMP", "IRI"], {"P": "minecraft:comparator"}),
}
MACHINE_BATCH = 4
WITH_ALU = [{"condition": "fabric:all_mods_loaded", "values": ["analog_alu"]}]
WITHOUT_ALU = [{"condition": "fabric:not", "value": WITH_ALU[0]}]


def write_recipes() -> None:
    shared = {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "M": f"{NS}:redstone_motor"}

    def shaped(name, pattern, key, count, conditions=None, result=None):
        used = {symbol for row in pattern for symbol in row if symbol != " "}
        recipe = {
            "type": "minecraft:crafting_shaped",
            "category": "redstone",
            "pattern": pattern,
            "key": {symbol: value for symbol, value in {**shared, **key}.items() if symbol in used},
            "result": {"id": f"{NS}:{result or name}", "count": count},
        }
        if conditions:
            recipe = {"fabric:load_conditions": conditions, **recipe}
        write_json(DATA / "recipe" / f"{name}.json", recipe)

    for name, (pattern, key) in RECIPES.items():
        with_chips = [row.replace("R", "S") for row in pattern]
        shaped(name, with_chips, {**key, "S": "analog_alu:semiconductor"}, MACHINE_BATCH, WITH_ALU)
        shaped(f"{name}_without_alu", pattern, key, 1, WITHOUT_ALU, name)
    shaped("drive_rail", ["GMG", "GSG", "GRG"], {"G": "minecraft:gold_ingot", "S": "minecraft:stick"}, 8)
    shaped("conveyor_belt", ["LLL", "IRI"], {"L": "minecraft:leather"}, 6)
    shaped("track_layer", ["TPT", " I ", " I "], {"T": "minecraft:rail", "P": "minecraft:piston"}, 1)
    shaped("cargo_tram", ["C", "T"], {"C": "minecraft:chest", "T": f"{NS}:redstone_tram"}, 1)
    shaped("elevator_core", ["IPI", "RMR", "ICI"], {"P": "minecraft:piston", "C": "minecraft:iron_chain"}, 1)
    shaped("ship_helm", ["WSW", "RMR", "WBW"], {"W": "#minecraft:planks", "S": "minecraft:stick", "B": "#minecraft:boats"}, 1)
    shaped("airship_helm", ["IFI", "RMR", "IBI"], {"F": "minecraft:feather", "B": f"{NS}:lift_balloon"}, 1)
    shaped("lift_balloon", ["WWW", "W W", "SIS"], {"W": "#minecraft:wool", "S": "minecraft:string"}, 4)
    shaped("elevator_call", ["IBI", "IRI"], {"B": "minecraft:stone_button"}, 2)
    shaped("structure_marker", ["  C", " S ", "S  "], {"C": "minecraft:copper_ingot", "S": "minecraft:stick"}, 1)
    shaped("route_beacon", ["IGI", "RMR", "III"], {"G": "minecraft:compass"}, 1)
    shaped(LEVELER, ["SPS", "RMR", "IFI"], {"S": "minecraft:iron_shovel", "P": "minecraft:diamond_pickaxe",
                                             "F": "minecraft:furnace"}, 1)
    upgrades = {
        1: (["III", "RFR", "III"], {}),
        2: (["GGG", "RFR", "GGG"], {"G": "minecraft:gold_ingot"}),
        3: (["DBD", "BFB", "DBD"], {"D": "minecraft:diamond", "B": "minecraft:blaze_rod"}),
    }
    bases = {"upgraded_furnace": "minecraft:furnace", "upgraded_blast_furnace": "minecraft:blast_furnace", "upgraded_smoker": "minecraft:smoker"}
    for kind, base in bases.items():
        for tier, (pattern, key) in upgrades.items():
            shaped(f"{kind}_{tier}", pattern, {**key, "F": base if tier == 1 else f"{NS}:{kind}_{tier - 1}"}, 1)


# --------------------------------------------------------------------------- tram models

WHITE = (232, 236, 233)
WHITE_DARK = (190, 198, 200)
NAVY = (32, 54, 74)
NAVY_DARK = (20, 34, 48)
RED = (226, 54, 46)
GRAPHITE = (44, 48, 56)
GRAPHITE_LIGHT = (72, 78, 88)
STEEL_MODEL = (140, 148, 158)
COPPER = (196, 122, 74)
ORANGE = (232, 124, 56)
ORANGE_DARK = (184, 88, 38)
GLASS = (150, 226, 234, 44)
LAMP = (255, 255, 255, 255)
SEAT = (178, 58, 52)


def shade(color, amount):
    return tuple(max(0, min(255, int(c * amount))) for c in color[:3]) + tuple(color[3:])


def solid(color):
    """Flat colour with a darker rim, which gives every box a panel-line edge."""
    def paint(face, x, y, w, h):
        rim = x in (0, w - 1) or y in (0, h - 1)
        lit = {"top": 1.0, "bottom": 0.72, "north": 0.92, "south": 0.92, "east": 0.84, "west": 0.84}[face]
        return shade(color, lit * (0.86 if rim and w > 1 and h > 1 else 1.0))
    return paint


def flat(color):
    def paint(face, x, y, w, h):
        return color if len(color) == 4 else (*color, 255)
    return paint


def side_wall():
    """White over a navy skirt with the red line that identifies the tram."""
    def paint(face, x, y, w, h):
        if face in ("top", "bottom"):
            return (*WHITE_DARK, 255)
        band = y / max(h - 1, 1)
        if band < 0.45:
            color = WHITE
        elif band < 0.62:
            color = RED
        else:
            color = NAVY
        # Door seams in the middle bay of the long faces.
        if face in ("north", "south") and w > 8 and abs(abs(x - w / 2) - 4) < 1:
            color = shade(color, 0.72)
        return (*color, 255)
    return paint


def skirt():
    def paint(face, x, y, w, h):
        color = NAVY_DARK if y >= h - 1 else NAVY
        if face in ("north", "south") and (x + 2) % 6 == 0:
            color = NAVY_DARK
        return (*color, 255)
    return paint


def nose():
    def paint(face, x, y, w, h):
        if face in ("top", "bottom"):
            return (*WHITE_DARK, 255)
        band = y / max(h - 1, 1)
        # One row of red, level with the line along the walls.
        color = WHITE if band < 0.4 else (RED if band < 0.5 else NAVY)
        return (*color, 255)
    return paint


def corrugated(color, dark):
    def paint(face, x, y, w, h):
        if face in ("top", "bottom"):
            return (*(color if x % 2 else dark), 255)
        c = dark if x % 2 == 0 else color
        if 0.42 < y / max(h - 1, 1) < 0.58:
            c = (240, 236, 224)
        return (*c, 255)
    return paint


def glass_pane():
    def paint(face, x, y, w, h):
        glint = (x + y) % 13 == 0 and face in ("north", "south", "east", "west")
        return (220, 248, 250, 76) if glint else GLASS
    return paint


@dataclass
class Cube:
    x: float
    y: float
    z: float
    w: int
    h: int
    d: int
    paint: object
    u: int = 0
    v: int = 0


@dataclass
class Part:
    name: str
    cubes: list = field(default_factory=list)
    pivot: tuple = (0, 0, 0)
    rotation: tuple = (0, 0, 0)
    children: list = field(default_factory=list)


def all_cubes(part: Part):
    yield from part.cubes
    for child in part.children:
        yield from all_cubes(child)


def pack(parts: list, size: int) -> None:
    cubes = [cube for part in parts for cube in all_cubes(part)]
    cubes.sort(key=lambda cube: -(cube.d + cube.h))
    x = y = shelf = 0
    for cube in cubes:
        width = 2 * (cube.d + cube.w)
        height = cube.d + cube.h
        if x + width > size:
            x = 0
            y += shelf
            shelf = 0
        cube.u, cube.v = x, y
        x += width
        shelf = max(shelf, height)
    if y + shelf > size:
        raise ValueError(f"model does not fit a {size}px atlas")


def paint_atlas(parts: list, size: int) -> Canvas:
    atlas = Canvas(size, size)
    for part in parts:
        for cube in all_cubes(part):
            u, v, w, h, d = cube.u, cube.v, cube.w, cube.h, cube.d
            faces = {
                "top": (u + d, v, w, d),
                "bottom": (u + d + w, v, w, d),
                "west": (u, v + d, d, h),
                "north": (u + d, v + d, w, h),
                "east": (u + d + w, v + d, d, h),
                "south": (u + d + w + d, v + d, w, h),
            }
            for face, (fx, fy, fw, fh) in faces.items():
                for py in range(fh):
                    for px in range(fw):
                        atlas.set(fx + px, fy + py, cube.paint(face, px, py, fw, fh))
    return atlas


HALF_LENGTH = 16
TILT = 10.5
# Model y=0 sits 0.6px above the rail surface, and the rail's steel runs 4.5px either side of centre.
AXLE_Y = -1.5
WHEEL_Z = 4.5
BOGIE_X = 10
WHEEL_SPREAD = 3.5


def bogie(center: float) -> list:
    """Cross member, side frames outside the wheels, and the two axles."""
    return [
        Cube(center - 1, -3, -5.5, 2, 1, 11, solid(GRAPHITE_LIGHT)),
        Cube(center - 4, -3.6, 5.4, 8, 1, 1, solid(GRAPHITE_LIGHT)),
        Cube(center - 4, -3.6, -6.4, 8, 1, 1, solid(GRAPHITE_LIGHT)),
        Cube(center - WHEEL_SPREAD - 0.5, AXLE_Y - 0.5, -4, 1, 1, 8, solid(STEEL_MODEL)),
        Cube(center + WHEEL_SPREAD - 0.5, AXLE_Y - 0.5, -4, 1, 1, 8, solid(STEEL_MODEL)),
    ]


def chassis() -> list:
    """The running gear both vehicles share."""
    half = HALF_LENGTH
    return [Part("frame", [
        # A narrow spine, so nothing above the bogies reaches the wheels.
        Cube(-15, -4, -3, 30, 1, 6, solid(GRAPHITE)),
        Cube(-half, -5, -6, 2 * half, 1, 12, solid(GRAPHITE_LIGHT)),
        Cube(-half, -7, -7, 2 * half, 3, 1, skirt()),
        Cube(-half, -7, 6, 2 * half, 3, 1, skirt()),
        *bogie(BOGIE_X),
        *bogie(-BOGIE_X),
        # Buffers and couplers, under the end of the floor.
        Cube(half, -5, -6, 1, 2, 12, solid(GRAPHITE)),
        Cube(-half - 1, -5, -6, 1, 2, 12, solid(GRAPHITE)),
        Cube(half + 1, -4.5, -1, 1, 1, 2, solid(COPPER)),
        Cube(-half - 2, -4.5, -1, 1, 1, 2, solid(COPPER)),
    ])]


def wheels() -> list:
    """A fixed round tyre with spokes that spin inside it, so nothing sweeps through the rail or the body."""
    parts = []
    index = 0
    for x in (-BOGIE_X - WHEEL_SPREAD, -BOGIE_X + WHEEL_SPREAD, BOGIE_X - WHEEL_SPREAD, BOGIE_X + WHEEL_SPREAD):
        for sign in (-1, 1):
            tyre_z = sign * WHEEL_Z - 0.5
            # Four bars at 45 degree steps outline an octagon; each sits 0.1 deeper so no two share a face.
            for step, (turn, w, h) in enumerate(((0, 4, 2), (0, 2, 4), (45, 4, 2), (45, 2, 4))):
                parts.append(Part(f"tyre_{index}_{step}", [Cube(-w / 2, -h / 2, tyre_z - sign * 0.1 * step, w, h, 1, solid(GRAPHITE))],
                                  pivot=(x, AXLE_Y, 0), rotation=(0, 0, turn)))
            hub_z = tyre_z + sign * 0.3
            parts.append(Part(f"wheel_{index}", [
                Cube(-1.5, -0.5, hub_z, 3, 1, 1, solid(STEEL_MODEL)),
                Cube(-0.5, -1.5, hub_z - sign * 0.1, 1, 3, 1, solid(STEEL_MODEL)),
            ], pivot=(x, AXLE_Y, 0)))
            index += 1
    return parts


def tram_body() -> list:
    posts = []
    for x in (-14, -5, 4, 13):
        for z in (-7, 6):
            posts.append(Cube(x, -27, z, 1, 12, 1, solid(WHITE)))
    body = Part("cabin", [
        # Lower walls, with the red line running the length of the car. They end on top of the skirt.
        Cube(-15, -15, -7, 30, 8, 1, side_wall()),
        Cube(-15, -15, 6, 30, 8, 1, side_wall()),
        *posts,
        # Roof, stepped so the crown reads as rounded.
        Cube(-14.5, -28, -7, 29, 1, 14, solid(WHITE)),
        Cube(-13.5, -29, -6, 27, 1, 12, solid(WHITE)),
        Cube(-12.5, -30, -4, 25, 1, 8, solid(WHITE_DARK)),
        # Cooling unit and the pantograph mount.
        Cube(-11, -32, -3, 7, 2, 6, solid(GRAPHITE_LIGHT)),
        Cube(-1, -31, -2, 9, 1, 4, solid(GRAPHITE)),
        # Interior: the seat under the rider, benches, and a driver's desk at each end.
        Cube(-3, -6, -4, 6, 1, 8, solid(SEAT)),
        Cube(-11, -8, -5, 4, 3, 10, solid(SEAT)),
        Cube(7, -8, -5, 4, 3, 10, solid(SEAT)),
        Cube(-12, -14, -5, 1, 6, 10, solid(SEAT)),
        Cube(11, -14, -5, 1, 6, 10, solid(SEAT)),
        Cube(13, -12, -5, 2, 7, 10, solid(GRAPHITE)),
        Cube(-15, -12, -5, 2, 7, 10, solid(GRAPHITE)),
    ])
    noses = []
    for sign, name in ((1, "front"), (-1, "rear")):
        plate_x = -1 if sign > 0 else 0
        # Narrower than the walls and pillars set in from them, so the tilted parts cut no shared surface.
        noses.append(Part(f"{name}_nose", [Cube(plate_x, -10, -6.5, 1, 10, 13, nose())],
                          pivot=(sign * 17, -5, 0), rotation=(0, 0, -sign * TILT)))
        noses.append(Part(f"{name}_posts", [
            Cube(plate_x, -12, -6.75, 1, 12, 1, solid(WHITE)),
            Cube(plate_x, -12, 5.75, 1, 12, 1, solid(WHITE)),
        ], pivot=(sign * 15.15, -15, 0), rotation=(0, 0, -sign * TILT)))
    return [body, *noses]


def tram_glass() -> list:
    panes = []
    for x in (-13, -4, 5):
        for z in (-6.6, 6.6):
            panes.append(Cube(x, -27, z, 8, 12, 0, glass_pane()))
    return [
        Part("side_panes", panes),
        Part("front_pane", [Cube(-0.5, -12, -6, 0, 12, 12, glass_pane())],
             pivot=(15.15, -15, 0), rotation=(0, 0, -TILT)),
        Part("rear_pane", [Cube(0.5, -12, -6, 0, 12, 12, glass_pane())],
             pivot=(-15.15, -15, 0), rotation=(0, 0, TILT)),
    ]


def nose_lamps() -> list:
    """Set into the tilted nose plate, standing 0.1 proud of its surface."""
    def end(sign, name):
        x = -0.9 if sign > 0 else -0.1
        return Part(name, [
            Cube(x, -5, -6, 1, 2, 3, flat(LAMP)),
            Cube(x, -5, 3, 1, 2, 3, flat(LAMP)),
        ], pivot=(sign * 17, -5, 0), rotation=(0, 0, -sign * TILT))
    return [end(1, "lamps_front"), end(-1, "lamps_rear")]


def cargo_lamps() -> list:
    """Set into the end beams, standing 0.1 proud of them."""
    def end(x, name):
        return Part(name, [
            Cube(x, -6.5, -5.5, 1, 1, 3, flat(LAMP)),
            Cube(x, -6.5, 2.5, 1, 1, 3, flat(LAMP)),
        ])
    return [end(16.1, "lamps_front"), end(-17.1, "lamps_rear")]


def glow_strip(y: int) -> list:
    return [Part("strip", [
        Cube(-15, y, -7.1, 30, 1, 0, flat(LAMP)),
        Cube(-15, y, 7.1, 30, 1, 0, flat(LAMP)),
    ])]


def pantograph() -> list:
    """Two folded arms; TramModel swings them apart to lift the copper head while moving."""
    head = Part("pantograph_head", [Cube(-0.5, -1, -6, 1, 1, 12, solid(COPPER))], pivot=(7, -0.5, 0))
    upper = Part("pantograph_upper", [Cube(0, -1, -2, 7, 1, 1, solid(STEEL_MODEL)),
                                      Cube(0, -1, 1, 7, 1, 1, solid(STEEL_MODEL))],
                 pivot=(7, -0.5, 0), rotation=(0, 0, 150), children=[head])
    lower = Part("pantograph", [Cube(0, -1, -2, 7, 1, 1, solid(STEEL_MODEL)),
                                Cube(0, -1, 1, 7, 1, 1, solid(STEEL_MODEL))],
                 pivot=(7, -31, 0), rotation=(0, 0, 188), children=[upper])
    return [lower]


def cargo_body() -> list:
    return [Part("container", [
        Cube(-16, -8, -7, 32, 1, 14, solid(GRAPHITE)),
        Cube(16, -7, -6, 1, 2, 12, solid(GRAPHITE_LIGHT)),
        Cube(-17, -7, -6, 1, 2, 12, solid(GRAPHITE_LIGHT)),
        Cube(-15, -19, -6, 14, 11, 12, corrugated(ORANGE, ORANGE_DARK)),
        Cube(1, -19, -6, 14, 11, 12, corrugated(ORANGE, ORANGE_DARK)),
        Cube(-15.5, -20, -6.5, 15, 1, 13, solid(GRAPHITE_LIGHT)),
        Cube(0.5, -20, -6.5, 15, 1, 13, solid(GRAPHITE_LIGHT)),
        Cube(-1, -12, -2, 2, 4, 4, solid(COPPER)),
    ])]


def flat_boxes(parts: list) -> list:
    """Model-space bounds of every cube not under a rotated part, with a label for error messages."""
    boxes = []

    def walk(part, origin):
        if any(part.rotation):
            return
        here = tuple(a + b for a, b in zip(origin, part.pivot))
        for cube in part.cubes:
            low = (here[0] + cube.x, here[1] + cube.y, here[2] + cube.z)
            boxes.append((f"{part.name} {low}", low, (low[0] + cube.w, low[1] + cube.h, low[2] + cube.d)))
        for child in part.children:
            walk(child, here)

    for part in parts:
        walk(part, (0, 0, 0))
    return boxes


def check_no_shared_faces(parts: list, name: str) -> None:
    """Two visible faces in one plane flicker in game, so a model that has any is refused."""
    problems = []
    boxes = flat_boxes(parts)
    for i, (label_a, low_a, high_a) in enumerate(boxes):
        for label_b, low_b, high_b in boxes[i + 1:]:
            for axis in range(3):
                others = [a for a in range(3) if a != axis]
                overlaps = all(min(high_a[a], high_b[a]) - max(low_a[a], low_b[a]) > 1e-6 for a in others)
                same_plane = abs(low_a[axis] - low_b[axis]) < 1e-6 or abs(high_a[axis] - high_b[axis]) < 1e-6
                if overlaps and same_plane:
                    problems.append(f"{label_a} and {label_b} share a face on axis {'xyz'[axis]}")
    if problems:
        raise ValueError(f"{name} model has overlapping faces:\n  " + "\n  ".join(problems))


def java_float(value) -> str:
    return f"{float(value)}F"


def one_sided(cube: Cube) -> str:
    """A zero-thickness box would draw two faces in one plane; one quad shows from both sides anyway."""
    if cube.w == 0:
        return ", Set.of(Direction.EAST)"
    if cube.d == 0:
        return ", Set.of(Direction.SOUTH)"
    return ""


def emit_part(part: Part, parent: str, lines: list, depth: int) -> None:
    variable = f"p{len(lines)}"
    cube_calls = "".join(
        f"\n                        .texOffs({c.u}, {c.v}).addBox({java_float(c.x)}, {java_float(c.y)}, {java_float(c.z)}, "
        f"{java_float(c.w)}, {java_float(c.h)}, {java_float(c.d)}{one_sided(c)})"
        for c in part.cubes
    )
    px, py, pz = part.pivot
    rx, ry, rz = (math.radians(angle) for angle in part.rotation)
    pose = (f"PartPose.offsetAndRotation({java_float(px)}, {java_float(py)}, {java_float(pz)}, "
            f"{java_float(round(rx, 5))}, {java_float(round(ry, 5))}, {java_float(round(rz, 5))})")
    prefix = f"PartDefinition {variable} = " if part.children else ""
    lines.append(f'        {prefix}{parent}.addOrReplaceChild("{part.name}", CubeListBuilder.create(){cube_calls},\n                {pose});')
    for child in part.children:
        emit_part(child, variable, lines, depth + 1)


def emit_layer(method: str, parts: list, size: int) -> str:
    lines = []
    for part in parts:
        emit_part(part, "root", lines, 0)
    body = "\n".join(lines)
    return (f"    static LayerDefinition {method}() {{\n"
            f"        MeshDefinition mesh = new MeshDefinition();\n"
            f"        PartDefinition root = mesh.getRoot();\n"
            f"{body}\n"
            f"        return LayerDefinition.create(mesh, {size}, {size});\n"
            f"    }}\n")


def write_tram_models() -> None:
    size = 256
    tram_main = chassis() + wheels() + tram_body() + pantograph()
    tram_panes = tram_glass()
    tram_lights = nose_lamps() + glow_strip(-11)
    cargo_main = chassis() + wheels() + cargo_body()
    cargo_lights = cargo_lamps() + glow_strip(-6)

    check_no_shared_faces(tram_main + tram_panes + tram_lights, "tram")
    check_no_shared_faces(cargo_main + cargo_lights, "cargo tram")

    pack(tram_main + tram_panes + tram_lights, size)
    pack(cargo_main + cargo_lights, size)
    paint_atlas(tram_main + tram_panes + tram_lights, size).save(ASSETS / "textures" / "entity" / "redstone_tram.png")
    paint_atlas(cargo_main + cargo_lights, size).save(ASSETS / "textures" / "entity" / "cargo_tram.png")

    source = (
        "package com.example.redstonetransportation.client;\n\n"
        "import net.minecraft.client.model.geom.PartPose;\n"
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;\n"
        "import net.minecraft.client.model.geom.builders.LayerDefinition;\n"
        "import net.minecraft.client.model.geom.builders.MeshDefinition;\n"
        "import net.minecraft.client.model.geom.builders.PartDefinition;\n"
        "import net.minecraft.core.Direction;\n\n"
        "import java.util.Set;\n\n"
        "/** Generated by tools/generate_assets.py together with the entity textures; edit the script, not this file. */\n"
        "final class TramGeometry {\n"
        "    private TramGeometry() {}\n\n"
        + emit_layer("tramBody", tram_main, size) + "\n"
        + emit_layer("tramGlass", tram_panes, size) + "\n"
        + emit_layer("tramLights", tram_lights, size) + "\n"
        + emit_layer("cargoBody", cargo_main, size) + "\n"
        + emit_layer("cargoLights", cargo_lights, size)
        + "}\n"
    )
    (JAVA / "client" / "TramGeometry.java").write_text(source, encoding="utf-8", newline="\n")


def main() -> None:
    write_block_textures()
    write_item_textures()
    for machine in MACHINES + [LEVELER]:
        write_machine(machine)
    write_controller()
    write_conveyor()
    write_drive_rail()
    for core in CORES:
        write_structure_core(core)
    write_balloon()
    write_elevator_call()
    write_route_beacon()
    for furnace in FURNACES:
        write_furnace(furnace)
    write_block_data()
    write_recipes()
    write_advancements()
    write_flat_item("track_layer", f"{NS}:item/track_layer")
    write_flat_item("cargo_tram", f"{NS}:item/cargo_tram")
    write_flat_item("structure_marker", f"{NS}:item/structure_marker")
    write_tram_models()
    print("generated")


if __name__ == "__main__":
    main()
