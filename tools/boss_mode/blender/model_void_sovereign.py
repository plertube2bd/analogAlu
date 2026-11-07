"""Void Sovereign (final boss) - Final BOSS.obj. A hulking tentacled titan with a crowned head, clawed arms, two
tentacle clusters on its shoulders, back tendrils and a looping tail.

OBJ: 2,266 vertices / 3,914 triangles / 96 fragments, 120 x 120 x 110 units, Y up, facing +Z, feet on Y = 0.
The model's right side is -X (the right claw reaches further forward). The biggest fragments (tail + back, both
tentacle clusters, the legs, the arms) span several body parts and are split per triangle.
"""
from anim_common import track, z, one, SMOOTH

OBJ = "Final BOSS.obj"
ENTITY = "void_sovereign"
SPLIT_THRESHOLD = 50

BONES = [
    ("root",            None,          (0, 0, 0),       None),
    ("body",            "root",        (0, 44, 0),      (0, 60, 0)),
    ("torso",           "body",        (0, 58, 0),      (0, 90, 2)),
    ("head",            "torso",       (0, 90, 6),      (0, 114, 16)),
    ("arm_right",       "torso",       (-28, 84, 2),    (-44, 64, 18)),
    ("hand_right",      "arm_right",   (-44, 64, 18),   (-48, 40, 38)),
    ("arm_left",        "torso",       (28, 84, 0),     (44, 64, 4)),
    ("hand_left",       "arm_left",    (44, 64, 4),     (50, 36, 8)),
    ("tentacles_right", "torso",       (-24, 88, -12),  (-50, 110, -4)),
    ("tentacles_left",  "torso",       (24, 88, -12),   (50, 110, -4)),
    ("mane",            "torso",       (0, 86, -18),    (0, 60, -32)),
    ("tail_1",          "body",        (0, 44, -18),    (-6, 34, -44)),
    ("tail_2",          "tail_1",      (-6, 34, -44),   (8, 20, -40)),
    ("leg_right",       "root",        (-20, 46, 4),    (-30, 3, 22)),
    ("leg_left",        "root",        (20, 46, 0),     (28, 3, 0)),
]

SEGMENTS = {
    "body":            ((0, 42, 0), (0, 58, 2)),
    "torso":           ((0, 60, 0), (0, 88, 2)),
    "head":            ((0, 92, 8), (0, 114, 16)),
    "arm_right":       ((-28, 84, 2), (-44, 64, 18)),
    "hand_right":      ((-44, 62, 20), (-48, 38, 38)),
    "arm_left":        ((28, 84, 0), (44, 64, 4)),
    "hand_left":       ((44, 62, 4), (50, 34, 8)),
    "tentacles_right": ((-26, 90, -12), (-52, 112, -4)),
    "tentacles_left":  ((26, 90, -12), (52, 112, -4)),
    "mane":            ((0, 84, -20), (0, 60, -32)),
    "tail_1":          ((0, 42, -20), (-6, 34, -44)),
    "tail_2":          ((-6, 32, -46), (8, 18, -40)),
    "leg_right":       ((-20, 44, 4), (-30, 2, 22)),
    "leg_left":        ((20, 44, 0), (28, 2, 0)),
}

ANIMATIONS = {}

# ---------------------------------------------------------------- locomotion (looping)
ANIMATIONS["idle"] = dict(length=3.0, loop=True, bones={
    "body": {"position": track(z(0), (1.5, [0, -0.6, 0]), z(3.0))},
    "torso": {"rotation": track(z(0), (1.5, [3, 0, 0]), z(3.0))},
    "head": {"rotation": track(z(0), (0.8, [2, 6, 0]), (1.5, [4, 0, 0]), (2.3, [2, -6, 0]), z(3.0))},
    "arm_right": {"rotation": track(z(0), (1.5, [-3, 0, 2]), z(3.0))},
    "arm_left": {"rotation": track(z(0), (1.5, [-3, 0, -2]), z(3.0))},
    "hand_right": {"rotation": track(z(0), (1.5, [-6, 0, 0]), z(3.0))},
    "hand_left": {"rotation": track(z(0), (1.5, [-6, 0, 0]), z(3.0))},
    "tentacles_right": {"rotation": track(z(0), (0.75, [4, 5, 6]), (2.25, [-4, -5, -3]), z(3.0))},
    "tentacles_left": {"rotation": track(z(0), (0.75, [-4, -5, -6]), (2.25, [4, 5, 3]), z(3.0))},
    "mane": {"rotation": track(z(0), (1.5, [-5, 0, 0]), z(3.0))},
    "tail_1": {"rotation": track(z(0), (0.75, [0, 8, 0]), (2.25, [0, -8, 0]), z(3.0))},
    "tail_2": {"rotation": track((0, [0, -4, 0]), (1.0, [4, 10, 0]), (2.5, [-4, -10, 0]), (3.0, [0, -4, 0]))},
})

ANIMATIONS["walk"] = dict(length=1.6, loop=True, bones={
    "leg_left": {"rotation": track((0, [-18, 0, 0]), (0.8, [18, 0, 0]), (1.6, [-18, 0, 0]))},
    "leg_right": {"rotation": track((0, [18, 0, 0]), (0.8, [-18, 0, 0]), (1.6, [18, 0, 0]))},
    "arm_left": {"rotation": track((0, [12, 0, 0]), (0.8, [-12, 0, 0]), (1.6, [12, 0, 0]))},
    "arm_right": {"rotation": track((0, [-12, 0, 0]), (0.8, [12, 0, 0]), (1.6, [-12, 0, 0]))},
    "body": {"position": track(z(0), (0.4, [0, 1.5, 0]), z(0.8), (1.2, [0, 1.5, 0]), z(1.6)),
             "rotation": track((0, [0, 0, -3]), (0.8, [0, 0, 3]), (1.6, [0, 0, -3]))},
    "torso": {"rotation": track((0, [4, 4, 0]), (0.8, [4, -4, 0]), (1.6, [4, 4, 0]))},
    "tentacles_right": {"rotation": track((0, [6, 0, 4]), (0.8, [-4, 0, -2]), (1.6, [6, 0, 4]))},
    "tentacles_left": {"rotation": track((0, [-4, 0, 2]), (0.8, [6, 0, -4]), (1.6, [-4, 0, 2]))},
    "mane": {"rotation": track((0, [-6, 0, 0]), (0.8, [-10, 0, 0]), (1.6, [-6, 0, 0]))},
    "tail_1": {"rotation": track((0, [0, 10, 0]), (0.8, [0, -10, 0]), (1.6, [0, 10, 0]))},
    "tail_2": {"rotation": track(z(0), (0.4, [0, 12, 0]), (1.2, [0, -12, 0]), z(1.6))},
})

# ---------------------------------------------------------------- lifecycle
# Spawn: rises out of the void (SPAWN_INTRO_TICKS = 60), roars at 2.2 s.
ANIMATIONS["spawn"] = dict(length=3.0, loop=False, bones={
    "root": {"position": track((0, [0, -124, 0]), (1.8, [0, -4, 0]), z(2.1), z(3.0), ease="easeOutCubic")},
    "torso": {"rotation": track((0, [30, 0, 0]), (1.8, [15, 0, 0]), (2.2, [-15, 0, 0]), (2.7, [-12, 0, 0]), z(3.0))},
    "head": {"rotation": track((0, [25, 0, 0]), (1.8, [10, 0, 0]), (2.2, [-30, 0, 0]), (2.7, [-28, 0, 0]), z(3.0))},
    "arm_right": {"rotation": track(z(0), (1.8, [-20, 0, 0]), (2.2, [-30, 0, 70]), (2.7, [-30, 0, 70]), z(3.0))},
    "arm_left": {"rotation": track(z(0), (1.8, [-20, 0, 0]), (2.2, [-30, 0, -70]), (2.7, [-30, 0, -70]), z(3.0))},
    "tentacles_right": {"rotation": track(z(0), (2.2, [-15, 0, 25]), (2.7, [-15, 0, 25]), z(3.0))},
    "tentacles_left": {"rotation": track(z(0), (2.2, [-15, 0, -25]), (2.7, [-15, 0, -25]), z(3.0))},
})
ANIMATIONS["death"] = dict(length=3.0, loop="hold_on_last_frame", bones={
    "body": {"position": track(z(0), (0.8, [0, -6, 0]), (1.8, [0, -22, 4]), (3.0, [0, -40, 8]))},
    "torso": {"rotation": track(z(0), (0.8, [-20, 0, 0]), (1.8, [40, 0, 10]), (3.0, [70, 0, 12]))},
    "head": {"rotation": track(z(0), (0.8, [-35, 0, 0]), (1.8, [30, 0, 0]), (3.0, [40, 10, 0]))},
    "arm_right": {"rotation": track(z(0), (0.8, [-40, 0, 60]), (1.8, [10, 0, 20]), (3.0, [20, 0, 10]))},
    "arm_left": {"rotation": track(z(0), (0.8, [-40, 0, -60]), (1.8, [10, 0, -20]), (3.0, [20, 0, -10]))},
    "leg_right": {"rotation": track(z(0), (1.8, [-50, 0, 0]), (3.0, [-80, 0, 0]))},
    "leg_left": {"rotation": track(z(0), (1.8, [-45, 0, 0]), (3.0, [-80, 0, 0]))},
    "tentacles_right": {"rotation": track(z(0), (0.8, [-20, 0, 30]), (3.0, [40, 0, -20]))},
    "tentacles_left": {"rotation": track(z(0), (0.8, [-20, 0, -30]), (3.0, [40, 0, 20]))},
    "mane": {"rotation": track(z(0), (3.0, [30, 0, 0]))},
    "tail_1": {"rotation": track(z(0), (3.0, [-25, 0, 0]))},
})
ANIMATIONS["hurt"] = dict(length=0.3, loop=False, bones={
    "torso": {"rotation": track(z(0), (0.08, [-5, 0, 0]), z(0.3))},
    "head": {"rotation": track(z(0), (0.08, [-8, 0, 0]), z(0.3))},
})
# Phase roar (PHASE_TRANSITION_TICKS = 50).
ANIMATIONS["roar"] = dict(length=2.5, loop=False, bones={
    "torso": {"rotation": track(z(0), (0.5, [-15, 0, 0]), (2.0, [-15, 0, 0]), z(2.5))},
    "head": {"rotation": track(z(0), (0.5, [-35, 0, 0]), (1.0, [-30, 5, 0]), (1.5, [-30, -5, 0]), (2.0, [-35, 0, 0]), z(2.5))},
    "arm_right": {"rotation": track(z(0), (0.5, [-30, 0, 75]), (2.0, [-30, 0, 75]), z(2.5))},
    "arm_left": {"rotation": track(z(0), (0.5, [-30, 0, -75]), (2.0, [-30, 0, -75]), z(2.5))},
    "hand_right": {"rotation": track(z(0), (0.5, [-25, 0, 0]), (2.0, [-25, 0, 0]), z(2.5))},
    "hand_left": {"rotation": track(z(0), (0.5, [-25, 0, 0]), (2.0, [-25, 0, 0]), z(2.5))},
    "tentacles_right": {"rotation": track(z(0), (0.5, [-20, 0, 30]), (1.25, [-10, 0, 20]), (2.0, [-20, 0, 30]), z(2.5))},
    "tentacles_left": {"rotation": track(z(0), (0.5, [-20, 0, -30]), (1.25, [-10, 0, -20]), (2.0, [-20, 0, -30]), z(2.5))},
    "mane": {"rotation": track(z(0), (0.5, [-25, 0, 0]), (2.0, [-25, 0, 0]), z(2.5))},
    "tail_1": {"rotation": track(z(0), (0.5, [20, 0, 0]), (2.0, [20, 0, 0]), z(2.5))},
})

# ---------------------------------------------------------------- attacks (HIT times match SovereignConfig *_TICK)
# Claw Swipe (normal). HIT 0.55 s (tick 11).
ANIMATIONS["claw_swipe"] = dict(length=1.2, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.35, [-120, 0, 30]), (0.55, [-45, -30, -25]), (0.75, [-35, -20, -20]), z(1.2))},
    "hand_right": {"rotation": track(z(0), (0.35, [-20, 0, 0]), (0.55, [10, 0, 0]), z(1.2))},
    "arm_left": {"rotation": track(z(0), (0.35, [-60, 0, -20]), (0.55, [-90, 20, 20]), (0.75, [-50, 10, 10]), z(1.2))},
    "torso": {"rotation": track(z(0), (0.35, [-6, 25, 0]), (0.55, [12, -25, 0]), (0.75, [8, -15, 0]), z(1.2))},
    "head": {"rotation": track(z(0), (0.35, [-8, 10, 0]), (0.55, [10, -10, 0]), z(1.2))},
    "leg_right": {"rotation": track(z(0), (0.55, [-12, 0, 0]), z(1.2))},
    "leg_left": {"rotation": track(z(0), (0.55, [8, 0, 0]), z(1.2))},
})
# Tentacle Lash (normal, all around). HIT 0.65 s (tick 13).
ANIMATIONS["tentacle_lash"] = dict(length=1.3, loop=False, bones={
    "tentacles_right": {"rotation": track(z(0), (0.45, [25, 30, -15]), (0.65, [-40, -45, 45]), (0.85, [-25, -30, 30]), z(1.3))},
    "tentacles_left": {"rotation": track(z(0), (0.45, [25, -30, 15]), (0.65, [-40, 45, -45]), (0.85, [-25, 30, -30]), z(1.3))},
    "mane": {"rotation": track(z(0), (0.45, [15, 0, 0]), (0.65, [-35, 0, 0]), z(1.3))},
    "tail_1": {"rotation": track(z(0), (0.45, [0, -35, 0]), (0.65, [10, 45, 0]), (0.9, [0, 20, 0]), z(1.3))},
    "tail_2": {"rotation": track(z(0), (0.5, [0, -30, 0]), (0.7, [0, 40, 0]), z(1.3))},
    "torso": {"rotation": track(z(0), (0.45, [0, -18, 0]), (0.65, [5, 22, 0]), z(1.3))},
    "head": {"rotation": track(z(0), (0.45, [-10, 0, 0]), (0.65, [5, 0, 0]), z(1.3))},
})
# Ground Pound: shockwave rings. HIT 0.9 s (tick 18).
ANIMATIONS["ground_pound"] = dict(length=1.8, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.6, [-170, 0, 10]), (0.9, [-25, 0, 0]), (1.3, [-20, 0, 0]), z(1.8))},
    "arm_left": {"rotation": track(z(0), (0.6, [-170, 0, -10]), (0.9, [-25, 0, 0]), (1.3, [-20, 0, 0]), z(1.8))},
    "hand_right": {"rotation": track(z(0), (0.6, [-30, 0, 0]), (0.9, [-40, 0, 0]), z(1.8))},
    "hand_left": {"rotation": track(z(0), (0.6, [-30, 0, 0]), (0.9, [-40, 0, 0]), z(1.8))},
    "torso": {"rotation": track(z(0), (0.6, [-18, 0, 0]), (0.9, [30, 0, 0]), (1.3, [26, 0, 0]), z(1.8))},
    "body": {"position": track(z(0), (0.6, [0, 3, 0]), (0.9, [0, -8, 2]), (1.3, [0, -7, 2]), z(1.8))},
    "leg_right": {"rotation": track(z(0), (0.9, [-25, 0, 0]), (1.3, [-25, 0, 0]), z(1.8))},
    "leg_left": {"rotation": track(z(0), (0.9, [20, 0, 0]), (1.3, [20, 0, 0]), z(1.8))},
    "head": {"rotation": track(z(0), (0.6, [-15, 0, 0]), (0.9, [15, 0, 0]), z(1.8))},
    "tentacles_right": {"rotation": track(z(0), (0.6, [-20, 0, 20]), (0.9, [20, 0, -10]), z(1.8))},
    "tentacles_left": {"rotation": track(z(0), (0.6, [-20, 0, -20]), (0.9, [20, 0, 10]), z(1.8))},
})
# Void Orbs: homing orbs released. HIT 0.8 s (tick 16).
ANIMATIONS["void_orbs"] = dict(length=1.6, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.5, [-70, 0, 45]), (0.8, [-100, 0, 10]), (1.1, [-90, 0, 10]), z(1.6))},
    "arm_left": {"rotation": track(z(0), (0.5, [-70, 0, -45]), (0.8, [-100, 0, -10]), (1.1, [-90, 0, -10]), z(1.6))},
    "hand_right": {"rotation": track(z(0), (0.5, [-10, 0, 0]), (0.8, [-30, 0, 0]), z(1.6))},
    "hand_left": {"rotation": track(z(0), (0.5, [-10, 0, 0]), (0.8, [-30, 0, 0]), z(1.6))},
    "torso": {"rotation": track(z(0), (0.5, [-10, 0, 0]), (0.8, [8, 0, 0]), z(1.6))},
    "head": {"rotation": track(z(0), (0.5, [-15, 0, 0]), (0.8, [5, 0, 0]), z(1.6))},
    "tentacles_right": {"rotation": track(z(0), (0.5, [20, 0, 20]), (0.8, [-35, 0, -10]), z(1.6))},
    "tentacles_left": {"rotation": track(z(0), (0.5, [20, 0, -20]), (0.8, [-35, 0, 10]), z(1.6))},
})
# Rift Eruption: claws into the ground at 0.9 s (tick 18); the rifts burst at tick 40.
ANIMATIONS["rift_eruption"] = dict(length=2.4, loop=False, bones={
    "body": {"position": track(z(0), (0.6, [0, 2, 0]), (0.9, [0, -12, 4]), (1.9, [0, -12, 4]), z(2.4))},
    "torso": {"rotation": track(z(0), (0.6, [-10, 0, 0]), (0.9, [40, 0, 0]), (1.9, [40, 0, 0]), z(2.4))},
    "arm_right": {"rotation": track(z(0), (0.6, [-150, 0, 20]), (0.9, [-40, 0, 5]), (1.9, [-40, 0, 5]), z(2.4))},
    "arm_left": {"rotation": track(z(0), (0.6, [-150, 0, -20]), (0.9, [-40, 0, -5]), (1.9, [-40, 0, -5]), z(2.4))},
    "hand_right": {"rotation": track(z(0), (0.9, [-35, 0, 0]), (1.9, [-35, 0, 0]), z(2.4))},
    "hand_left": {"rotation": track(z(0), (0.9, [-35, 0, 0]), (1.9, [-35, 0, 0]), z(2.4))},
    "leg_right": {"rotation": track(z(0), (0.9, [-40, 0, 0]), (1.9, [-40, 0, 0]), z(2.4))},
    "leg_left": {"rotation": track(z(0), (0.9, [-40, 0, 0]), (1.9, [-40, 0, 0]), z(2.4))},
    "head": {"rotation": track(z(0), (0.9, [-20, 0, 0]), (2.0, [-25, 0, 0]), z(2.4))},
    "tentacles_right": {"rotation": track(z(0), (0.9, [30, 0, 30]), (2.0, [30, 0, 30]), z(2.4))},
    "tentacles_left": {"rotation": track(z(0), (0.9, [30, 0, -30]), (2.0, [30, 0, -30]), z(2.4))},
})
# Void Grasp: tentacles seize at 0.6 s (tick 12), drag until 2.6 s (tick 52), claw slam at 2.8 s (tick 56).
ANIMATIONS["void_grasp"] = dict(length=3.3, loop=False, bones={
    "tentacles_right": {"rotation": track(z(0), (0.35, [20, 0, 20]), (0.6, [-70, -20, -10]), (1.6, [-45, -10, 0]), (2.6, [-20, 0, 10]), z(3.3))},
    "tentacles_left": {"rotation": track(z(0), (0.35, [20, 0, -20]), (0.6, [-70, 20, 10]), (1.6, [-45, 10, 0]), (2.6, [-20, 0, -10]), z(3.3))},
    "torso": {"rotation": track(z(0), (0.6, [10, 0, 0]), (2.0, [-8, 0, 0]), (2.6, [-10, 0, 0]), (2.8, [20, 0, 0]), z(3.3))},
    "arm_right": {"rotation": track(z(0), (0.6, [-40, 0, 20]), (2.6, [-160, 0, 15]), (2.8, [-30, 0, 0]), (3.0, [-25, 0, 0]), z(3.3))},
    "hand_right": {"rotation": track(z(0), (2.6, [-20, 0, 0]), (2.8, [5, 0, 0]), z(3.3))},
    "arm_left": {"rotation": track(z(0), (0.6, [-50, 0, -30]), (2.6, [-50, 0, -30]), z(3.3))},
    "head": {"rotation": track(z(0), (0.6, [10, 0, 0]), (2.6, [-10, 0, 0]), (2.8, [15, 0, 0]), z(3.3))},
})
# Black Hole: conjured at 1.0 s (tick 20), collapses at 5.0 s (tick 100).
ANIMATIONS["black_hole"] = dict(length=5.5, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (1.0, [-165, 0, 25]), (3.0, [-160, 0, 20]), (5.0, [-165, 0, 25]), (5.2, [-60, 0, 10]), z(5.5))},
    "arm_left": {"rotation": track(z(0), (1.0, [-165, 0, -25]), (3.0, [-160, 0, -20]), (5.0, [-165, 0, -25]), (5.2, [-60, 0, -10]), z(5.5))},
    "hand_right": {"rotation": track(z(0), (1.0, [-40, 0, 0]), (5.0, [-40, 0, 0]), z(5.5))},
    "hand_left": {"rotation": track(z(0), (1.0, [-40, 0, 0]), (5.0, [-40, 0, 0]), z(5.5))},
    "torso": {"rotation": track(z(0), (1.0, [-12, 0, 0]), (5.0, [-12, 0, 0]), (5.2, [15, 0, 0]), z(5.5))},
    "head": {"rotation": track(z(0), (1.0, [-30, 0, 0]), (5.0, [-30, 0, 0]), (5.2, [10, 0, 0]), z(5.5))},
    "tentacles_right": {"rotation": track(z(0), (1.0, [-30, 0, 35]), (2.0, [-20, 0, 25]), (3.0, [-30, 0, 35]), (4.0, [-20, 0, 25]), (5.0, [-30, 0, 35]), z(5.5))},
    "tentacles_left": {"rotation": track(z(0), (1.0, [-30, 0, -35]), (2.0, [-20, 0, -25]), (3.0, [-30, 0, -35]), (4.0, [-20, 0, -25]), (5.0, [-30, 0, -35]), z(5.5))},
    "mane": {"rotation": track(z(0), (1.0, [-20, 0, 0]), (5.0, [-20, 0, 0]), z(5.5))},
})
# Void Step: sinks at 0.7 s (tick 14, teleports), reforms, claw slam at 1.2 s (tick 24).
ANIMATIONS["void_step"] = dict(length=1.8, loop=False, bones={
    "root": {"scale": track(one(0), (0.6, [0.2, 0.2, 0.2]), (0.7, [0.05, 0.05, 0.05]), (0.9, [1, 1, 1]), one(1.8)),
             "position": track(z(0), (0.7, [0, -20, 0]), z(0.9), z(1.8))},
    "arm_right": {"rotation": track(z(0), (0.9, [-150, 0, 10]), (1.2, [-30, 0, 0]), (1.4, [-25, 0, 0]), z(1.8))},
    "arm_left": {"rotation": track(z(0), (0.9, [-150, 0, -10]), (1.2, [-30, 0, 0]), (1.4, [-25, 0, 0]), z(1.8))},
    "torso": {"rotation": track(z(0), (0.9, [-15, 0, 0]), (1.2, [25, 0, 0]), z(1.8))},
})
# Eclipse (ultimate): arms raised from 1.2 s; meteors fall from 1.5 s (tick 30) to 6.5 s (tick 130).
ANIMATIONS["eclipse"] = dict(length=7.5, loop=False, bones={
    "body": {"position": track(z(0), (1.2, [0, 4, 0]), (6.5, [0, 4, 0]), z(7.5))},
    "arm_right": {"rotation": track(z(0), (1.2, [-170, 0, 40]), (4.0, [-165, 0, 35]), (6.5, [-170, 0, 40]), z(7.5))},
    "arm_left": {"rotation": track(z(0), (1.2, [-170, 0, -40]), (4.0, [-165, 0, -35]), (6.5, [-170, 0, -40]), z(7.5))},
    "hand_right": {"rotation": track(z(0), (1.2, [-30, 0, 0]), (6.5, [-30, 0, 0]), z(7.5))},
    "hand_left": {"rotation": track(z(0), (1.2, [-30, 0, 0]), (6.5, [-30, 0, 0]), z(7.5))},
    "torso": {"rotation": track(z(0), (1.2, [-20, 0, 0]), (6.5, [-20, 0, 0]), z(7.5))},
    "head": {"rotation": track(z(0), (1.2, [-45, 0, 0]), (6.5, [-45, 0, 0]), z(7.5))},
    "tentacles_right": {"rotation": track(z(0), (1.2, [-35, 0, 45]), (3.0, [-25, 0, 35]), (5.0, [-35, 0, 45]), (6.5, [-35, 0, 45]), z(7.5))},
    "tentacles_left": {"rotation": track(z(0), (1.2, [-35, 0, -45]), (3.0, [-25, 0, -35]), (5.0, [-35, 0, -45]), (6.5, [-35, 0, -45]), z(7.5))},
    "mane": {"rotation": track(z(0), (1.2, [-30, 0, 0]), (6.5, [-30, 0, 0]), z(7.5))},
    "tail_1": {"rotation": track(z(0), (1.2, [25, 0, 0]), (6.5, [25, 0, 0]), z(7.5))},
})
# Soul Drain: channel from 0.8 s (tick 16) to 3.8 s (tick 76).
ANIMATIONS["soul_drain"] = dict(length=4.3, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.8, [-75, 0, 50]), (3.8, [-75, 0, 50]), z(4.3))},
    "arm_left": {"rotation": track(z(0), (0.8, [-75, 0, -50]), (3.8, [-75, 0, -50]), z(4.3))},
    "hand_right": {"rotation": track(z(0), (0.8, [-45, 0, 0]), (2.3, [-30, 0, 0]), (3.8, [-45, 0, 0]), z(4.3))},
    "hand_left": {"rotation": track(z(0), (0.8, [-45, 0, 0]), (2.3, [-30, 0, 0]), (3.8, [-45, 0, 0]), z(4.3))},
    "torso": {"rotation": track(z(0), (0.8, [-15, 0, 0]), (3.8, [-15, 0, 0]), z(4.3))},
    "head": {"rotation": track(z(0), (0.8, [-30, 0, 0]), (3.8, [-30, 0, 0]), z(4.3))},
    "tentacles_right": {"rotation": track(z(0), (0.8, [-50, 0, 40]), (2.3, [-40, 0, 30]), (3.8, [-50, 0, 40]), z(4.3))},
    "tentacles_left": {"rotation": track(z(0), (0.8, [-50, 0, -40]), (2.3, [-40, 0, -30]), (3.8, [-50, 0, -40]), z(4.3))},
})
# Summon: calls the shard legion. HIT 1.0 s (tick 20).
ANIMATIONS["summon"] = dict(length=2.0, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.7, [-165, 0, 15]), (1.0, [-60, 0, 10]), (1.4, [-55, 0, 10]), z(2.0))},
    "hand_right": {"rotation": track(z(0), (0.7, [-20, 0, 0]), (1.0, [10, 0, 0]), z(2.0))},
    "arm_left": {"rotation": track(z(0), (0.7, [-40, 0, -40]), (1.4, [-40, 0, -40]), z(2.0))},
    "head": {"rotation": track(z(0), (0.7, [-25, 0, 0]), (1.0, [10, 0, 0]), z(2.0))},
    "tentacles_right": {"rotation": track(z(0), (0.7, [-25, 0, 30]), (1.0, [10, 0, 0]), z(2.0))},
    "tentacles_left": {"rotation": track(z(0), (0.7, [-25, 0, -30]), (1.0, [10, 0, 0]), z(2.0))},
})
# Void Shield: raised at 1.0 s (tick 20) when a pylon phase begins.
ANIMATIONS["shield"] = dict(length=2.5, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (1.0, [-85, 0, -35]), (2.0, [-85, 0, -35]), z(2.5))},
    "arm_left": {"rotation": track(z(0), (1.0, [-85, 0, 35]), (2.0, [-85, 0, 35]), z(2.5))},
    "hand_right": {"rotation": track(z(0), (1.0, [-50, 0, 0]), (2.0, [-50, 0, 0]), z(2.5))},
    "hand_left": {"rotation": track(z(0), (1.0, [-50, 0, 0]), (2.0, [-50, 0, 0]), z(2.5))},
    "head": {"rotation": track(z(0), (1.0, [20, 0, 0]), (2.0, [20, 0, 0]), z(2.5))},
    "torso": {"rotation": track(z(0), (1.0, [10, 0, 0]), (2.0, [10, 0, 0]), z(2.5))},
    "tentacles_right": {"rotation": track(z(0), (1.0, [-55, 0, -20]), (2.0, [-55, 0, -20]), z(2.5))},
    "tentacles_left": {"rotation": track(z(0), (1.0, [-55, 0, 20]), (2.0, [-55, 0, 20]), z(2.5))},
})

# ---------------------------------------------------------------- phases 1-10 additions (counters to aerial smashes + new patterns)
# Skyward Lash: the tentacles snap straight up. HIT 0.3 s (tick 6).
ANIMATIONS["skyward_lash"] = dict(length=1.1, loop=False, bones={
    "tentacles_right": {"rotation": track(z(0), (0.15, [30, 0, 15]), (0.3, [-115, 0, 50]), (0.55, [-95, 0, 40]), z(1.1))},
    "tentacles_left": {"rotation": track(z(0), (0.15, [30, 0, -15]), (0.3, [-115, 0, -50]), (0.55, [-95, 0, -40]), z(1.1))},
    "arm_right": {"rotation": track(z(0), (0.15, [-20, 0, 10]), (0.3, [-150, 0, 35]), (0.6, [-120, 0, 30]), z(1.1))},
    "arm_left": {"rotation": track(z(0), (0.15, [-20, 0, -10]), (0.3, [-150, 0, -35]), (0.6, [-120, 0, -30]), z(1.1))},
    "torso": {"rotation": track(z(0), (0.15, [10, 0, 0]), (0.3, [-18, 0, 0]), z(1.1))},
    "head": {"rotation": track(z(0), (0.15, [10, 0, 0]), (0.3, [-35, 0, 0]), (0.6, [-25, 0, 0]), z(1.1))},
    "mane": {"rotation": track(z(0), (0.3, [-30, 0, 0]), z(1.1))},
})
# Gravity Well: palms pressed down on the world from 0.8 s (tick 16) to 6.8 s (tick 136).
ANIMATIONS["gravity_well"] = dict(length=7.3, loop=False, bones={
    "body": {"position": track(z(0), (0.8, [0, -5, 0]), (6.8, [0, -5, 0]), z(7.3))},
    "torso": {"rotation": track(z(0), (0.8, [18, 0, 0]), (3.8, [14, 0, 0]), (6.8, [18, 0, 0]), z(7.3))},
    "arm_right": {"rotation": track(z(0), (0.5, [-120, 0, 25]), (0.8, [-70, 0, 30]), (6.8, [-70, 0, 30]), z(7.3))},
    "arm_left": {"rotation": track(z(0), (0.5, [-120, 0, -25]), (0.8, [-70, 0, -30]), (6.8, [-70, 0, -30]), z(7.3))},
    "hand_right": {"rotation": track(z(0), (0.8, [45, 0, 0]), (6.8, [45, 0, 0]), z(7.3))},
    "hand_left": {"rotation": track(z(0), (0.8, [45, 0, 0]), (6.8, [45, 0, 0]), z(7.3))},
    "tentacles_right": {"rotation": track(z(0), (0.8, [-30, 0, 60]), (2.3, [-20, 0, 50]), (3.8, [-30, 0, 60]), (5.3, [-20, 0, 50]), (6.8, [-30, 0, 60]), z(7.3))},
    "tentacles_left": {"rotation": track(z(0), (0.8, [-30, 0, -60]), (2.3, [-20, 0, -50]), (3.8, [-30, 0, -60]), (5.3, [-20, 0, -50]), (6.8, [-30, 0, -60]), z(7.3))},
    "head": {"rotation": track(z(0), (0.8, [20, 0, 0]), (6.8, [20, 0, 0]), z(7.3))},
    "leg_right": {"rotation": track(z(0), (0.8, [-20, 0, 0]), (6.8, [-20, 0, 0]), z(7.3))},
    "leg_left": {"rotation": track(z(0), (0.8, [15, 0, 0]), (6.8, [15, 0, 0]), z(7.3))},
})
# Abyssal Carapace: curled behind crossed arms from 0.6 s (tick 12), bursts out at 4.6 s (tick 92).
ANIMATIONS["abyssal_carapace"] = dict(length=5.0, loop=False, bones={
    "body": {"position": track(z(0), (0.6, [0, -7, 0]), (4.6, [0, -7, 0]), (4.75, [0, 2, 0]), z(5.0))},
    "torso": {"rotation": track(z(0), (0.6, [24, 0, 0]), (4.6, [24, 0, 0]), (4.75, [-15, 0, 0]), z(5.0))},
    "arm_right": {"rotation": track(z(0), (0.6, [-75, 0, -45]), (4.6, [-75, 0, -45]), (4.75, [-60, 0, 70]), z(5.0))},
    "arm_left": {"rotation": track(z(0), (0.6, [-75, 0, 45]), (4.6, [-75, 0, 45]), (4.75, [-60, 0, -70]), z(5.0))},
    "hand_right": {"rotation": track(z(0), (0.6, [-40, 0, 0]), (4.6, [-40, 0, 0]), z(5.0))},
    "hand_left": {"rotation": track(z(0), (0.6, [-40, 0, 0]), (4.6, [-40, 0, 0]), z(5.0))},
    "tentacles_right": {"rotation": track(z(0), (0.6, [45, 0, -35]), (4.6, [45, 0, -35]), (4.75, [-80, 0, 60]), z(5.0))},
    "tentacles_left": {"rotation": track(z(0), (0.6, [45, 0, 35]), (4.6, [45, 0, 35]), (4.75, [-80, 0, -60]), z(5.0))},
    "head": {"rotation": track(z(0), (0.6, [30, 0, 0]), (4.6, [30, 0, 0]), (4.75, [-30, 0, 0]), z(5.0))},
    "mane": {"rotation": track(z(0), (0.6, [-35, 0, 0]), (4.6, [-35, 0, 0]), z(5.0))},
    "tail_1": {"rotation": track(z(0), (0.6, [-20, 30, 0]), (4.6, [-20, 30, 0]), z(5.0))},
})
# Soul Chains: arms flung out at 0.8 s (tick 16), fists clenched, pulling until 5.8 s (tick 116).
ANIMATIONS["soul_chains"] = dict(length=6.3, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.5, [-40, 0, 20]), (0.8, [-70, 0, 75]), (3.3, [-60, 0, 60]), (5.8, [-70, 0, 75]), z(6.3))},
    "arm_left": {"rotation": track(z(0), (0.5, [-40, 0, -20]), (0.8, [-70, 0, -75]), (3.3, [-60, 0, -60]), (5.8, [-70, 0, -75]), z(6.3))},
    "hand_right": {"rotation": track(z(0), (0.8, [-50, 0, 0]), (5.8, [-50, 0, 0]), z(6.3))},
    "hand_left": {"rotation": track(z(0), (0.8, [-50, 0, 0]), (5.8, [-50, 0, 0]), z(6.3))},
    "torso": {"rotation": track(z(0), (0.8, [-12, 0, 0]), (5.8, [-12, 0, 0]), z(6.3))},
    "head": {"rotation": track(z(0), (0.8, [-15, 0, 0]), (3.3, [-10, 0, 0]), (5.8, [-15, 0, 0]), z(6.3))},
    "tentacles_right": {"rotation": track(z(0), (0.8, [25, 0, 45]), (5.8, [25, 0, 45]), z(6.3))},
    "tentacles_left": {"rotation": track(z(0), (0.8, [25, 0, -45]), (5.8, [25, 0, -45]), z(6.3))},
    "leg_right": {"rotation": track(z(0), (0.8, [-15, 0, 0]), (5.8, [-15, 0, 0]), z(6.3))},
    "leg_left": {"rotation": track(z(0), (0.8, [12, 0, 0]), (5.8, [12, 0, 0]), z(6.3))},
})
# Null Sweep: gathers the blade until 1.2 s (tick 24), sweeps it until 4.2 s (tick 84).
ANIMATIONS["null_sweep"] = dict(length=4.8, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (1.2, [-80, 0, 25]), (4.2, [-80, 0, 25]), z(4.8))},
    "hand_right": {"rotation": track(z(0), (1.2, [25, 0, 0]), (4.2, [25, 0, 0]), z(4.8))},
    "arm_left": {"rotation": track(z(0), (1.2, [-30, 0, -35]), (4.2, [-30, 0, -35]), z(4.8))},
    "torso": {"rotation": track(z(0), (1.2, [15, -30, 0]), (2.2, [15, 25, 0]), (3.2, [15, -25, 0]), (4.2, [15, 30, 0]), z(4.8))},
    "body": {"position": track(z(0), (1.2, [0, -6, 0]), (4.2, [0, -6, 0]), z(4.8))},
    "head": {"rotation": track(z(0), (1.2, [15, 0, 0]), (4.2, [15, 0, 0]), z(4.8))},
    "tentacles_right": {"rotation": track(z(0), (1.2, [30, 0, 60]), (4.2, [30, 0, 60]), z(4.8))},
    "tentacles_left": {"rotation": track(z(0), (1.2, [30, 0, -60]), (4.2, [30, 0, -60]), z(4.8))},
    "leg_right": {"rotation": track(z(0), (1.2, [-25, 0, 0]), (4.2, [-25, 0, 0]), z(4.8))},
    "leg_left": {"rotation": track(z(0), (1.2, [20, 0, 0]), (4.2, [20, 0, 0]), z(4.8))},
})
# Tentacle Field: both claws driven into the ground at 1.5 s; eruptions at 1.8, 2.8 and 3.8 s (ticks 36, 56, 76).
ANIMATIONS["tentacle_field"] = dict(length=4.5, loop=False, bones={
    "body": {"position": track(z(0), (1.2, [0, 2, 0]), (1.5, [0, -12, 4]), (1.8, [0, -10, 4]), (2.8, [0, -12, 4]), (3.8, [0, -10, 4]), z(4.5))},
    "torso": {"rotation": track(z(0), (1.2, [-10, 0, 0]), (1.5, [40, 0, 0]), (1.8, [34, 0, 0]), (2.8, [40, 0, 0]), (3.8, [34, 0, 0]), z(4.5))},
    "arm_right": {"rotation": track(z(0), (1.2, [-150, 0, 20]), (1.5, [-40, 0, 5]), (3.9, [-40, 0, 5]), z(4.5))},
    "arm_left": {"rotation": track(z(0), (1.2, [-150, 0, -20]), (1.5, [-40, 0, -5]), (3.9, [-40, 0, -5]), z(4.5))},
    "hand_right": {"rotation": track(z(0), (1.5, [-35, 0, 0]), (3.9, [-35, 0, 0]), z(4.5))},
    "hand_left": {"rotation": track(z(0), (1.5, [-35, 0, 0]), (3.9, [-35, 0, 0]), z(4.5))},
    "tentacles_right": {"rotation": track(z(0), (1.8, [-40, 0, 45]), (2.3, [20, 0, 20]), (2.8, [-40, 0, 45]), (3.3, [20, 0, 20]), (3.8, [-40, 0, 45]), z(4.5))},
    "tentacles_left": {"rotation": track(z(0), (1.8, [-40, 0, -45]), (2.3, [20, 0, -20]), (2.8, [-40, 0, -45]), (3.3, [20, 0, -20]), (3.8, [-40, 0, -45]), z(4.5))},
    "leg_right": {"rotation": track(z(0), (1.5, [-40, 0, 0]), (3.9, [-40, 0, 0]), z(4.5))},
    "leg_left": {"rotation": track(z(0), (1.5, [-40, 0, 0]), (3.9, [-40, 0, 0]), z(4.5))},
    "head": {"rotation": track(z(0), (1.5, [-25, 0, 0]), (3.9, [-25, 0, 0]), z(4.5))},
})
# Dimension Rend: aims until 0.7 s, the claw rises and tears down at 1.2 s (tick 24).
ANIMATIONS["dimension_rend"] = dict(length=4.7, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.9, [-178, 0, 15]), (1.2, [-25, 0, -35]), (2.5, [-25, 0, -30]), z(4.7))},
    "hand_right": {"rotation": track(z(0), (0.9, [-25, 0, 0]), (1.2, [15, 0, 0]), z(4.7))},
    "arm_left": {"rotation": track(z(0), (0.9, [-40, 0, -35]), (1.2, [-60, 0, -20]), z(4.7))},
    "torso": {"rotation": track(z(0), (0.9, [-15, 25, 0]), (1.2, [25, -25, 0]), (2.5, [20, -20, 0]), z(4.7))},
    "head": {"rotation": track(z(0), (0.9, [-15, 10, 0]), (1.2, [15, -10, 0]), z(4.7))},
    "body": {"position": track(z(0), (1.2, [0, -6, 3]), (2.5, [0, -5, 3]), z(4.7))},
    "leg_right": {"rotation": track(z(0), (1.2, [-30, 0, 0]), (2.5, [-30, 0, 0]), z(4.7))},
    "leg_left": {"rotation": track(z(0), (1.2, [20, 0, 0]), (2.5, [20, 0, 0]), z(4.7))},
})
# Abyssal Spears: calls them down from 0.8 s (tick 16); an arm pump for each of the five volleys (0.6 s apart).
ANIMATIONS["abyssal_spears"] = dict(length=4.8, loop=False, bones={
    "arm_right": {"rotation": track(z(0), (0.8, [-165, 0, 20]), (1.1, [-120, 0, 20]), (1.4, [-165, 0, 20]), (1.7, [-120, 0, 20]), (2.0, [-165, 0, 20]),
                                    (2.3, [-120, 0, 20]), (2.6, [-165, 0, 20]), (2.9, [-120, 0, 20]), (3.2, [-165, 0, 20]), (3.5, [-120, 0, 20]), z(4.8))},
    "arm_left": {"rotation": track(z(0), (0.8, [-165, 0, -20]), (1.1, [-120, 0, -20]), (1.4, [-165, 0, -20]), (1.7, [-120, 0, -20]), (2.0, [-165, 0, -20]),
                                   (2.3, [-120, 0, -20]), (2.6, [-165, 0, -20]), (2.9, [-120, 0, -20]), (3.2, [-165, 0, -20]), (3.5, [-120, 0, -20]), z(4.8))},
    "torso": {"rotation": track(z(0), (0.8, [-15, 0, 0]), (3.5, [-15, 0, 0]), z(4.8))},
    "head": {"rotation": track(z(0), (0.8, [-40, 0, 0]), (3.5, [-40, 0, 0]), z(4.8))},
    "tentacles_right": {"rotation": track(z(0), (0.8, [-60, 0, 30]), (3.5, [-60, 0, 30]), z(4.8))},
    "tentacles_left": {"rotation": track(z(0), (0.8, [-60, 0, -30]), (3.5, [-60, 0, -30]), z(4.8))},
})
# Oblivion (ultimate): arms spread wide and slowly close overhead while it rises (1.0 s .. 6.0 s), released at 6.0 s (tick 120).
ANIMATIONS["oblivion"] = dict(length=7.0, loop=False, bones={
    "body": {"position": track(z(0), (1.0, [0, 3, 0]), (5.8, [0, 8, 0]), (6.0, [0, -6, 0]), (6.4, [0, -4, 0]), z(7.0))},
    "arm_right": {"rotation": track(z(0), (1.0, [-110, 0, 75]), (5.8, [-175, 0, 20]), (6.0, [-30, 0, 85]), (6.4, [-30, 0, 80]), z(7.0))},
    "arm_left": {"rotation": track(z(0), (1.0, [-110, 0, -75]), (5.8, [-175, 0, -20]), (6.0, [-30, 0, -85]), (6.4, [-30, 0, -80]), z(7.0))},
    "hand_right": {"rotation": track(z(0), (1.0, [-30, 0, 0]), (5.8, [-45, 0, 0]), z(7.0))},
    "hand_left": {"rotation": track(z(0), (1.0, [-30, 0, 0]), (5.8, [-45, 0, 0]), z(7.0))},
    "torso": {"rotation": track(z(0), (1.0, [-20, 0, 0]), (5.8, [-25, 0, 0]), (6.0, [30, 0, 0]), (6.4, [25, 0, 0]), z(7.0))},
    "head": {"rotation": track(z(0), (1.0, [-40, 0, 0]), (5.8, [-50, 0, 0]), (6.0, [20, 0, 0]), z(7.0))},
    "tentacles_right": {"rotation": track(z(0), (1.0, [-50, 0, 60]), (3.4, [-40, 0, 45]), (5.8, [-70, 0, 60]), (6.0, [10, 0, 80]), z(7.0))},
    "tentacles_left": {"rotation": track(z(0), (1.0, [-50, 0, -60]), (3.4, [-40, 0, -45]), (5.8, [-70, 0, -60]), (6.0, [10, 0, -80]), z(7.0))},
    "mane": {"rotation": track(z(0), (1.0, [-30, 0, 0]), (5.8, [-40, 0, 0]), (6.0, [20, 0, 0]), z(7.0))},
    "tail_1": {"rotation": track(z(0), (1.0, [25, 0, 0]), (5.8, [30, 0, 0]), z(7.0))},
})

TEXTURES = {
    "void_sovereign": dict(parts={"head": (0.070, 0.030, 0.060), "tentacles_right": (0.085, 0.020, 0.060),
                                  "tentacles_left": (0.085, 0.020, 0.060), "hand_right": (0.050, 0.020, 0.040),
                                  "hand_left": (0.050, 0.020, 0.040), "mane": (0.060, 0.020, 0.050)},
                           default=(0.032, 0.020, 0.048), crystal=(0.72, 0.07, 0.30), glow=(1.00, 0.18, 0.42),
                           crystal_chance=0.10, crystal_chance_high=0.25, high_y=100,
                           bone_crystal_chance={"head": 0.55, "hand_right": 0.35, "hand_left": 0.35, "tentacles_right": 0.25,
                                                "tentacles_left": 0.25}, veins=0.45, seed=77),
}
