"""Generate chunk-loader and LED assets, recipes, drops, and silicon advancements."""
import json
import os
import struct
import zlib

ROOT = "src/main/resources"
NAMESPACE = "analog_alu"


def write_json(path, value):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as output:
        json.dump(value, output, ensure_ascii=False, indent=2)
        output.write("\n")


def write_png(path, pixels):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    payload = b"\x89PNG\r\n\x1a\n"
    payload += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    payload += chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    with open(path, "wb") as output:
        output.write(payload)


def led_texture(color, lit, digital):
    pixels = [[(31, 39, 43, 255) for _ in range(16)] for _ in range(16)]
    edge = (13, 18, 21, 255)
    surface = (49, 59, 62, 255)
    tint = color if lit else tuple(channel // 3 for channel in color)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                pixels[y][x] = edge
            elif x in (1, 14) or y in (1, 14):
                pixels[y][x] = surface
            elif 3 <= x <= 12 and 3 <= y <= 12:
                pixels[y][x] = (*tint, 255)
    if digital:
        pattern = ((6, 4), (7, 4), (8, 4), (9, 4), (5, 5), (10, 5),
                   (5, 6), (10, 6), (6, 7), (7, 7), (8, 7), (9, 7),
                   (5, 8), (10, 8), (5, 9), (10, 9), (6, 10), (7, 10),
                   (8, 10), (9, 10))
        for x, y in pattern:
            pixels[y][x] = (244, 246, 232, 255) if lit else (98, 103, 99, 255)
    else:
        for x in range(5, 11):
            pixels[7][x] = pixels[8][x] = (248, 249, 235, 255) if lit else (99, 105, 101, 255)
        for y in range(5, 11):
            pixels[y][7] = pixels[y][8] = (248, 249, 235, 255) if lit else (99, 105, 101, 255)
    return pixels


def generate_block_cube(name, texture, with_item=True):
    model = {"parent": "minecraft:block/cube_all", "textures": {
        "all": f"{NAMESPACE}:block/{texture}", "particle": f"{NAMESPACE}:block/{texture}"}}
    write_json(f"{ROOT}/assets/{NAMESPACE}/models/block/{name}.json", model)
    if with_item:
        write_json(f"{ROOT}/assets/{NAMESPACE}/items/{name}.json", {
            "model": {"type": "minecraft:model", "model": f"{NAMESPACE}:block/{name}"}})
        write_json(f"{ROOT}/data/{NAMESPACE}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{NAMESPACE}:{name}"}]}]})


COLORS = {
    "red": ((224, 67, 63), "Red"),
    "green": ((64, 190, 110), "Green"),
    "blue": ((71, 133, 225), "Blue"),
    "white": ((224, 224, 206), "White"),
}

for tier, tint in (("chunk_loader", (64, 190, 198)), ("chunk_loader_tier_2", (215, 164, 64))):
    pixels = [[(29, 38, 42, 255) for _ in range(16)] for _ in range(16)]
    for y in range(2, 14):
        for x in range(2, 14):
            pixels[y][x] = (51, 62, 65, 255)
    for y in range(4, 12):
        for x in range(4, 12):
            pixels[y][x] = (*tint, 255)
    for x, y in ((7, 3), (8, 3), (6, 4), (9, 4), (5, 5), (10, 5),
                 (5, 6), (10, 6), (4, 7), (11, 7), (4, 8), (11, 8),
                 (5, 9), (10, 9), (5, 10), (10, 10), (6, 11), (9, 11)):
        pixels[y][x] = (235, 242, 226, 255)
    write_png(f"{ROOT}/assets/{NAMESPACE}/textures/block/{tier}.png", pixels)
    generate_block_cube(tier, tier)
    write_json(f"{ROOT}/assets/{NAMESPACE}/blockstates/{tier}.json", {
        "variants": {"": {"model": f"{NAMESPACE}:block/{tier}"}}})

for mode in ("analog", "digital"):
    for color, (tint, display) in COLORS.items():
        name = f"{mode}_led_{color}"
        off_texture = f"{name}_off"
        on_texture = f"{name}_on"
        write_png(f"{ROOT}/assets/{NAMESPACE}/textures/block/{off_texture}.png",
                  led_texture(tint, False, mode == "digital"))
        write_png(f"{ROOT}/assets/{NAMESPACE}/textures/block/{on_texture}.png",
                  led_texture(tint, True, mode == "digital"))
        for suffix, texture in (("off", off_texture), ("on", on_texture)):
            generate_block_cube(f"{name}_{suffix}", texture, with_item=False)
        write_json(f"{ROOT}/data/{NAMESPACE}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{NAMESPACE}:{name}"}]}]})
        active_powers = "|".join(str(power) for power in range(1, 16))
        write_json(f"{ROOT}/assets/{NAMESPACE}/blockstates/{name}.json", {
            "multipart": [
                {"when": {"power": "0"}, "apply": {"model": f"{NAMESPACE}:block/{name}_off"}},
                {"when": {"power": active_powers}, "apply": {"model": f"{NAMESPACE}:block/{name}_on"}},
            ]})
        write_json(f"{ROOT}/assets/{NAMESPACE}/models/item/{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NAMESPACE}:block/{off_texture}"}})
        write_json(f"{ROOT}/assets/{NAMESPACE}/items/{name}.json", {
            "model": {"type": "minecraft:model", "model": f"{NAMESPACE}:item/{name}"}})

        ingredient = "minecraft:redstone_torch" if mode == "digital" else "minecraft:redstone"
        recipe = {
            "type": "minecraft:crafting_shapeless", "category": "redstone",
            "ingredients": ["minecraft:redstone_lamp", ingredient, f"minecraft:{color}_dye"],
            "result": {"id": f"{NAMESPACE}:{name}", "count": 1},
        }
        write_json(f"{ROOT}/data/{NAMESPACE}/recipe/{name}.json", recipe)

        en = json.load(open(f"{ROOT}/assets/{NAMESPACE}/lang/en_us.json", encoding="utf-8"))
        ko = json.load(open(f"{ROOT}/assets/{NAMESPACE}/lang/ko_kr.json", encoding="utf-8"))
        en[f"block.{NAMESPACE}.{name}"] = f"{display} {mode.title()} LED"
        ko_mode = "아날로그" if mode == "analog" else "디지털"
        ko_color = {"red": "빨강", "green": "초록", "blue": "파랑", "white": "흰색"}[color]
        ko[f"block.{NAMESPACE}.{name}"] = f"{ko_color} {ko_mode} LED"
        write_json(f"{ROOT}/assets/{NAMESPACE}/lang/en_us.json", en)
        write_json(f"{ROOT}/assets/{NAMESPACE}/lang/ko_kr.json", ko)

write_json(f"{ROOT}/data/{NAMESPACE}/recipe/chunk_loader.json", {
    "type": "minecraft:crafting_shaped", "category": "redstone",
    "pattern": ["IRI", "RER", "IRI"],
    "key": {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "E": "minecraft:ender_pearl"},
    "result": {"id": f"{NAMESPACE}:chunk_loader", "count": 1}})
write_json(f"{ROOT}/data/{NAMESPACE}/recipe/chunk_loader_tier_2.json", {
    "type": "minecraft:crafting_shapeless", "category": "redstone",
    "ingredients": [f"{NAMESPACE}:chunk_loader", "minecraft:diamond"],
    "result": {"id": f"{NAMESPACE}:chunk_loader_tier_2", "count": 1}})

write_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", {
    "replace": False,
    "values": [
        "analog_alu:upward_hopper",
        "analog_alu:semiconductor_block",
        "analog_alu:waterproof_semiconductor_block",
        "analog_alu:chunk_loader",
        "analog_alu:chunk_loader_tier_2",
        *[f"analog_alu:{mode}_led_{color}" for mode in ("analog", "digital") for color in COLORS],
    ],
})

for name, text_en, text_ko in (
        ("chunk_loader", "Chunk Loader", "청크 로더 I"),
        ("chunk_loader_tier_2", "Chunk Loader II", "청크 로더 II")):
    en = json.load(open(f"{ROOT}/assets/{NAMESPACE}/lang/en_us.json", encoding="utf-8"))
    ko = json.load(open(f"{ROOT}/assets/{NAMESPACE}/lang/ko_kr.json", encoding="utf-8"))
    en[f"block.{NAMESPACE}.{name}"] = text_en
    ko[f"block.{NAMESPACE}.{name}"] = text_ko
    write_json(f"{ROOT}/assets/{NAMESPACE}/lang/en_us.json", en)
    write_json(f"{ROOT}/assets/{NAMESPACE}/lang/ko_kr.json", ko)

stages = [
    ("silicon_boule", "silicon_boule", "First Crystal", "첫 실리콘 원형", "Make a silicon boule.", "실리콘 원형을 만드세요."),
    ("purified_silicon", "purified_silicon", "Pure Enough", "정제 완료", "Purify silicon for wafer production.", "웨이퍼 생산을 위해 실리콘을 정제하세요."),
    ("monocrystalline_silicon", "monocrystalline_silicon", "One Perfect Crystal", "단결정 성장", "Grow monocrystalline silicon.", "단결정 실리콘을 성장시키세요."),
    ("silicon_wafer", "silicon_wafer", "A Thin Slice", "얇은 웨이퍼", "Cut a silicon wafer.", "실리콘 웨이퍼를 절단하세요."),
    ("ion_implanted_silicon", "ion_implanted_silicon", "Doped with Ions", "이온 주입", "Implant ions into the wafer.", "웨이퍼에 이온을 주입하세요."),
    ("mass_selected_silicon", "mass_selected_silicon", "Mass Selection", "질량 선별", "Select the desired silicon mass.", "원하는 질량의 실리콘을 선별하세요."),
    ("accelerated_focused_silicon", "accelerated_focused_silicon", "Focused Beam", "집속된 빔", "Accelerate and focus the silicon.", "실리콘을 가속하고 집속하세요."),
    ("inactive_doped_wafer", "inactive_doped_wafer", "Ready for Activation", "활성화 준비", "Prepare the doped wafer.", "도핑된 웨이퍼를 준비하세요."),
    ("semiconductor", "semiconductor", "석기에서 정보화로", "석기에서 정보화로", "Create your first semiconductor.", "첫 반도체를 제작하세요."),
]

previous = None
en = json.load(open(f"{ROOT}/assets/{NAMESPACE}/lang/en_us.json", encoding="utf-8"))
ko = json.load(open(f"{ROOT}/assets/{NAMESPACE}/lang/ko_kr.json", encoding="utf-8"))
for index, (advancement, item, title_en, title_ko, desc_en, desc_ko) in enumerate(stages):
    title_key = f"advancements.{NAMESPACE}.progression.{advancement}.title"
    description_key = f"advancements.{NAMESPACE}.progression.{advancement}.description"
    display_title = {"translate": title_key}
    display_desc = {"translate": description_key}
    frame = "challenge" if index == len(stages) - 1 else "task"
    if index == len(stages) - 1:
        display_title["color"] = "#AA00AA"
        title_en = "From Stone to Information Age"
    en[title_key] = title_en
    ko[title_key] = title_ko
    en[description_key] = desc_en
    ko[description_key] = desc_ko
    value = {
        "criteria": {"obtained": {"trigger": "minecraft:inventory_changed",
                                     "conditions": {"items": [{"items": f"{NAMESPACE}:{item}"}]}}},
        "requirements": [["obtained"]],
        "display": {
            "icon": {"id": f"{NAMESPACE}:{item}"},
            "title": display_title,
            "description": display_desc,
            **({"background": "minecraft:textures/gui/advancements/backgrounds/stone.png"}
               if previous is None else {}),
            "frame": frame,
            "show_toast": True,
            "announce_to_chat": True,
            "hidden": False,
        },
    }
    if previous is not None:
        value["parent"] = f"{NAMESPACE}:progression/{previous}"
    write_json(f"{ROOT}/data/{NAMESPACE}/advancement/progression/{advancement}.json", value)
    previous = advancement

write_json(f"{ROOT}/assets/{NAMESPACE}/lang/en_us.json", en)
write_json(f"{ROOT}/assets/{NAMESPACE}/lang/ko_kr.json", ko)
