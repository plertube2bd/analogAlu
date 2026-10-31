"""구리선(신호 세기별 밝기 + 잠금 표시)과 질량 분석기(진행도 막대)의 텍스처/모델/블록스테이트를 생성한다.

실행: 프로젝트 루트에서  python3 tools/gen_wire_analyzer_assets.py

구리선
  입력 텍스처: copper_wire_dot.png, copper_wire_line.png        (세기 0: 꺼진 구리색)
               copper_wire_dot_on.png, copper_wire_line_on.png  (세기 15: 가장 밝은 색 - 이 스크립트의 입력이므로 지우지 말 것)
  세기 1~14는 두 색 사이를 보간한 텍스처로 만들고, 모델에는 light_emission = 세기를 넣어
  어두운 곳에서도 세기에 비례해 스스로 빛나게 한다. (세기 0은 기존 모델 그대로, 발광 없음)
  잠금(lock 1~16) 상태에는 중앙 점 주위에 파란 모서리 괄호 표시를 덧그린다.

질량 분석기
  stage 0 = 투입물 없음, 1~8 = 진행도 1/8 ~ 8/8. stage 1~8일 때 네 옆면에 진행 막대를 덧그린다.
"""
import json, os, struct, zlib

R = "src/main/resources"
NS = "analog_alu"
TEX = f"{R}/assets/{NS}/textures/block"


# ---------------------------------------------------------------- 공통 입출력
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w", encoding="utf-8"), indent=2, ensure_ascii=False)


def write_png(path, grid):
    """grid[y][x] = (r, g, b, a), 16x16"""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(grid[y][x]) for x in range(16)) for y in range(16))

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    open(path, "wb").write(data)


def read_png(path):
    """8비트 RGB/RGBA PNG를 읽어 rows[y][x] = (r, g, b, a) 로 돌려준다 (Pillow 없이)."""
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos, idat = 8, b""
    width = height = color_type = None
    while pos < len(data):
        n, t = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        pos += 12 + n
        if t == b"IHDR":
            width, height, depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert depth == 8 and interlace == 0 and color_type in (2, 6), f"unsupported PNG: {path}"
        elif t == b"IDAT":
            idat += body
    bpp = 4 if color_type == 6 else 3
    raw, stride = zlib.decompress(idat), width * bpp
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        ft, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if ft == 1:
                line[x] = (line[x] + a) & 255
            elif ft == 2:
                line[x] = (line[x] + b) & 255
            elif ft == 3:
                line[x] = (line[x] + ((a + b) >> 1)) & 255
            elif ft == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        prev = line
        rows.append([tuple(line[k:k + bpp]) + ((255,) if bpp == 3 else ()) for k in range(0, stride, bpp)])
    return rows


# ---------------------------------------------------------------- 구리선
def ramp_factor(power):
    """세기 1 -> 0.20 (눈에 띄게 켜진 어두운 주황), 세기 15 -> 1.00 (기존 'on' 색)"""
    return 0.2 + 0.8 * (power - 1) / 14


def blend(off, on, f):
    out = []
    for row_off, row_on in zip(off, on):
        row = []
        for po, pn in zip(row_off, row_on):
            if po[3] == 0 and pn[3] == 0:
                row.append((0, 0, 0, 0))
            else:
                row.append(tuple(round(po[k] + (pn[k] - po[k]) * f) for k in range(3)) + (255,))
        out.append(row)
    return out


def wire_model(tex, frm, to, face, uv, emission):
    element = {"from": frm, "to": to, "faces": {face: {"uv": uv, "texture": "#t"}}}
    if emission:
        element["light_emission"] = emission
    return {"textures": {"t": f"{NS}:block/{tex}", "particle": f"{NS}:item/copper_wire"}, "elements": [element]}


def gen_wire():
    ramps = {}
    for kind in ("dot", "line"):
        off = read_png(f"{TEX}/copper_wire_{kind}.png")
        on = read_png(f"{TEX}/copper_wire_{kind}_on.png")
        ramps[kind] = (off, on)

    for p in range(1, 16):
        for kind in ("dot", "line"):
            off, on = ramps[kind]
            write_png(f"{TEX}/copper_wire_{kind}_{p}.png", blend(off, on, ramp_factor(p)))
        m = f"{R}/assets/{NS}/models/block"
        w(f"{m}/copper_wire_dot_{p}.json", wire_model(f"copper_wire_dot_{p}", [0, 0.25, 0], [16, 0.25, 16], "up", [0, 0, 16, 16], p))
        w(f"{m}/copper_wire_side_{p}.json", wire_model(f"copper_wire_line_{p}", [0, 0.25, 0], [16, 0.25, 8], "up", [0, 0, 16, 8], p))
        w(f"{m}/copper_wire_up_{p}.json", wire_model(f"copper_wire_line_{p}", [0, 0, 0.25], [16, 16, 0.25], "south", [0, 0, 16, 16], p))

    # 잠금 표시: 중앙 점(6~9) 바깥의 8x8 모서리 괄호
    lock = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    blue = (130, 200, 255, 255)
    for (x, y) in ((4, 4), (5, 4), (4, 5), (11, 4), (10, 4), (11, 5), (4, 11), (5, 11), (4, 10), (11, 11), (10, 11), (11, 10)):
        lock[y][x] = blue
    write_png(f"{TEX}/copper_wire_lock.png", lock)
    w(f"{R}/assets/{NS}/models/block/copper_wire_lock.json",
      wire_model("copper_wire_lock", [0, 0.5, 0], [16, 0.5, 16], "up", [0, 0, 16, 16], 8))

    # 블록스테이트: 세기(0~15) x (중앙 점 / 4방향 팔 / 4방향 벽) + 잠금 오버레이
    def names(p, suffix):
        return f"{NS}:block/copper_wire_{suffix}" if p == 0 else f"{NS}:block/copper_wire_{suffix}_{p}"

    parts = []
    for p in range(16):
        parts.append({"when": {"power": str(p)}, "apply": {"model": names(p, "dot")}})
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for p in range(16):
            parts.append({"when": {"AND": [{facing: "side|up"}, {"power": str(p)}]},
                          "apply": {"model": names(p, "side"), "y": rot}})
            parts.append({"when": {"AND": [{facing: "up"}, {"power": str(p)}]},
                          "apply": {"model": names(p, "up"), "y": rot}})
    parts.append({"when": {"lock": "|".join(str(i) for i in range(1, 17))}, "apply": {"model": f"{NS}:block/copper_wire_lock"}})
    w(f"{R}/assets/{NS}/blockstates/copper_wire.json", {"multipart": parts})


# ---------------------------------------------------------------- 질량 분석기
def bar_texture(stage):
    border, track = (12, 16, 20, 255), (36, 42, 48, 255)
    fill_hi, fill_lo = (110, 235, 240, 255), (60, 190, 205, 255)
    g = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for x in range(16):
        g[6][x] = border
        g[9][x] = border
        for y, color in ((7, fill_hi), (8, fill_lo)):
            g[y][x] = color if x < stage * 2 else track
    return g


def gen_analyzer():
    face = lambda: {"uv": [0, 0, 16, 16], "texture": "#bar"}
    parts = [{"when": {"active": "false"}, "apply": {"model": f"{NS}:block/mass_analyzer"}},
             {"when": {"active": "true"}, "apply": {"model": f"{NS}:block/mass_analyzer_active"}}]
    for s in range(1, 9):
        write_png(f"{TEX}/mass_analyzer_bar_{s}.png", bar_texture(s))
        w(f"{R}/assets/{NS}/models/block/mass_analyzer_bar_{s}.json", {
            "textures": {"bar": f"{NS}:block/mass_analyzer_bar_{s}", "particle": f"{NS}:block/mass_analyzer_side"},
            "elements": [{"from": [-0.01, -0.01, -0.01], "to": [16.01, 16.01, 16.01], "shade": False, "light_emission": 15,
                          "faces": {"north": face(), "south": face(), "east": face(), "west": face()}}]})
        parts.append({"when": {"stage": str(s)}, "apply": {"model": f"{NS}:block/mass_analyzer_bar_{s}"}})
    w(f"{R}/assets/{NS}/blockstates/mass_analyzer.json", {"multipart": parts})


gen_wire()
gen_analyzer()
print("wire + analyzer assets generated")
