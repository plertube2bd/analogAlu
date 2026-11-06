# Tram model assets

## Entities

The tram uses the entity ID `redstone_transportation:redstone_tram`, the freight car `redstone_transportation:cargo_tram`. Both subclass Minecraft's minecart classes, so they use the rails, curves, slopes and passenger handling shared by every cart. Motors, stations, couplers and drive rails treat all minecarts alike; only the renderer and the top speed are tram-specific.

## Visual implementation

Since 1.6.0 the vehicles are drawn by `client/RedstoneTramRenderer.java` from dedicated entity model layers instead of a display block:

- `main`: the solid body, with wheels and a pantograph animated by `client/TramModel.java`
- `glass`: translucent window panes, drawn in a second pass (tram only)
- `lights`: full-bright lamps and side strip, tinted per frame by direction and speed

## Changing the look

The model geometry and its texture are generated together, so the UV layout can never drift from the cubes:

```text
tools/generate_assets.py        edit tram_body(), tram_glass(), cargo_body(), chassis(), wheels(), pantograph()
  -> client/TramGeometry.java               generated, do not edit
  -> textures/entity/redstone_tram.png      generated
  -> textures/entity/cargo_tram.png         generated
```

Run `python tools/generate_assets.py` from anywhere, then rebuild. Model space is 16 units per block with -Y up, +X the leading end, and y=0 at rail level. Colours and panel patterns are the `solid()`, `side_wall()`, `corrugated()` painters in the same script.

Generation stops if two unrotated cubes share a face in one plane (`check_no_shared_faces()`), because such faces flicker in game. Nudge one of them by 0.1 instead. Only `wheel_0` to `wheel_7` spin, so anything with corners that would sweep through the rail or the body belongs in a fixed part.

If the drawn body length changes, update `TramVehicle.BODY_HALF_LENGTH` so coupled cars keep from overlapping.

The old `blockbench/RedstoneTram.bbmodel` is the pre-1.6.0 tram and is no longer used by the game. `redstone_tram_body` stays registered only so older saves load cleanly.
