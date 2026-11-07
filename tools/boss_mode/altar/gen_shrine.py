"""Builds the Shard Shrine structure template (data/boss_mode/structure/shard_shrine.nbt).

Layout (template space, +z = south, the altar faces south):
  * north: a raised dais with the Shard Altar, two crystal-capped obelisks, soul lanterns and a loot chest;
  * south: a round summoning arena (radius 7.5) whose centre lies exactly ALTAR_SUMMON_DISTANCE (9) blocks in front
    of the altar - that is where the Shard Dragon rises - ringed by crystal pillars (two of them broken).
Everything above the footprint is filled with air so the site is cleared; the rest of the bounding box is left out,
so surrounding terrain stays. Weathering (cracked bricks, crying obsidian, gilded blackstone) is added at generation
time by the processor list boss_mode:shard_shrine_weathering.

Run:  python tools/altar/gen_shrine.py
"""
import math
import os

import nbt

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "data", "boss_mode", "structure", "shard_shrine.nbt")
DATA_VERSION = 5023  # Minecraft 26.3 (read from the vanilla templates)

SIZE_X, SIZE_Y, SIZE_Z = 17, 10, 20
ALTAR = (8, 2, 3)
SUMMON_DISTANCE = 9
ARENA_CENTER = (ALTAR[0], ALTAR[2] + SUMMON_DISTANCE)  # (8, 12)
ARENA_RADIUS = 7.5
DAIS = (3, 13, 0, 5)  # x0, x1, z0, z1 (inclusive)

palette = []
palette_index = {}
blocks = {}


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


# ------------------------------------------------------------------ clear the site
for x in range(SIZE_X):
    for z in range(SIZE_Z):
        if in_footprint(x, z):
            for y in range(1, SIZE_Y):
                put(x, y, z, "minecraft:air")

# ------------------------------------------------------------------ arena floor (y = 0)
for x in range(SIZE_X):
    for z in range(SIZE_Z):
        d = arena_distance(x, z)
        if d > ARENA_RADIUS or in_dais(x, z):
            continue
        dx, dz = x - ARENA_CENTER[0], z - ARENA_CENTER[1]
        if d < 0.5:
            block = "minecraft:amethyst_block"
        elif d <= 1.5:
            block = "minecraft:crying_obsidian"
        elif d <= 5.0:
            spoke = dx == 0 or dz == 0 or abs(dx) == abs(dz)
            block = "minecraft:polished_blackstone_bricks" if spoke else "minecraft:polished_blackstone"
        elif d <= 6.0:
            block = "minecraft:obsidian"
        else:
            block = "minecraft:polished_blackstone_bricks"
        put(x, 0, z, block)

# ------------------------------------------------------------------ dais (floor y = 0, top y = 1)
for x in range(DAIS[0], DAIS[1] + 1):
    for z in range(DAIS[2], DAIS[3] + 1):
        put(x, 0, z, "minecraft:polished_blackstone")
        edge = x in (DAIS[0], DAIS[1]) or z in (DAIS[2], DAIS[3])
        put(x, 1, z, "minecraft:chiseled_polished_blackstone" if edge and (x + z) % 4 == 0 else "minecraft:polished_blackstone_bricks")
# steps down to the arena
for x in range(6, 11):
    put(x, 1, DAIS[3] + 1, "minecraft:polished_blackstone_brick_stairs", facing="north", half="bottom", shape="straight", waterlogged=False)
# obsidian inlay leading from the altar to the steps
for z in range(ALTAR[2] + 1, DAIS[3] + 1):
    put(ALTAR[0], 1, z, "minecraft:obsidian")

# the altar itself, facing the arena
put(*ALTAR, "boss_mode:shard_altar", facing="south", charge=0, ritual=0)
# offering chest behind it
put(ALTAR[0], 2, 1, "minecraft:chest", {"id": "minecraft:chest", "LootTable": "boss_mode:chests/shard_shrine"},
    facing="south", type="single", waterlogged=False)
# soul lanterns on the dais corners
for x, z in ((4, 5), (12, 5), (4, 1), (12, 1)):
    put(x, 2, z, "minecraft:soul_lantern", hanging=False, waterlogged=False)


def obelisk(x, z, base_y, height, capped=True):
    put(x, base_y, z, "minecraft:chiseled_polished_blackstone")
    for y in range(base_y + 1, base_y + height - 1):
        put(x, y, z, "minecraft:polished_blackstone_bricks")
    top = base_y + height - 1
    if capped:
        put(x, top, z, "minecraft:crying_obsidian")
        put(x, top + 1, z, "minecraft:amethyst_cluster", facing="up", waterlogged=False)
    else:
        put(x, top, z, "minecraft:polished_blackstone_brick_wall", east="none", north="none", south="none", west="none", up=True,
            waterlogged=False)


# two tall obelisks flanking the altar
obelisk(5, 2, 2, 6)
obelisk(11, 2, 2, 6)

# ------------------------------------------------------------------ crystal pillars around the arena (south half)
for angle, height, capped in ((0, 5, True), (45, 3, False), (90, 6, True), (135, 3, False), (180, 5, True)):
    a = math.radians(angle)
    x = round(ARENA_CENTER[0] + math.cos(a) * 7)
    z = round(ARENA_CENTER[1] + math.sin(a) * 7)
    obelisk(x, z, 1, height, capped)
    if capped:
        # amethyst crown under the cluster
        put(x, height - 1, z, "minecraft:amethyst_block")

# ------------------------------------------------------------------ write
block_list = []
for (x, y, z), (idx, extra) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
    entry = {"pos": nbt.TypedList(nbt.INT, [x, y, z]), "state": idx}
    if extra:
        entry["nbt"] = extra
    block_list.append(entry)

template = {
    "size": nbt.TypedList(nbt.INT, [SIZE_X, SIZE_Y, SIZE_Z]),
    "entities": nbt.TypedList(nbt.COMPOUND, []),
    "blocks": nbt.TypedList(nbt.COMPOUND, block_list),
    "palette": nbt.TypedList(nbt.COMPOUND, palette),
    "DataVersion": DATA_VERSION,
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
nbt.save(OUT, template)

# round-trip check + ASCII preview of the floor and the y=2 layer
back = nbt.load(OUT)
assert len(back["blocks"]) == len(block_list) and back["palette"] == palette
glyph = {"minecraft:amethyst_block": "A", "minecraft:crying_obsidian": "c", "minecraft:obsidian": "o",
         "minecraft:polished_blackstone": ".", "minecraft:polished_blackstone_bricks": "#", "boss_mode:shard_altar": "@",
         "minecraft:chest": "C", "minecraft:soul_lantern": "l", "minecraft:chiseled_polished_blackstone": "X",
         "minecraft:polished_blackstone_brick_stairs": "^", "minecraft:air": " ", "minecraft:amethyst_cluster": "*",
         "minecraft:polished_blackstone_brick_wall": "|"}
for layer in (0, 1, 2):
    print(f"y={layer}")
    for z in range(SIZE_Z):
        row = ""
        for x in range(SIZE_X):
            b = blocks.get((x, layer, z))
            row += glyph.get(palette[b[0]]["id"], "?") if b else "~"
        print("  " + row)
print(f"{len(block_list)} blocks, {len(palette)} palette entries -> {os.path.normpath(OUT)}")
