"""연산 블록(가산기/감산기/연장기/시프트)의 텍스처, 모델, 블록스테이트, 레시피, 루트 테이블, 번역을 생성한다."""
import json, os, zlib, struct

R = "src/main/resources"
NS = "analog_alu"

def png(path, grid):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(grid[y][x]) for x in range(16)) for y in range(16))
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    open(path, "wb").write(data)

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w", encoding="utf-8"), indent=2, ensure_ascii=False)

def dim(c, f):
    return tuple(int(v * f) for v in c) + (255,)

def solid(c):
    return tuple(c) + (255,)

CHEV_R = ["#...#..", ".#...#.", "..#...#", ".#...#.", "#...#.."]
SYMS = {
    "adder": ["..#..", "..#..", "#####", "..#..", "..#.."],
    "subtractor": [".....", ".....", "#####", ".....", "....."],
    "extender": ["...#...", "....#..", "#######", "....#..", "...#..."],
    "shift_right": CHEV_R,
    "shift_left": [row[::-1] for row in CHEV_R],
    "or_gate": ["#...#", "#...#", ".#.#.", ".#.#.", "..#.."],
    "and_gate": ["..#..", ".#.#.", ".#.#.", "#...#", "#...#"],
    "xor_gate": [".###.", "#.#.#", "#####", "#.#.#", ".###."],
    "bitwise_not": [".#...", "#.#.#", "...#.", ".....", "....."],
    "logical_not": ["..#..", "..#..", "..#..", ".....", "..#.."],
}
COLORS = {
    "adder": (60, 175, 80), "subtractor": (80, 125, 215), "extender": (235, 145, 50),
    "shift_right": (175, 90, 205), "shift_left": (60, 190, 205),
    "or_gate": (225, 80, 80), "and_gate": (225, 200, 70), "xor_gate": (90, 210, 150),
    "bitwise_not": (230, 100, 165), "logical_not": (200, 130, 90),
}
ARITH = ("adder", "subtractor")
GATES = ("or_gate", "and_gate", "xor_gate")
NAMES = {
    "adder": ("Adder", "가산기"), "subtractor": ("Subtractor", "감산기"),
    "extender": ("Redstone Extender", "레드스톤 연장기"),
    "shift_right": ("Bit Shift Right (SHR)", "비트 시프트 우 (SHR)"),
    "shift_left": ("Bit Shift Left (SHL)", "비트 시프트 좌 (SHL)"),
    "cram": ("CRAM (4-bit Memory)", "CRAM (4비트 메모리)"),
    "bitwise_not": ("Bitwise NOT", "비트 NOT"),
    "logical_not": ("Logical NOT", "논리 NOT"),
    "or_gate": ("OR Gate (right-click: NOR)", "OR 게이트 (우클릭: NOR)"),
    "and_gate": ("AND Gate (right-click: NAND)", "AND 게이트 (우클릭: NAND)"),
    "xor_gate": ("XOR Gate (right-click: XNOR)", "XOR 게이트 (우클릭: XNOR)"),
}

def top_texture(name, lit, carry_on):
    base = (142, 142, 142) if lit else (112, 112, 112)
    border = (92, 92, 92)
    g = [[solid(border if x in (0, 15) or y in (0, 15) else base) for x in range(16)] for y in range(16)]
    sc = dim(COLORS[name], 1.0 if lit else 0.5)
    def put(x, y, c):
        g[y][x] = c
    for x, y in ((7, 1), (8, 1), (6, 2), (7, 2), (8, 2), (9, 2)):
        put(x, y, sc)
    sym = SYMS[name]
    x0, y0 = (16 - len(sym[0])) // 2, 5
    for j, row in enumerate(sym):
        for i, ch in enumerate(row):
            if ch == "#":
                put(x0 + i, y0 + j, sc)
    def block(xs, ys, c):
        for x in xs:
            for y in ys:
                put(x, y, c)
    f = 1.0 if lit else 0.6
    if name in ARITH or name in GATES:
        block((1, 2), (7, 8), dim((200, 60, 60), f))     # 입력 A (왼쪽)
        block((13, 14), (7, 8), dim((60, 90, 200), f))   # 입력 B (오른쪽)
        if name in ARITH:
            block((7, 8), (13, 14), dim((220, 200, 60), f) if carry_on else solid((45, 45, 45)))  # Carry/Borrow (뒷면)
        elif carry_on:
            block((7, 8), (3, 4), dim((240, 240, 240), f))  # 반전(NOR/NAND/XNOR) 표시
    else:
        block((7, 8), (13, 14), dim((225, 225, 225), f))  # 입력 (뒷면)
    return g

side = [[solid((100, 100, 100) if x in (0, 15) or y in (0, 15) else (118, 118, 118)) for x in range(16)] for y in range(16)]
png(f"{R}/assets/{NS}/textures/block/alu_side.png", side)

def model(top):
    face = lambda tex, uv, **kw: dict({"uv": uv, "texture": tex}, **kw)
    return {
        "textures": {"top": f"{NS}:block/{top}", "side": f"{NS}:block/alu_side", "particle": f"{NS}:block/alu_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
            "up": face("#top", [0, 0, 16, 16]),
            "down": face("#side", [0, 0, 16, 16], cullface="down"),
            "north": face("#side", [0, 14, 16, 16]), "south": face("#side", [0, 14, 16, 16]),
            "east": face("#side", [0, 14, 16, 16]), "west": face("#side", [0, 14, 16, 16])}}]}

POW = "|".join(str(i) for i in range(1, 16))
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

for name in COLORS:
    arith = name in ARITH
    gate = name in GATES
    variants = [("", False), ("_inv", True)] if gate else [("", True)] + ([("_nocarry", False)] if arith else [])
    for vsuf, carry_on in variants:
        for lsuf, lit in (("", False), ("_on", True)):
            top = f"{name}_top{vsuf}{lsuf}"
            png(f"{R}/assets/{NS}/textures/block/{top}.png", top_texture(name, lit, carry_on))
            w(f"{R}/assets/{NS}/models/block/{name}{vsuf}{lsuf}.json", model(top))

    parts = []
    for f, rot in FACINGS:
        for vsuf, carry_on in variants:
            cond_carry = [{"carry_enabled": "true" if carry_on else "false"}] if arith else ([{"inverted": "true" if carry_on else "false"}] if gate else [])
            off_when = [{"facing": f}] + cond_carry + [{"power": "0"}] + ([{"carry": "false"}] if arith else [])
            lit_when = {"power": POW} if not arith else {"OR": [{"power": POW}, {"carry": "true"}]}
            on_when = [{"facing": f}] + cond_carry + [lit_when]
            parts.append({"when": {"AND": off_when}, "apply": {"model": f"{NS}:block/{name}{vsuf}", "y": rot}})
            parts.append({"when": {"AND": on_when}, "apply": {"model": f"{NS}:block/{name}{vsuf}_on", "y": rot}})
    w(f"{R}/assets/{NS}/blockstates/{name}.json", {"multipart": parts})
    w(f"{R}/assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
    w(f"{R}/data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "conditions": [{"condition": "minecraft:survives_explosion"}],
                   "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}]}]})

# 제작법 (서바이벌, JEI/REI에서 조회 가능한 일반 레시피)
def shaped(name, pattern, key):
    w(f"{R}/data/{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": pattern, "key": key, "result": {"id": f"{NS}:{name}", "count": 1}})

def shapeless(name, ingredients):
    w(f"{R}/data/{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": "redstone",
        "ingredients": ingredients, "result": {"id": f"{NS}:{name}", "count": 1}})

STONES = ["minecraft:stone", "minecraft:cobblestone", "minecraft:cobbled_deepslate"]
K = {"C": "minecraft:copper_ingot", "R": "minecraft:redstone", "S": STONES,
     "P": "minecraft:comparator", "T": "minecraft:redstone_torch", "Q": "minecraft:repeater"}
shaped("adder", ["CRC", "RSR"], {k: K[k] for k in "CRS"})
shaped("subtractor", ["CTC", "RSR"], {k: K[k] for k in "CTRS"})
shaped("extender", [" C ", "RSR"], {k: K[k] for k in "CRS"})
shaped("or_gate", [" R ", "CSC"], {k: K[k] for k in "RCS"})
shaped("and_gate", [" T ", "CSC"], {k: K[k] for k in "TCS"})
shaped("xor_gate", [" R ", "CTC"], {k: K[k] for k in "RCT"})
shaped("logical_not", ["T", "S"], {k: K[k] for k in "TS"})
shapeless("bitwise_not", [f"{NS}:logical_not", "minecraft:copper_ingot"])
shapeless("shift_right", ["minecraft:copper_ingot", "minecraft:redstone_torch", STONES])
shapeless("shift_left", ["minecraft:copper_ingot", "minecraft:redstone", STONES])

# 번역 (기존 항목 유지)
for lang, idx in (("en_us", 0), ("ko_kr", 1)):
    p = f"{R}/assets/{NS}/lang/{lang}.json"
    d = json.load(open(p, encoding="utf-8"))
    for name, names in NAMES.items():
        d[f"block.{NS}.{name}"] = names[idx]
    w(p, d)


# ---------------------------------------------------------------- CRAM (정육면체)
def face(base, marks, border=(70, 70, 70)):
    g = [[solid(border if x in (0, 15) or y in (0, 15) else base) for x in range(16)] for y in range(16)]
    for (x, y), c in marks:
        g[y][x] = solid(c)
    return g

ORANGE, WHITE, RED = (235, 145, 50), (235, 235, 235), (215, 60, 60)
arrow_out = [((x, y), ORANGE) for y, xs in ((4, (7, 8)), (5, (6, 7, 8, 9)), (6, (5, 6, 7, 8, 9, 10)),
                                            *[(yy, (7, 8)) for yy in range(7, 12)]) for x in xs]
arrow_in = [((x, y), WHITE) for y, xs in ((11, (7, 8)), (10, (6, 7, 8, 9)), (9, (5, 6, 7, 8, 9, 10)),
                                          *[(yy, (7, 8)) for yy in range(4, 9)]) for x in xs]
cross = [((x, y), RED) for i in range(5, 11) for x, y in ((i, i), (i, 15 - i))]
chip = lambda c: [((x, y), c) for x in range(4, 12) for y in range(4, 12)]
pins = lambda c: [((x, y), c) for x in range(4, 12, 2) for y in (2, 13)] + [((x, y), c) for y in range(4, 12, 2) for x in (2, 13)]

png(f"{R}/assets/{NS}/textures/block/cram_out.png", face((112, 112, 112), arrow_out))
png(f"{R}/assets/{NS}/textures/block/cram_in.png", face((112, 112, 112), arrow_in))
png(f"{R}/assets/{NS}/textures/block/cram_reset.png", face((112, 112, 112), cross))
png(f"{R}/assets/{NS}/textures/block/cram_side.png", face((112, 112, 112), []))
png(f"{R}/assets/{NS}/textures/block/cram_top.png", face((45, 105, 115), chip((25, 60, 70)) + pins((190, 190, 190))))
png(f"{R}/assets/{NS}/textures/block/cram_top_on.png", face((70, 175, 190), chip((40, 110, 125)) + pins((250, 250, 250))))

for lsuf in ("", "_on"):
    t = lambda n: f"{NS}:block/{n}"
    f = lambda tex, d: {"uv": [0, 0, 16, 16], "texture": tex, "cullface": d}
    w(f"{R}/assets/{NS}/models/block/cram{lsuf}.json", {
        "textures": {"out": t("cram_out"), "in": t("cram_in"), "reset": t("cram_reset"),
                     "side": t("cram_side"), "top": t("cram_top" + lsuf), "particle": t("cram_side")},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            "north": f("#out", "north"), "south": f("#in", "south"), "west": f("#reset", "west"),
            "east": f("#side", "east"), "up": f("#top", "up"), "down": f("#side", "down")}}]})

w(f"{R}/assets/{NS}/blockstates/cram.json", {"multipart": [
    part for fc, rot in FACINGS for part in (
        {"when": {"AND": [{"facing": fc}, {"power": "0"}]}, "apply": {"model": f"{NS}:block/cram", "y": rot}},
        {"when": {"AND": [{"facing": fc}, {"power": POW}]}, "apply": {"model": f"{NS}:block/cram_on", "y": rot}})]})
w(f"{R}/assets/{NS}/items/cram.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/cram"}})
w(f"{R}/data/{NS}/loot_table/blocks/cram.json", {
    "type": "minecraft:block",
    "pools": [{"rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:survives_explosion"}],
               "entries": [{"type": "minecraft:item", "name": f"{NS}:cram"}]}]})
shaped("cram", ["RCR", "RSR", "SSS"], {k: K[k] for k in "RCS"})

for lang, idx in (("en_us", 0), ("ko_kr", 1)):
    p = f"{R}/assets/{NS}/lang/{lang}.json"
    d = json.load(open(p, encoding="utf-8"))
    d[f"block.{NS}.cram"] = NAMES["cram"][idx]
    w(p, d)
