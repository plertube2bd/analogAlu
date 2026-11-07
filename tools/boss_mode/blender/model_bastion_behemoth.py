"""Bastion Behemoth (tank boss) - BOSS3.obj. A rock-armoured quadruped: a blocky head with a heavy jaw, four pillar
legs, crystal-studded armour plates and back spikes, and a tail that curls to its left (+X).

OBJ: 2,314 vertices / 3,873 triangles / 188 small fragments (all attached whole), 109 x 90 x 137 units, Y up, facing +Z,
feet on Y = 0.
"""
from anim_common import track, z, one, SMOOTH

OBJ = "BOSS3.obj"
ENTITY = "bastion_behemoth"
SPLIT_THRESHOLD = 10 ** 9

BONES = [
    ("root",             None,     (0, 0, 0),        None),
    ("body",             "root",   (0, 38, -5),      (0, 48, 25)),
    ("crest",            "body",   (0, 62, 22),      (0, 80, 30)),
    ("head",             "body",   (-2, 50, 40),     (-2, 50, 64)),
    ("jaw",              "head",   (-2, 42, 48),     (-2, 36, 66)),
    ("leg_front_left",   "body",   (32, 46, 30),     (38, 3, 45)),
    ("leg_front_right",  "body",   (-32, 46, 30),    (-38, 3, 48)),
    ("leg_back_left",    "body",   (24, 40, -24),    (26, 3, -28)),
    ("leg_back_right",   "body",   (-34, 40, -24),   (-44, 3, -28)),
    ("tail_1",           "body",   (4, 34, -36),     (20, 32, -52)),
    ("tail_2",           "tail_1", (20, 32, -52),    (42, 26, -66)),
]

SEGMENTS = {
    "body":            ((0, 40, -20), (0, 52, 26)),
    "crest":           ((-36, 68, 26), (36, 68, 26)),
    "head":            ((-2, 50, 44), (-2, 50, 64)),
    "jaw":             ((-2, 42, 50), (-2, 36, 66)),
    "leg_front_left":  ((32, 42, 32), (38, 3, 45)),
    "leg_front_right": ((-32, 42, 32), (-38, 3, 48)),
    "leg_back_left":   ((24, 36, -24), (26, 3, -28)),
    "leg_back_right":  ((-34, 36, -24), (-44, 3, -28)),
    "tail_1":          ((4, 32, -38), (20, 32, -52)),
    "tail_2":          ((20, 30, -54), (42, 26, -66)),
}

ANIMATIONS = {}

# conventions (anim_common): legs +x swing the foot backward; body / head / jaw (forward bones) +x pitch down;
# tail +x lifts the tip; z on the left legs (+X) negative = outward.

FL, FR, BL, BR = "leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"

# ---------------------------------------------------------------- locomotion (looping)
ANIMATIONS["idle"] = dict(length=3.0, loop=True, bones={
    "body": {"position": track(z(0), (1.5, [0, -0.8, 0]), z(3.0)), "rotation": track(z(0), (1.5, [1.5, 0, 0]), z(3.0))},
    "head": {"rotation": track(z(0), (0.8, [2, 5, 0]), (2.2, [2, -5, 0]), z(3.0))},
    "jaw": {"rotation": track(z(0), (1.5, [6, 0, 0]), z(3.0))},
    "tail_1": {"rotation": track(z(0), (1.5, [0, 8, 0]), z(3.0))},
    "tail_2": {"rotation": track(z(0), (1.5, [0, -10, 0]), z(3.0))},
})
ANIMATIONS["walk"] = dict(length=1.6, loop=True, bones={
    FL: {"rotation": track((0, [-18, 0, 0]), (0.8, [18, 0, 0]), (1.6, [-18, 0, 0]))},
    BR: {"rotation": track((0, [-18, 0, 0]), (0.8, [18, 0, 0]), (1.6, [-18, 0, 0]))},
    FR: {"rotation": track((0, [18, 0, 0]), (0.8, [-18, 0, 0]), (1.6, [18, 0, 0]))},
    BL: {"rotation": track((0, [18, 0, 0]), (0.8, [-18, 0, 0]), (1.6, [18, 0, 0]))},
    "body": {"position": track(z(0), (0.4, [0, 1.5, 0]), z(0.8), (1.2, [0, 1.5, 0]), z(1.6)),
             "rotation": track((0, [0, 0, -3]), (0.8, [0, 0, 3]), (1.6, [0, 0, -3]))},
    "head": {"rotation": track((0, [3, -4, 0]), (0.8, [3, 4, 0]), (1.6, [3, -4, 0]))},
    "tail_1": {"rotation": track((0, [0, 10, 0]), (0.8, [0, -10, 0]), (1.6, [0, 10, 0]))},
    "tail_2": {"rotation": track(z(0), (0.4, [0, 12, 0]), (1.2, [0, -12, 0]), z(1.6))},
})

# ---------------------------------------------------------------- lifecycle
# Spawn (SPAWN_INTRO_TICKS = 60): heaves itself out of the ground, roars at 2.2 s.
ANIMATIONS["spawn"] = dict(length=3.0, loop=False, bones={
    "root": {"position": track((0, [0, -92, 0]), (1.8, [0, -3, 0]), z(2.1), z(3.0), ease="easeOutCubic")},
    "head": {"rotation": track((0, [25, 0, 0]), (1.8, [10, 0, 0]), (2.2, [-30, 0, 0]), (2.7, [-28, 0, 0]), z(3.0))},
    "jaw": {"rotation": track(z(0), (1.8, [5, 0, 0]), (2.2, [35, 0, 0]), (2.7, [35, 0, 0]), z(3.0))},
    "body": {"rotation": track((0, [15, 0, 0]), (1.8, [5, 0, 0]), (2.2, [-10, 0, 0]), (2.7, [-8, 0, 0]), z(3.0))},
})
ANIMATIONS["death"] = dict(length=3.0, loop="hold_on_last_frame", bones={
    "body": {"position": track(z(0), (0.8, [0, -4, 0]), (1.8, [0, -16, 0]), (3.0, [0, -24, 0])),
             "rotation": track(z(0), (0.8, [-10, 0, 0]), (1.8, [10, 0, 12]), (3.0, [12, 0, 18]))},
    FL: {"rotation": track(z(0), (1.8, [-40, 0, -30]), (3.0, [-55, 0, -40]))},
    FR: {"rotation": track(z(0), (1.8, [-40, 0, 30]), (3.0, [-55, 0, 40]))},
    BL: {"rotation": track(z(0), (1.8, [40, 0, -25]), (3.0, [55, 0, -35]))},
    BR: {"rotation": track(z(0), (1.8, [40, 0, 25]), (3.0, [55, 0, 35]))},
    "head": {"rotation": track(z(0), (0.8, [-30, 0, 0]), (1.8, [25, 0, 0]), (3.0, [35, 10, 0]))},
    "jaw": {"rotation": track(z(0), (0.8, [35, 0, 0]), (3.0, [25, 0, 0]))},
    "tail_1": {"rotation": track(z(0), (3.0, [-20, 20, 0]))},
})
ANIMATIONS["hurt"] = dict(length=0.3, loop=False, bones={
    "body": {"rotation": track(z(0), (0.08, [-3, 0, 0]), z(0.3))},
    "head": {"rotation": track(z(0), (0.08, [-8, 0, 0]), z(0.3))},
})
ANIMATIONS["roar"] = dict(length=2.5, loop=False, bones={
    "body": {"rotation": track(z(0), (0.5, [-12, 0, 0]), (2.0, [-12, 0, 0]), z(2.5))},
    "head": {"rotation": track(z(0), (0.5, [-35, 0, 0]), (1.0, [-32, 6, 0]), (1.5, [-32, -6, 0]), (2.0, [-35, 0, 0]), z(2.5))},
    "jaw": {"rotation": track(z(0), (0.5, [40, 0, 0]), (2.0, [40, 0, 0]), z(2.5))},
    "crest": {"rotation": track(z(0), (0.5, [-8, 0, 0]), (2.0, [-8, 0, 0]), z(2.5))},
    FL: {"rotation": track(z(0), (0.5, [-15, 0, 0]), (2.0, [-15, 0, 0]), z(2.5))},
    FR: {"rotation": track(z(0), (0.5, [-15, 0, 0]), (2.0, [-15, 0, 0]), z(2.5))},
})

# ---------------------------------------------------------------- attacks (HIT times match BastionConfig *_TICK)
# Maul (normal): the right forefoot comes down. HIT 0.6 s (tick 12).
ANIMATIONS["maul"] = dict(length=1.2, loop=False, bones={
    FR: {"rotation": track(z(0), (0.4, [-85, 0, 10]), (0.6, [12, 0, 0]), (0.8, [8, 0, 0]), z(1.2))},
    "body": {"rotation": track(z(0), (0.4, [-8, -10, 0]), (0.6, [8, 5, 0]), z(1.2)), "position": track(z(0), (0.6, [0, -2, 4]), z(1.2))},
    "head": {"rotation": track(z(0), (0.4, [-10, 0, 0]), (0.6, [15, 0, 0]), z(1.2))},
    "jaw": {"rotation": track(z(0), (0.4, [20, 0, 0]), (0.6, [5, 0, 0]), z(1.2))},
})
# Tail Sweep (normal). HIT 0.65 s (tick 13).
ANIMATIONS["tail_sweep"] = dict(length=1.3, loop=False, bones={
    "body": {"rotation": track(z(0), (0.4, [0, 20, 0]), (0.65, [0, -28, 0]), (0.9, [0, -15, 0]), z(1.3))},
    "tail_1": {"rotation": track(z(0), (0.4, [5, -50, 0]), (0.65, [0, 60, 0]), (0.9, [0, 30, 0]), z(1.3))},
    "tail_2": {"rotation": track(z(0), (0.45, [0, -40, 0]), (0.7, [0, 50, 0]), z(1.3))},
    "head": {"rotation": track(z(0), (0.65, [0, 15, 0]), z(1.3))},
})
# Earthquake: rears up and stamps. HIT 1.0 s (tick 20).
ANIMATIONS["earthquake"] = dict(length=2.2, loop=False, bones={
    "body": {"rotation": track(z(0), (0.7, [-28, 0, 0]), (1.0, [8, 0, 0]), (1.4, [4, 0, 0]), z(2.2)),
             "position": track(z(0), (0.7, [0, 6, 0]), (1.0, [0, -4, 0]), (1.4, [0, -3, 0]), z(2.2))},
    FL: {"rotation": track(z(0), (0.7, [-50, 0, 0]), (1.0, [12, 0, 0]), z(2.2))},
    FR: {"rotation": track(z(0), (0.7, [-50, 0, 0]), (1.0, [12, 0, 0]), z(2.2))},
    BL: {"rotation": track(z(0), (0.7, [25, 0, 0]), (1.0, [0, 0, 0]), z(2.2))},
    BR: {"rotation": track(z(0), (0.7, [25, 0, 0]), (1.0, [0, 0, 0]), z(2.2))},
    "head": {"rotation": track(z(0), (0.7, [-20, 0, 0]), (1.0, [20, 0, 0]), z(2.2))},
    "jaw": {"rotation": track(z(0), (0.7, [30, 0, 0]), (1.0, [10, 0, 0]), z(2.2))},
})
# Boulder Throw: rips up a boulder (0.5 s) and flings it. HIT 1.0 s (tick 20).
ANIMATIONS["boulder_throw"] = dict(length=2.0, loop=False, bones={
    "head": {"rotation": track(z(0), (0.5, [45, 0, 0]), (0.8, [40, 0, 0]), (1.0, [-40, 0, 0]), (1.3, [-30, 0, 0]), z(2.0))},
    "jaw": {"rotation": track(z(0), (0.4, [35, 0, 0]), (0.5, [10, 0, 0]), (1.0, [40, 0, 0]), (1.3, [10, 0, 0]), z(2.0))},
    "body": {"rotation": track(z(0), (0.5, [12, 0, 0]), (1.0, [-12, 0, 0]), z(2.0))},
    FL: {"rotation": track(z(0), (0.5, [-10, 0, 0]), (1.0, [8, 0, 0]), z(2.0))},
    FR: {"rotation": track(z(0), (0.5, [-10, 0, 0]), (1.0, [8, 0, 0]), z(2.0))},
})
# Rolling Charge: curls up (0.8 s), rolls from tick 16 to tick 46 (2.3 s), uncurls.
ANIMATIONS["rolling_charge"] = dict(length=3.0, loop=False, bones={
    "body": {"rotation": track(z(0), (0.8, [0, 0, 0]), (2.3, [1080, 0, 0]), (3.0, [1080, 0, 0]), ease="linear"),
             "position": track(z(0), (0.6, [0, -10, 0]), (2.3, [0, -10, 0]), z(3.0))},
    "head": {"rotation": track(z(0), (0.6, [60, 0, 0]), (2.3, [60, 0, 0]), z(3.0))},
    "tail_1": {"rotation": track(z(0), (0.6, [-60, 0, 0]), (2.3, [-60, 0, 0]), z(3.0))},
    "tail_2": {"rotation": track(z(0), (0.6, [-50, 0, 0]), (2.3, [-50, 0, 0]), z(3.0))},
    FL: {"rotation": track(z(0), (0.6, [70, 0, 0]), (2.3, [70, 0, 0]), z(3.0))},
    FR: {"rotation": track(z(0), (0.6, [70, 0, 0]), (2.3, [70, 0, 0]), z(3.0))},
    BL: {"rotation": track(z(0), (0.6, [-70, 0, 0]), (2.3, [-70, 0, 0]), z(3.0))},
    BR: {"rotation": track(z(0), (0.6, [-70, 0, 0]), (2.3, [-70, 0, 0]), z(3.0))},
})
# Fortify: plants itself at 0.5 s (tick 10), holds until 5.0 s (tick 100).
ANIMATIONS["fortify"] = dict(length=5.5, loop=False, bones={
    "body": {"position": track(z(0), (0.5, [0, -8, 0]), (5.0, [0, -8, 0]), z(5.5))},
    FL: {"rotation": track(z(0), (0.5, [-20, 0, -25]), (5.0, [-20, 0, -25]), z(5.5))},
    FR: {"rotation": track(z(0), (0.5, [-20, 0, 25]), (5.0, [-20, 0, 25]), z(5.5))},
    BL: {"rotation": track(z(0), (0.5, [20, 0, -25]), (5.0, [20, 0, -25]), z(5.5))},
    BR: {"rotation": track(z(0), (0.5, [20, 0, 25]), (5.0, [20, 0, 25]), z(5.5))},
    "head": {"rotation": track(z(0), (0.5, [25, 0, 0]), (5.0, [25, 0, 0]), z(5.5))},
    "crest": {"rotation": track(z(0), (0.5, [-15, 0, 0]), (2.75, [-12, 0, 0]), (5.0, [-15, 0, 0]), z(5.5))},
    "tail_1": {"rotation": track(z(0), (0.5, [0, 45, 0]), (5.0, [0, 45, 0]), z(5.5))},
    "tail_2": {"rotation": track(z(0), (0.5, [0, 40, 0]), (5.0, [0, 40, 0]), z(5.5))},
})
# Crystal Spikes: both forefeet stamp. HIT 0.8 s (tick 16).
ANIMATIONS["crystal_spikes"] = dict(length=1.6, loop=False, bones={
    FL: {"rotation": track(z(0), (0.55, [-45, 0, 0]), (0.8, [12, 0, 0]), z(1.6))},
    FR: {"rotation": track(z(0), (0.55, [-45, 0, 0]), (0.8, [12, 0, 0]), z(1.6))},
    "body": {"rotation": track(z(0), (0.55, [-15, 0, 0]), (0.8, [6, 0, 0]), z(1.6)), "position": track(z(0), (0.55, [0, 3, 0]), (0.8, [0, -2, 0]), z(1.6))},
    "crest": {"rotation": track(z(0), (0.8, [-10, 0, 0]), (0.9, [5, 0, 0]), z(1.6))},
})
# Magma Breach: head driven into the ground at 1.0 s (tick 20), the fissures burst at 2.2 s (tick 44).
ANIMATIONS["magma_breach"] = dict(length=2.4, loop=False, bones={
    "head": {"rotation": track(z(0), (0.7, [-25, 0, 0]), (1.0, [45, 0, 0]), (2.0, [42, 0, 0]), (2.2, [-20, 0, 0]), z(2.4))},
    "jaw": {"rotation": track(z(0), (0.7, [30, 0, 0]), (1.0, [5, 0, 0]), (2.2, [35, 0, 0]), z(2.4))},
    "body": {"rotation": track(z(0), (0.7, [-10, 0, 0]), (1.0, [15, 0, 0]), (2.0, [12, 0, 0]), z(2.4))},
})
# Seismic Roar. HIT 0.7 s (tick 14).
ANIMATIONS["seismic_roar"] = dict(length=1.8, loop=False, bones={
    "head": {"rotation": track(z(0), (0.5, [-35, 0, 0]), (0.7, [-20, 0, 0]), (1.3, [-22, 0, 0]), z(1.8))},
    "jaw": {"rotation": track(z(0), (0.5, [45, 0, 0]), (1.3, [45, 0, 0]), z(1.8))},
    "body": {"rotation": track(z(0), (0.5, [-12, 0, 0]), (0.7, [5, 0, 0]), z(1.8))},
    "crest": {"rotation": track(z(0), (0.7, [-10, 0, 0]), z(1.8))},
})
# Summon: stamps and bellows. HIT 1.0 s (tick 20).
ANIMATIONS["summon"] = dict(length=2.0, loop=False, bones={
    FL: {"rotation": track(z(0), (0.7, [-40, 0, 0]), (1.0, [10, 0, 0]), z(2.0))},
    "head": {"rotation": track(z(0), (0.7, [-30, 0, 0]), (1.0, [5, 0, 0]), z(2.0))},
    "jaw": {"rotation": track(z(0), (0.7, [40, 0, 0]), (1.0, [10, 0, 0]), z(2.0))},
})

TEXTURES = {
    "bastion_behemoth": dict(parts={}, default=(0.075, 0.065, 0.060), crystal=(1.00, 0.55, 0.12), glow=(1.00, 0.55, 0.15),
                             crystal_chance=0.14, crystal_chance_high=0.25, high_y=60, bone_crystal_chance={}, seed=303),
}
