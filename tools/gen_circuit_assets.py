"""전선(copper_cable)과 회로 장치(battery, circuit_switch, resistor_load)의 텍스처/모델/블록스테이트를 생성한다.

실행: 프로젝트 루트에서  python3 tools/gen_circuit_assets.py

전선: 가운데 기둥(core) + 연결된 방향으로 뻗는 팔(arm)을 멀티파트 블록스테이트로 조합한다.
      (north/east/south/west/up/down 속성은 CopperCableBlock이 이웃에 맞춰 계산한다.)
장치: 모델의 north 면 = FACING 방향 단자(+), south 면 = 반대쪽 단자(-). 블록스테이트가 FACING에 맞게 y 회전한다.
"""
import json
import math
import os
import random
import struct
import zlib

NS = "analog_alu"
ASSETS = f"src/main/resources/assets/{NS}"
TEX = f"{ASSETS}/textures/block"


# ---------------------------------------------------------------- 입출력
def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w", encoding="utf-8"), indent=2, ensure_ascii=False)


def write_png(name, grid):
    path = f"{TEX}/{name}.png"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(grid[y][x]) for x in range(16)) for y in range(16))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    open(path, "wb").write(data)


# ---------------------------------------------------------------- 그리기 도구
def rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def new(fill):
    return [[fill] * 16 for _ in range(16)]


def rect(g, x1, y1, x2, y2, c):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                g[y][x] = c


def px(g, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        g[y][x] = c


def frame(g, border, inner=None):
    rect(g, 0, 0, 15, 0, border)
    rect(g, 0, 15, 15, 15, border)
    rect(g, 0, 0, 0, 15, border)
    rect(g, 15, 0, 15, 15, border)
    if inner:
        rect(g, 1, 1, 14, 1, inner)
        rect(g, 1, 1, 1, 14, inner)


def disc(g, cx, cy, r, c):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - cx, y - cy) <= r:
                g[y][x] = c


def ring(g, cx, cy, r_out, r_in, c):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if r_in < d <= r_out:
                g[y][x] = c


def noise(g, seed, amount, only=None):
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if only and not only(g[y][x]):
                continue
            d = rnd.randint(-amount, amount)
            r, gr, b, a = g[y][x]
            g[y][x] = (max(0, min(255, r + d)), max(0, min(255, gr + d)), max(0, min(255, b + d)), a)


COPPER = rgb("#c87f3d")
COPPER_LIGHT = rgb("#ecaa63")
COPPER_DARK = rgb("#8a4f26")


def terminal(g, polarity):
    """단자 면: 둥근 구리 접점. polarity: '+', '-', None"""
    ring_color = {"+": rgb("#d94b4b"), "-": rgb("#4b7bd9"), None: rgb("#555b63")}[polarity]
    ring(g, 7.5, 7.5, 6.2, 4.6, ring_color)
    disc(g, 7.5, 7.5, 4.6, COPPER)
    disc(g, 7.5, 7.5, 3.4, COPPER_LIGHT)
    mark = rgb("#5a2f12")
    if polarity == "+":
        rect(g, 6, 7, 9, 8, mark)
        rect(g, 7, 6, 8, 9, mark)
    elif polarity == "-":
        rect(g, 6, 7, 9, 8, mark)


# ---------------------------------------------------------------- 전선
def cable_texture():
    g = new(rgb("#9c5c2e"))
    noise(g, 11, 7)
    rect(g, 0, 2, 15, 2, rgb("#6e3f1f"))   # 피복 마디
    rect(g, 0, 13, 15, 13, rgb("#6e3f1f"))
    rect(g, 6, 6, 9, 9, COPPER_LIGHT)       # 노출된 구리 심선 (팔 끝/기둥 접점)
    rect(g, 7, 7, 8, 8, rgb("#f6c98a"))
    write_png("copper_cable", g)


def cable_models_and_state():
    def face(texture, uv):
        return {"uv": uv, "texture": texture}

    models = f"{ASSETS}/models/block"
    tex = {"cable": f"{NS}:block/copper_cable", "particle": f"{NS}:block/copper_cable"}
    # 가운데 기둥 6x6x6
    write_json(f"{models}/copper_cable_core.json", {
        "textures": tex,
        "elements": [{"from": [5, 5, 5], "to": [11, 11, 11],
                      "faces": {d: face("#cable", [5, 5, 11, 11]) for d in ("north", "south", "east", "west", "up", "down")}}]})
    # 북쪽으로 뻗는 팔 (다른 방향은 블록스테이트에서 회전)
    write_json(f"{models}/copper_cable_arm.json", {
        "textures": tex,
        "elements": [{"from": [6, 6, 0], "to": [10, 10, 5], "faces": {
            "north": face("#cable", [6, 6, 10, 10]),
            "up": face("#cable", [6, 0, 10, 5]), "down": face("#cable", [6, 0, 10, 5]),
            "east": face("#cable", [0, 6, 5, 10]), "west": face("#cable", [0, 6, 5, 10])}}]})
    # 아이템/GUI용: 십자 모양
    arms = [
        {"from": [6, 6, 0], "to": [10, 10, 5], "faces": {"north": face("#cable", [6, 6, 10, 10]), "up": face("#cable", [6, 0, 10, 5]), "down": face("#cable", [6, 0, 10, 5]), "east": face("#cable", [0, 6, 5, 10]), "west": face("#cable", [0, 6, 5, 10])}},
        {"from": [6, 6, 11], "to": [10, 10, 16], "faces": {"south": face("#cable", [6, 6, 10, 10]), "up": face("#cable", [6, 11, 10, 16]), "down": face("#cable", [6, 11, 10, 16]), "east": face("#cable", [11, 6, 16, 10]), "west": face("#cable", [11, 6, 16, 10])}},
        {"from": [0, 6, 6], "to": [5, 10, 10], "faces": {"west": face("#cable", [6, 6, 10, 10]), "up": face("#cable", [0, 6, 5, 10]), "down": face("#cable", [0, 6, 5, 10]), "north": face("#cable", [0, 6, 5, 10]), "south": face("#cable", [0, 6, 5, 10])}},
        {"from": [11, 6, 6], "to": [16, 10, 10], "faces": {"east": face("#cable", [6, 6, 10, 10]), "up": face("#cable", [11, 6, 16, 10]), "down": face("#cable", [11, 6, 16, 10]), "north": face("#cable", [11, 6, 16, 10]), "south": face("#cable", [11, 6, 16, 10])}},
    ]
    write_json(f"{models}/copper_cable.json", {
        "parent": "minecraft:block/block", "textures": tex,
        "elements": [{"from": [5, 5, 5], "to": [11, 11, 11],
                      "faces": {d: face("#cable", [5, 5, 11, 11]) for d in ("north", "south", "east", "west", "up", "down")}}] + arms})
    # 멀티파트 블록스테이트
    arm = f"{NS}:block/copper_cable_arm"
    parts = [{"apply": {"model": f"{NS}:block/copper_cable_core"}}]
    for name, rot in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}),
                      ("up", {"x": 270}), ("down", {"x": 90})):
        parts.append({"when": {name: "true"}, "apply": {"model": arm, **rot}})
    write_json(f"{ASSETS}/blockstates/copper_cable.json", {"multipart": parts})


# ---------------------------------------------------------------- 장치 공통
def cube_model(name, textures, parent_textures=None):
    """north/south/east/west/up/down 텍스처 이름을 받아 큐브 모델을 쓴다."""
    tex = {k: f"{NS}:block/{v}" for k, v in textures.items()}
    tex["particle"] = tex["side"]
    faces = {"north": "#front", "south": "#back", "east": "#side", "west": "#side", "up": "#top", "down": "#bottom"}
    write_json(f"{ASSETS}/models/block/{name}.json", {
        "textures": tex,
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                      "faces": {d: {"uv": [0, 0, 16, 16], "texture": t, "cullface": d} for d, t in faces.items()}}]})


# ---------------------------------------------------------------- 배터리
def battery_textures():
    casing, edge, dark = rgb("#2c3a48"), rgb("#4a6178"), rgb("#18222c")
    g = new(casing); noise(g, 21, 4); frame(g, edge)
    rect(g, 2, 3, 13, 12, rgb("#e6be3c"))        # 노란 라벨
    rect(g, 2, 3, 13, 3, rgb("#f6d96a"))
    for x, y in ((9, 4), (8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 8), (7, 9), (6, 10)):
        px(g, x, y, dark)                          # 번개 무늬
    write_png("battery_side", g)
    g = new(casing); noise(g, 22, 4); frame(g, edge); rect(g, 3, 3, 12, 12, rgb("#222f3b"))
    write_png("battery_top", g)
    g = new(casing); frame(g, edge); terminal(g, "+"); write_png("battery_front", g)
    g = new(casing); frame(g, edge); terminal(g, "-"); write_png("battery_back", g)
    cube_model("battery", {"front": "battery_front", "back": "battery_back", "side": "battery_side",
                           "top": "battery_top", "bottom": "battery_top"})


# ---------------------------------------------------------------- 스위치
def switch_textures():
    metal, edge, dark = rgb("#8a929c"), rgb("#5b636d"), rgb("#2a2f35")
    g = new(metal); noise(g, 31, 5); frame(g, edge, rgb("#a9b0b8"))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(g, x, y, dark)                          # 리벳
    write_png("circuit_switch_side", g)
    for state, knob_rows, knob_color, led in (("off", (9, 12), rgb("#d94b4b"), rgb("#5a1a1a")),
                                              ("on", (3, 6), rgb("#52d96b"), rgb("#52d96b"))):
        g = new(metal); noise(g, 32, 5); frame(g, edge, rgb("#a9b0b8"))
        rect(g, 6, 2, 9, 13, dark)                 # 레버 홈
        rect(g, 6, knob_rows[0], 9, knob_rows[1], knob_color)
        rect(g, 7, knob_rows[0], 8, knob_rows[0], rgb("#ffffff") if state == "on" else rgb("#ff9a9a"))
        rect(g, 12, 2, 13, 3, led)                 # 상태 LED
        write_png(f"circuit_switch_top_{state}", g)
    g = new(metal); frame(g, edge, rgb("#a9b0b8")); terminal(g, None)
    write_png("circuit_switch_end", g)
    for state in ("off", "on"):
        cube_model(f"circuit_switch_{state}", {"front": "circuit_switch_end", "back": "circuit_switch_end",
                                               "side": "circuit_switch_side", "top": f"circuit_switch_top_{state}",
                                               "bottom": "circuit_switch_side"})


# ---------------------------------------------------------------- 저항 부하
def resistor_textures():
    body, edge = rgb("#d2b88a"), rgb("#a68a5c")
    bands = ((3, rgb("#7a3f1c")), (6, rgb("#1e1e1e")), (9, rgb("#c0352b")), (13, rgb("#d4af37")))

    def banded(vertical):
        g = new(body); noise(g, 41, 5); frame(g, edge)
        for pos, color in bands:
            if vertical:
                rect(g, pos, 1, pos + 1, 14, color)
            else:
                rect(g, 1, pos, 14, pos + 1, color)
        return g

    write_png("resistor_load_side", banded(True))    # 옆면: 길이 방향(좌우)으로 띠
    write_png("resistor_load_top", banded(False))    # 윗면: 길이 방향(앞뒤)으로 띠
    g = new(body); frame(g, edge); terminal(g, None); write_png("resistor_load_end", g)
    cube_model("resistor_load", {"front": "resistor_load_end", "back": "resistor_load_end", "side": "resistor_load_side",
                                 "top": "resistor_load_top", "bottom": "resistor_load_top"})


cable_texture()
cable_models_and_state()
battery_textures()
switch_textures()
resistor_textures()
print("circuit assets generated")
