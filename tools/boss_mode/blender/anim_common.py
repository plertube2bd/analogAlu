"""Keyframe helpers shared by the model definition files.

Values use the Bedrock / GeckoLib animation convention (1 unit = 1 model pixel before entity scaling):

  rotation [x, y, z] in degrees
    +x  pitches a forward-pointing bone (neck, head, jaw) DOWN, tilts an upward bone (torso) FORWARD,
        swings a downward limb's end BACKWARD, lifts a backward-pointing bone's tip (tail) UP
    z   limbs on the model's left (+X): -z raises them outward/up; limbs on the right (-X): +z raises them
    y   yaw (twist)
  position [x, y, z] in pixels: +y up, -z forward (toward the head), +x toward the model's left
  scale [x, y, z]: multipliers (1 = unchanged)
"""

import json

SMOOTH = "easeInOutSine"


def track(*keys, ease=SMOOTH):
    """keys: (time, [x, y, z]) tuples -> list of (time, vector, easing)."""
    return [(t, list(v), ease) for t, v in keys]


def z(t):
    return (t, [0, 0, 0])


def one(t):
    return (t, [1, 1, 1])


def rx(t, v):
    return (t, [v, 0, 0])


def legs_swing(times, amp_a, amp_b):
    """Alternating keyframes for a limb: amp_a on even keys, amp_b on odd keys."""
    return [(t, [amp_a if i % 2 == 0 else amp_b, 0, 0]) for i, t in enumerate(times)]


def mirror_z(keys):
    """Same keys with the z rotation negated (left limb -> right limb)."""
    return [(t, [v[0], -v[1], -v[2]], e) for t, v, e in keys]


def _num(v, digits=3):
    return round(float(v), digits)


def _fmt_time(t):
    return ("%.4f" % t).rstrip("0").rstrip(".") if t != int(t) else "%.1f" % t


def export_animations(path, model):
    """Writes model.ANIMATIONS as a GeckoLib / Bedrock animation file. Pure Python: runs inside or outside Blender."""
    out = {"format_version": "1.8.0", "animations": {}}
    for name, clip in model.ANIMATIONS.items():
        bones = {}
        for bone, channels in clip["bones"].items():
            bones[bone] = {}
            for channel, keys in channels.items():
                frames = {}
                for t, vec, ease in keys:
                    frames[_fmt_time(t)] = {"vector": [_num(c) for c in vec], "easing": ease}
                bones[bone][channel] = frames
        out["animations"]["animation.%s.%s" % (model.ENTITY, name)] = {"loop": clip["loop"], "animation_length": clip["length"], "bones": bones}
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
