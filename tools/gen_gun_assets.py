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

    shapeless("black_powder", ["minecraft:gunpowder", "minecraft:charcoal"], 1, "misc")
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


def write_damage() -> None:
    write_json(DATA / "damage_type" / "bullet.json", {
        "exhaustion": 0.1,
        "message_id": f"{NS}.bullet",
        "scaling": "when_caused_by_living_non_player",
    })
    # Projectile Protection counts against bullets, as it does against arrows.
    write_json(RES / "data" / "minecraft" / "tags" / "damage_type" / "is_projectile.json",
               {"replace": False, "values": [f"{NS}:bullet"]})
    # Every bullet lands in full, even a second shot straight after the first or pellets of the next blast.
    write_json(RES / "data" / "minecraft" / "tags" / "damage_type" / "bypasses_cooldown.json",
               {"replace": False, "values": [f"{NS}:bullet"]})


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
    write_recipes()
    write_damage()
    write_lang()
    print("guns generated")


if __name__ == "__main__":
    main()
