"""Re-exports only the GeckoLib animation files from the model definition files - no Blender needed.

    python tools/blender/export_anims.py            # all models
    python tools/blender/export_anims.py shard_dragon

convert.py (inside Blender) writes the same files as part of the full pipeline; use this after editing ANIMATIONS.
"""
import importlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import anim_common  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "boss_mode", "geckolib", "animations", "entity")

for name in sys.argv[1:] or ["shard_dragon", "shard_hound", "shard_warlord"]:
    model = importlib.import_module("model_" + name)
    anim_common.export_animations(os.path.join(OUT, name + ".animation.json"), model)
    print("%s: %d clips (%s)" % (name, len(model.ANIMATIONS), ", ".join(model.ANIMATIONS)))
