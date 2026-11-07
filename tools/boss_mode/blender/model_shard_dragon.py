"""Shard Dragon (boss) - BOSS.obj. Rig, texture palette and animations.

Coordinates are raw OBJ coordinates exported by Blender: Y up, head toward +Z, the dragon's left side is +X,
1 OBJ unit = 1 model pixel. Rotation / position conventions of the animation values: see anim_common.py.
"""
from anim_common import track, z, legs_swing, SMOOTH

OBJ = "BOSS.obj"
ENTITY = "shard_dragon"
SPLIT_THRESHOLD = 10 ** 9  # every fragment is small: attach whole fragments

BONES = [
    ("root",            None,     (0, 0, 0),     None),
    ("body",            "root",   (2, 16, 2),    (2, 20, 12)),
    ("neck",            "body",   (2, 22, 10),   (3, 28, 20)),
    ("head",            "neck",   (3, 30, 20),   (5, 34, 32)),
    ("jaw",             "head",   (4, 27, 23),   (5, 22, 34)),
    ("wing_left",       "body",   (11, 29, 4),   (36, 36, -4)),
    ("wing_right",      "body",   (-9, 28, 8),   (-36, 26, 12)),
    ("leg_front_left",  "body",   (12, 17, 14),  (18, 3, 26)),
    ("leg_front_right", "body",   (-8, 17, 14),  (-10, 3, 24)),
    ("leg_back_left",   "body",   (12, 14, -3),  (18, 2, -8)),
    ("leg_back_right",  "body",   (-6, 14, -5),  (-6, 2, -14)),
    ("tail_1",          "body",   (3, 15, -6),   (6, 12, -20)),
    ("tail_2",          "tail_1", (6, 12, -19),  (14, 10, -32)),
    ("tail_3",          "tail_2", (14, 9, -31),  (36, 6, -32)),
]

# Axis segments used to attach fragments to bones (pivots sit at the joints).
SEGMENTS = {
    "body":            ((2, 16, 2), (2, 20, 12)),
    "neck":            ((2, 22, 12), (3, 28, 20)),
    "head":            ((3, 28, 20), (5, 34, 32)),
    "jaw":             ((4, 26, 24), (5, 22, 34)),
    "wing_left":       ((12, 30, 5), (36, 36, -4)),
    "wing_right":      ((-10, 28, 8), (-36, 26, 12)),
    "leg_front_left":  ((12, 16, 14), (18, 3, 26)),
    "leg_front_right": ((-8, 16, 14), (-10, 3, 24)),
    "leg_back_left":   ((12, 12, -2), (18, 2, -8)),
    "leg_back_right":  ((-6, 12, -4), (-6, 2, -14)),
    "tail_1":          ((3, 14, -8), (6, 12, -20)),
    "tail_2":          ((6, 12, -20), (14, 10, -32)),
    "tail_3":          ((14, 10, -32), (36, 6, -32)),
}

ANIMATIONS = {}

# ---------------------------------------------------------------- locomotion (looping)
ANIMATIONS["idle"] = dict(length=3.0, loop=True, bones={
    "body": {"position": track(z(0), (1.5, [0, 0.6, 0]), z(3.0)),
             "rotation": track(z(0), (1.5, [-1, 0, 0]), z(3.0))},
    "neck": {"rotation": track(z(0), (1.5, [-3, 0, 0]), z(3.0))},
    "head": {"rotation": track(z(0), (0.8, [2, 5, 0]), (1.5, [3, 0, 0]), (2.3, [2, -5, 0]), z(3.0))},
    "jaw": {"rotation": track(z(0), (1.5, [5, 0, 0]), z(3.0))},
    "wing_left": {"rotation": track(z(0), (1.5, [0, 0, -5]), z(3.0))},
    "wing_right": {"rotation": track(z(0), (1.5, [0, 0, 5]), z(3.0))},
    "tail_1": {"rotation": track(z(0), (0.75, [0, 6, 0]), (2.25, [0, -6, 0]), z(3.0))},
    "tail_2": {"rotation": track((0, [0, -3, 0]), (1.0, [0, 8, 0]), (2.5, [0, -8, 0]), (3.0, [0, -3, 0]))},
    "tail_3": {"rotation": track((0, [0, -8, 0]), (1.25, [0, 10, 0]), (2.75, [0, -10, 0]), (3.0, [0, -8, 0]))},
})

WALK_T = [0, 0.6, 1.2]
ANIMATIONS["walk"] = dict(length=1.2, loop=True, bones={
    "leg_front_left": {"rotation": track(*legs_swing(WALK_T, -20, 20))},
    "leg_back_right": {"rotation": track(*legs_swing(WALK_T, -20, 20))},
    "leg_front_right": {"rotation": track(*legs_swing(WALK_T, 20, -20))},
    "leg_back_left": {"rotation": track(*legs_swing(WALK_T, 20, -20))},
    "body": {"position": track(z(0), (0.3, [0, 0.8, 0]), z(0.6), (0.9, [0, 0.8, 0]), z(1.2)),
             "rotation": track((0, [0, 0, -2]), (0.6, [0, 0, 2]), (1.2, [0, 0, -2]))},
    "neck": {"rotation": track((0, [3, 0, 0]), z(0.3), (0.6, [3, 0, 0]), z(0.9), (1.2, [3, 0, 0]))},
    "head": {"rotation": track((0, [0, -3, 0]), (0.6, [0, 3, 0]), (1.2, [0, -3, 0]))},
    "wing_left": {"rotation": track((0, [0, 0, 3]), (0.6, [0, 0, -3]), (1.2, [0, 0, 3]))},
    "wing_right": {"rotation": track((0, [0, 0, -3]), (0.6, [0, 0, 3]), (1.2, [0, 0, -3]))},
    "tail_1": {"rotation": track((0, [0, 8, 0]), (0.6, [0, -8, 0]), (1.2, [0, 8, 0]))},
    "tail_2": {"rotation": track(z(0), (0.3, [0, 10, 0]), (0.9, [0, -10, 0]), z(1.2))},
    "tail_3": {"rotation": track((0, [0, -10, 0]), (0.6, [0, 12, 0]), (1.2, [0, -10, 0]))},
})

RUN_T = [0, 0.35, 0.7]
ANIMATIONS["run"] = dict(length=0.7, loop=True, bones={
    "leg_front_left": {"rotation": track(*legs_swing(RUN_T, -32, 32))},
    "leg_back_right": {"rotation": track(*legs_swing(RUN_T, -32, 32))},
    "leg_front_right": {"rotation": track(*legs_swing(RUN_T, 32, -32))},
    "leg_back_left": {"rotation": track(*legs_swing(RUN_T, 32, -32))},
    "body": {"position": track(z(0), (0.175, [0, 1.2, 0]), z(0.35), (0.525, [0, 1.2, 0]), z(0.7)),
             "rotation": track((0, [4, 0, -2]), (0.35, [4, 0, 2]), (0.7, [4, 0, -2]))},
    "neck": {"rotation": track((0, [10, 0, 0]), (0.35, [7, 0, 0]), (0.7, [10, 0, 0]))},
    "head": {"rotation": track((0, [-4, 0, 0]), (0.35, [-2, 0, 0]), (0.7, [-4, 0, 0]))},
    "wing_left": {"rotation": track((0, [0, -8, -12]), (0.35, [0, -8, -16]), (0.7, [0, -8, -12]))},
    "wing_right": {"rotation": track((0, [0, 8, 12]), (0.35, [0, 8, 16]), (0.7, [0, 8, 12]))},
    "tail_1": {"rotation": track((0, [5, 5, 0]), (0.35, [5, -5, 0]), (0.7, [5, 5, 0]))},
    "tail_2": {"rotation": track(z(0), (0.175, [0, 6, 0]), (0.525, [0, -6, 0]), z(0.7))},
    "tail_3": {"rotation": track((0, [0, -6, 0]), (0.35, [0, 8, 0]), (0.7, [0, -6, 0]))},
})

# ---------------------------------------------------------------- combat (one-shot)
# Shard Bite. HIT at 0.45 s (tick 9).
ANIMATIONS["attack"] = dict(length=0.9, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.3, [-18, 0, 0]), (0.45, [22, 0, 0]), (0.6, [14, 0, 0]), z(0.9))},
    "head": {"rotation": track(z(0), (0.3, [-14, 0, 0]), (0.45, [16, 0, 0]), (0.6, [8, 0, 0]), z(0.9))},
    "jaw": {"rotation": track(z(0), (0.3, [30, 0, 0]), (0.42, [30, 0, 0]), z(0.47), z(0.9))},
    "body": {"position": track(z(0), (0.3, [0, 0.5, 2]), (0.45, [0, -0.5, -3.5]), (0.6, [0, 0, -2.5]), z(0.9)),
             "rotation": track(z(0), (0.3, [-4, 0, 0]), (0.45, [5, 0, 0]), z(0.9))},
    "leg_front_left": {"rotation": track(z(0), (0.3, [-8, 0, 0]), (0.45, [-15, 0, 0]), z(0.9))},
    "leg_front_right": {"rotation": track(z(0), (0.3, [-8, 0, 0]), (0.45, [-15, 0, 0]), z(0.9))},
    "wing_left": {"rotation": track(z(0), (0.3, [0, 0, -10]), (0.45, [0, 0, 5]), z(0.9))},
    "wing_right": {"rotation": track(z(0), (0.3, [0, 0, 10]), (0.45, [0, 0, -5]), z(0.9))},
})

# Special 1 - Shatter Slam. HIT (ground impact) at 1.2 s (tick 24).
ANIMATIONS["special_slam"] = dict(length=1.8, loop=False, bones={
    "body": {"rotation": track(z(0), (0.8, [-24, 0, 0]), (1.0, [-28, 0, 0]), (1.2, [8, 0, 0]), (1.5, [3, 0, 0]), z(1.8)),
             "position": track(z(0), (0.8, [0, 6, 3]), (1.0, [0, 7, 3]), (1.2, [0, -1, -1]), z(1.5), z(1.8))},
    "leg_front_left": {"rotation": track(z(0), (0.8, [-40, 0, -8]), (1.0, [-45, 0, -8]), (1.2, [12, 0, 0]), (1.5, [4, 0, 0]), z(1.8))},
    "leg_front_right": {"rotation": track(z(0), (0.8, [-40, 0, 8]), (1.0, [-45, 0, 8]), (1.2, [12, 0, 0]), (1.5, [4, 0, 0]), z(1.8))},
    "leg_back_left": {"rotation": track(z(0), (0.8, [22, 0, 0]), (1.0, [26, 0, 0]), (1.2, [-6, 0, 0]), z(1.8))},
    "leg_back_right": {"rotation": track(z(0), (0.8, [22, 0, 0]), (1.0, [26, 0, 0]), (1.2, [-6, 0, 0]), z(1.8))},
    "neck": {"rotation": track(z(0), (0.8, [-12, 0, 0]), (1.0, [-15, 0, 0]), (1.2, [18, 0, 0]), (1.5, [6, 0, 0]), z(1.8))},
    "head": {"rotation": track(z(0), (0.8, [-10, 0, 0]), (1.2, [12, 0, 0]), z(1.8))},
    "jaw": {"rotation": track(z(0), (0.8, [25, 0, 0]), (1.1, [30, 0, 0]), (1.2, [8, 0, 0]), z(1.8))},
    "wing_left": {"rotation": track(z(0), (0.8, [0, 0, -30]), (1.0, [0, 0, -35]), (1.2, [0, 0, 12]), z(1.5), z(1.8))},
    "wing_right": {"rotation": track(z(0), (0.8, [0, 0, 30]), (1.0, [0, 0, 35]), (1.2, [0, 0, -12]), z(1.5), z(1.8))},
    "tail_1": {"rotation": track(z(0), (0.8, [18, 0, 0]), (1.0, [20, 0, 0]), (1.2, [-6, 0, 0]), z(1.8))},
})

# Special 2 - Shard Volley. HIT (projectiles released) at 0.9 s (tick 18).
ANIMATIONS["special_volley"] = dict(length=2.0, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.6, [-28, 0, 0]), (0.9, [8, 0, 0]), (1.3, [6, 0, 0]), (1.6, [2, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.6, [-16, 0, 0]), (0.9, [6, 0, 0]), (1.1, [6, -8, 0]), (1.3, [6, 8, 0]), (1.6, [2, 0, 0]), z(2.0))},
    "jaw": {"rotation": track(z(0), (0.6, [12, 0, 0]), (0.85, [38, 0, 0]), (1.4, [34, 0, 0]), (1.7, [8, 0, 0]), z(2.0))},
    "body": {"position": track(z(0), (0.6, [0, 0.5, 1.5]), (0.9, [0, 0, -1]), z(2.0))},
    "wing_left": {"rotation": track(z(0), (0.6, [0, 0, -15]), (0.9, [0, 0, -8]), z(2.0))},
    "wing_right": {"rotation": track(z(0), (0.6, [0, 0, 15]), (0.9, [0, 0, 8]), z(2.0))},
})

# Special 3 - Wing Tempest. HIT (gusts) at 0.8 s (tick 16, damage) and 1.4 s (tick 28, push only).
ANIMATIONS["special_tempest"] = dict(length=2.0, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.5, [0, -10, -50]), (0.8, [0, 5, 30]), (1.1, [0, -5, -40]), (1.4, [0, 5, 28]), (1.7, [0, 0, -10]), z(2.0))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, 10, 50]), (0.8, [0, -5, -30]), (1.1, [0, 5, 40]), (1.4, [0, -5, -28]), (1.7, [0, 0, 10]), z(2.0))},
    "body": {"position": track(z(0), (0.5, [0, 3, 0]), (0.8, [0, -0.5, 0]), (1.1, [0, 2.5, 0]), (1.4, [0, -0.5, 0]), z(2.0))},
    "neck": {"rotation": track(z(0), (0.5, [-12, 0, 0]), (0.8, [4, 0, 0]), (1.1, [-10, 0, 0]), (1.4, [4, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.5, [-8, 0, 0]), (0.8, [6, 0, 0]), z(2.0))},
    "jaw": {"rotation": track(z(0), (0.5, [20, 0, 0]), (1.5, [20, 0, 0]), z(2.0))},
    "leg_front_left": {"rotation": track(z(0), (0.5, [-10, 0, 0]), (0.8, [5, 0, 0]), z(2.0))},
    "leg_front_right": {"rotation": track(z(0), (0.5, [-10, 0, 0]), (0.8, [5, 0, 0]), z(2.0))},
})

# Obsidian Charge: wind-up 0-0.75 s (15 ticks), dash 0.75-1.75 s (<= 20 ticks), recovery to 2.25 s.
GALLOP_T = [0.75 + i * 0.125 for i in range(9)]  # 0.75 .. 1.75
ANIMATIONS["charge"] = dict(length=2.25, loop=False, bones={
    "body": {"position": track(z(0), (0.4, [0, -1.5, 1]), (0.75, [0, -1.5, 1]),
                               *[(t, [0, 1.2 if i % 2 else 0, -1]) for i, t in enumerate(GALLOP_T)], z(2.25)),
             "rotation": track(z(0), (0.4, [6, 0, 0]), (0.75, [6, 0, 0]), (0.85, [8, 0, 0]), (1.75, [8, 0, 0]), z(2.25))},
    "neck": {"rotation": track(z(0), (0.4, [22, 0, 0]), (0.75, [22, 0, 0]), (1.75, [18, 0, 0]), z(2.25))},
    "head": {"rotation": track(z(0), (0.4, [4, 0, 0]), (1.75, [4, 0, 0]), z(2.25))},
    "jaw": {"rotation": track(z(0), (0.4, [15, 0, 0]), (1.75, [20, 0, 0]), z(2.25))},
    "wing_left": {"rotation": track(z(0), (0.4, [0, -20, 15]), (1.75, [0, -20, 15]), z(2.25))},
    "wing_right": {"rotation": track(z(0), (0.4, [0, 20, -15]), (1.75, [0, 20, -15]), z(2.25))},
    "leg_front_left": {"rotation": track(z(0), (0.4, [10, 0, 0]), *legs_swing(GALLOP_T, -30, 30), z(2.25))},
    "leg_front_right": {"rotation": track(z(0), (0.4, [10, 0, 0]), *legs_swing(GALLOP_T, -30, 30), z(2.25))},
    "leg_back_left": {"rotation": track(z(0), (0.4, [-6, 0, 0]), *legs_swing(GALLOP_T, 30, -30), z(2.25))},
    "leg_back_right": {"rotation": track(z(0), (0.4, [-6, 0, 0]), *legs_swing(GALLOP_T, 30, -30), z(2.25))},
    "tail_1": {"rotation": track(z(0), (0.4, [8, 0, 0]), (1.75, [8, 0, 0]), z(2.25))},
})

# Summon Shard Wraiths. HIT (minions appear) at 1.0 s (tick 20).
ANIMATIONS["summon"] = dict(length=2.0, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.6, [-30, 0, 0]), (1.4, [-30, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.6, [-18, 0, 0]), (0.9, [-18, 6, 0]), (1.1, [-18, -6, 0]), (1.4, [-18, 0, 0]), z(2.0))},
    "jaw": {"rotation": track(z(0), (0.6, [38, 0, 0]), (1.4, [38, 0, 0]), z(2.0))},
    "wing_left": {"rotation": track(z(0), (0.6, [0, 0, -38]), (1.4, [0, 0, -38]), z(2.0))},
    "wing_right": {"rotation": track(z(0), (0.6, [0, 0, 38]), (1.4, [0, 0, 38]), z(2.0))},
    "body": {"rotation": track(z(0), (0.6, [-8, 0, 0]), (1.4, [-8, 0, 0]), z(2.0)),
             "position": track(z(0), (0.6, [0, 2, 0]), (1.4, [0, 2, 0]), z(2.0))},
})

# Phase change / enrage roar (boss is invulnerable while it plays).
ANIMATIONS["roar"] = dict(length=2.0, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.5, [-35, 0, 0]), (1.5, [-35, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.5, [-20, 0, 0]), (0.7, [-20, 7, 0]), (0.9, [-20, -7, 0]), (1.1, [-20, 7, 0]), (1.3, [-20, -7, 0]), (1.5, [-20, 0, 0]), z(2.0))},
    "jaw": {"rotation": track(z(0), (0.5, [42, 0, 0]), (1.5, [42, 0, 0]), z(2.0))},
    "wing_left": {"rotation": track(z(0), (0.5, [0, 0, -50]), (1.5, [0, 0, -50]), z(2.0))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, 0, 50]), (1.5, [0, 0, 50]), z(2.0))},
    "body": {"rotation": track(z(0), (0.5, [-12, 0, 0]), (1.5, [-12, 0, 0]), z(2.0)),
             "position": track(z(0), (0.5, [0, 3, 0]), (1.5, [0, 3, 0]), z(2.0))},
    "leg_front_left": {"rotation": track(z(0), (0.5, [-15, 0, 0]), (1.5, [-15, 0, 0]), z(2.0))},
    "leg_front_right": {"rotation": track(z(0), (0.5, [-15, 0, 0]), (1.5, [-15, 0, 0]), z(2.0))},
    "tail_1": {"rotation": track(z(0), (0.5, [12, 0, 0]), (1.5, [12, 0, 0]), z(2.0))},
})

# Additive flinch layered on top of whatever else is playing.
ANIMATIONS["hurt"] = dict(length=0.35, loop=False, bones={
    "body": {"rotation": track(z(0), (0.08, [4, 0, 0]), z(0.35))},
    "neck": {"rotation": track(z(0), (0.08, [10, 0, 0]), z(0.35))},
    "head": {"rotation": track(z(0), (0.08, [8, 0, 0]), z(0.35))},
    "wing_left": {"rotation": track(z(0), (0.08, [0, 0, 6]), z(0.35))},
    "wing_right": {"rotation": track(z(0), (0.08, [0, 0, -6]), z(0.35))},
})

ANIMATIONS["death"] = dict(length=2.5, loop="hold_on_last_frame", bones={
    "neck": {"rotation": track(z(0), (0.4, [-28, 0, 0]), (1.2, [8, 0, 0]), (2.0, [35, 10, 0]), (2.5, [35, 10, 0]))},
    "head": {"rotation": track(z(0), (0.4, [-20, 0, 0]), (1.2, [8, 0, 0]), (2.0, [22, 0, -10]), (2.5, [22, 0, -10]))},
    "jaw": {"rotation": track(z(0), (0.4, [40, 0, 0]), (1.2, [25, 0, 0]), (2.0, [20, 0, 0]), (2.5, [20, 0, 0]))},
    "body": {"position": track(z(0), (0.4, [0, 1.5, 0]), (1.2, [0, -4, 0]), (2.0, [0, -9, 0]), (2.5, [0, -9, 0])),
             "rotation": track(z(0), (0.4, [-6, 0, 0]), (1.2, [3, 0, 6]), (2.0, [4, 0, 14]), (2.5, [4, 0, 14]))},
    "leg_front_left": {"rotation": track(z(0), (0.4, [-8, 0, 0]), (1.2, [-35, 0, 0]), (2.0, [-65, 0, 0]), (2.5, [-65, 0, 0]))},
    "leg_front_right": {"rotation": track(z(0), (0.4, [-8, 0, 0]), (1.2, [-35, 0, 0]), (2.0, [-65, 0, 0]), (2.5, [-65, 0, 0]))},
    "leg_back_left": {"rotation": track(z(0), (1.2, [35, 0, 0]), (2.0, [65, 0, 0]), (2.5, [65, 0, 0]))},
    "leg_back_right": {"rotation": track(z(0), (1.2, [35, 0, 0]), (2.0, [65, 0, 0]), (2.5, [65, 0, 0]))},
    "wing_left": {"rotation": track(z(0), (0.4, [0, 0, -32]), (1.2, [0, 0, -10]), (2.0, [0, -10, 30]), (2.5, [0, -10, 30]))},
    "wing_right": {"rotation": track(z(0), (0.4, [0, 0, 32]), (1.2, [0, 0, 10]), (2.0, [0, 10, -30]), (2.5, [0, 10, -30]))},
    "tail_1": {"rotation": track(z(0), (2.0, [-6, 20, 0]), (2.5, [-6, 20, 0]))},
    "tail_2": {"rotation": track(z(0), (2.0, [0, 15, 0]), (2.5, [0, 15, 0]))},
    "tail_3": {"rotation": track(z(0), (2.0, [0, 10, 0]), (2.5, [0, 10, 0]))},
})

# Rises out of the ground, then roars. The boss is invulnerable and passive while this plays.
ANIMATIONS["spawn"] = dict(length=2.5, loop=False, bones={
    "root": {"position": track((0, [0, -24, 0]), (0.9, [0, -4, 0]), z(1.2), z(2.5), ease="easeOutCubic")},
    "neck": {"rotation": track((0, [30, 0, 0]), (1.2, [20, 0, 0]), (1.6, [-32, 0, 0]), (2.1, [-32, 0, 0]), z(2.5))},
    "head": {"rotation": track((0, [20, 0, 0]), (1.2, [10, 0, 0]), (1.6, [-18, 0, 0]), (1.8, [-18, 5, 0]), (1.95, [-18, -5, 0]), (2.1, [-18, 0, 0]), z(2.5))},
    "jaw": {"rotation": track(z(0), z(1.3), (1.6, [40, 0, 0]), (2.1, [40, 0, 0]), z(2.5))},
    "wing_left": {"rotation": track((0, [0, -25, 30]), (1.2, [0, -15, 20]), (1.6, [0, 0, -48]), (2.1, [0, 0, -48]), z(2.5))},
    "wing_right": {"rotation": track((0, [0, 25, -30]), (1.2, [0, 15, -20]), (1.6, [0, 0, 48]), (2.1, [0, 0, 48]), z(2.5))},
    "body": {"rotation": track(z(0), z(1.2), (1.6, [-10, 0, 0]), (2.1, [-10, 0, 0]), z(2.5)),
             "position": track(z(0), z(1.2), (1.6, [0, 2, 0]), (2.1, [0, 2, 0]), z(2.5))},
})

# ======================================================================== new patterns (10-phase update)
# Tail Sweep - rear / side arc. HIT at 0.65 s (tick 13).
ANIMATIONS["tail_sweep"] = dict(length=1.2, loop=False, bones={
    "body": {"rotation": track(z(0), (0.4, [0, 20, 0]), (0.65, [0, -30, 0]), (0.9, [0, -10, 0]), z(1.2))},
    "tail_1": {"rotation": track(z(0), (0.4, [5, -35, 0]), (0.65, [0, 45, 0]), (0.9, [0, 20, 0]), z(1.2))},
    "tail_2": {"rotation": track(z(0), (0.4, [0, -30, 0]), (0.7, [0, 40, 0]), (0.95, [0, 15, 0]), z(1.2))},
    "tail_3": {"rotation": track(z(0), (0.45, [0, -25, 0]), (0.75, [0, 35, 0]), (1.0, [0, 10, 0]), z(1.2))},
    "neck": {"rotation": track(z(0), (0.4, [0, -15, 0]), (0.65, [0, 20, 0]), z(1.2))},
    "leg_back_left": {"rotation": track(z(0), (0.65, [-10, 0, 0]), z(1.2))},
    "leg_back_right": {"rotation": track(z(0), (0.65, [-10, 0, 0]), z(1.2))},
})

# Crystal Rain - roar, then a casting pose while three waves of shards fall (ticks 16 / 36 / 56 marked).
_rain_left = [z(0), (0.6, [0, 0, -40]), (1.6, [0, 0, -40]), (2.0, [0, 0, -25]), (2.4, [0, 0, -40]), (2.8, [0, 0, -25]),
              (3.2, [0, 0, -40]), (3.6, [0, 0, -25]), (4.0, [0, 0, -35]), z(4.5)]
ANIMATIONS["crystal_rain"] = dict(length=4.5, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.6, [-35, 0, 0]), (1.6, [-35, 0, 0]), (2.2, [-15, 0, 0]), (4.0, [-15, 0, 0]), z(4.5))},
    "head": {"rotation": track(z(0), (0.6, [-20, 0, 0]), (1.6, [-20, 0, 0]), (2.2, [-5, 0, 0]), (4.0, [-5, 0, 0]), z(4.5))},
    "jaw": {"rotation": track(z(0), (0.6, [40, 0, 0]), (1.6, [40, 0, 0]), (2.2, [15, 0, 0]), (4.0, [15, 0, 0]), z(4.5))},
    "wing_left": {"rotation": track(*_rain_left)},
    "wing_right": {"rotation": track(*[(t, [v[0], -v[1], -v[2]]) for t, v in _rain_left])},
    "body": {"rotation": track(z(0), (0.6, [-8, 0, 0]), (4.0, [-8, 0, 0]), z(4.5)),
             "position": track(z(0), (0.6, [0, 2, 0]), (4.0, [0, 2, 0]), z(4.5))},
})

# Spike Eruption - rear up and stomp; crystal spikes race outward. HIT at 0.75 s (tick 15).
ANIMATIONS["spike_eruption"] = dict(length=2.0, loop=False, bones={
    "body": {"rotation": track(z(0), (0.5, [-14, 0, 0]), (0.75, [8, 0, 0]), (1.1, [3, 0, 0]), z(2.0)),
             "position": track(z(0), (0.5, [0, 3, 1]), (0.75, [0, -1, 0]), z(1.1), z(2.0))},
    "leg_front_left": {"rotation": track(z(0), (0.5, [-35, 0, 0]), (0.75, [10, 0, 0]), (1.1, [4, 0, 0]), z(2.0))},
    "leg_front_right": {"rotation": track(z(0), (0.5, [-35, 0, 0]), (0.75, [10, 0, 0]), (1.1, [4, 0, 0]), z(2.0))},
    "neck": {"rotation": track(z(0), (0.5, [-12, 0, 0]), (0.75, [16, 0, 0]), (1.2, [6, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.75, [10, 0, 0]), z(2.0))},
    "jaw": {"rotation": track(z(0), (0.5, [25, 0, 0]), (0.8, [10, 0, 0]), z(2.0))},
    "wing_left": {"rotation": track(z(0), (0.5, [0, 0, -25]), (0.75, [0, 0, 10]), z(2.0))},
    "wing_right": {"rotation": track(z(0), (0.5, [0, 0, 25]), (0.75, [0, 0, -10]), z(2.0))},
})

# Prism Beam - charge 0-1.0 s, sustained beam 1.0-3.0 s (ticks 20-60), recover.
ANIMATIONS["prism_beam"] = dict(length=3.5, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.8, [-30, 0, 0]), (1.0, [5, 0, 0]), (3.0, [5, 0, 0]), z(3.5))},
    "head": {"rotation": track(z(0), (0.8, [-15, 0, 0]), (1.0, [8, 0, 0]), (3.0, [8, 0, 0]), z(3.5))},
    "jaw": {"rotation": track(z(0), (0.8, [20, 0, 0]), (1.0, [45, 0, 0]), (3.0, [45, 0, 0]), z(3.5))},
    "body": {"position": track(z(0), (0.8, [0, 1, 2]), (1.0, [0, 0, -1]), (3.0, [0, 0, -1]), z(3.5)),
             "rotation": track(z(0), (0.8, [-6, 0, 0]), (1.0, [3, 0, 0]), (3.0, [3, 0, 0]), z(3.5))},
    "wing_left": {"rotation": track(z(0), (0.8, [0, 0, -30]), (1.0, [0, 0, -15]), (2.0, [0, 0, -20]), (3.0, [0, 0, -15]), z(3.5))},
    "wing_right": {"rotation": track(z(0), (0.8, [0, 0, 30]), (1.0, [0, 0, 15]), (2.0, [0, 0, 20]), (3.0, [0, 0, 15]), z(3.5))},
    "leg_front_left": {"rotation": track(z(0), (1.0, [-12, 0, 0]), (3.0, [-12, 0, 0]), z(3.5))},
    "leg_front_right": {"rotation": track(z(0), (1.0, [-12, 0, 0]), (3.0, [-12, 0, 0]), z(3.5))},
})

# Void Roar - dips, then a shrieking roar. HIT at 0.7 s (tick 14).
ANIMATIONS["void_roar"] = dict(length=1.8, loop=False, bones={
    "neck": {"rotation": track(z(0), (0.4, [20, 0, 0]), (0.7, [-38, 0, 0]), (1.4, [-38, 0, 0]), z(1.8))},
    "head": {"rotation": track(z(0), (0.4, [10, 0, 0]), (0.7, [-22, 0, 0]), (0.85, [-22, 8, 0]), (1.0, [-22, -8, 0]), (1.15, [-22, 8, 0]), (1.4, [-22, 0, 0]), z(1.8))},
    "jaw": {"rotation": track(z(0), (0.4, [10, 0, 0]), (0.7, [45, 0, 0]), (1.4, [45, 0, 0]), z(1.8))},
    "wing_left": {"rotation": track(z(0), (0.4, [0, 0, 10]), (0.7, [0, 0, -55]), (1.4, [0, 0, -55]), z(1.8))},
    "wing_right": {"rotation": track(z(0), (0.4, [0, 0, -10]), (0.7, [0, 0, 55]), (1.4, [0, 0, 55]), z(1.8))},
    "body": {"rotation": track(z(0), (0.4, [6, 0, 0]), (0.7, [-12, 0, 0]), (1.4, [-12, 0, 0]), z(1.8))},
})

# Meteor Dive - crouch, leap (server moves the entity along an arc 0.5-1.8 s), crash down. HIT at 1.8 s (tick 36).
_dive_left = [z(0), (0.5, [0, 0, 10]), (0.7, [0, 0, -50]), (0.9, [0, 0, 15]), (1.1, [0, 0, -45]), (1.3, [0, 0, 10]),
              (1.5, [0, 0, -30]), (1.8, [0, 0, 20]), z(2.6)]
ANIMATIONS["meteor_dive"] = dict(length=2.6, loop=False, bones={
    "body": {"position": track(z(0), (0.5, [0, -3, 0]), (0.6, [0, 1, 0]), z(1.8), (1.9, [0, -3, 0]), z(2.6)),
             "rotation": track(z(0), (0.5, [8, 0, 0]), (0.7, [-15, 0, 0]), (1.5, [10, 0, 0]), (1.8, [12, 0, 0]), (2.0, [4, 0, 0]), z(2.6))},
    "wing_left": {"rotation": track(*_dive_left)},
    "wing_right": {"rotation": track(*[(t, [v[0], -v[1], -v[2]]) for t, v in _dive_left])},
    "leg_front_left": {"rotation": track(z(0), (0.5, [15, 0, 0]), (0.7, [40, 0, 0]), (1.5, [-40, 0, 0]), (1.8, [-10, 0, 0]), z(2.6))},
    "leg_front_right": {"rotation": track(z(0), (0.5, [15, 0, 0]), (0.7, [40, 0, 0]), (1.5, [-40, 0, 0]), (1.8, [-10, 0, 0]), z(2.6))},
    "leg_back_left": {"rotation": track(z(0), (0.5, [15, 0, 0]), (0.7, [45, 0, 0]), (1.5, [20, 0, 0]), z(1.8), z(2.6))},
    "leg_back_right": {"rotation": track(z(0), (0.5, [15, 0, 0]), (0.7, [45, 0, 0]), (1.5, [20, 0, 0]), z(1.8), z(2.6))},
    "neck": {"rotation": track(z(0), (0.5, [15, 0, 0]), (0.7, [-20, 0, 0]), (1.5, [25, 0, 0]), (1.8, [20, 0, 0]), z(2.6))},
    "jaw": {"rotation": track(z(0), (1.5, [35, 0, 0]), (1.8, [10, 0, 0]), z(2.6))},
    "tail_1": {"rotation": track(z(0), (0.7, [20, 0, 0]), (1.5, [-10, 0, 0]), z(2.6))},
})

# Cataclysm (ultimate) - rise and gather 0-2.5 s, blast at 2.5 s (tick 50), exhausted and EXPOSED until 6.0 s.
_cata_left = [z(0), (1.5, [0, 0, -60]), (2.3, [0, 0, -70]), (2.5, [0, 0, 25]), (2.8, [0, -10, 35]), (5.5, [0, -10, 35]), z(6.0)]
ANIMATIONS["cataclysm"] = dict(length=6.0, loop=False, bones={
    "body": {"position": track(z(0), (1.5, [0, 5, 0]), (2.3, [0, 7, 0]), (2.5, [0, -2, 0]), (2.8, [0, -6, 0]), (3.5, [0, -5, 0]),
                               (4.2, [0, -6.5, 0]), (4.9, [0, -5, 0]), (5.5, [0, -6, 0]), z(6.0)),
             "rotation": track(z(0), (1.5, [-22, 0, 0]), (2.3, [-26, 0, 0]), (2.5, [10, 0, 0]), (2.8, [14, 0, 0]), (5.5, [14, 0, 0]), z(6.0))},
    "wing_left": {"rotation": track(*_cata_left)},
    "wing_right": {"rotation": track(*[(t, [v[0], -v[1], -v[2]]) for t, v in _cata_left])},
    "neck": {"rotation": track(z(0), (1.5, [-38, 0, 0]), (2.3, [-40, 0, 0]), (2.5, [25, 0, 0]), (2.8, [40, 0, 0]), (5.5, [40, 0, 0]), z(6.0))},
    "head": {"rotation": track(z(0), (1.5, [-20, 0, 0]), (2.5, [15, 0, 0]), (2.8, [25, 0, 0]), (5.5, [25, 0, 0]), z(6.0))},
    "jaw": {"rotation": track(z(0), (1.5, [45, 0, 0]), (2.3, [45, 0, 0]), (2.5, [20, 0, 0]), (2.8, [30, 0, 0]), (3.6, [15, 0, 0]),
                              (4.4, [30, 0, 0]), (5.2, [15, 0, 0]), (5.5, [30, 0, 0]), z(6.0))},
    "leg_front_left": {"rotation": track(z(0), (1.5, [-40, 0, 0]), (2.3, [-45, 0, 0]), (2.5, [15, 0, 0]), (2.8, [-30, 0, 0]), (5.5, [-30, 0, 0]), z(6.0))},
    "leg_front_right": {"rotation": track(z(0), (1.5, [-40, 0, 0]), (2.3, [-45, 0, 0]), (2.5, [15, 0, 0]), (2.8, [-30, 0, 0]), (5.5, [-30, 0, 0]), z(6.0))},
    "leg_back_left": {"rotation": track(z(0), (1.5, [25, 0, 0]), (2.5, [-5, 0, 0]), (2.8, [30, 0, 0]), (5.5, [30, 0, 0]), z(6.0))},
    "leg_back_right": {"rotation": track(z(0), (1.5, [25, 0, 0]), (2.5, [-5, 0, 0]), (2.8, [30, 0, 0]), (5.5, [30, 0, 0]), z(6.0))},
    "tail_1": {"rotation": track(z(0), (1.5, [20, 0, 0]), (2.8, [-8, 0, 0]), (5.5, [-8, 0, 0]), z(6.0))},
})

# ---------------------------------------------------------------- flight (phases 6-10)
# Hover / glide loop: one full wing beat, legs tucked, tail streaming. Base controller, loops while airborne.
ANIMATIONS["fly"] = dict(length=1.2, loop=True, bones={
    "wing_left": {"rotation": track((0, [0, 0, -40]), (0.6, [0, 5, 30]), (1.2, [0, 0, -40]))},
    "wing_right": {"rotation": track((0, [0, 0, 40]), (0.6, [0, -5, -30]), (1.2, [0, 0, 40]))},
    "body": {"position": track((0, [0, -1, 0]), (0.6, [0, 1.5, 0]), (1.2, [0, -1, 0])),
             "rotation": track((0, [-3, 0, -2]), (0.6, [-3, 0, 2]), (1.2, [-3, 0, -2]))},
    "neck": {"rotation": track((0, [-6, 0, 0]), (0.6, [-2, 0, 0]), (1.2, [-6, 0, 0]))},
    "head": {"rotation": track((0, [4, 0, 0]), (0.6, [1, 0, 0]), (1.2, [4, 0, 0]))},
    "jaw": {"rotation": track((0, [3, 0, 0]), (1.2, [3, 0, 0]))},
    "leg_front_left": {"rotation": track((0, [35, 0, 0]), (1.2, [35, 0, 0]))},
    "leg_front_right": {"rotation": track((0, [35, 0, 0]), (1.2, [35, 0, 0]))},
    "leg_back_left": {"rotation": track((0, [50, 0, 0]), (1.2, [50, 0, 0]))},
    "leg_back_right": {"rotation": track((0, [50, 0, 0]), (1.2, [50, 0, 0]))},
    "tail_1": {"rotation": track((0, [8, 0, 0]), (0.6, [4, 0, 0]), (1.2, [8, 0, 0]))},
    "tail_2": {"rotation": track((0, [4, 4, 0]), (0.6, [6, -4, 0]), (1.2, [4, 4, 0]))},
    "tail_3": {"rotation": track((0, [0, -6, 0]), (0.6, [4, 6, 0]), (1.2, [0, -6, 0]))},
})

# Takeoff. Crouch, then the launching downstroke at 0.4 s (tick 8 = TAKEOFF_LAUNCH_TICK); ends in the fly pose.
ANIMATIONS["takeoff"] = dict(length=1.5, loop=False, bones={
    "wing_left": {"rotation": track(z(0), (0.3, [0, -10, -60]), (0.5, [0, 5, 35]), (0.8, [0, 0, -45]), (1.1, [0, 5, 30]), (1.5, [0, 0, -40]))},
    "wing_right": {"rotation": track(z(0), (0.3, [0, 10, 60]), (0.5, [0, -5, -35]), (0.8, [0, 0, 45]), (1.1, [0, -5, -30]), (1.5, [0, 0, 40]))},
    "body": {"position": track(z(0), (0.3, [0, -2.5, 1]), (0.4, [0, -2.5, 1]), (0.6, [0, 2, 0]), (1.0, [0, 0.5, 0]), (1.5, [0, -1, 0])),
             "rotation": track(z(0), (0.3, [-8, 0, 0]), (0.6, [-4, 0, 0]), (1.5, [-3, 0, -2]))},
    "neck": {"rotation": track(z(0), (0.3, [-14, 0, 0]), (0.6, [-8, 0, 0]), (1.5, [-6, 0, 0]))},
    "head": {"rotation": track(z(0), (0.3, [-6, 0, 0]), (1.5, [4, 0, 0]))},
    "jaw": {"rotation": track(z(0), (0.3, [18, 0, 0]), (0.7, [4, 0, 0]), (1.5, [3, 0, 0]))},
    "leg_front_left": {"rotation": track(z(0), (0.3, [-10, 0, 0]), (0.6, [35, 0, 0]), (1.5, [35, 0, 0]))},
    "leg_front_right": {"rotation": track(z(0), (0.3, [-10, 0, 0]), (0.6, [35, 0, 0]), (1.5, [35, 0, 0]))},
    "leg_back_left": {"rotation": track(z(0), (0.3, [-15, 0, 0]), (0.6, [50, 0, 0]), (1.5, [50, 0, 0]))},
    "leg_back_right": {"rotation": track(z(0), (0.3, [-15, 0, 0]), (0.6, [50, 0, 0]), (1.5, [50, 0, 0]))},
    "tail_1": {"rotation": track(z(0), (0.3, [-6, 0, 0]), (0.7, [10, 0, 0]), (1.5, [8, 0, 0]))},
})

# Landing. Braking flare, legs reaching for the ground; touchdown at 1.0 s (tick 20 = LAND_IMPACT_TICK).
ANIMATIONS["land"] = dict(length=1.7, loop=False, bones={
    "wing_left": {"rotation": track((0, [0, 0, -40]), (0.4, [0, -15, -55]), (0.7, [0, 5, 10]), (1.0, [0, 0, -20]), (1.3, [0, 0, 5]), z(1.7))},
    "wing_right": {"rotation": track((0, [0, 0, 40]), (0.4, [0, 15, 55]), (0.7, [0, -5, -10]), (1.0, [0, 0, 20]), (1.3, [0, 0, -5]), z(1.7))},
    "body": {"position": track(z(0), (0.9, [0, 1, 0]), (1.0, [0, -2, 0]), (1.3, [0, -0.5, 0]), z(1.7)),
             "rotation": track((0, [-3, 0, 0]), (0.6, [-10, 0, 0]), (1.0, [6, 0, 0]), z(1.7))},
    "neck": {"rotation": track((0, [-6, 0, 0]), (0.6, [-15, 0, 0]), (1.0, [14, 0, 0]), (1.3, [4, 0, 0]), z(1.7))},
    "head": {"rotation": track((0, [4, 0, 0]), (1.0, [10, 0, 0]), z(1.7))},
    "jaw": {"rotation": track(z(0), (0.9, [10, 0, 0]), (1.0, [28, 0, 0]), (1.4, [6, 0, 0]), z(1.7))},
    "leg_front_left": {"rotation": track((0, [35, 0, 0]), (0.6, [-25, 0, 0]), (1.0, [-10, 0, 0]), (1.2, [8, 0, 0]), z(1.7))},
    "leg_front_right": {"rotation": track((0, [35, 0, 0]), (0.6, [-25, 0, 0]), (1.0, [-10, 0, 0]), (1.2, [8, 0, 0]), z(1.7))},
    "leg_back_left": {"rotation": track((0, [50, 0, 0]), (0.6, [-10, 0, 0]), (1.0, [5, 0, 0]), z(1.7))},
    "leg_back_right": {"rotation": track((0, [50, 0, 0]), (0.6, [-10, 0, 0]), (1.0, [5, 0, 0]), z(1.7))},
    "tail_1": {"rotation": track((0, [8, 0, 0]), (1.0, [-8, 0, 0]), z(1.7))},
})

# Shot down. Flails, crashes at 0.8 s (tick 16 = CRASH_IMPACT_TICK), lies stunned (EXPOSED) and gets up at the end.
ANIMATIONS["crash"] = dict(length=3.8, loop=False, bones={
    "body": {"rotation": track(z(0), (0.3, [-15, 0, 25]), (0.6, [10, 0, -20]), (0.8, [12, 0, 8]), (1.0, [8, 0, 12]), (3.2, [8, 0, 12]), z(3.8)),
             "position": track(z(0), (0.8, [0, -1, 0]), (1.0, [0, -3, 0]), (3.2, [0, -3, 0]), z(3.8))},
    "wing_left": {"rotation": track(z(0), (0.2, [0, 10, -60]), (0.4, [0, -10, 20]), (0.6, [0, 10, -50]), (0.8, [0, 0, 35]), (3.2, [0, 0, 35]), z(3.8))},
    "wing_right": {"rotation": track(z(0), (0.2, [0, -10, 60]), (0.4, [0, 10, -20]), (0.6, [0, -10, 50]), (0.8, [0, 0, -35]), (3.2, [0, 0, -35]), z(3.8))},
    "neck": {"rotation": track(z(0), (0.4, [-25, 0, 0]), (0.8, [25, 0, 0]), (1.0, [30, 10, 0]), (2.0, [26, 6, 0]), (3.2, [28, 8, 0]), z(3.8))},
    "head": {"rotation": track(z(0), (0.8, [15, 0, 0]), (1.0, [18, 0, -8]), (3.2, [18, 0, -8]), z(3.8))},
    "jaw": {"rotation": track(z(0), (0.4, [30, 0, 0]), (0.8, [10, 0, 0]), (1.0, [22, 0, 0]), (2.0, [14, 0, 0]), (3.2, [22, 0, 0]), z(3.8))},
    "leg_front_left": {"rotation": track(z(0), (0.3, [-30, 0, 0]), (0.6, [20, 0, 0]), (0.8, [-35, 0, -10]), (3.2, [-35, 0, -10]), z(3.8))},
    "leg_front_right": {"rotation": track(z(0), (0.3, [20, 0, 0]), (0.6, [-30, 0, 0]), (0.8, [-30, 0, 10]), (3.2, [-30, 0, 10]), z(3.8))},
    "leg_back_left": {"rotation": track(z(0), (0.3, [30, 0, 0]), (0.8, [25, 0, -8]), (3.2, [25, 0, -8]), z(3.8))},
    "leg_back_right": {"rotation": track(z(0), (0.3, [-10, 0, 0]), (0.8, [28, 0, 8]), (3.2, [28, 0, 8]), z(3.8))},
    "tail_1": {"rotation": track(z(0), (0.4, [20, 10, 0]), (0.8, [-10, 0, 0]), (3.2, [-10, -5, 0]), z(3.8))},
    "tail_2": {"rotation": track(z(0), (0.5, [0, -15, 0]), (1.0, [0, 15, 0]), (3.2, [0, 12, 0]), z(3.8))},
    "tail_3": {"rotation": track(z(0), (0.6, [0, 20, 0]), (1.0, [0, -20, 0]), (3.2, [0, -16, 0]), z(3.8))},
})

# ======================================================================== texture palette (baked, BOSS.mtl has no map_Kd)
_HIDE = (0.050, 0.038, 0.070)
_LIMB = (0.045, 0.036, 0.062)
TEXTURES = {
    "shard_dragon": dict(
        parts={"body": _HIDE, "neck": (0.055, 0.040, 0.075), "head": (0.065, 0.045, 0.085), "jaw": (0.110, 0.030, 0.045),
               "wing_left": (0.120, 0.022, 0.060), "wing_right": (0.120, 0.022, 0.060),
               "leg_front_left": _LIMB, "leg_front_right": _LIMB, "leg_back_left": _LIMB, "leg_back_right": _LIMB,
               "tail_1": _HIDE, "tail_2": _HIDE, "tail_3": (0.060, 0.040, 0.080)},
        default=_HIDE, crystal=(0.42, 0.16, 0.85), glow=(0.70, 0.35, 1.00),
        crystal_chance=0.14, crystal_chance_high=0.30, high_y=30, bone_crystal_chance={}, seed=1337),
}
