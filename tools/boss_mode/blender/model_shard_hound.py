"""Shard Hound (minion) - Mob.obj. One mesh / rig / animation set shared by three minion types that differ in
texture and behaviour: shard_hound (melee), shard_spitter (ranged), volatile_hound (explodes).

OBJ: 263 small fragments, 19 x 20 x 24 units, Y up, head toward +Z, feet on Y = 0.
"""
from anim_common import track, z, one, legs_swing, SMOOTH

OBJ = "Mob.obj"
ENTITY = "shard_hound"
SPLIT_THRESHOLD = 10 ** 9

BONES = [
    ("root",            None,   (0, 0, 0),   None),
    ("body",            "root", (0, 9, 0),   (0, 11, 8)),
    ("head",            "body", (0, 8, 7),   (0, 8, 12)),
    ("leg_front_left",  "body", (6, 9, 6),   (7, 1, 7)),
    ("leg_front_right", "body", (-6, 9, 6),  (-7, 1, 7)),
    ("leg_back_left",   "body", (6, 9, -6),  (7, 1, -8)),
    ("leg_back_right",  "body", (-6, 9, -6), (-7, 1, -8)),
]

SEGMENTS = {
    "body":            ((0, 11, -9), (0, 11, 6)),
    "head":            ((0, 8, 8), (0, 7, 12)),
    "leg_front_left":  ((6, 7, 6), (7, 1, 7)),
    "leg_front_right": ((-6, 7, 6), (-7, 1, 7)),
    "leg_back_left":   ((6, 7, -7), (7, 1, -8)),
    "leg_back_right":  ((-6, 7, -7), (-7, 1, -8)),
}

ANIMATIONS = {}
ANIMATIONS["idle"] = dict(length=2.0, loop=True, bones={
    "body": {"position": track(z(0), (1.0, [0, 0.3, 0]), z(2.0)), "rotation": track(z(0), (1.0, [-1, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.5, [0, 8, 0]), (1.5, [0, -8, 0]), z(2.0))},
})
_W = [0, 0.4, 0.8]
ANIMATIONS["walk"] = dict(length=0.8, loop=True, bones={
    "leg_front_left": {"rotation": track(*legs_swing(_W, -22, 22))},
    "leg_back_right": {"rotation": track(*legs_swing(_W, -22, 22))},
    "leg_front_right": {"rotation": track(*legs_swing(_W, 22, -22))},
    "leg_back_left": {"rotation": track(*legs_swing(_W, 22, -22))},
    "body": {"position": track(z(0), (0.2, [0, 0.5, 0]), z(0.4), (0.6, [0, 0.5, 0]), z(0.8))},
    "head": {"rotation": track((0, [3, 0, 0]), (0.4, [-2, 0, 0]), (0.8, [3, 0, 0]))},
})
_R = [0, 0.225, 0.45]
ANIMATIONS["run"] = dict(length=0.45, loop=True, bones={
    "leg_front_left": {"rotation": track(*legs_swing(_R, -35, 35))},
    "leg_front_right": {"rotation": track(*legs_swing(_R, -35, 35))},
    "leg_back_left": {"rotation": track(*legs_swing(_R, 35, -35))},
    "leg_back_right": {"rotation": track(*legs_swing(_R, 35, -35))},
    "body": {"rotation": track((0, [-6, 0, 0]), (0.225, [6, 0, 0]), (0.45, [-6, 0, 0])),
             "position": track(z(0), (0.11, [0, 1, 0]), z(0.225), (0.34, [0, 1, 0]), z(0.45))},
    "head": {"rotation": track((0, [6, 0, 0]), (0.225, [-4, 0, 0]), (0.45, [6, 0, 0]))},
})
# Lunge bite. HIT at 0.3 s (tick 6).
ANIMATIONS["attack"] = dict(length=0.6, loop=False, bones={
    "body": {"position": track(z(0), (0.2, [0, 0.5, 2]), (0.3, [0, 0, -4]), (0.45, [0, 0, -2]), z(0.6))},
    "head": {"rotation": track(z(0), (0.2, [-20, 0, 0]), (0.3, [20, 0, 0]), (0.45, [10, 0, 0]), z(0.6))},
    "leg_front_left": {"rotation": track(z(0), (0.2, [10, 0, 0]), (0.3, [-35, 0, 0]), z(0.6))},
    "leg_front_right": {"rotation": track(z(0), (0.2, [10, 0, 0]), (0.3, [-35, 0, 0]), z(0.6))},
    "leg_back_left": {"rotation": track(z(0), (0.3, [25, 0, 0]), z(0.6))},
    "leg_back_right": {"rotation": track(z(0), (0.3, [25, 0, 0]), z(0.6))},
})
# Spitter shot. HIT at 0.5 s (tick 10).
ANIMATIONS["spit"] = dict(length=0.8, loop=False, bones={
    "head": {"rotation": track(z(0), (0.35, [-28, 0, 0]), (0.5, [15, 0, 0]), (0.65, [5, 0, 0]), z(0.8))},
    "body": {"position": track(z(0), (0.35, [0, 0.5, 1.5]), (0.5, [0, 0, -1]), z(0.8)),
             "rotation": track(z(0), (0.35, [-6, 0, 0]), (0.5, [4, 0, 0]), z(0.8))},
})
# Volatile fuse: shakes and swells, detonates at 1.5 s (tick 30).
_jitter = [(round(0.1 * i, 2), [0, 0, (4 + i) * (1 if i % 2 else -1)]) for i in range(1, 15)]
ANIMATIONS["fuse"] = dict(length=1.5, loop="hold_on_last_frame", bones={
    "body": {"scale": track(one(0), (0.5, [1.05, 1.05, 1.05]), (1.0, [1.12, 1.1, 1.12]), (1.3, [1.2, 1.15, 1.2]), (1.5, [1.3, 1.25, 1.3])),
             "rotation": track(z(0), *_jitter, z(1.5), ease="linear")},
    "head": {"rotation": track(z(0), (0.3, [-15, 0, 0]), (1.5, [-20, 0, 0]))},
})
ANIMATIONS["hurt"] = dict(length=0.3, loop=False, bones={
    "body": {"rotation": track(z(0), (0.08, [8, 0, 0]), z(0.3))},
    "head": {"rotation": track(z(0), (0.08, [12, 0, 0]), z(0.3))},
})
ANIMATIONS["death"] = dict(length=1.0, loop="hold_on_last_frame", bones={
    "body": {"rotation": track(z(0), (0.4, [0, 0, 40]), (1.0, [0, 0, 85])), "position": track(z(0), (1.0, [0, -3, 0]))},
    "leg_front_left": {"rotation": track(z(0), (1.0, [-30, 0, 0]))},
    "leg_front_right": {"rotation": track(z(0), (1.0, [-30, 0, 0]))},
    "leg_back_left": {"rotation": track(z(0), (1.0, [30, 0, 0]))},
    "leg_back_right": {"rotation": track(z(0), (1.0, [30, 0, 0]))},
    "head": {"rotation": track(z(0), (1.0, [25, 0, 0]))},
})
ANIMATIONS["spawn"] = dict(length=1.0, loop=False, bones={
    "root": {"position": track((0, [0, -14, 0]), (0.6, [0, -2, 0]), z(0.8), z(1.0), ease="easeOutCubic")},
    "head": {"rotation": track((0, [25, 0, 0]), (0.8, [-25, 0, 0]), z(1.0))},
})

_HIDE = (0.050, 0.040, 0.068)
TEXTURES = {
    "shard_hound": dict(parts={"head": (0.065, 0.045, 0.085)}, default=_HIDE,
                        crystal=(0.42, 0.16, 0.85), glow=(0.70, 0.35, 1.00),
                        crystal_chance=0.25, crystal_chance_high=0.55, high_y=12, bone_crystal_chance={}, seed=11),
    "shard_spitter": dict(parts={"head": (0.030, 0.070, 0.080)}, default=(0.025, 0.050, 0.060),
                          crystal=(0.08, 0.55, 0.75), glow=(0.30, 0.90, 1.00),
                          crystal_chance=0.25, crystal_chance_high=0.55, high_y=12, bone_crystal_chance={"head": 0.6}, seed=12),
    "volatile_hound": dict(parts={"head": (0.110, 0.040, 0.020)}, default=(0.080, 0.030, 0.020),
                           crystal=(0.95, 0.35, 0.05), glow=(1.00, 0.55, 0.10),
                           crystal_chance=0.35, crystal_chance_high=0.65, high_y=12, bone_crystal_chance={}, seed=13, veins=0.45),
}
