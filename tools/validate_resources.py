#!/usr/bin/env python3
"""Static checks for Redstone Transportation resource files (no third-party packages)."""
from __future__ import annotations
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
NS = "redstone_transportation:"
MACHINES = {
    "tree_cutter", "auto_planter", "growth_booster", "crop_harvester",
    "quarry_drill", "item_collector",
}
KNOWN_MOD_ITEMS = {
    "vehicle_controller", "redstone_motor", "redstone_motor_2", "redstone_motor_3",
    "redstone_motor_4", "power_cell", "vehicle_station", "signal_relay",
    "redstone_tram", "tram_coupler", "alu_train_controller",
    "cargo_tram", "track_layer", "factory_controller",
    "conveyor_belt", "drive_rail", "elevator_core", "ship_helm", "airship_helm",
    "lift_balloon", "elevator_call", "structure_marker", "route_beacon",
} | MACHINES | {f"{name}_{tier}" for name in ("upgraded_furnace", "upgraded_blast_furnace", "upgraded_smoker") for tier in (1, 2, 3)}
KNOWN_MINECRAFT_ITEMS = {
    "iron_ingot", "copper_ingot", "redstone", "gold_ingot", "piston",
    "amethyst_shard", "diamond", "redstone_block", "iron_chain",
    "comparator", "detector_rail", "netherite_ingot",
    "iron_axe", "iron_hoe", "diamond_pickaxe", "chest", "dispenser", "hopper",
    "bone_block", "wheat_seeds", "minecart", "stick", "leather", "rail",
    "feather", "stone_button", "string", "furnace", "blast_furnace", "smoker", "blaze_rod", "compass",
}
errors: list[str] = []
json_files = list(RES.rglob("*.json"))
parsed: dict[Path, object] = {}
for path in json_files:
    try:
        parsed[path] = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"JSON parse error: {path.relative_to(ROOT)}: {exc}")

recipe_dir = RES / "data" / "redstone_transportation" / "recipe"
recipes = sorted(recipe_dir.glob("*.json"))
if not recipes:
    errors.append("No recipes found")
for path in recipes:
    recipe = parsed.get(path)
    if not isinstance(recipe, dict):
        continue
    pattern = recipe.get("pattern", [])
    key = recipe.get("key", {})
    if not isinstance(pattern, list) or not isinstance(key, dict):
        errors.append(f"Malformed shaped recipe: {path.name}")
        continue
    used = {char for row in pattern for char in row if char != " "}
    if used - set(key):
        errors.append(f"Recipe {path.name} uses symbols without keys: {sorted(used - set(key))}")
    if set(key) - used:
        errors.append(f"Recipe {path.name} has unused keys: {sorted(set(key) - used)}")
    external_alu_ingredients = {
        "analog_alu:copper_wire", "analog_alu:adder", "analog_alu:semiconductor",
        "analog_alu:semiconductor_block", "analog_alu:cram"
    }
    condition_list = recipe.get("fabric:load_conditions", [])
    alu_conditioned = any(
        isinstance(condition, dict)
        and condition.get("condition") == "fabric:all_mods_loaded"
        and "analog_alu" in condition.get("values", [])
        for condition in condition_list
    )
    for symbol, ingredient in key.items():
        if not isinstance(ingredient, str) or ":" not in ingredient:
            errors.append(f"Recipe {path.name} key {symbol} is not a string resource ID")
            continue
        namespace, item = ingredient.split(":", 1)
        if namespace == "analog_alu" and ingredient not in external_alu_ingredients:
            errors.append(f"Recipe {path.name} uses unapproved Analog ALU ingredient ID: {ingredient}")
        if namespace == "analog_alu" and not alu_conditioned:
            errors.append(f"Recipe {path.name} uses Analog ALU items without an all_mods_loaded condition")
        if namespace == "minecraft" and item not in KNOWN_MINECRAFT_ITEMS:
            errors.append(f"Recipe {path.name} uses unapproved vanilla ingredient ID: {ingredient}")
        if namespace == "redstone_transportation" and item not in KNOWN_MOD_ITEMS:
            errors.append(f"Recipe {path.name} uses unknown mod ingredient ID: {ingredient}")
    result = recipe.get("result", {}).get("id", "")
    if result.startswith(NS) and result[len(NS):] not in KNOWN_MOD_ITEMS:
        errors.append(f"Recipe {path.name} has unknown result ID: {result}")

# Every automation machine must stay craftable with Analog ALU and without it.
for machine in sorted(MACHINES | {"factory_controller"}):
    with_alu = parsed.get(recipe_dir / f"{machine}.json", {})
    without_alu = parsed.get(recipe_dir / f"{machine}_without_alu.json", {})
    if "analog_alu:semiconductor" not in with_alu.get("key", {}).values():
        errors.append(f"{machine} has no semiconductor in its recipe")
    if with_alu.get("result", {}).get("count") != 4:
        errors.append(f"{machine} does not make a batch of 4")
    if without_alu.get("result", {}).get("id") != NS + machine:
        errors.append(f"{machine} has no recipe for playing without Analog ALU")

# Known 26.3 rename regression: never reintroduce the removed identifier.
for path in recipes:
    if '"minecraft:chain"' in path.read_text(encoding="utf-8"):
        errors.append(f"Removed item ID minecraft:chain found in {path.name}; use minecraft:iron_chain")

# Confirm all local model texture references exist.
for path, obj in parsed.items():
    if not isinstance(obj, dict):
        continue
    textures = obj.get("textures")
    if isinstance(textures, dict):
        for texture_id in textures.values():
            if not isinstance(texture_id, str) or ":" not in texture_id:
                continue
            namespace, texture = texture_id.split(":", 1)
            if namespace == "redstone_transportation":
                target = RES / "assets" / namespace / "textures" / (texture + ".png")
                if not target.is_file():
                    errors.append(f"Missing texture {texture_id} referenced by {path.relative_to(ROOT)}")

# Confirm all blockstate variants/multipart entries point to an existing local block model.
blockstate_dir = RES / "assets" / "redstone_transportation" / "blockstates"
model_dir = RES / "assets" / "redstone_transportation" / "models" / "block"
for path in blockstate_dir.glob("*.json"):
    state = parsed.get(path)
    if not isinstance(state, dict):
        continue
    model_ids: list[str] = []
    for variant in state.get("variants", {}).values():
        if isinstance(variant, dict) and isinstance(variant.get("model"), str):
            model_ids.append(variant["model"])
    for part in state.get("multipart", []):
        apply = part.get("apply", {}) if isinstance(part, dict) else {}
        if isinstance(apply, dict) and isinstance(apply.get("model"), str):
            model_ids.append(apply["model"])
    for model_id in model_ids:
        namespace, separator, model_path = model_id.partition(":")
        if not separator:
            errors.append(f"Invalid model ID {model_id} in {path.name}")
        elif namespace == "redstone_transportation" and not (RES / "assets" / namespace / "models" / (model_path + ".json")).is_file():
            errors.append(f"Missing block model {model_id} referenced by {path.name}")

# Ensure the ALU controller recipe is conditional and the integration does not hard-link friend-mod classes.
alu_recipe = parsed.get(recipe_dir / "alu_train_controller.json")
if isinstance(alu_recipe, dict):
    conditions = alu_recipe.get("fabric:load_conditions", [])
    if not any(isinstance(c, dict) and c.get("condition") == "fabric:all_mods_loaded"
               and "analog_alu" in c.get("values", []) for c in conditions):
        errors.append("ALU controller recipe must be guarded by fabric:all_mods_loaded for analog_alu")
metadata = json.loads((RES / "fabric.mod.json").read_text(encoding="utf-8"))
if "analog_alu" in metadata.get("depends", {}):
    errors.append("Analog ALU should remain optional; move it from depends to suggests")
if metadata.get("suggests", {}).get("analog_alu") is None:
    errors.append("Optional analog_alu suggestion is missing from fabric.mod.json")
java_sources = list((ROOT / "src" / "main" / "java").rglob("*.java"))
for source in java_sources:
    source_text = source.read_text(encoding="utf-8")
    if "com.example.analogalu." in source_text:
        errors.append(f"Direct friend-mod class reference is not allowed in optional integration: {source.relative_to(ROOT)}")

# Check the tier-IV registration and recipe names.
mod_content = (ROOT / "src/main/java/com/example/redstonetransportation/content/ModContent.java").read_text(encoding="utf-8")
main_java = (ROOT / "src/main/java/com/example/redstonetransportation/RedstoneTransportation.java").read_text(encoding="utf-8")
if '"redstone_motor_4"' not in mod_content or "REDSTONE_MOTOR_4_ITEM" not in main_java:
    errors.append("Motor IV registration or creative-tab entry is missing")
if not (recipe_dir / "redstone_motor_4.json").is_file():
    errors.append("Motor IV recipe is missing")

# Every placeable block must drop itself, mine with a pickaxe, and have a language entry in both files.
BLOCK_ITEMS = KNOWN_MOD_ITEMS - {"redstone_tram", "cargo_tram", "tram_coupler", "track_layer", "structure_marker"}
pickaxe_tag = parsed.get(RES / "data" / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json", {})
pickaxe_values = pickaxe_tag.get("values", []) if isinstance(pickaxe_tag, dict) else []
for block in sorted(BLOCK_ITEMS):
    if not (RES / "data" / "redstone_transportation" / "loot_table" / "blocks" / f"{block}.json").is_file():
        errors.append(f"Missing loot table for {block}")
    if NS + block not in pickaxe_values:
        errors.append(f"{block} is missing from the mineable/pickaxe tag")

languages = {name: parsed.get(RES / "assets" / "redstone_transportation" / "lang" / f"{name}.json", {}) for name in ("en_us", "ko_kr")}
for key in sorted(set(languages["en_us"]) ^ set(languages["ko_kr"])):
    errors.append(f"Language key exists in only one file: {key}")
for block in sorted(BLOCK_ITEMS):
    if f"block.redstone_transportation.{block}" not in languages["ko_kr"]:
        errors.append(f"Missing Korean block name: {block}")

# Advancements must hang off one another, show a real item, and read properly in both languages.
advancement_dir = RES / "data" / "redstone_transportation" / "advancement"
advancements = {path.relative_to(advancement_dir).with_suffix("").as_posix(): parsed.get(path) for path in advancement_dir.rglob("*.json")}
if "root" not in advancements:
    errors.append("The advancement tab has no root")
for name, advancement in advancements.items():
    if not isinstance(advancement, dict):
        continue
    parent = advancement.get("parent", "")
    if name != "root" and parent[len(NS):] not in advancements:
        errors.append(f"Advancement {name} has an unknown parent: {parent}")
    display = advancement.get("display", {})
    icon = display.get("icon", {}).get("id", "")
    if icon[len(NS):] not in KNOWN_MOD_ITEMS:
        errors.append(f"Advancement {name} has an unknown icon: {icon}")
    for field in ("title", "description"):
        key = display.get(field, {}).get("translate", "")
        for language in ("en_us", "ko_kr"):
            if key not in languages[language]:
                errors.append(f"Advancement {name} has no {language} {field}: {key}")
    if {member for group in advancement.get("requirements", []) for member in group} != set(advancement.get("criteria", {})):
        errors.append(f"Advancement {name} requires something other than its criteria")

if errors:
    print("RESOURCE VALIDATION FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)
print(f"RESOURCE VALIDATION PASSED: {len(json_files)} JSON files, {len(recipes)} recipes, {len(advancements)} advancements, conditional Analog ALU recipe, no hard class dependency, local model textures and Motor IV registration checked.")
