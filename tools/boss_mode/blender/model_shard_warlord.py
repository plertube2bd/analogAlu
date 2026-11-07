"""Shard Warlord (mid-boss) - Mob2.obj. Armoured humanoid holding a crystal staff in its right hand.

OBJ: 92 fragments (the torso/legs are one connected piece, split per triangle), 26 x 35 x 18 units, Y up, facing +Z,
feet on Y = 0. The model's right side is -X (staff hand).
"""
from anim_common import track, z, one, SMOOTH

OBJ = "Mob2.obj"
ENTITY = "shard_warlord"
SPLIT_THRESHOLD = 60  # fragments bigger than this are split per triangle between bones

BONES = [
    ("root",      None,        (0, 0, 0),    None),
    ("body",      "root",      (0, 19, 0),   (0, 28, 0)),
    ("head",      "body",      (0, 29, 1),   (0, 34, 2)),
    ("arm_right", "body",      (-8, 27, 0),  (-9, 19, 4)),
    ("staff",     "arm_right", (-9, 23, 5),  (-9, 33, 6)),
    ("arm_left",  "body",      (8, 27, 0),   (10, 15, 2)),
    ("leg_right", "root",      (-4, 18, 0),  (-5, 1, 1)),
    ("leg_left",  "root",      (4, 18, 0),   (5, 1, 1)),
    ("cape",      "body",      (0, 22, -3),  (0, 5, -6)),
]

SEGMENTS = {
    "body":      ((0, 20, 0), (0, 28, 0)),
    "head":      ((0, 30, 1), (0, 34, 2)),
    "arm_right": ((-8, 27, 0), (-9, 20, 3)),
    "staff":     ((-9, 1, 6), (-9, 33, 6)),
    "arm_left":  ((8, 27, 0), (10, 15, 2)),
    "leg_right": ((-4, 16, 0), (-5, 1, 1)),
    "leg_left":  ((4, 16, 0), (5, 1, 1)),
    "cape":      ((0, 20, -4), (0, 4, -7)),
}

ANIMATIONS = {}
ANIMATIONS["idle"] = dict(length=2.5, loop=True, bones={
    "body": {"rotation": track(z(0), (1.25, [2, 0, 0]), z(2.5)), "position": track(z(0), (1.25, [0, 0.3, 0]), z(2.5))},
    "head": {"rotation": track(z(0), (0.6, [0, 6, 0]), (1.9, [0, -6, 0]), z(2.5))},
    "cape": {"rotation": track(z(0), (1.25, [-5, 0, 0]), z(2.5))},
    "arm_left": {"rotation": track(z(0), (1.25, [-4, 0, 0]), z(2.5))},
})
ANIMATIONS["walk"] = dict(length=1.1, loop=True, bones={
    "leg_left": {"rotation": track((0, [-22, 0, 0]), (0.55, [22, 0, 0]), (1.1, [-22, 0, 0]))},
    "leg_right": {"rotation": track((0, [22, 0, 0]), (0.55, [-22, 0, 0]), (1.1, [22, 0, 0]))},
    "arm_left": {"rotation": track((0, [18, 0, 0]), (0.55, [-18, 0, 0]), (1.1, [18, 0, 0]))},
    "arm_right": {"rotation": track((0, [-8, 0, 0]), (0.55, [4, 0, 0]), (1.1, [-8, 0, 0]))},
    "body": {"position": track(z(0), (0.275, [0, 0.6, 0]), z(0.55), (0.825, [0, 0.6, 0]), z(1.1))},
    "cape": {"rotation": track((0, [-8, 0, 0]), (0.55, [-14, 0, 0]), (1.1, [-8, 0, 0]))},
})
# Staff swing. HIT at 0.55 s (tick 11).
ANIMATIONS["attack"] = dict(length=1.0, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.4, [-150, 0, 15]), (0.55, [-35, 0, -10]), (0.7, [-25, 0, -5]), z(1.0))},
    "staff": {"rotation": track(z(0), (0.4, [-10, 0, 0]), (0.55, [20, 0, 0]), z(1.0))},
    "body": {"rotation": track(z(0), (0.4, [-8, 15, 0]), (0.55, [14, -20, 0]), z(1.0))},
    "leg_right": {"rotation": track(z(0), (0.55, [-15, 0, 0]), z(1.0))},
    "leg_left": {"rotation": track(z(0), (0.55, [10, 0, 0]), z(1.0))},
    "cape": {"rotation": track(z(0), (0.55, [-20, 0, 0]), z(1.0))},
})
# Shard Bolt cast. HIT at 0.5 s (tick 10).
ANIMATIONS["cast"] = dict(length=1.0, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.35, [-40, 0, 0]), (0.5, [-90, 0, 0]), (0.7, [-85, 0, 0]), z(1.0))},
    "staff": {"rotation": track(z(0), (0.5, [-30, 0, 0]), (0.7, [-25, 0, 0]), z(1.0))},
    "arm_left": {"rotation": track(z(0), (0.35, [-60, 0, -20]), (0.5, [-90, 0, -10]), z(1.0))},
    "body": {"rotation": track(z(0), (0.35, [-5, 0, 0]), (0.5, [10, 0, 0]), z(1.0))},
    "head": {"rotation": track(z(0), (0.5, [5, 0, 0]), z(1.0))},
})
# Staff slam -> ground spike line. HIT at 0.75 s (tick 15).
ANIMATIONS["slam"] = dict(length=1.4, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.6, [-165, 0, 5]), (0.75, [-50, 0, 0]), (1.0, [-45, 0, 0]), z(1.4))},
    "arm_left": {"rotation": track(z(0), (0.6, [-160, 0, -5]), (0.75, [-55, 0, 0]), (1.0, [-50, 0, 0]), z(1.4))},
    "body": {"rotation": track(z(0), (0.6, [-10, 0, 0]), (0.75, [25, 0, 0]), (1.0, [20, 0, 0]), z(1.4)),
             "position": track(z(0), (0.75, [0, -2, 0]), (1.0, [0, -2, 0]), z(1.4))},
    "leg_left": {"rotation": track(z(0), (0.75, [-25, 0, 0]), (1.0, [-25, 0, 0]), z(1.4))},
    "leg_right": {"rotation": track(z(0), (0.75, [20, 0, 0]), (1.0, [20, 0, 0]), z(1.4))},
    "head": {"rotation": track(z(0), (0.75, [10, 0, 0]), z(1.4))},
    "cape": {"rotation": track(z(0), (0.6, [-30, 0, 0]), (0.75, [10, 0, 0]), z(1.4))},
})
# War cry: buffs allies and calls hounds. HIT at 0.6 s (tick 12).
ANIMATIONS["warcry"] = dict(length=1.6, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.5, [-170, 0, 0]), (1.2, [-170, 0, 0]), z(1.6))},
    "staff": {"rotation": track(z(0), (0.6, [0, 0, 8]), (0.7, [0, 0, -8]), (0.8, [0, 0, 8]), z(0.9), z(1.6))},
    "arm_left": {"rotation": track(z(0), (0.5, [-20, 0, -60]), (1.2, [-20, 0, -60]), z(1.6))},
    "head": {"rotation": track(z(0), (0.5, [-25, 0, 0]), (1.2, [-25, 0, 0]), z(1.6))},
    "body": {"rotation": track(z(0), (0.5, [-10, 0, 0]), (1.2, [-10, 0, 0]), z(1.6))},
    "cape": {"rotation": track(z(0), (0.5, [-30, 0, 0]), (0.8, [-20, 0, 0]), (1.2, [-30, 0, 0]), z(1.6))},
})
# Blink: collapses into a sliver of light, teleports at 0.45 s (tick 9), reforms.
ANIMATIONS["blink"] = dict(length=1.0, loop=False, bones={
    "root": {"scale": track(one(0), (0.4, [0.1, 1.4, 0.1]), (0.45, [0.1, 1.4, 0.1]), one(0.75), one(1.0))},
    "body": {"rotation": track(z(0), (0.4, [0, 40, 0]), z(0.75), z(1.0))},
})
ANIMATIONS["hurt"] = dict(length=0.3, loop=False, bones={
    "body": {"rotation": track(z(0), (0.08, [-6, 0, 0]), z(0.3))},
    "head": {"rotation": track(z(0), (0.08, [-10, 0, 0]), z(0.3))},
})
ANIMATIONS["death"] = dict(length=2.0, loop="hold_on_last_frame", bones={
    "body": {"rotation": track(z(0), (0.6, [20, 0, 0]), (1.4, [70, 0, 0]), (2.0, [85, 0, 0])),
             "position": track(z(0), (0.6, [0, -2, 0]), (1.4, [0, -10, -4]), (2.0, [0, -14, -6]))},
    "leg_left": {"rotation": track(z(0), (1.4, [-30, 0, 0]), (2.0, [-30, 0, 0]))},
    "leg_right": {"rotation": track(z(0), (1.4, [-30, 0, 0]), (2.0, [-30, 0, 0]))},
    "arm_right": {"rotation": track(z(0), (0.6, [-30, 0, 30]), (1.4, [0, 0, 60]), (2.0, [0, 0, 60]))},
    "staff": {"rotation": track(z(0), (0.6, [0, 0, 20]), (1.4, [0, 0, 70]), (2.0, [0, 0, 70]))},
    "head": {"rotation": track(z(0), (1.4, [30, 0, 0]), (2.0, [30, 0, 0]))},
    "cape": {"rotation": track(z(0), (1.4, [40, 0, 0]), (2.0, [40, 0, 0]))},
})
ANIMATIONS["spawn"] = dict(length=1.5, loop=False, bones={
    "root": {"position": track((0, [0, -36, 0]), (1.0, [0, -2, 0]), z(1.2), z(1.5), ease="easeOutCubic")},
    "arm_right": {"rotation": track(z(0), (1.0, [-10, 0, 0]), (1.2, [-160, 0, 0]), z(1.5))},
    "head": {"rotation": track((0, [20, 0, 0]), (1.0, [5, 0, 0]), (1.2, [-20, 0, 0]), z(1.5))},
})

TEXTURES = {
    "shard_warlord": dict(parts={"cape": (0.240, 0.025, 0.060), "head": (0.070, 0.050, 0.090), "staff": (0.080, 0.050, 0.110)},
                          default=(0.060, 0.045, 0.080),
                          crystal=(0.55, 0.18, 0.90), glow=(0.95, 0.35, 0.95),
                          crystal_chance=0.18, crystal_chance_high=0.45, high_y=29,
                          bone_crystal_chance={"staff": 0.75, "head": 0.55, "cape": 0.0}, seed=21),
}
