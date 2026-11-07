# Blender → Minecraft pipeline

The source of truth for every model is the Blender export in `Desktop/BOSS`. `convert.py` turns each OBJ into
GeckoLib resources the mod loads at runtime.

| OBJ | Entity | Role | Model file |
|---|---|---|---|
| `BOSS.obj` | `shard_dragon` | boss (10 phases) | `model_shard_dragon.py` |
| `Mob.obj` | `shard_hound` (+ `shard_spitter`, `volatile_hound` textures) | minions | `model_shard_hound.py` |
| `Mob2.obj` | `shard_warlord` | mid-boss | `model_shard_warlord.py` |
| `Final BOSS.obj` | `void_sovereign` | final boss | `model_void_sovereign.py` |
| `BOSS2.obj` | `tempest_wyvern` | flying raid boss | `model_tempest_wyvern.py` |
| `BOSS3.obj` | `bastion_behemoth` | armoured raid boss | `model_bastion_behemoth.py` |

```
Blender ──export──▶ *.obj + *.mtl
                        │  convert.py (Blender 5.1, headless)
                        ├─ rig (model_*.py + rig_common.py) ─▶ output/<entity>_rigged.blend (armature + vertex groups)
                        ├─ texture bake per variant (Cycles) ─▶ textures/entity/<texture>.png + _glowmask.png
                        ├─ Bedrock geometry 1.21.0 ─────────▶ geckolib/models/entity/<entity>.geo.json
                        ├─ keyframes (model_*.py) ───────────▶ geckolib/animations/entity/<entity>.animation.json
                        └─ 64x64 HUD portraits ─────────────▶ textures/gui/portrait_<entity>.png
                                            │
                  PolyMeshGeoLoader (client) ─▶ GeckoLib BakedGeoModel ─▶ GeoEntityRenderer
```

Run it again whenever a Blender model changes:

```bash
"C:/Program Files/Blender Foundation/Blender 5.1/blender.exe" -b --factory-startup --python-exit-code 1 -P tools/blender/convert.py -- --model all
```

Options (after `--`): `--model all|shard_dragon|shard_hound|shard_warlord|void_sovereign|tempest_wyvern|bastion_behemoth`, `--source <folder>` (default
`~/Desktop/BOSS`), `--project <mod root>`, `--texture-size 1024`, `--skip-bake` (keep the current texture PNGs).

## What the source files contain

| | BOSS.obj | Mob.obj | Mob2.obj |
|---|---|---|---|
| Mesh | 3,345 v / 5,202 tris / 367 fragments | 2,219 v / 3,386 tris / 263 fragments | 2,307 v / 4,005 tris / 92 fragments (torso connected) |
| Size (units = model pixels) | 76 × 50 × 75 | 19 × 20 × 24 | 26 × 35 × 18 |
| Facing | head +Z, Y up | head +Z, Y up | faces +Z, Y up, staff in the right hand (-X) |
| Material | `Material_0`, no `map_Kd` | `Material_0.002`, no `map_Kd` | `Material_0`, no `map_Kd` |
| Armature / animation | none | none | none |

None of the MTLs reference a texture, so the converter bakes textures onto each OBJ's **own UV layout**. If an original
texture turns up, add `map_Kd <file>.png` to the MTL: the converter then uses it as-is (UVs unchanged).

## Coordinates and scale

* 1 OBJ unit = 1 Minecraft model pixel (1/16 block) at entity scale 1.
* OBJ `(x, y, z)` → Bedrock geometry `(x, y, -z)`; GeckoLib negates X when baking, so the net transform is a 180°
  rotation about Y - no mirroring, heads face the look direction, feet stay on the ground.
* Entity size comes from the vanilla `scale` attribute: dragon ×2.0 (phase 1) growing to ×4.0 (phase 10),
  warlord ×1.5, hounds ×1.

## Rigs (none exist in the OBJs - fitted by hand, verified with colour-coded renders)

* Dragon (14 bones): `root → body → neck → head → jaw`, `body → wing_left / wing_right / 4 legs / tail_1 → tail_2 → tail_3`.
* Hound (7 bones): `root → body → head / 4 legs`.
* Warlord (9 bones): `root → body → head / arm_right → staff / arm_left / cape`, `root → leg_left / leg_right`.
* Void Sovereign (15 bones): `root → body → torso → head / arm_* → hand_* / tentacles_left / tentacles_right / mane`,
  `body → tail_1 → tail_2`, `root → leg_left / leg_right` (2,266 v / 3,914 tris / 96 fragments; > 50-vertex fragments split per triangle).

Small fragments are attached whole to the nearest bone axis (rigid, nothing tears); fragments above the model's
`SPLIT_THRESHOLD` (the warlord's connected torso) are split per triangle.

## Animations (authored against the rigs, Bedrock/GeckoLib keyframe format, openable in Blockbench)

* Dragon (20): idle, walk, run, spawn, death, hurt, roar, attack, tail_sweep, special_slam, special_volley,
  special_tempest, charge, crystal_rain, spike_eruption, prism_beam, void_roar, meteor_dive, cataclysm, summon.
* Hound (9): idle, walk, run, attack, spit, fuse, hurt, death, spawn.
* Warlord (10): idle, walk, attack, cast, slam, warcry, blink, hurt, death, spawn.
* Void Sovereign (18): idle, walk, spawn, death, hurt, roar, claw_swipe, tentacle_lash, ground_pound, void_orbs, rift_eruption,
  void_grasp, black_hole, void_step, eclipse, soul_drain, summon, shield.

Server hit timings in `BossConfig` (`*_HIT_TICK`) are matched to the keyframes commented `HIT` in the model files.

## Why a custom GeckoLib loader

GeckoLib 5.5.7 (the 26.3 build) parses Bedrock `poly_mesh` bones but does not render them (cubes only). Converting
the meshes to cubes would destroy their shapes, so the mod registers `PolyMeshGeoLoader` through GeckoLib's public
`GeckoLibUtil.addResourceLoader` hook: it bakes the triangles into `PolyMeshGeoBone`s inside a normal GeckoLib
`BakedGeoModel`, so GeckoLib's controllers, keyframes and server-triggered animations drive the real meshes.

## Animations only

`model_*.py` hold the keyframes (`ANIMATIONS`). After changing only those, re-export the GeckoLib animation files
without Blender:

```bash
python tools/blender/export_anims.py            # all models
python tools/blender/export_anims.py shard_dragon
```

The Shard Dragon's flight clips (`fly`, `takeoff`, `land`, `crash`) were added this way; their hit ticks match
`BossConfig.TAKEOFF_LAUNCH_TICK`, `LAND_IMPACT_TICK` and `CRASH_IMPACT_TICK`.
