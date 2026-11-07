"""Tempest Wyvern (flying boss) - BOSS2.obj. A rearing wyvern: wide membrane wings, two clawed forelimbs, head turned
slightly to its left (+X), a tail curling round behind and to its left.

OBJ: 2,276 vertices / 4,116 triangles / 60 fragments, 74.5 x 70 x 45.7 units, Y up, facing +Z, feet on Y = 0.
The body (neck, torso, legs, tail) is one connected 1,051-vertex fragment and is split per triangle; the wings are
separate fragments attached whole.
"""
from anim_common import track, z, one, SMOOTH

OBJ = "BOSS2.obj"
ENTITY = "tempest_wyvern"
SPLIT_THRESHOLD = 200

BONES = [
    ("root",        None,       (0, 0, 0),       None),
    ("body",        "root",     (0, 22, 0),      (2, 40, 8)),
    ("neck",        "body",     (3, 38, 7),      (8, 43, 12)),
    ("head",        "neck",     (8, 42, 11),     (12, 48, 18)),
    ("wing_left",   "body",     (6, 46, -3),     (30, 58, 0)),
    ("wing_right",  "body",     (-6, 46, -3),    (-30, 58, 0)),
    ("arm_left",    "body",     (7, 34, 8),      (13, 22, 16)),
    ("arm_right",   "body",     (-8, 33, 8),     (-15, 25, 14)),
    ("leg_left",    "root",     (8, 20, -4),     (13, 3, 0)),
    ("leg_right",   "root",     (-4, 20, -4),    (-5, 4, 2)),
    ("tail_1",      "body",     (4, 16, -12),    (18, 9, -17)),
    ("tail_2",      "tail_1",   (18, 9, -17),    (30, 3, 6)),
]

SEGMENTS = {
    "body":       ((0, 22, 2), (2, 38, 8)),
    "neck":       ((3, 38, 8), (8, 43, 12)),
    "head":       ((8, 43, 12), (12, 50, 18)),
    "wing_left":  ((6, 48, -3), (32, 60, 0)),
    "wing_right": ((-6, 48, -3), (-32, 60, 0)),
    "arm_left":   ((7, 33, 9), (13, 20, 16)),
    "arm_right":  ((-8, 32, 9), (-16, 23, 14)),
    "leg_left":   ((8, 19, -4), (13, 2, 0)),
    "leg_right":  ((-4, 19, -4), (-5, 2, 2)),
    "tail_1":     ((4, 15, -12), (18, 9, -17)),
    "tail_2":     ((18, 8, -17), (30, 2, 6)),
}

ANIMATIONS = {}

# conventions (anim_common): wing_left (+X) raises with -z, wing_right with +z; limbs/legs +x swing backward;
# neck/head +x pitch down; body +x leans forward; tail +x lifts the tip.

# ---------------------------------------------------------------- stances (looping)
# Perched on the ground: wings half folded, breathing, tail swaying.
ANIMATIONS["idle"] = dict(length=2.4, loop=True, bones={
    "body": {"position": track(z(0), (1.2, [0, -0.4, 0]), z(2.4)), "rotation": track(z(0), (1.2, [2, 0, 0]), z(2.4))},
    "neck": {"rotation": track(z(0), (1.2, [-4, 3, 0]), z(2.4))},
    "head": {"rotation": track(z(0), (0.6, [2, 8, 0]), (1.8, [2, -6, 0]), z(2.4))},
    "wing_left": {"rotation": track((0, [0, 10, 20]), (1.2, [0, 12, 15]), (2.4, [0, 10, 20]))},
    "wing_right": {"rotation": track((0, [0, -10, -20]), (1.2, [0, -12, -15]), (2.4, [0, -10, -20]))},
    "tail_1": {"rotation": track(z(0), (1.2, [0, 8, 0]), z(2.4))},
    "tail_2": {"rotation": track(z(0), (1.2, [0, -10, 0]), z(2.4))},
})
ANIMATIONS["walk"] = dict(length=1.0, loop=True, bones={
    "leg_left": {"rotation": track((0, [-20, 0, 0]), (0.5, [20, 0, 0]), (1.0, [-20, 0, 0]))},
    "leg_right": {"rotation": track((0, [20, 0, 0]), (0.5, [-20, 0, 0]), (1.0, [20, 0, 0]))},
    "arm_left": {"rotation": track((0, [10, 0, 0]), (0.5, [-10, 0, 0]), (1.0, [10, 0, 0]))},
    "arm_right": {"rotation": track((0, [-10, 0, 0]), (0.5, [10, 0, 0]), (1.0, [-10, 0, 0]))},
    "body": {"position": track(z(0), (0.25, [0, 1, 0]), z(0.5), (0.75, [0, 1, 0]), z(1.0))},
    "wing_left": {"rotation": track((0, [0, 10, 18]), (0.5, [0, 10, 12]), (1.0, [0, 10, 18]))},
    "wing_right": {"rotation": track((0, [0, -10, -18]), (0.5, [0, -10, -12]), (1.0, [0, -10, -18]))},
    "tail_1": {"rotation": track((0, [0, 10, 0]), (0.5, [0, -10, 0]), (1.0, [0, 10, 0]))},
})
# Airborne: one wing beat per 0.9 s, limbs tucked, body leaning into the flight.
ANIMATIONS["fly"] = dict(length=0.9, loop=True, bones={
    "wing_left": {"rotation": track((0, [0, 0, -45]), (0.45, [0, 8, 35]), (0.9, [0, 0, -45]))},
    "wing_right": {"rotation": track((0, [0, 0, 45]), (0.45, [0, -8, -35]), (0.9, [0, 0, 45]))},
    "body": {"position": track((0, [0, -1, 0]), (0.45, [0, 1.5, 0]), (0.9, [0, -1, 0])),
             "rotation": track((0, [22, 0, 0]), (0.45, [18, 0, 0]), (0.9, [22, 0, 0]))},
    "neck": {"rotation": track((0, [-18, 0, 0]), (0.45, [-14, 0, 0]), (0.9, [-18, 0, 0]))},
    "head": {"rotation": track((0, [-6, 0, 0]), (0.9, [-6, 0, 0]))},
    "arm_left": {"rotation": track((0, [40, 0, 0]), (0.9, [40, 0, 0]))},
    "arm_right": {"rotation": track((0, [40, 0, 0]), (0.9, [40, 0, 0]))},
    "leg_left": {"rotation": track((0, [55, 0, 0]), (0.9, [55, 0, 0]))},
    "leg_right": {"rotation": track((0, [55, 0, 0]), (0.9, [55, 0, 0]))},
    "tail_1": {"rotation": track((0, [15, 4, 0]), (0.45, [12, -4, 0]), (0.9, [15, 4, 0]))},
    "tail_2": {"rotation": track((0, [8, -6, 0]), (0.45, [10, 6, 0]), (0.9, [8, -6, 0]))},
})

# ---------------------------------------------------------------- lifecycle
# Spawn (SPAWN_INTRO_TICKS = 50): unfurls its wings out of the storm and screams at 1.6 s.
ANIMATIONS["spawn"] = dict(length=2.5, loop=False, bones={
    "wing_left": {"rotation": track((0, [0, 30, 60]), (1.2, [0, 0, -30]), (1.6, [0, 0, -60]), (2.2, [0, 0, -55]), z(2.5))},
    "wing_right": {"rotation": track((0, [0, -30, -60]), (1.2, [0, 0, 30]), (1.6, [0, 0, 60]), (2.2, [0, 0, 55]), z(2.5))},
    "neck": {"rotation": track((0, [30, 0, 0]), (1.2, [10, 0, 0]), (1.6, [-35, 0, 0]), (2.2, [-30, 0, 0]), z(2.5))},
    "head": {"rotation": track((0, [20, 0, 0]), (1.6, [-20, 0, 0]), (2.2, [-15, 0, 0]), z(2.5))},
    "body": {"position": track((0, [0, -6, 0]), (1.2, [0, 0, 0]), z(2.5))},
})
ANIMATIONS["death"] = dict(length=2.5, loop="hold_on_last_frame", bones={
    "body": {"rotation": track(z(0), (0.6, [-20, 0, 15]), (1.4, [40, 0, 60]), (2.5, [60, 0, 80])),
             "position": track(z(0), (1.4, [0, -10, 0]), (2.5, [0, -18, 0]))},
    "wing_left": {"rotation": track(z(0), (0.5, [0, 0, -60]), (1.4, [0, 20, 40]), (2.5, [0, 20, 50]))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, 0, 60]), (1.4, [0, -20, -40]), (2.5, [0, -20, -50]))},
    "neck": {"rotation": track(z(0), (0.6, [-30, 0, 0]), (1.4, [30, 0, 0]), (2.5, [40, 0, 0]))},
    "head": {"rotation": track(z(0), (1.4, [20, 0, 0]), (2.5, [25, 0, 0]))},
    "tail_1": {"rotation": track(z(0), (2.5, [-20, 0, 0]))},
})
ANIMATIONS["hurt"] = dict(length=0.3, loop=False, bones={
    "body": {"rotation": track(z(0), (0.08, [-6, 0, 0]), z(0.3))},
    "neck": {"rotation": track(z(0), (0.08, [-10, 0, 0]), z(0.3))},
})
ANIMATIONS["roar"] = dict(length=2.5, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.5, [0, 0, -65]), (2.0, [0, 0, -65]), z(2.5))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, 0, 65]), (2.0, [0, 0, 65]), z(2.5))},
    "neck": {"rotation": track(z(0), (0.5, [-35, 0, 0]), (2.0, [-35, 0, 0]), z(2.5))},
    "head": {"rotation": track(z(0), (0.5, [-20, 0, 0]), (1.0, [-18, 6, 0]), (1.5, [-18, -6, 0]), (2.0, [-20, 0, 0]), z(2.5))},
    "body": {"rotation": track(z(0), (0.5, [-12, 0, 0]), (2.0, [-12, 0, 0]), z(2.5))},
})

# ---------------------------------------------------------------- attacks (HIT times match TempestConfig *_TICK)
# Talon Strike (normal). HIT 0.45 s (tick 9).
ANIMATIONS["talon_strike"] = dict(length=0.9, loop=False, bones={
    "arm_left": {"rotation": track(z(0), (0.3, [-80, 0, -10]), (0.45, [35, 0, 0]), (0.6, [25, 0, 0]), z(0.9))},
    "arm_right": {"rotation": track(z(0), (0.3, [-80, 0, 10]), (0.45, [35, 0, 0]), (0.6, [25, 0, 0]), z(0.9))},
    "body": {"rotation": track(z(0), (0.3, [-12, 0, 0]), (0.45, [18, 0, 0]), z(0.9)), "position": track(z(0), (0.45, [0, 0, 3]), z(0.9))},
    "neck": {"rotation": track(z(0), (0.3, [-15, 0, 0]), (0.45, [12, 0, 0]), z(0.9))},
    "wing_left": {"rotation": track(z(0), (0.3, [0, 0, -30]), (0.45, [0, 0, 10]), z(0.9))},
    "wing_right": {"rotation": track(z(0), (0.3, [0, 0, 30]), (0.45, [0, 0, -10]), z(0.9))},
})
# Tail Whip (normal). HIT 0.55 s (tick 11).
ANIMATIONS["tail_whip"] = dict(length=1.1, loop=False, bones={
    "body": {"rotation": track(z(0), (0.35, [0, 25, 0]), (0.55, [0, -30, 0]), (0.8, [0, -15, 0]), z(1.1))},
    "tail_1": {"rotation": track(z(0), (0.35, [10, -45, 0]), (0.55, [5, 55, 0]), (0.8, [5, 25, 0]), z(1.1))},
    "tail_2": {"rotation": track(z(0), (0.4, [0, -35, 0]), (0.6, [0, 45, 0]), z(1.1))},
    "wing_left": {"rotation": track(z(0), (0.55, [0, 0, -20]), z(1.1))},
    "wing_right": {"rotation": track(z(0), (0.55, [0, 0, 20]), z(1.1))},
})
# Wind Gust: a huge downstroke. HIT 0.7 s (tick 14).
ANIMATIONS["wind_gust"] = dict(length=1.6, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.5, [0, -20, -75]), (0.7, [0, 25, 40]), (1.0, [0, 10, 20]), z(1.6))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, 20, 75]), (0.7, [0, -25, -40]), (1.0, [0, -10, -20]), z(1.6))},
    "body": {"rotation": track(z(0), (0.5, [-15, 0, 0]), (0.7, [10, 0, 0]), z(1.6)), "position": track(z(0), (0.5, [0, 2, 0]), (0.7, [0, -1, 0]), z(1.6))},
    "neck": {"rotation": track(z(0), (0.5, [-20, 0, 0]), (0.7, [5, 0, 0]), z(1.6))},
})
# Lightning Breath: head lowered, breathing from 0.9 s (tick 18) to 2.7 s (tick 54).
ANIMATIONS["lightning_breath"] = dict(length=3.2, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.6, [-25, 0, 0]), (0.9, [15, 0, 0]), (1.8, [12, -8, 0]), (2.7, [12, 8, 0]), z(3.2))},
    "head": {"rotation": track(z(0), (0.6, [-15, 0, 0]), (0.9, [10, 0, 0]), (2.7, [10, 0, 0]), z(3.2))},
    "body": {"rotation": track(z(0), (0.6, [-10, 0, 0]), (0.9, [8, 0, 0]), (2.7, [8, 0, 0]), z(3.2))},
    "wing_left": {"rotation": track(z(0), (0.9, [0, 0, -40]), (2.7, [0, 0, -40]), z(3.2))},
    "wing_right": {"rotation": track(z(0), (0.9, [0, 0, 40]), (2.7, [0, 0, 40]), z(3.2))},
})
# Storm Bolts: calls the lightning. HIT 0.9 s (tick 18).
ANIMATIONS["storm_bolts"] = dict(length=2.0, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.6, [-40, 0, 0]), (1.4, [-40, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.6, [-25, 0, 0]), (1.4, [-25, 0, 0]), z(2.0))},
    "wing_left": {"rotation": track(z(0), (0.6, [0, 0, -70]), (0.9, [0, 0, -60]), (1.4, [0, 0, -70]), z(2.0))},
    "wing_right": {"rotation": track(z(0), (0.6, [0, 0, 70]), (0.9, [0, 0, 60]), (1.4, [0, 0, 70]), z(2.0))},
    "body": {"rotation": track(z(0), (0.6, [-15, 0, 0]), (1.4, [-15, 0, 0]), z(2.0))},
})
# Dive Bomb: wings swept back, plunges; impact at 1.6 s (tick 32), lands.
ANIMATIONS["dive_bomb"] = dict(length=2.4, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.4, [0, 0, -60]), (0.6, [0, -50, 20]), (1.5, [0, -50, 20]), (1.6, [0, 10, -40]), (2.0, [0, 5, -20]), z(2.4))},
    "wing_right": {"rotation": track(z(0), (0.4, [0, 0, 60]), (0.6, [0, 50, -20]), (1.5, [0, 50, -20]), (1.6, [0, -10, 40]), (2.0, [0, -5, 20]), z(2.4))},
    "body": {"rotation": track(z(0), (0.6, [55, 0, 0]), (1.5, [55, 0, 0]), (1.6, [5, 0, 0]), z(2.4))},
    "arm_left": {"rotation": track(z(0), (0.6, [30, 0, 0]), (1.5, [-70, 0, 0]), (1.6, [20, 0, 0]), z(2.4))},
    "arm_right": {"rotation": track(z(0), (0.6, [30, 0, 0]), (1.5, [-70, 0, 0]), (1.6, [20, 0, 0]), z(2.4))},
    "neck": {"rotation": track(z(0), (0.6, [-20, 0, 0]), (1.6, [20, 0, 0]), z(2.4))},
    "tail_1": {"rotation": track(z(0), (0.6, [30, 0, 0]), (1.6, [-10, 0, 0]), z(2.4))},
})
# Cyclone: a spinning flap that spawns the whirlwind. HIT 1.0 s (tick 20).
ANIMATIONS["cyclone"] = dict(length=2.2, loop=False, bones={
    "root": {"rotation": track(z(0), (0.4, [0, 0, 0]), (1.0, [0, 360, 0]), (2.2, [0, 360, 0]), ease="linear")},
    "wing_left": {"rotation": track(z(0), (0.4, [0, 0, -50]), (1.0, [0, 0, -50]), (1.3, [0, 0, 20]), z(2.2))},
    "wing_right": {"rotation": track(z(0), (0.4, [0, 0, 50]), (1.0, [0, 0, 50]), (1.3, [0, 0, -20]), z(2.2))},
    "tail_1": {"rotation": track(z(0), (1.0, [20, 0, 0]), z(2.2))},
})
# Feather Barrage: wings snap forward, releasing the feathers. HIT 0.8 s (tick 16).
ANIMATIONS["feather_barrage"] = dict(length=1.6, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.5, [0, 40, -50]), (0.8, [0, -40, 10]), (1.1, [0, -20, 0]), z(1.6))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, -40, 50]), (0.8, [0, 40, -10]), (1.1, [0, 20, 0]), z(1.6))},
    "body": {"rotation": track(z(0), (0.5, [-12, 0, 0]), (0.8, [10, 0, 0]), z(1.6))},
    "neck": {"rotation": track(z(0), (0.5, [-15, 0, 0]), (0.8, [5, 0, 0]), z(1.6))},
})
# Thunder Roar. HIT 0.7 s (tick 14).
ANIMATIONS["thunder_roar"] = dict(length=1.8, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.5, [-40, 0, 0]), (0.7, [-10, 0, 0]), (1.3, [-12, 0, 0]), z(1.8))},
    "head": {"rotation": track(z(0), (0.5, [-20, 0, 0]), (0.7, [10, 0, 0]), (1.3, [8, 0, 0]), z(1.8))},
    "wing_left": {"rotation": track(z(0), (0.5, [0, 0, -60]), (0.7, [0, 0, -35]), z(1.8))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, 0, 60]), (0.7, [0, 0, 35]), z(1.8))},
    "body": {"rotation": track(z(0), (0.5, [-18, 0, 0]), (0.7, [8, 0, 0]), z(1.8))},
})
# Static Field: charges the air from 0.8 s (tick 16) to 3.0 s (tick 60).
ANIMATIONS["static_field"] = dict(length=3.5, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.8, [0, 0, -80]), (1.9, [0, 0, -72]), (3.0, [0, 0, -80]), z(3.5))},
    "wing_right": {"rotation": track(z(0), (0.8, [0, 0, 80]), (1.9, [0, 0, 72]), (3.0, [0, 0, 80]), z(3.5))},
    "neck": {"rotation": track(z(0), (0.8, [-30, 0, 0]), (3.0, [-30, 0, 0]), z(3.5))},
    "body": {"rotation": track(z(0), (0.8, [-10, 0, 0]), (3.0, [-10, 0, 0]), z(3.5)),
             "position": track(z(0), (0.8, [0, 2, 0]), (1.9, [0, 3, 0]), (3.0, [0, 2, 0]), z(3.5))},
})
# Summon: calls the breezes. HIT 1.0 s (tick 20).
ANIMATIONS["summon"] = dict(length=2.0, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.7, [-35, 0, 0]), (1.0, [5, 0, 0]), z(2.0))},
    "wing_left": {"rotation": track(z(0), (0.7, [0, 0, -65]), (1.0, [0, 0, -20]), z(2.0))},
    "wing_right": {"rotation": track(z(0), (0.7, [0, 0, 65]), (1.0, [0, 0, 20]), z(2.0))},
})
# Takeoff: launch at 0.35 s (tick 7).
ANIMATIONS["takeoff"] = dict(length=1.2, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.25, [0, -10, -70]), (0.45, [0, 5, 40]), (0.75, [0, 0, -40]), (1.2, [0, 0, -45]))},
    "wing_right": {"rotation": track(z(0), (0.25, [0, 10, 70]), (0.45, [0, -5, -40]), (0.75, [0, 0, 40]), (1.2, [0, 0, 45]))},
    "body": {"position": track(z(0), (0.25, [0, -2, 0]), (0.5, [0, 2, 0]), z(1.2)), "rotation": track(z(0), (0.5, [10, 0, 0]), (1.2, [22, 0, 0]))},
    "leg_left": {"rotation": track(z(0), (0.25, [-15, 0, 0]), (0.5, [55, 0, 0]), (1.2, [55, 0, 0]))},
    "leg_right": {"rotation": track(z(0), (0.25, [-15, 0, 0]), (0.5, [55, 0, 0]), (1.2, [55, 0, 0]))},
})
# Landing: touchdown at 0.9 s (tick 18).
ANIMATIONS["land"] = dict(length=1.4, loop=False, bones={
    "wing_left": {"rotation": track((0, [0, 0, -45]), (0.5, [0, -15, -60]), (0.9, [0, 0, -15]), (1.1, [0, 5, 15]), z(1.4))},
    "wing_right": {"rotation": track((0, [0, 0, 45]), (0.5, [0, 15, 60]), (0.9, [0, 0, 15]), (1.1, [0, -5, -15]), z(1.4))},
    "leg_left": {"rotation": track((0, [55, 0, 0]), (0.6, [-20, 0, 0]), (0.9, [-5, 0, 0]), z(1.4))},
    "leg_right": {"rotation": track((0, [55, 0, 0]), (0.6, [-20, 0, 0]), (0.9, [-5, 0, 0]), z(1.4))},
    "body": {"rotation": track((0, [22, 0, 0]), (0.6, [-10, 0, 0]), (0.9, [8, 0, 0]), z(1.4)),
             "position": track(z(0), (0.9, [0, -2, 0]), z(1.4))},
})
# Shot down: flails, crashes at 0.7 s (tick 14), lies stunned (EXPOSED) and rises at the end.
ANIMATIONS["crash"] = dict(length=3.5, loop=False, bones={
    "body": {"rotation": track(z(0), (0.3, [-20, 0, 30]), (0.7, [20, 0, 50]), (3.0, [20, 0, 55]), z(3.5)),
             "position": track(z(0), (0.7, [0, -4, 0]), (3.0, [0, -5, 0]), z(3.5))},
    "wing_left": {"rotation": track(z(0), (0.2, [0, 10, -70]), (0.45, [0, -10, 30]), (0.7, [0, 20, 40]), (3.0, [0, 20, 40]), z(3.5))},
    "wing_right": {"rotation": track(z(0), (0.2, [0, -10, 70]), (0.45, [0, 10, -30]), (0.7, [0, -20, -40]), (3.0, [0, -20, -40]), z(3.5))},
    "neck": {"rotation": track(z(0), (0.4, [-30, 0, 0]), (0.7, [30, 0, 0]), (3.0, [28, 8, 0]), z(3.5))},
    "head": {"rotation": track(z(0), (0.7, [15, 0, 10]), (3.0, [15, 0, 10]), z(3.5))},
    "tail_1": {"rotation": track(z(0), (0.7, [-15, 20, 0]), (3.0, [-15, 20, 0]), z(3.5))},
})

TEXTURES = {
    "tempest_wyvern": dict(parts={"wing_left": (0.050, 0.090, 0.130), "wing_right": (0.050, 0.090, 0.130)}, default=(0.040, 0.060, 0.090), crystal=(0.20, 0.75, 1.00), glow=(0.35, 0.90, 1.00),
                           crystal_chance=0.12, crystal_chance_high=0.20, high_y=50,
                           bone_crystal_chance={"wing_left": 0.0, "wing_right": 0.0, "head": 0.5, "arm_left": 0.35, "arm_right": 0.35,
                                                "tail_2": 0.3}, veins=0.42, seed=202),
}
