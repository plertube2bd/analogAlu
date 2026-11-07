"""Builds the Void Sanctum structure template (data/boss_mode/structure/void_sanctum.nbt) - the home of the Void Altar.

Layout (template space, +z = south, the altar faces south):
  * north: a deepslate-brick dais with the Void Altar, four obelisks capped with crying obsidian and end rods, soul
    campfires and a loot chest;
  * south: a round arena (radius 10) whose centre lies exactly ALTAR_SUMMON_DISTANCE (12) blocks in front of the altar -
    where the Void Sovereign rises - ringed by eight pillars (three of them broken).
Everything above the footprint is cleared with air. Weathering is added by boss_mode:void_sanctum_weathering.

Run:  python tools/altar/gen_void_sanctum.py
"""
import math
import os

import nbt

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "data", "boss_mode", "structure", "void_sanctum.nbt")
DATA_VERSION = 5023  # Minecraft 26.3

SIZE_X, SIZE_Y, SIZE_Z = 25, 12, 30
ALTAR = (12, 2, 5)
SUMMON_DISTANCE = 12
ARENA_CENTER = (ALTAR[0], ALTAR[2] + SUMMON_DISTANCE)  # (12, 17)
ARENA_RADIUS = 11.5
DAIS = (5, 19, 0, 9)  # x0, x1, z0, z1

palette, palette_index, blocks = [], {}, {}


def state(block_id, **props):
    key = (block_id, tuple(sorted(props.items())))
    if key not in palette_index:
        entry = {"id": block_id}
        if props:
            entry["properties"] = {k: str(v).lower() for k, v in props.items()}
        palette_index[key] = len(palette)
        palette.append(entry)
    return palette_index[key]


def put(x, y, z, block_id, nbt_data=None, **props):
    assert 0 <= x < SIZE_X and 0 <= y < SIZE_Y and 0 <= z < SIZE_Z, (x, y, z)
    blocks[(x, y, z)] = (state(block_id, **props), nbt_data)


def arena_distance(x, z):
    return math.hypot(x - ARENA_CENTER[0], z - ARENA_CENTER[1])


def in_dais(x, z):
    return DAIS[0] <= x <= DAIS[1] and DAIS[2] <= z <= DAIS[3]


def in_footprint(x, z):
    return in_dais(x, z) or arena_distance(x, z) <= ARENA_RADIUS


# clear the site
for x in range(SIZE_X):
    for z in range(SIZE_Z):
        if in_footprint(x, z):
            for y in range(1, SIZE_Y):
                put(x, y, z, "minecraft:air")

# arena floor (y = 0)
for x in range(SIZE_X):
    for z in range(SIZE_Z):
        d = arena_distance(x, z)
        if d > ARENA_RADIUS or in_dais(x, z):
            continue
        dx, dz = x - ARENA_CENTER[0], z - ARENA_CENTER[1]
        if d < 1.6:
            block = "minecraft:crying_obsidian"
        elif abs(d - 5.0) < 0.55 or abs(d - 9.0) < 0.55:
            block = "minecraft:obsidian"
        elif d < 9.0:
            spoke = abs(math.sin(math.atan2(dz, dx) * 3.0)) * d < 0.7
            block = "minecraft:polished_deepslate" if spoke else ("minecraft:deepslate_tiles" if d < 5 else "minecraft:sculk")
        else:
            block = "minecraft:deepslate_bricks"
        put(x, 0, z, block)

# dais (floor y = 0, top y = 1)
for x in range(DAIS[0], DAIS[1] + 1):
    for z in range(DAIS[2], DAIS[3] + 1):
        put(x, 0, z, "minecraft:polished_deepslate")
        edge = x in (DAIS[0], DAIS[1]) or z in (DAIS[2], DAIS[3])
        put(x, 1, z, "minecraft:chiseled_deepslate" if edge and (x + z) % 3 == 0 else "minecraft:deepslate_bricks")
for x in range(9, 16):
    put(x, 1, DAIS[3] + 1, "minecraft:deepslate_brick_stairs", facing="north", half="bottom", shape="straight", waterlogged=False)
for z in range(ALTAR[2] + 1, DAIS[3] + 1):
    put(ALTAR[0], 1, z, "minecraft:crying_obsidian")
put(ALTAR[0] - 1, 1, ALTAR[2], "minecraft:obsidian")
put(ALTAR[0] + 1, 1, ALTAR[2], "minecraft:obsidian")

# the altar, its chest and the fires
put(*ALTAR, "boss_mode:void_altar", facing="south", heart=False, eye=False, core=False, ritual=0)
put(ALTAR[0], 2, 1, "minecraft:chest", {"id": "minecraft:chest", "LootTable": "boss_mode:chests/void_sanctum"},
    facing="south", type="single", waterlogged=False)
for x, z in ((8, 7), (16, 7)):
    put(x, 2, z, "minecraft:soul_campfire", facing="south", lit=True, signal_fire=False, waterlogged=False)


def pillar(x, z, base_y, height, capped, lantern=False):
    put(x, base_y, z, "minecraft:chiseled_deepslate")
    for y in range(base_y + 1, base_y + height - 1):
        put(x, y, z, "minecraft:deepslate_bricks")
    top = base_y + height - 1
    if capped:
        put(x, top, z, "minecraft:crying_obsidian")
        put(x, top + 1, z, "minecraft:end_rod", facing="up")
        if lantern:
            put(x, top - 2, z + 1, "minecraft:soul_lantern", hanging=False, waterlogged=False)
    else:
        put(x, top, z, "minecraft:cobbled_deepslate_wall", east="none", north="none", south="none", west="none", up=True, waterlogged=False)


# four obelisks on the dais
for x, z in ((6, 1), (18, 1), (6, 8), (18, 8)):
    pillar(x, z, 2, 8 if z == 1 else 6, True)

# eight arena pillars (the southern half and the flanks)
for i, angle in enumerate((0, 30, 60, 90, 120, 150, 180, 210, 330)):
    if angle == 210:
        continue
    a = math.radians(angle)
    x = round(ARENA_CENTER[0] + math.cos(a) * 10.5)
    z = round(ARENA_CENTER[1] + math.sin(a) * 10.5)
    if in_dais(x, z) or not (0 <= x < SIZE_X and 0 <= z < SIZE_Z):
        continue
    broken = i in (1, 4, 6)
    pillar(x, z, 1, 3 if broken else 7, not broken)

# write
block_list = []
for (x, y, z), (idx, extra) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
    entry = {"pos": nbt.TypedList(nbt.INT, [x, y, z]), "state": idx}
    if extra:
        entry["nbt"] = extra
    block_list.append(entry)
template = {"size": nbt.TypedList(nbt.INT, [SIZE_X, SIZE_Y, SIZE_Z]), "entities": nbt.TypedList(nbt.COMPOUND, []),
            "blocks": nbt.TypedList(nbt.COMPOUND, block_list), "palette": nbt.TypedList(nbt.COMPOUND, palette), "DataVersion": DATA_VERSION}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
nbt.save(OUT, template)
back = nbt.load(OUT)
assert len(back["blocks"]) == len(block_list)

glyph = {"minecraft:crying_obsidian": "c", "minecraft:obsidian": "o", "minecraft:polished_deepslate": ".", "minecraft:deepslate_tiles": ",",
         "minecraft:sculk": "s", "minecraft:deepslate_bricks": "#", "boss_mode:void_altar": "@", "minecraft:chest": "C",
         "minecraft:soul_campfire": "f", "minecraft:chiseled_deepslate": "X", "minecraft:deepslate_brick_stairs": "^", "minecraft:air": " ",
         "minecraft:end_rod": "|", "minecraft:cobbled_deepslate_wall": "w", "minecraft:soul_lantern": "l"}
for layer in (0, 1, 2):
    print("y=%d" % layer)
    for z in range(SIZE_Z):
        print("  " + "".join(glyph.get(palette[blocks[(x, layer, z)][0]]["id"], "?") if (x, layer, z) in blocks else "~" for x in range(SIZE_X)))
print("%d blocks, %d palette entries -> %s" % (len(block_list), len(palette), os.path.normpath(OUT)))
