"""Generates the Shard Altar block assets: textures (pure-Python PNG), block models, the multipart blockstate and the
item model definition - and, with other palettes, the Tempest Altar (storm cyan on grey stone) and the Bastion Altar
(ember amber on dark basalt), which share the Shard Altar's shape and block states.

Model parts (all in the 16-px block grid, the crystal reaches 23 px = 1.44 blocks):
  base          plinth, step, basin slab and the four rim walls (always drawn)
  core_dormant  rune column (dim), dark pool, grey-violet crystals             (charge = 0)
  core_active   rune column (lit), glowing pool, shining crystals, emissive     (charge >= 1)
  nub_1..12     one lit crystal stud on the rim per offered shard              (charge >= i)
  item          base + active core, for the inventory

Side textures use automatic UVs (u = x, v = 16 - y), so shard_altar_side is painted as a side view of the silhouette:
rows 13-15 plinth, 11-12 step, 5-10 rune column, 2-4 basin and rim.

Run:  python tools/altar/gen_altar_assets.py      (all three altars)
"""
import json
import math
import os
import random
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "boss_mode")
TEX = os.path.join(ASSETS, "textures", "block")
MODELS = os.path.join(ASSETS, "models", "block")
COST = 12  # BossConfig.ALTAR_SHARD_COST


# ---------------------------------------------------------------------------------------------------------- PNG
def write_png(path, pixels, width=16, height=16):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(bytes(max(0, min(255, int(c))) for c in pixels[y][x]))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def canvas(color=(0, 0, 0, 255)):
    return [[list(color) for _ in range(16)] for _ in range(16)]


def mix(a, b, t):
    return [a[i] + (b[i] - a[i]) * t for i in range(3)] + [255]


def jitter(rng, color, amount):
    d = rng.uniform(-amount, amount)
    return [color[0] + d, color[1] + d, color[2] + d * 1.2, 255]


# Palettes: stone (tile / dark mortar / light), accent (runes: normal / dim / bright), pool (lit centre -> edge / swirl,
# dormant dark -> light), crystal (lit low -> high, dormant low -> high), socket.
PALETTES = {
    "shard_altar": dict(stone=(40, 35, 46), stone_dark=(24, 21, 29), stone_light=(72, 64, 82),
                        accent=(126, 72, 196), accent_dim=(64, 42, 96), accent_bright=(222, 180, 255),
                        pool_lit=((150, 90, 230), (238, 214, 255), (110, 60, 190)), pool_dim=((34, 20, 52), (58, 38, 84)),
                        crystal_lit=((96, 44, 170), (236, 208, 255)), crystal_dim=((52, 42, 72), (124, 108, 156)), socket=(14, 10, 18)),
    "tempest_altar": dict(stone=(78, 84, 92), stone_dark=(44, 48, 56), stone_light=(128, 136, 146),
                          accent=(52, 168, 214), accent_dim=(40, 78, 98), accent_bright=(190, 246, 255),
                          pool_lit=((60, 170, 230), (220, 250, 255), (30, 110, 170)), pool_dim=((22, 36, 50), (44, 66, 84)),
                          crystal_lit=((30, 120, 190), (214, 250, 255)), crystal_dim=((50, 66, 80), (118, 140, 156)), socket=(16, 20, 26)),
    "bastion_altar": dict(stone=(46, 38, 34), stone_dark=(24, 19, 17), stone_light=(86, 72, 62),
                          accent=(226, 118, 34), accent_dim=(96, 50, 22), accent_bright=(255, 214, 140),
                          pool_lit=((240, 120, 30), (255, 236, 170), (190, 70, 16)), pool_dim=((48, 22, 12), (80, 42, 20)),
                          crystal_lit=((190, 90, 20), (255, 230, 150)), crystal_dim=((74, 52, 38), (150, 118, 86)), socket=(18, 12, 10)),
}
P = PALETTES["shard_altar"]
NAME = "shard_altar"


def stone_tile(seed):
    """Polished blackstone-like tile: two rows of offset bricks with dark mortar."""
    rng = random.Random(seed)
    img = canvas()
    for y in range(16):
        for x in range(16):
            mortar = y in (0, 8) or (x == (0 if y < 8 else 8))
            img[y][x] = jitter(rng, P["stone_dark"] if mortar else P["stone"], 3 if mortar else 6)
            if not mortar and (y in (1, 9)):
                img[y][x] = jitter(rng, mix(P["stone"], P["stone_light"], 0.45), 4)
    return img


def rune_pixels():
    """A 6x6 rune glyph centred on the column face (u 5..10, v 5..10)."""
    glyph = [
        "..#...",
        ".###..",
        "#.#.#.",
        "..#..#",
        ".#.#..",
        "#...#.",
    ]
    return {(5 + x, 5 + y) for y, row in enumerate(glyph) for x, c in enumerate(row) if c == "#"}


def side_texture(lit, seed=11):
    STONE, STONE_DARK, STONE_LIGHT = P["stone"], P["stone_dark"], P["stone_light"]
    VIOLET, VIOLET_DIM, VIOLET_BRIGHT = P["accent"], P["accent_dim"], P["accent_bright"]
    rng = random.Random(seed)
    img = canvas()
    runes = rune_pixels()
    for v in range(16):
        for u in range(16):
            if v >= 13:  # plinth
                if v == 13:
                    c = jitter(rng, STONE_LIGHT, 4)
                elif v == 14 and u % 4 == 1:
                    c = jitter(rng, VIOLET if lit else VIOLET_DIM, 6)
                else:
                    c = jitter(rng, STONE if v == 14 else STONE_DARK, 5)
            elif v >= 11:  # step
                c = jitter(rng, STONE_LIGHT if v == 11 else STONE, 5)
            elif v >= 5:  # rune column
                edge = u in (4, 11)
                c = jitter(rng, mix(STONE, STONE_LIGHT, 0.35) if edge else STONE_DARK, 4)
                if (u, v) in runes:
                    c = jitter(rng, VIOLET_BRIGHT if lit else VIOLET_DIM, 5)
                elif lit and any((u + du, v + dv) in runes for du, dv in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    c = mix(c, VIOLET, 0.35)
            elif v >= 2:  # basin + rim
                if v == 2:
                    c = jitter(rng, mix(STONE_LIGHT, VIOLET, 0.35), 5)
                elif v == 4:
                    c = jitter(rng, STONE_DARK, 3)
                else:
                    c = jitter(rng, STONE, 5)
            else:
                c = jitter(rng, STONE, 5)
            img[v][u] = c
    return img


NUB_POSITIONS = [(2, 2), (6, 2), (10, 2), (14, 2), (14, 6), (14, 10), (14, 14), (10, 14), (6, 14), (2, 14), (2, 10), (2, 6)]


def rim_texture(seed=21):
    """Rim top: violet-edged stone with a dark socket where each shard stud sits."""
    rng = random.Random(seed)
    img = canvas()
    for v in range(16):
        for u in range(16):
            ring = min(u, v, 15 - u, 15 - v)
            c = jitter(rng, mix(P["stone_light"], P["accent"], 0.4) if ring == 1 else P["stone"], 5)
            img[v][u] = c
    for x, z in NUB_POSITIONS:
        for du in (-1, 0):
            for dv in (-1, 0):
                img[z + dv][x + du] = jitter(rng, P["socket"], 2)
    return img


def pool_texture(lit, seed=31):
    rng = random.Random(seed)
    img = canvas()
    for v in range(16):
        for u in range(16):
            dx, dy = u - 7.5, v - 7.5
            r = math.hypot(dx, dy) / 7.5
            swirl = 0.5 + 0.5 * math.sin(math.atan2(dy, dx) * 3 + r * 7)
            if lit:
                edge, centre, swirl_c = P["pool_lit"]
                base = mix(edge, centre, max(0.0, 1 - r) ** 1.5)
                c = mix(base, swirl_c, swirl * 0.3 * r)
            else:
                c = mix(P["pool_dim"][0], P["pool_dim"][1], swirl * (1 - r * 0.6))
            img[v][u] = jitter(rng, c, 4)
    return img


def crystal_texture(lit, seed=41):
    rng = random.Random(seed)
    img = canvas()
    low, high = P["crystal_lit"] if lit else P["crystal_dim"]
    for v in range(16):
        for u in range(16):
            t = 1 - v / 15
            c = mix(low, high, t ** 0.8)
            if u % 4 == 0:  # facet edges
                c = mix(c, high, 0.35)
            if (u + v) % 7 == 0:
                c = mix(c, (255, 255, 255) if lit else high, 0.25)
            img[v][u] = jitter(rng, c, 5)
    return img


# ------------------------------------------------------------------------------------------------------- models
def faces(tex, sides=True, up=None, down=None, uv_side=None, uv_up=None, cull_bottom=False):
    out = {}
    if sides:
        for f in ("north", "south", "east", "west"):
            out[f] = {"texture": tex}
            if uv_side:
                out[f]["uv"] = uv_side
    if up:
        out["up"] = {"texture": up}
        if uv_up:
            out["up"]["uv"] = uv_up
    if down:
        out["down"] = {"texture": down}
        if cull_bottom:
            out["down"]["cullface"] = "down"
    return out


def box(frm, to, face_map, rotation=None, emission=None):
    e = {"from": frm, "to": to, "faces": face_map}
    if rotation:
        e["rotation"] = rotation
    if emission:
        e["light_emission"] = emission
    return e


def base_elements():
    return [
        box([0, 0, 0], [16, 3, 16], faces("#side", up="#stone", down="#stone", cull_bottom=True)),
        box([2, 3, 2], [14, 5, 14], faces("#side", up="#stone")),
        box([1, 11, 1], [15, 13, 15], faces("#side", up="#stone", down="#stone")),
        box([1, 13, 1], [15, 14, 3], faces("#side", up="#rim")),
        box([1, 13, 13], [15, 14, 15], faces("#side", up="#rim")),
        box([1, 13, 3], [3, 14, 13], faces("#side", up="#rim")),
        box([13, 13, 3], [15, 14, 13], faces("#side", up="#rim")),
    ]


def crystal(cx, cz, half, y0, y1, angle, emission):
    return box([cx - half, y0, cz - half], [cx + half, y1, cz + half],
               faces("#crystal", up="#crystal", uv_side=[5, 0, 8, 16], uv_up=[5, 0, 8, 3]),
               rotation={"origin": [cx, y0, cz], "axis": "y", "angle": angle}, emission=emission)


def core_elements(lit):
    e = 15 if lit else None
    return [
        box([4, 5, 4], [12, 11, 12], faces("#side")),
        box([3, 13, 3], [13, 13.5, 13], faces("#side", up="#pool"), emission=12 if lit else None),
        crystal(8, 8, 1.5, 13.5, 23, 45, e),
        crystal(5.25, 5.25, 0.75, 13.5, 17, 22.5, e),
        crystal(10.75, 5.25, 0.75, 13.5, 16, -22.5, e),
        crystal(10.75, 10.75, 0.75, 13.5, 18, 22.5, e),
        crystal(5.25, 10.75, 0.75, 13.5, 15.5, -22.5, e),
    ]


def model(elements, textures):
    return {"parent": "minecraft:block/block", "textures": dict(textures, particle=f"boss_mode:block/{NAME}_side"),
            "elements": elements}


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for name in PALETTES:
        generate(name)


def generate(name):
    global P, NAME
    P, NAME = PALETTES[name], name
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    textures = {
        f"{name}_stone": stone_tile(3),
        f"{name}_side": side_texture(False),
        f"{name}_side_lit": side_texture(True),
        f"{name}_rim": rim_texture(),
        f"{name}_pool": pool_texture(False),
        f"{name}_pool_lit": pool_texture(True),
        f"{name}_crystal": crystal_texture(False),
        f"{name}_crystal_lit": crystal_texture(True),
    }
    for tex_name, img in textures.items():
        write_png(os.path.join(TEX, tex_name + ".png"), img)

    base_tex = {"side": f"boss_mode:block/{name}_side", "stone": f"boss_mode:block/{name}_stone", "rim": f"boss_mode:block/{name}_rim"}
    dormant_tex = {"side": f"boss_mode:block/{name}_side", "pool": f"boss_mode:block/{name}_pool", "crystal": f"boss_mode:block/{name}_crystal"}
    active_tex = {"side": f"boss_mode:block/{name}_side_lit", "pool": f"boss_mode:block/{name}_pool_lit",
                  "crystal": f"boss_mode:block/{name}_crystal_lit"}
    write_json(os.path.join(MODELS, f"{name}_base.json"), model(base_elements(), base_tex))
    write_json(os.path.join(MODELS, f"{name}_core_dormant.json"), model(core_elements(False), dormant_tex))
    write_json(os.path.join(MODELS, f"{name}_core_active.json"), model(core_elements(True), active_tex))
    # The inventory model needs both texture sets, so the lit core's side texture gets its own key.
    item_elements = base_elements() + [dict(e, faces={k: dict(f, texture="#side_lit" if f["texture"] == "#side" else f["texture"])
                                                      for k, f in e["faces"].items()}) for e in core_elements(True)]
    write_json(os.path.join(MODELS, f"{name}.json"), model(item_elements, dict(base_tex, side_lit=active_tex["side"],
                                                                                    pool=active_tex["pool"], crystal=active_tex["crystal"])))
    for i, (x, z) in enumerate(NUB_POSITIONS, start=1):
        angle = 22.5 if i % 2 else -22.5
        nub = crystal(x, z, 0.7, 14, 16.5 + (i % 3) * 0.75, angle, 15)
        write_json(os.path.join(MODELS, f"{name}_nub_{i}.json"),
                   {"textures": {"crystal": f"boss_mode:block/{name}_crystal_lit", "particle": f"boss_mode:block/{name}_crystal_lit"},
                    "elements": [nub]})

    parts = [
        {"apply": {"model": f"boss_mode:block/{name}_base"}},
        {"when": {"charge": "0"}, "apply": {"model": f"boss_mode:block/{name}_core_dormant"}},
        {"when": {"charge": "|".join(str(c) for c in range(1, COST + 1))}, "apply": {"model": f"boss_mode:block/{name}_core_active"}},
    ]
    for i in range(1, COST + 1):
        parts.append({"when": {"charge": "|".join(str(c) for c in range(i, COST + 1))}, "apply": {"model": f"boss_mode:block/{name}_nub_{i}"}})
    os.makedirs(os.path.join(ASSETS, "blockstates"), exist_ok=True)
    write_json(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"multipart": parts})
    write_json(os.path.join(ASSETS, "items", f"{name}.json"), {"model": {"type": "minecraft:model", "model": f"boss_mode:block/{name}"}})
    print(f"{len(textures)} textures, {3 + 1 + COST} models, blockstate + item definition written under {os.path.normpath(ASSETS)}")


if __name__ == "__main__":
    main()
