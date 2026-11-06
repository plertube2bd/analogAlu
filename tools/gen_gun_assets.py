#!/usr/bin/env python3
"""Textures, models, recipes and names for black powder, the three guns and their ammunition.

Standard library only. Run from anywhere; everything is written under src/main/resources.
"""
from __future__ import annotations

import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / "analog_alu"
DATA = RES / "data" / "analog_alu"
NS = "analog_alu"

O = (28, 28, 34)          # outline
M = (184, 190, 198)       # bright steel
m = (124, 130, 140)       # steel
d = (78, 82, 92)          # dark steel
W = (150, 92, 50)         # wood
w = (112, 66, 34)         # dark wood
B = (214, 172, 78)        # brass
b = (164, 124, 48)        # dark brass
R = (176, 44, 40)         # shell red
r = (128, 30, 30)         # dark red
P = (52, 50, 54)          # powder
p = (86, 84, 88)          # light powder
G = (150, 154, 160)       # lead
g = (104, 108, 114)       # dark lead

PALETTE = {"O": O, "M": M, "m": m, "d": d, "W": W, "w": w, "B": B, "b": b, "R": R, "r": r,
           "P": P, "p": p, "G": G, "g": g}

ART = {
    # Barrels point right; the hand holds the grip on the left.
    "revolver": [
        "................",
        "................",
        "................",
        "................",
        "......OOOOOOOOOO",
        "....OOMMMMMMMMMO",
        "...OmMdMmmmmmmmO",
        "...OmdMdOOOOOOOO",
        "...OmmmmOd.O....",
        "....OWWOOOOO....",
        "...OWWwO........",
        "..OWWwO.........",
        "..OWwwO.........",
        "..OOOO..........",
        "................",
        "................",
    ],
    "musket": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "....OOOOOOOOOOOO",
        "...OMMMMMMMMMMMO",
        "..OdOOWWWWWWWWOO",
        ".OdmOWWWWWWWOO..",
        "OWOOWWWwOOOO....",
        "OWWWWwwO........",
        "OWWwwO..........",
        "OOOOO...........",
        "................",
        "................",
        "................",
    ],
    "shotgun": [
        "................",
        "................",
        "................",
        "................",
        ".......OOOOOOOOO",
        "......OMMMMMMMMO",
        "......OdddddddOO",
        "......OMMMMMMMMO",
        "..OOOOOmmOOOOOOO",
        ".OWWWOOdOO......",
        "OWWWwOOO........",
        "OWWwO...........",
        "OwwO............",
        "OOO.............",
        "................",
        "................",
    ],
    "bullet_9mm": [
        "................",
        "................",
        "................",
        "....M...........",
        "...MMm....M.....",
        "...OOO...MMm....",
        "...BBb...OOO....",
        "...BBb...BBb....",
        "...BBb...BBb....",
        "...BBb...BBb....",
        "...bbb...BBb....",
        "...OOO...bbb....",
        ".........OOO....",
        "................",
        "................",
        "................",
    ],
    "musket_ball": [
        "................",
        "................",
        "................",
        "................",
        "......OOO.......",
        ".....OGGgO......",
        ".....OGggO......",
        "..OOO.OOO.OOO...",
        ".OGGgO...OGGgO..",
        ".OGggO...OGggO..",
        "..OOO.....OOO...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "shotgun_shell": [
        "................",
        "................",
        "................",
        "...OOOO.........",
        "...ORRrO..OOOO..",
        "...ORRrO..ORRrO.",
        "...ORRrO..ORRrO.",
        "...ORRrO..ORRrO.",
        "...ORRrO..ORRrO.",
        "...OBBbO..ORRrO.",
        "...OBBbO..OBBbO.",
        "...OOOOO..OBBbO.",
        "..........OOOOO.",
        "................",
        "................",
        "................",
    ],
    "black_powder": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......pP........",
        ".....PPpPP......",
        "....PpPPPpP.....",
        "...PPPPpPPPP....",
        "..PpPPPPPPpPP...",
        "..PPPPpPPPPPPp..",
        "................",
        "................",
        "................",
        "................",
    ],
}

GUNS = ["revolver", "musket", "shotgun"]
AMMO = ["bullet_9mm", "musket_ball", "shotgun_shell", "black_powder"]


def write_png(path: Path, rows: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = b""
    for row in rows:
        assert len(row) == 16, row
        raw += b"\x00" + b"".join(bytes((*PALETTE[c], 255)) if c in PALETTE else b"\x00\x00\x00\x00" for c in row)

    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


# A flat sprite turned side-on in the hand, barrel forward, rather than the diagonal of a tool.
GUN_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 90], "translation": [0, 2.5, 1.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -90], "translation": [0, 2.5, 1.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -160, 0], "translation": [1.13, 3.6, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 160, 0], "translation": [1.13, 3.6, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


# Solid colours for the 3D guns, each a 2x2 patch of one shared texture.
SWATCH = {"steel": (0, 0), "metal": (2, 0), "dark": (4, 0), "bore": (6, 0), "brass": (8, 0),
          "wood": (0, 2), "darkwood": (2, 2)}
SWATCH_COLORS = {"steel": M, "metal": m, "dark": d, "bore": (14, 14, 18), "brass": B, "wood": W, "darkwood": w}


def write_parts_texture() -> None:
    pixels = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for name, (u, v) in SWATCH.items():
        for y in range(v, v + 2):
            for x in range(u, u + 2):
                pixels[y][x] = (*SWATCH_COLORS[name], 255)
    path = ASSETS / "textures" / "item" / "gun_parts.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)

    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def box(start, end, color, front=None, rotation=None):
    """One cuboid of a gun, every face one colour; {front} is the colour of the face looking down the barrel (+x)."""
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        u, v = SWATCH[front if face == "east" and front else color]
        faces[face] = {"uv": [u, v, u + 2, v + 2], "texture": "#parts"}
    element = {"from": start, "to": end, "faces": faces}
    if rotation:
        element["rotation"] = rotation
    return element


GRIP_SLANT = 22.5

# Barrels point along +x and the top is +y; the hand holds the grip near x = 0.
GUN_ELEMENTS = {
    "revolver": [
        box([7, 9, 7.25], [16, 10.5, 8.75], "steel", front="bore"),
        box([8, 8.25, 7.5], [15, 9, 8.5], "metal"),
        box([15, 10.5, 7.75], [15.75, 11, 8.25], "dark"),
        box([4, 8, 6.75], [7, 10.75, 9.25], "metal"),
        box([2, 8, 7.25], [4, 11, 8.75], "dark"),
        box([2, 11, 7.6], [3, 12, 8.4], "dark"),
        box([4, 6.5, 7.6], [7, 7, 8.4], "dark"),
        box([6.5, 7, 7.6], [7, 8, 8.4], "dark"),
        box([5, 7, 7.75], [5.5, 8, 8.25], "dark"),
        box([0.5, 3.5, 7.1], [3, 8.25, 8.9], "wood",
            rotation={"angle": GRIP_SLANT, "axis": "z", "origin": [2, 8, 8]}),
        box([0.5, 3, 7.1], [3, 3.5, 8.9], "dark",
            rotation={"angle": GRIP_SLANT, "axis": "z", "origin": [2, 8, 8]}),
    ],
    "musket": [
        box([-2, 9.5, 7.4], [26, 10.7, 8.6], "metal", front="bore"),
        box([-2, 8.3, 7.2], [22, 9.5, 8.8], "wood"),
        box([2, 7.9, 7.75], [22, 8.3, 8.25], "darkwood"),
        box([-3, 8.6, 6.9], [0, 10, 7.2], "dark"),
        box([-2, 10.5, 7.6], [-1, 11.5, 8.4], "dark"),
        box([-4, 7.6, 7.6], [-1, 8.0, 8.4], "brass"),
        box([-6, 7.5, 7.2], [-2, 9.6, 8.8], "wood"),
        box([-14, 5.5, 7.1], [-6, 9.3, 8.9], "wood",
            rotation={"angle": GRIP_SLANT, "axis": "z", "origin": [-6, 8, 8]}),
        box([-14.5, 5.5, 7.1], [-14, 9.3, 8.9], "brass",
            rotation={"angle": GRIP_SLANT, "axis": "z", "origin": [-6, 8, 8]}),
    ],
    "shotgun": [
        box([3, 9.5, 6.9], [22, 10.6, 8], "steel", front="bore"),
        box([3, 9.5, 8], [22, 10.6, 9.1], "steel", front="bore"),
        box([3, 10.6, 7.9], [22, 10.8, 8.1], "dark"),
        box([8, 8.4, 7.1], [17, 9.5, 8.9], "wood"),
        box([-1, 8.2, 7.0], [3, 10.8, 9.0], "dark"),
        box([-1, 10.8, 7.2], [0, 11.6, 7.7], "dark"),
        box([-1, 10.8, 8.3], [0, 11.6, 8.8], "dark"),
        box([-1, 7.0, 7.6], [2, 7.4, 8.4], "dark"),
        box([0.5, 7.4, 7.75], [1, 8.2, 8.25], "dark"),
        box([-10, 5.5, 7.1], [-1, 9.5, 8.9], "wood",
            rotation={"angle": GRIP_SLANT, "axis": "z", "origin": [-1, 8, 8]}),
    ],
}

# How each gun sits in the hand. First person: barrel straight ahead, turned a touch toward the
# crosshair. Third person: barrel forward from the hand, grip in the fist.
GUN_HELD = {
    "revolver": {"scale": 0.68, "first": [-1.0, 2.0, -1.0], "third_scale": 0.8, "third": [0, 4.5, 1.5]},
    "musket": {"scale": 0.55, "first": [-1.0, 1.5, -4.0], "third_scale": 0.6, "third": [0, 6.0, 0.0]},
    "shotgun": {"scale": 0.6, "first": [-1.0, 1.5, -3.0], "third_scale": 0.62, "third": [0, 5.0, 0.3]},
}
AIM_TURN = 94


def held_display(name: str) -> dict:
    held = GUN_HELD[name]
    first = [held["scale"]] * 3
    third = [held["third_scale"]] * 3
    return {
        "firstperson_righthand": {"rotation": [0, AIM_TURN, 0], "translation": held["first"], "scale": first},
        "firstperson_lefthand": {"rotation": [0, -AIM_TURN, 0], "translation": held["first"], "scale": first},
        "thirdperson_righthand": {"rotation": [0, 90, 90], "translation": held["third"], "scale": third},
        "thirdperson_lefthand": {"rotation": [0, -90, -90], "translation": held["third"], "scale": third},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [held["scale"] * 0.6] * 3},
    }


def write_3d_guns() -> None:
    write_parts_texture()
    for name in GUNS:
        write_json(ASSETS / "models" / "item" / f"{name}_3d.json", {
            "textures": {"parts": f"{NS}:item/gun_parts", "particle": f"{NS}:item/gun_parts"},
            "elements": GUN_ELEMENTS[name],
            "display": held_display(name),
        })
        # The inventory keeps the crisp flat icon; everywhere else the gun is solid.
        write_json(ASSETS / "items" / f"{name}.json", {"model": {
            "type": "minecraft:select",
            "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "fixed"], "model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}}],
            "fallback": {"type": "minecraft:model", "model": f"{NS}:item/{name}_3d"},
        }})


def write_items() -> None:
    for name in GUNS + AMMO:
        write_png(ASSETS / "textures" / "item" / f"{name}.png", ART[name])
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}}
        if name in GUNS:
            model["display"] = GUN_DISPLAY
        write_json(ASSETS / "models" / "item" / f"{name}.json", model)
        write_json(ASSETS / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})


def write_recipes() -> None:
    def shapeless(name, ingredients, count, category="equipment"):
        write_json(DATA / "recipe" / f"{name}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": category,
            "ingredients": ingredients,
            "result": {"id": f"{NS}:{name}", "count": count},
        })

    def shaped(name, pattern, key):
        write_json(DATA / "recipe" / f"{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "equipment",
            "pattern": pattern,
            "key": key,
            "result": {"id": f"{NS}:{name}", "count": 1},
        })

    # Any coal will do, coal or charcoal (the minecraft:coals tag).
    shapeless("black_powder", ["minecraft:gunpowder", "#minecraft:coals"], 1, "misc")
    # Gunpowder itself, which vanilla cannot craft: bone meal stands in for saltpetre, then coal and flint.
    write_json(DATA / "recipe" / "gunpowder.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": ["minecraft:bone_meal", "#minecraft:coals", "minecraft:flint"],
        "result": {"id": "minecraft:gunpowder", "count": 1},
    })
    shapeless("bullet_9mm", [f"{NS}:black_powder", "minecraft:iron_ingot"], 4)
    shapeless("musket_ball", [f"{NS}:black_powder", "minecraft:iron_ingot", "minecraft:iron_ingot"], 4)
    shapeless("shotgun_shell", [f"{NS}:black_powder"] + ["minecraft:iron_nugget"] * 8, 1)
    shaped("revolver", ["III", " TP", "  P"],
           {"I": "minecraft:iron_ingot", "T": "minecraft:tripwire_hook", "P": "#minecraft:planks"})
    shaped("musket", ["I  ", " IF", "  L"],
           {"I": "minecraft:iron_ingot", "F": "minecraft:flint_and_steel", "L": "#minecraft:logs"})
    shaped("shotgun", ["II ", "ITF", " LL"],
           {"I": "minecraft:iron_ingot", "T": "minecraft:tripwire_hook", "F": "minecraft:flint_and_steel",
            "L": "#minecraft:logs"})


def add_to_tag(path: Path, values: list[str]) -> None:
    """Adds to a tag file other code also writes to (ALU's electrocution and stun gun), keeping what is there."""
    existing = json.loads(path.read_text(encoding="utf-8")).get("values", []) if path.exists() else []
    write_json(path, {"replace": False, "values": existing + [v for v in values if v not in existing]})


def write_damage() -> None:
    write_json(DATA / "damage_type" / "bullet.json", {
        "exhaustion": 0.1,
        "message_id": f"{NS}.bullet",
        "scaling": "when_caused_by_living_non_player",
    })
    # Projectile Protection counts against bullets, as it does against arrows.
    add_to_tag(RES / "data" / "minecraft" / "tags" / "damage_type" / "is_projectile.json", [f"{NS}:bullet"])
    # Every bullet lands in full, even a second shot straight after the first or pellets of the next blast.
    add_to_tag(RES / "data" / "minecraft" / "tags" / "damage_type" / "bypasses_cooldown.json", [f"{NS}:bullet"])


LANG = {
    "ko_kr": {
        "item.analog_alu.revolver": "리볼버",
        "item.analog_alu.musket": "머스킷",
        "item.analog_alu.shotgun": "샷건",
        "item.analog_alu.bullet_9mm": "9mm 총알",
        "item.analog_alu.musket_ball": "머스킷 총알",
        "item.analog_alu.shotgun_shell": "45게이지 산탄",
        "item.analog_alu.black_powder": "흑색화약",
        "tooltip.analog_alu.gun.damage": "피해 %s · 사거리 %s블록",
        "tooltip.analog_alu.gun.pellets": "산탄 %s알 × 피해 %s · 사거리 %s블록",
        "tooltip.analog_alu.gun.ammo": "탄약: %s",
        "tooltip.analog_alu.gun.loaded": "장전: %s/%s",
        "tooltip.analog_alu.revolver": "쏠 때마다 R로 공이치기를 당깁니다. 6발을 다 쓰면 R로 재장전(4초)",
        "tooltip.analog_alu.musket": "총알을 한 발씩 바로 넣어 쏩니다. 다음 발까지 3초",
        "tooltip.analog_alu.shotgun": "R로 재장전(3.5초) 하면 두 번 쏠 수 있습니다",
        "message.analog_alu.gun.reloading": "재장전 중...",
        "message.analog_alu.gun.empty": "총알이 없습니다 · R로 재장전",
        "message.analog_alu.gun.cock_first": "R을 눌러 공이치기를 당기세요",
        "message.analog_alu.gun.no_ammo": "탄약 없음: %s",
        "message.analog_alu.gun.musket_loads_itself": "머스킷은 쏠 때 바로 장전됩니다",
        "message.analog_alu.gun.already_cocked": "공이치기가 이미 당겨져 있습니다",
        "message.analog_alu.gun.cocked": "공이치기 당김 · %s/%s",
        "message.analog_alu.gun.full": "이미 가득 장전되어 있습니다",
        "message.analog_alu.gun.reload_started": "재장전 중... (%s초)",
        "message.analog_alu.gun.reloaded": "장전 완료 · %s/%s",
        "message.analog_alu.gun.load": "%s/%s",
        "message.analog_alu.gun.spare": "남은 %s: %s",
        "key.analog_alu.gun_action": "총: 공이치기 / 재장전",
        "key.category.analog_alu.guns": "총기",
        "death.attack.analog_alu.bullet": "%1$s님이 %2$s에게 총에 맞았습니다",
        "death.attack.analog_alu.bullet.item": "%1$s님이 %2$s의 %3$s에 맞았습니다",
        "death.attack.analog_alu.bullet.player": "%1$s님이 %2$s와 싸우다 총에 맞았습니다",
    },
    "en_us": {
        "item.analog_alu.revolver": "Revolver",
        "item.analog_alu.musket": "Musket",
        "item.analog_alu.shotgun": "Shotgun",
        "item.analog_alu.bullet_9mm": "9mm Round",
        "item.analog_alu.musket_ball": "Musket Ball",
        "item.analog_alu.shotgun_shell": "45 Gauge Shell",
        "item.analog_alu.black_powder": "Black Powder",
        "tooltip.analog_alu.gun.damage": "Damage %s · Range %s blocks",
        "tooltip.analog_alu.gun.pellets": "%s pellets × %s damage · Range %s blocks",
        "tooltip.analog_alu.gun.ammo": "Ammo: %s",
        "tooltip.analog_alu.gun.loaded": "Loaded: %s/%s",
        "tooltip.analog_alu.revolver": "Press R to cock before every shot. Once all six are gone, R reloads (4 s)",
        "tooltip.analog_alu.musket": "Loads one ball at a time as it fires. 3 s between shots",
        "tooltip.analog_alu.shotgun": "R reloads (3.5 s) for two shots",
        "message.analog_alu.gun.reloading": "Reloading...",
        "message.analog_alu.gun.empty": "Empty · press R to reload",
        "message.analog_alu.gun.cock_first": "Press R to cock the hammer",
        "message.analog_alu.gun.no_ammo": "Out of %s",
        "message.analog_alu.gun.musket_loads_itself": "The musket loads itself as it fires",
        "message.analog_alu.gun.already_cocked": "The hammer is already back",
        "message.analog_alu.gun.cocked": "Cocked · %s/%s",
        "message.analog_alu.gun.full": "Already fully loaded",
        "message.analog_alu.gun.reload_started": "Reloading... (%s s)",
        "message.analog_alu.gun.reloaded": "Loaded · %s/%s",
        "message.analog_alu.gun.load": "%s/%s",
        "message.analog_alu.gun.spare": "%s left: %s",
        "key.analog_alu.gun_action": "Gun: cock / reload",
        "key.category.analog_alu.guns": "Guns",
        "death.attack.analog_alu.bullet": "%1$s was shot by %2$s",
        "death.attack.analog_alu.bullet.item": "%1$s was shot by %2$s using %3$s",
        "death.attack.analog_alu.bullet.player": "%1$s was shot while fighting %2$s",
    },
}


def write_lang() -> None:
    for name, entries in LANG.items():
        path = ASSETS / "lang" / f"{name}.json"
        language = json.loads(path.read_text(encoding="utf-8"))
        language.update(entries)
        path.write_text(json.dumps(language, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def main() -> None:
    write_items()
    write_3d_guns()
    write_recipes()
    write_damage()
    write_lang()
    print("guns generated")


if __name__ == "__main__":
    main()
