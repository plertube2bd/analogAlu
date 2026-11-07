"""Builds the Tempest Aerie and Bastion Den structure templates (data/boss_mode/structure/tempest_aerie.nbt and
bastion_den.nbt) - the homes of the Tempest Altar and the Bastion Altar.

Both follow the Shard Shrine's plan (template space, +z = south, the altar faces south):
  * north: a raised dais with the altar, two capped obelisks, lanterns and a loot chest;
  * south: a round summoning arena whose centre lies exactly the boss's ALTAR_SUMMON_DISTANCE in front of the altar
    (TempestConfig 10 / BastionConfig 12), ringed by pillars (two of them broken).
Everything above the footprint is filled with air so the site is cleared; the rest of the bounding box is left out, so
surrounding terrain stays. Weathering is added at generation time by the processor lists boss_mode:<name>_weathering.

Run:  python tools/altar/gen_themed_shrine.py
"""
import math
import os

import nbt

HERE = os.path.dirname(os.path.abspath(__file__))
STRUCTURES = os.path.join(HERE, "..", "..", "src", "main", "resources", "data", "boss_mode", "structure")
DATA_VERSION = 5023  # Minecraft 26.3

ROD = dict(facing="up", powered=False, waterlogged=False)
THEMES = {
    # storm-swept stone and weathered copper on a mountain top
    "tempest_aerie": dict(
        altar="boss_mode:tempest_altar", chest_loot="boss_mode:chests/tempest_aerie", summon=10, radius=8.5,
        centre=("minecraft:waxed_oxidized_cut_copper", {}), inner=("minecraft:calcite", {}),
        spoke=("minecraft:polished_tuff", {}), field=("minecraft:smooth_stone", {}), ring=("minecraft:waxed_oxidized_cut_copper", {}),
        rim=("minecraft:stone_bricks", {}),
        dais_floor=("minecraft:polished_andesite", {}), dais_top=("minecraft:stone_bricks", {}), dais_accent=("minecraft:chiseled_tuff_bricks", {}),
        inlay=("minecraft:calcite", {}),
        stairs=("minecraft:stone_brick_stairs", dict(facing="north", half="bottom", shape="straight", waterlogged=False)),
        lantern=("minecraft:waxed_oxidized_copper_lantern", dict(hanging=False, waterlogged=False)),
        pillar_base=("minecraft:chiseled_tuff_bricks", {}), pillar=("minecraft:tuff_bricks", {}),
        cap=("minecraft:waxed_oxidized_copper", {}), crown=("minecraft:waxed_oxidized_lightning_rod", ROD),
        broken=("minecraft:tuff_brick_wall", dict(east="none", north="none", south="none", west="none", up=True, waterlogged=False)),
    ),
    # a scorched basalt den in the badlands
    "bastion_den": dict(
        altar="boss_mode:bastion_altar", chest_loot="boss_mode:chests/bastion_den", summon=12, radius=9.5,
        centre=("minecraft:shroomlight", {}), inner=("minecraft:magma_block", {}),
        spoke=("minecraft:polished_blackstone_bricks", {}), field=("minecraft:polished_basalt", dict(axis="y")),
        ring=("minecraft:shroomlight", {}), rim=("minecraft:polished_blackstone_bricks", {}),
        dais_floor=("minecraft:blackstone", {}), dais_top=("minecraft:polished_blackstone_bricks", {}),
        dais_accent=("minecraft:gilded_blackstone", {}), inlay=("minecraft:magma_block", {}),
        stairs=("minecraft:polished_blackstone_brick_stairs", dict(facing="north", half="bottom", shape="straight", waterlogged=False)),
        lantern=("minecraft:lantern", dict(hanging=False, waterlogged=False)),
        pillar_base=("minecraft:chiseled_polished_blackstone", {}), pillar=("minecraft:basalt", dict(axis="y")),
        cap=("minecraft:shroomlight", {}), crown=("minecraft:campfire", dict(facing="south", lit=True, signal_fire=False, waterlogged=False)),
        broken=("minecraft:polished_blackstone_brick_wall", dict(east="none", north="none", south="none", west="none", up=True, waterlogged=False)),
    ),
}


def build(name, t):
    radius = t["radius"]
    altar = (int(math.ceil(radius)), 2, 3)
    centre = (altar[0], altar[2] + t["summon"])
    size_x = 2 * altar[0] + 1
    size_y = 10
    size_z = int(math.ceil(centre[1] + radius)) + 1
    dais = (altar[0] - 5, altar[0] + 5, 0, 5)
    palette, palette_index, blocks = [], {}, {}

    def state(block_id, props):
        key = (block_id, tuple(sorted(props.items())))
        if key not in palette_index:
            entry = {"id": block_id}
            if props:
                entry["properties"] = {k: str(v).lower() for k, v in props.items()}
            palette_index[key] = len(palette)
            palette.append(entry)
        return palette_index[key]

    def put(x, y, z, block, nbt_data=None):
        assert 0 <= x < size_x and 0 <= y < size_y and 0 <= z < size_z, (name, x, y, z)
        blocks[(x, y, z)] = (state(block[0], block[1]), nbt_data)

    def arena_distance(x, z):
        return math.hypot(x - centre[0], z - centre[1])

    def in_dais(x, z):
        return dais[0] <= x <= dais[1] and dais[2] <= z <= dais[3]

    air = ("minecraft:air", {})
    for x in range(size_x):
        for z in range(size_z):
            if in_dais(x, z) or arena_distance(x, z) <= radius:
                for y in range(1, size_y):
                    put(x, y, z, air)
    # arena floor (y = 0)
    for x in range(size_x):
        for z in range(size_z):
            d = arena_distance(x, z)
            if d > radius or in_dais(x, z):
                continue
            dx, dz = x - centre[0], z - centre[1]
            if d < 0.5:
                block = t["centre"]
            elif d <= 1.5:
                block = t["inner"]
            elif d <= radius - 2.5:
                spoke = dx == 0 or dz == 0 or abs(dx) == abs(dz)
                block = t["spoke"] if spoke else t["field"]
            elif d <= radius - 1.5:
                block = t["ring"]
            else:
                block = t["rim"]
            put(x, 0, z, block)
    # dais (floor y = 0, top y = 1) and the steps down to the arena
    for x in range(dais[0], dais[1] + 1):
        for z in range(dais[2], dais[3] + 1):
            put(x, 0, z, t["dais_floor"])
            edge = x in (dais[0], dais[1]) or z in (dais[2], dais[3])
            put(x, 1, z, t["dais_accent"] if edge and (x + z) % 4 == 0 else t["dais_top"])
    for x in range(altar[0] - 2, altar[0] + 3):
        put(x, 1, dais[3] + 1, t["stairs"])
    for z in range(altar[2] + 1, dais[3] + 1):
        put(altar[0], 1, z, t["inlay"])
    put(*altar, (t["altar"], dict(facing="south", charge=0, ritual=0)))
    put(altar[0], 2, 1, ("minecraft:chest", dict(facing="south", type="single", waterlogged=False)),
        {"id": "minecraft:chest", "LootTable": t["chest_loot"]})
    for x, z in ((dais[0] + 1, 5), (dais[1] - 1, 5), (dais[0] + 1, 1), (dais[1] - 1, 1)):
        put(x, 2, z, t["lantern"])

    def obelisk(x, z, base_y, height, capped=True):
        put(x, base_y, z, t["pillar_base"])
        for y in range(base_y + 1, base_y + height - 1):
            put(x, y, z, t["pillar"])
        top = base_y + height - 1
        if capped:
            put(x, top, z, t["cap"])
            put(x, top + 1, z, t["crown"])
        else:
            put(x, top, z, t["broken"])

    obelisk(altar[0] - 3, 2, 2, 6)
    obelisk(altar[0] + 3, 2, 2, 6)
    for angle, height, capped in ((0, 5, True), (45, 3, False), (90, 6, True), (135, 3, False), (180, 5, True)):
        a = math.radians(angle)
        x = round(centre[0] + math.cos(a) * (radius - 0.5))
        z = round(centre[1] + math.sin(a) * (radius - 0.5))
        obelisk(x, z, 1, height, capped)

    block_list = []
    for (x, y, z), (idx, extra) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        entry = {"pos": nbt.TypedList(nbt.INT, [x, y, z]), "state": idx}
        if extra:
            entry["nbt"] = extra
        block_list.append(entry)
    template = {"size": nbt.TypedList(nbt.INT, [size_x, size_y, size_z]), "entities": nbt.TypedList(nbt.COMPOUND, []),
                "blocks": nbt.TypedList(nbt.COMPOUND, block_list), "palette": nbt.TypedList(nbt.COMPOUND, palette),
                "DataVersion": DATA_VERSION}
    out = os.path.join(STRUCTURES, name + ".nbt")
    nbt.save(out, template)
    back = nbt.load(out)
    assert len(back["blocks"]) == len(block_list) and back["palette"] == palette
    print(f"{name}: {size_x}x{size_y}x{size_z}, altar {altar}, arena centre {centre}, {len(block_list)} blocks, "
          f"{len(palette)} palette entries -> {os.path.normpath(out)}")
    glyph = {}
    for layer in (0, 2):
        print(f"  y={layer}")
        for z in range(size_z):
            row = ""
            for x in range(size_x):
                b = blocks.get((x, layer, z))
                if not b:
                    row += "~"
                    continue
                block_id = palette[b[0]]["id"]
                row += " " if block_id == "minecraft:air" else glyph.setdefault(block_id, "@" if block_id.startswith("boss_mode") else
                                                                               "abcdefghijklmnopqrstuvwxyz"[len(glyph) % 26])
            print("    " + row)


if __name__ == "__main__":
    os.makedirs(STRUCTURES, exist_ok=True)
    for theme_name, theme in THEMES.items():
        build(theme_name, theme)
