"""Generates the Void Altar block assets: textures (pure-Python PNG), block models, the multipart blockstate (rotated per
facing) and the item model definition. Reuses the helpers of gen_altar_assets.py.

Model (16-px grid, drawn facing south - the open side of the crown faces the arena):
  base          plinth, step, dish                                      (always)
  core_dormant  dim rune column, dark orb and the three crown prongs    (no trophy set)
  core_active   the orb burns crimson, the column runes glow            (any trophy set)
  trophy_heart  crimson Wyrm Heart gem on the back prong                (heart = true)
  trophy_eye    cyan Tempest Eye gem on the left prong                  (eye = true)
  trophy_core   amber Behemoth Core gem on the right prong              (core = true)
  item          base + active core + all three gems

Run:  python tools/altar/gen_void_altar_assets.py
"""
import math
import os
import random

import gen_altar_assets as ga

# (block state property, model / texture suffix, gem colours dark -> bright) per prong, in PRONGS order
TROPHIES = [("heart", "heart", (150, 10, 50), (255, 150, 185)),
            ("eye", "eye", (20, 110, 170), (200, 250, 255)),
            ("core", "core_gem", (190, 80, 10), (255, 225, 140))]
STONE = (26, 22, 30)
STONE_DARK = (12, 10, 16)
STONE_LIGHT = (52, 46, 60)
CRIMSON = (200, 30, 80)
CRIMSON_DIM = (70, 15, 35)
CRIMSON_BRIGHT = (255, 120, 160)


def jitter(rng, c, a):
    return ga.jitter(rng, c, a)


def stone_tile(seed=5):
    rng = random.Random(seed)
    img = ga.canvas()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img[y][x] = jitter(rng, STONE_LIGHT if edge else STONE, 4)
            if (x == 7 or y == 7) and not edge:
                img[y][x] = jitter(rng, STONE_DARK, 2)
    return img


GLYPH = ["#....#", ".#..#.", "..##..", "..##..", ".#..#.", "#....#"]


def side_texture(lit, seed=15):
    rng = random.Random(seed)
    runes = {(5 + x, 5 + y) for y, row in enumerate(GLYPH) for x, c in enumerate(row) if c == "#"}
    img = ga.canvas()
    for v in range(16):
        for u in range(16):
            if v >= 13:
                if v == 13:
                    c = jitter(rng, STONE_LIGHT, 3)
                elif v == 14 and u % 3 == 1:
                    c = jitter(rng, CRIMSON if lit else CRIMSON_DIM, 5)
                else:
                    c = jitter(rng, STONE if v == 14 else STONE_DARK, 4)
            elif v >= 11:
                c = jitter(rng, STONE_LIGHT if v == 11 else STONE, 4)
            elif v >= 5:
                c = jitter(rng, ga.mix(STONE, STONE_LIGHT, 0.3) if u in (4, 11) else STONE_DARK, 3)
                if (u, v) in runes:
                    c = jitter(rng, CRIMSON_BRIGHT if lit else CRIMSON_DIM, 4)
            elif v >= 2:
                c = jitter(rng, ga.mix(STONE_LIGHT, CRIMSON, 0.25) if v == 2 else STONE, 4)
            else:
                c = jitter(rng, STONE, 4)
            img[v][u] = c
    return img


def top_texture(seed=25):
    """Dish top: dark stone with a crimson rune circle."""
    rng = random.Random(seed)
    img = ga.canvas()
    for v in range(16):
        for u in range(16):
            d = math.hypot(u - 7.5, v - 7.5)
            c = jitter(rng, STONE, 4)
            if abs(d - 5.5) < 0.6:
                c = jitter(rng, CRIMSON_DIM, 4)
            if abs(d - 3.0) < 0.5:
                c = jitter(rng, STONE_DARK, 2)
            img[v][u] = c
    return img


def prong_texture(seed=35):
    rng = random.Random(seed)
    img = ga.canvas()
    for v in range(16):
        for u in range(16):
            t = 1 - v / 15
            c = ga.mix((14, 8, 20), (60, 30, 70), t)
            if u % 4 == 0:
                c = ga.mix(c, (120, 40, 90), 0.4)
            img[v][u] = jitter(rng, c, 3)
    return img


def orb_texture(lit, seed=45):
    rng = random.Random(seed)
    img = ga.canvas()
    for v in range(16):
        for u in range(16):
            dx, dy = u - 7.5, v - 7.5
            r = math.hypot(dx, dy) / 7.5
            swirl = 0.5 + 0.5 * math.sin(math.atan2(dy, dx) * 3 + r * 8)
            if lit:
                c = ga.mix(ga.mix((120, 10, 45), (255, 130, 170), max(0.0, 1 - r) ** 1.4), (60, 0, 25), swirl * 0.4 * r)
            else:
                c = ga.mix((6, 4, 10), (40, 10, 25), swirl * (1 - r * 0.6))
            img[v][u] = jitter(rng, c, 3)
    return img


def gem_texture(dark, bright, seed=55):
    rng = random.Random(seed)
    img = ga.canvas()
    for v in range(16):
        for u in range(16):
            t = 1 - v / 15
            c = ga.mix(dark, bright, t ** 0.9)
            if (u + v) % 5 == 0:
                c = ga.mix(c, (255, 255, 255), 0.25)
            img[v][u] = jitter(rng, c, 4)
    return img


# --------------------------------------------------------------------------------------------------------- models
def base_elements():
    f = ga.faces
    return [
        ga.box([0, 0, 0], [16, 3, 16], f("#side", up="#stone", down="#stone", cull_bottom=True)),
        ga.box([2, 3, 2], [14, 5, 14], f("#side", up="#stone")),
        ga.box([2, 12, 2], [14, 14, 14], f("#side", up="#top", down="#stone")),
    ]


# three prongs: back (north) and both sides; the south side stays open toward the arena
PRONGS = [
    dict(x=8.0, z=3.5, axis="x", angle=22.5, tip=(8.0, 21.0, 6.0)),
    dict(x=3.5, z=9.5, axis="z", angle=-22.5, tip=(6.4, 21.0, 9.5)),
    dict(x=12.5, z=9.5, axis="z", angle=22.5, tip=(9.6, 21.0, 9.5)),
]


def prong(p, emission):
    h = 0.8
    return ga.box([p["x"] - h, 14, p["z"] - h], [p["x"] + h, 22, p["z"] + h],
                  ga.faces("#prong", up="#prong", uv_side=[6, 0, 8, 16], uv_up=[6, 0, 8, 2]),
                  rotation={"origin": [p["x"], 14, p["z"]], "axis": p["axis"], "angle": p["angle"]}, emission=emission)


def core_elements(lit):
    e = 15 if lit else None
    orb = ga.box([6.5, 14, 6.5], [9.5, 17, 9.5], ga.faces("#orb", up="#orb", down="#orb", uv_side=[4, 4, 12, 12], uv_up=[4, 4, 12, 12]),
                 rotation={"origin": [8, 14, 8], "axis": "y", "angle": 45}, emission=e)
    column = ga.box([4.5, 5, 4.5], [11.5, 12, 11.5], ga.faces("#side"), emission=10 if lit else None)
    return [column, orb] + [prong(p, None) for p in PRONGS]


def gem_element(i, tex="#gem"):
    x, y, z = PRONGS[i]["tip"]
    return ga.box([x - 1.0, y - 1.2, z - 1.0], [x + 1.0, y + 1.2, z + 1.0],
                  ga.faces(tex, up=tex, down=tex, uv_side=[4, 2, 8, 8], uv_up=[4, 4, 8, 8]),
                  rotation={"origin": [x, y, z], "axis": "y", "angle": 45}, emission=15)


def main():
    tex_dir, model_dir = ga.TEX, ga.MODELS
    textures = {
        "void_altar_stone": stone_tile(), "void_altar_side": side_texture(False), "void_altar_side_lit": side_texture(True),
        "void_altar_top": top_texture(), "void_altar_prong": prong_texture(), "void_altar_orb": orb_texture(False),
        "void_altar_orb_lit": orb_texture(True),
    }
    for _, suffix, dark, bright in TROPHIES:
        textures["void_altar_" + suffix] = gem_texture(dark, bright)
    for name, img in textures.items():
        ga.write_png(os.path.join(tex_dir, name + ".png"), img)

    t = lambda n: "boss_mode:block/" + n
    base_tex = {"side": t("void_altar_side"), "stone": t("void_altar_stone"), "top": t("void_altar_top")}
    dormant_tex = {"side": t("void_altar_side"), "orb": t("void_altar_orb"), "prong": t("void_altar_prong")}
    active_tex = {"side": t("void_altar_side_lit"), "orb": t("void_altar_orb_lit"), "prong": t("void_altar_prong")}

    def model(elements, textures_):
        return {"parent": "minecraft:block/block", "textures": dict(textures_, particle=t("void_altar_side")), "elements": elements}

    ga.write_json(os.path.join(model_dir, "void_altar_base.json"), model(base_elements(), base_tex))
    ga.write_json(os.path.join(model_dir, "void_altar_core_dormant.json"), model(core_elements(False), dormant_tex))
    ga.write_json(os.path.join(model_dir, "void_altar_core_active.json"), model(core_elements(True), active_tex))
    for i, (prop, suffix, _, _) in enumerate(TROPHIES):
        ga.write_json(os.path.join(model_dir, "void_altar_trophy_%s.json" % prop),
                      {"textures": {"gem": t("void_altar_" + suffix), "particle": t("void_altar_" + suffix)}, "elements": [gem_element(i)]})
        old = os.path.join(model_dir, "void_altar_heart_%d.json" % (i + 1))  # the old three-heart models
        if os.path.exists(old):
            os.remove(old)
    item_core = [dict(e, faces={k: dict(f, texture="#side_lit" if f["texture"] == "#side" else f["texture"]) for k, f in e["faces"].items()})
                 for e in core_elements(True)]
    ga.write_json(os.path.join(model_dir, "void_altar.json"),
                  model(base_elements() + item_core + [gem_element(i, "#" + prop) for i, (prop, _, _, _) in enumerate(TROPHIES)],
                        dict(base_tex, side_lit=active_tex["side"], orb=active_tex["orb"], prong=active_tex["prong"],
                             **{prop: t("void_altar_" + suffix) for prop, suffix, _, _ in TROPHIES})))

    parts = []
    for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        def apply(name):
            a = {"model": "boss_mode:block/" + name}
            if y:
                a["y"] = y
            return a
        parts.append({"when": {"facing": facing}, "apply": apply("void_altar_base")})
        parts.append({"when": {"facing": facing, "heart": "false", "eye": "false", "core": "false"}, "apply": apply("void_altar_core_dormant")})
        parts.append({"when": {"OR": [{"facing": facing, prop: "true"} for prop, _, _, _ in TROPHIES]}, "apply": apply("void_altar_core_active")})
        for prop, _, _, _ in TROPHIES:
            parts.append({"when": {"facing": facing, prop: "true"}, "apply": apply("void_altar_trophy_" + prop)})
    ga.write_json(os.path.join(ga.ASSETS, "blockstates", "void_altar.json"), {"multipart": parts})
    ga.write_json(os.path.join(ga.ASSETS, "items", "void_altar.json"), {"model": {"type": "minecraft:model", "model": "boss_mode:block/void_altar"}})
    print("%d textures, %d models, blockstate (%d parts) written" % (len(textures), 4 + len(TROPHIES), len(parts)))


if __name__ == "__main__":
    main()
