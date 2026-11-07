"""Blender -> Minecraft (GeckoLib) conversion pipeline for every model in Desktop/BOSS.

Source of truth: the Blender models exported as OBJ + MTL:
    BOSS.obj -> shard_dragon (boss)      model_shard_dragon.py
    Mob.obj  -> shard_hound  (minions)   model_shard_hound.py   (textures: shard_hound, shard_spitter, volatile_hound)
    Mob2.obj -> shard_warlord (mid-boss) model_shard_warlord.py

Run with Blender 5.1 (headless):
    blender -b --factory-startup --python-exit-code 1 -P tools/blender/convert.py -- \
        [--model all|shard_dragon|shard_hound|shard_warlord] [--source <Desktop/BOSS>] [--project <mod root>]
        [--texture-size 1024] [--skip-bake]

Per model:
  1. Parse the OBJ (positions / UVs / normals / triangles, original order) and its MTL.
  2. Rig: attach the mesh to the hand-fitted bones of the model file (rig_common.py) and build a real Blender
     armature + vertex groups -> tools/blender/output/<entity>_rigged.blend.
  3. Texture: the MTL's map_Kd if it exists, otherwise bake each texture variant onto the OBJ's own UV layout with
     Cycles (albedo * ambient occlusion) plus a `_glowmask` emissive map.
  4. Export geckolib/models/entity/<entity>.geo.json (Bedrock geometry 1.21.0, one poly_mesh per bone).
  5. Export geckolib/animations/entity/<entity>.animation.json.
  6. Render a 64x64 HUD portrait (boss bars) and preview images.
Finally the 16x16 item textures are drawn.

Coordinate conversion: OBJ (x, y, z) -> Bedrock geometry (x, y, -z) in model pixels (1 OBJ unit = 1 pixel).
GeckoLib negates X when baking, so the net transform into Minecraft render space is a 180 degree rotation about Y.
"""
import argparse
import importlib
import json
import os
import random
import sys

import bpy
import numpy as np
from mathutils import Vector

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import anim_common  # noqa: E402
import rig_common  # noqa: E402

MOD_ID = "boss_mode"
MODELS = ["shard_dragon", "shard_hound", "shard_warlord", "void_sovereign", "tempest_wyvern", "bastion_behemoth"]
PORTRAITS = {"shard_dragon": (Vector((0, 0, 24)), Vector((55, -120, 60)), 70), "shard_warlord": (Vector((0, 0, 24)), Vector((25, -90, 30)), 26),
             "void_sovereign": (Vector((0, -4, 88)), Vector((35, -160, 104)), 66),
             "tempest_wyvern": (Vector((6, -8, 42)), Vector((60, -130, 62)), 44),
             "bastion_behemoth": (Vector((0, -40, 50)), Vector((80, -170, 90)), 78)}


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="all")
    parser.add_argument("--source", default=os.path.join(os.path.expanduser("~"), "Desktop", "BOSS"))
    parser.add_argument("--project", default=os.path.normpath(os.path.join(HERE, "..", "..")))
    parser.add_argument("--texture-size", type=int, default=1024)
    parser.add_argument("--skip-bake", action="store_true", help="reuse the existing texture PNGs")
    return parser.parse_args(argv)


# ------------------------------------------------------------------------------------------------ OBJ / MTL
def parse_obj(path):
    positions, uvs, normals, faces = [], [], [], []
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            parts = line.split()
            if not parts:
                continue
            if parts[0] == "v":
                positions.append(tuple(float(p) for p in parts[1:4]))
            elif parts[0] == "vt":
                uvs.append((float(parts[1]), float(parts[2])))
            elif parts[0] == "vn":
                normals.append(tuple(float(p) for p in parts[1:4]))
            elif parts[0] == "f":
                corners = []
                for token in parts[1:]:
                    idx = token.split("/")
                    corners.append((int(idx[0]) - 1, int(idx[1]) - 1 if len(idx) > 1 and idx[1] else -1,
                                    int(idx[2]) - 1 if len(idx) > 2 and idx[2] else -1))
                for i in range(1, len(corners) - 1):
                    faces.append((corners[0], corners[i], corners[i + 1]))
    return positions, uvs, normals, faces


def parse_mtl(path):
    materials, current = {}, None
    if not os.path.isfile(path):
        return materials
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            parts = line.strip().split(None, 1)
            if not parts or parts[0].startswith("#"):
                continue
            if parts[0] == "newmtl":
                current = materials.setdefault(parts[1], {})
            elif current is not None and len(parts) > 1:
                current[parts[0]] = parts[1]
    return materials


# ------------------------------------------------------------------------------------------------ Blender scene
def import_mesh(obj_path, name):
    """Default OBJ axes (-Z forward, Y up) so the saved .blend is upright (Z up)."""
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.wm.obj_import(filepath=obj_path, forward_axis='NEGATIVE_Z', up_axis='Y')
    mesh_obj = [o for o in bpy.context.scene.objects if o.type == 'MESH'][0]
    mesh_obj.name = name
    bpy.context.view_layer.objects.active = mesh_obj
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    return mesh_obj


def obj_to_blender(p):
    return Vector((p[0], -p[2], p[1]))


def build_armature(model, mesh_obj, bone_of_vertex):
    arm_data = bpy.data.armatures.new(model.ENTITY + "_rig")
    arm_obj = bpy.data.objects.new(model.ENTITY + "_rig", arm_data)
    bpy.context.scene.collection.objects.link(arm_obj)
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='EDIT')
    edit_bones = {}
    for name, parent, pivot, end in model.BONES:
        bone = arm_data.edit_bones.new(name)
        bone.head = obj_to_blender(pivot)
        bone.tail = obj_to_blender(end) if end else obj_to_blender((pivot[0], pivot[1] + 4, pivot[2]))
        if parent:
            bone.parent = edit_bones[parent]
        edit_bones[name] = bone
    bpy.ops.object.mode_set(mode='OBJECT')
    groups = {name: mesh_obj.vertex_groups.new(name=name) for name, *_ in model.BONES}
    by_bone = {}
    for index, bone in enumerate(bone_of_vertex):
        by_bone.setdefault(bone, []).append(index)
    for bone, indices in by_bone.items():
        groups[bone].add(indices, 1.0, 'REPLACE')
    modifier = mesh_obj.modifiers.new("Armature", 'ARMATURE')
    modifier.object = arm_obj
    mesh_obj.parent = arm_obj


# ------------------------------------------------------------------------------------------------ texture bake
def fill_attributes(mesh, palette, bone_of_vertex, fragment_of, fragments, positions, split_threshold):
    rng = random.Random(palette.get("seed", 1))
    frag_crystal, frag_tone = [], []
    for members in fragments:
        tone = rng.uniform(0.75, 1.35)
        roll = rng.random()
        if len(members) > split_threshold:
            frag_crystal.append(0.0)  # big connected armour pieces stay solid
        else:
            bone = bone_of_vertex[members[0]]
            cy = sum(positions[i][1] for i in members) / len(members)
            chance = palette["bone_crystal_chance"].get(bone,
                                                        palette["crystal_chance_high"] if cy > palette["high_y"] else palette["crystal_chance"])
            frag_crystal.append(1.0 if roll < chance else 0.0)
        frag_tone.append(tone)
    part = mesh.color_attributes.get("part") or mesh.color_attributes.new("part", 'FLOAT_COLOR', 'POINT')
    info = mesh.color_attributes.get("info") or mesh.color_attributes.new("info", 'FLOAT_COLOR', 'POINT')
    for i in range(len(mesh.vertices)):
        base = palette["parts"].get(bone_of_vertex[i], palette["default"])
        tone = frag_tone[fragment_of[i]]
        part.data[i].color = (base[0] * tone, base[1] * tone, base[2] * tone, 1.0)
        info.data[i].color = (frag_crystal[fragment_of[i]], 0.0, 0.0, 1.0)


def build_material(mesh_obj, palette, scale):
    """Procedural 'obsidian hide with crystal shards' material driven by the per-vertex attributes."""
    mat = bpy.data.materials.new("Bake")
    if mat.node_tree is None:
        mat.use_nodes = True
    nt = mat.node_tree
    nt.nodes.clear()
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    bsdf = nt.nodes.new("ShaderNodeBsdfPrincipled")
    nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])

    def mix_rgba(a, b, fac=1.0, blend='MIX'):
        node = nt.nodes.new("ShaderNodeMix")
        node.data_type = 'RGBA'
        node.blend_type = blend
        for socket_id, value in (("Factor_Float", fac), ("A_Color", a), ("B_Color", b)):
            socket = next(s for s in node.inputs if s.identifier == socket_id)
            if isinstance(value, bpy.types.NodeSocket):
                nt.links.new(value, socket)
            else:
                socket.default_value = value
        return next(s for s in node.outputs if s.identifier == "Result_Color")

    def ramp(fac_socket, stops):
        node = nt.nodes.new("ShaderNodeValToRGB")
        elements = node.color_ramp.elements
        while len(elements) < len(stops):
            elements.new(0.5)
        for element, (position, color) in zip(elements, stops):
            element.position = position
            element.color = color
        nt.links.new(fac_socket, node.inputs["Fac"])
        return node.outputs["Color"]

    attr_part = nt.nodes.new("ShaderNodeAttribute"); attr_part.attribute_name = "part"
    attr_info = nt.nodes.new("ShaderNodeAttribute"); attr_info.attribute_name = "info"
    sep_info = nt.nodes.new("ShaderNodeSeparateColor")
    nt.links.new(attr_info.outputs["Color"], sep_info.inputs["Color"])
    crystal_mask = sep_info.outputs["Red"]
    texco = nt.nodes.new("ShaderNodeTexCoord")

    voronoi = nt.nodes.new("ShaderNodeTexVoronoi")
    voronoi.feature = 'DISTANCE_TO_EDGE'
    voronoi.inputs["Scale"].default_value = 0.32 * scale
    nt.links.new(texco.outputs["Object"], voronoi.inputs["Vector"])
    edges = ramp(voronoi.outputs["Distance"], [(0.0, (0.35, 0.35, 0.35, 1)), (0.12, (1, 1, 1, 1))])
    noise = nt.nodes.new("ShaderNodeTexNoise")
    noise.inputs["Scale"].default_value = 0.09 * scale
    noise.inputs["Detail"].default_value = 6.0
    nt.links.new(texco.outputs["Object"], noise.inputs["Vector"])
    mottling = ramp(noise.outputs["Fac"], [(0.3, (0.7, 0.7, 0.7, 1)), (0.75, (1.35, 1.3, 1.45, 1))])
    hide = mix_rgba(mix_rgba(attr_part.outputs["Color"], mottling, blend='MULTIPLY'), edges, blend='MULTIPLY')
    crystal_color = palette["crystal"]
    crystal = ramp(voronoi.outputs["Distance"], [(0.0, (*[c * 0.55 for c in crystal_color], 1)), (0.25, (*crystal_color, 1))])
    nt.links.new(mix_rgba(hide, crystal, fac=crystal_mask), bsdf.inputs["Base Color"])

    vein_voronoi = nt.nodes.new("ShaderNodeTexVoronoi")
    vein_voronoi.feature = 'DISTANCE_TO_EDGE'
    vein_voronoi.inputs["Scale"].default_value = 0.11 * scale
    nt.links.new(texco.outputs["Object"], vein_voronoi.inputs["Vector"])
    vein_lines = ramp(vein_voronoi.outputs["Distance"], [(0.0, (1, 1, 1, 1)), (0.035, (0, 0, 0, 1))])
    vein_noise = nt.nodes.new("ShaderNodeTexNoise")
    vein_noise.inputs["Scale"].default_value = 0.05 * scale
    nt.links.new(texco.outputs["Object"], vein_noise.inputs["Vector"])
    threshold = palette.get("veins", 0.5)
    vein_mask = ramp(vein_noise.outputs["Fac"], [(threshold, (0, 0, 0, 1)), (threshold + 0.1, (1, 1, 1, 1))])
    veins = mix_rgba(vein_lines, vein_mask, blend='MULTIPLY')
    crystal_glow = mix_rgba((0, 0, 0, 1), (0.55, 0.55, 0.55, 1), fac=crystal_mask)
    glow = mix_rgba(veins, crystal_glow, blend='LIGHTEN')
    nt.links.new(mix_rgba(glow, (*palette["glow"], 1), blend='MULTIPLY'), bsdf.inputs["Emission Color"])
    bsdf.inputs["Emission Strength"].default_value = 1.0
    bsdf.inputs["Roughness"].default_value = 0.6
    mesh_obj.data.materials.clear()
    mesh_obj.data.materials.append(mat)
    return mat


def bake_pass(mesh_obj, mat, image, bake_type, pass_filter=None):
    nt = mat.node_tree
    for node in nt.nodes:
        node.select = False
    tex = nt.nodes.get("BakeTarget") or nt.nodes.new("ShaderNodeTexImage")
    tex.name = "BakeTarget"
    tex.image = image
    tex.select = True
    nt.nodes.active = tex
    bpy.ops.object.select_all(action='DESELECT')
    mesh_obj.select_set(True)
    bpy.context.view_layer.objects.active = mesh_obj
    kwargs = dict(type=bake_type, margin=16, use_clear=True)
    if pass_filter:
        kwargs["pass_filter"] = pass_filter
    bpy.ops.object.bake(**kwargs)


def image_to_array(image):
    arr = np.empty(image.size[0] * image.size[1] * 4, dtype=np.float32)
    image.pixels.foreach_get(arr)
    return arr.reshape(image.size[1], image.size[0], 4)


def save_array(arr, path, colorspace='sRGB'):
    h, w = arr.shape[:2]
    img = bpy.data.images.new(os.path.basename(path), w, h, alpha=True)
    img.colorspace_settings.name = colorspace
    img.pixels.foreach_set(np.clip(arr, 0, 1).astype(np.float32).ravel())
    img.filepath_raw = path
    img.file_format = 'PNG'
    img.save()
    return img


def bake_textures(mesh_obj, mat, size, albedo_path, glow_path):
    scene = bpy.context.scene
    scene.render.engine = 'CYCLES'
    scene.cycles.device = 'CPU'
    scene.cycles.samples = 64
    scene.world = scene.world or bpy.data.worlds.new("World")
    albedo = bpy.data.images.new("bake_albedo", size, size, alpha=False)
    ao = bpy.data.images.new("bake_ao", size, size, alpha=False)
    ao.colorspace_settings.name = 'Non-Color'
    emit = bpy.data.images.new("bake_emit", size, size, alpha=False)
    bake_pass(mesh_obj, mat, albedo, 'DIFFUSE', {'COLOR'})
    bake_pass(mesh_obj, mat, ao, 'AO')
    bake_pass(mesh_obj, mat, emit, 'EMIT')
    a, o, e = image_to_array(albedo), image_to_array(ao)[..., :1], image_to_array(emit)
    color = a[..., :3] * (0.30 + 0.70 * o) + e[..., :3] * 0.55
    save_array(np.concatenate([color, np.ones_like(color[..., :1])], axis=-1), albedo_path)
    strength = np.clip(e[..., :3].max(axis=-1, keepdims=True) * 1.6, 0, 1)
    glow = np.concatenate([np.clip(e[..., :3] * 1.4, 0, 1), strength], axis=-1)
    glow[glow[..., 3] < 0.08] = 0.0
    save_array(glow, glow_path)
    for img in (albedo, ao, emit):
        bpy.data.images.remove(img)


# ------------------------------------------------------------------------------------------------ geo.json
def r(v, digits=4):
    return round(float(v), digits)


def export_geo(path, model, positions, uvs, normals, faces, bone_of_triangle, texture_size):
    per_bone = {name: {"positions": [], "normals": [], "uvs": [], "polys": [], "pmap": {}, "nmap": {}, "umap": {}} for name, *_ in model.BONES}
    for tri, bone in zip(faces, bone_of_triangle):
        data = per_bone[bone]
        poly = []
        for vi, ti, ni in tri:
            if vi not in data["pmap"]:
                x, y, zz = positions[vi]
                data["pmap"][vi] = len(data["positions"])
                data["positions"].append([r(x), r(y), r(-zz)])
            if ni not in data["nmap"]:
                nx, ny, nz = normals[ni] if ni >= 0 else (0.0, 1.0, 0.0)
                data["nmap"][ni] = len(data["normals"])
                data["normals"].append([r(nx), r(ny), r(-nz)])
            if ti not in data["umap"]:
                u, v = uvs[ti] if ti >= 0 else (0.0, 0.0)
                data["umap"][ti] = len(data["uvs"])
                data["uvs"].append([r(u, 6), r(v, 6)])
            poly.append([data["pmap"][vi], data["nmap"][ni], data["umap"][ti]])
        data["polys"].append(poly)
    bones_json = []
    for name, parent, pivot, _end in model.BONES:
        bone = {"name": name, "pivot": [pivot[0], pivot[1], -pivot[2]]}
        if parent:
            bone["parent"] = parent
        data = per_bone[name]
        if data["polys"]:
            bone["poly_mesh"] = {"normalized_uvs": True, "positions": data["positions"], "normals": data["normals"],
                                 "uvs": data["uvs"], "polys": data["polys"]}
        bones_json.append(bone)
    xs = [p[0] for p in positions]; ys = [p[1] for p in positions]; zs = [p[2] for p in positions]
    geo = {"format_version": "1.21.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + model.ENTITY, "texture_width": texture_size, "texture_height": texture_size,
                        "visible_bounds_width": r(max(max(xs) - min(xs), max(zs) - min(zs)) / 16 + 1, 2),
                        "visible_bounds_height": r((max(ys) - min(ys)) / 16 + 1, 2),
                        "visible_bounds_offset": [0, r((max(ys) - min(ys)) / 32, 2), 0]},
        "bones": bones_json}]}
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(geo, fh, separators=(",", ":"))
    return {name: len(d["polys"]) for name, d in per_bone.items()}


def fmt_time(t):
    return ("%.4f" % t).rstrip("0").rstrip(".") if t != int(t) else "%.1f" % t


def export_animations(path, model):
    anim_common.export_animations(path, model)


# ------------------------------------------------------------------------------------------------ images
def textured_material(texture_path):
    img = bpy.data.images.load(texture_path, check_existing=False)
    mat = bpy.data.materials.new("Preview")
    if mat.node_tree is None:
        mat.use_nodes = True
    tex = mat.node_tree.nodes.new("ShaderNodeTexImage")
    tex.image = img
    mat.node_tree.nodes.active = tex
    return mat


def camera(scene, ortho):
    cam_data = bpy.data.cameras.new("Cam")
    cam_data.type = 'ORTHO'
    cam_data.ortho_scale = ortho
    cam = bpy.data.objects.new("Cam", cam_data)
    scene.collection.objects.link(cam)
    scene.camera = cam
    return cam


def aim(cam, pos, target):
    cam.location = pos
    cam.rotation_euler = (target - cam.location).to_track_quat('-Z', 'Y').to_euler()


def render_images(model, mesh_obj, texture_path, out_dir, gui_dir, size_hint):
    scene = bpy.context.scene
    scene.render.engine = 'BLENDER_WORKBENCH'
    scene.display.shading.light = 'STUDIO'
    scene.display.shading.color_type = 'TEXTURE'
    mesh_obj.data.materials.clear()
    mesh_obj.data.materials.append(textured_material(texture_path))
    height = max(v.co.z for v in mesh_obj.data.vertices)
    target = Vector((0, 0, height * 0.45))
    size_hint = max(size_hint, height * 0.8)
    cam = camera(scene, size_hint * 2.2)
    scene.render.resolution_x = scene.render.resolution_y = 640
    scene.render.film_transparent = False
    for name, direction in {"front": (0.35, -1.0, 0.45), "side": (1.0, 0.0, 0.25), "back": (-0.7, 0.9, 0.55)}.items():
        aim(cam, target + Vector(direction).normalized() * size_hint * 3, target)
        scene.render.filepath = os.path.join(out_dir, "%s_preview_%s.png" % (model.ENTITY, name))
        bpy.ops.render.render(write_still=True)
    if model.ENTITY in PORTRAITS:
        look_at, pos, ortho = PORTRAITS[model.ENTITY]
        cam.data.ortho_scale = ortho
        aim(cam, pos, look_at)
        scene.render.resolution_x = scene.render.resolution_y = 64
        scene.render.film_transparent = True
        scene.render.image_settings.color_mode = 'RGBA'
        scene.render.filepath = os.path.join(gui_dir, "portrait_%s.png" % model.ENTITY)
        bpy.ops.render.render(write_still=True)


def pixel_art(rows, palette):
    arr = np.zeros((len(rows), 16, 4), dtype=np.float32)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                arr[len(rows) - 1 - y, x] = palette[ch]
    return arr


def srgb(hex_color):
    return [int(hex_color[i:i + 2], 16) / 255 for i in (0, 2, 4)] + [1.0]


SHARD = ["................", "..........a.....", ".........aba....", "........abcba...", ".......abccda...", "......abccdda...",
         ".....abccdda....", "....abccdda.....", "...abccdda......", "...bccdda.......", "..bccdda........", "..bcdda.........",
         "..edda..........", "..eea...........", "...e............", "................"]
EGG = ["................", "......aaaa......", ".....abbbba.....", "....abbcbbba....", "...abbbbbcbba...", "...abcbbbbbba...",
       "..abbbbbcbbbba..", "..abbcbbbbbcba..", "..abbbbbbbbbba..", "..abbbcbbbbbba..", "..abbbbbbcbbba..", "...abcbbbbbba...",
       "...abbbbcbbba...", "....abbbbbba....", ".....aaaaaa.....", "................"]
EGGS = {"shard_dragon_spawn_egg": ("150d1f", "2d1f3f", "a45cff"), "shard_hound_spawn_egg": ("1a1422", "3b2d4f", "b57cff"),
        "shard_spitter_spawn_egg": ("0d1a1f", "1f3a40", "4fe0ff"), "volatile_hound_spawn_egg": ("1f0d08", "40201a", "ff8a1f"),
        "shard_warlord_spawn_egg": ("120a14", "3d0f1c", "e35cff"), "void_sovereign_spawn_egg": ("08040a", "1d0710", "ff2d6a"),
        "tempest_wyvern_spawn_egg": ("0a1620", "1f3a52", "4fe0ff"), "bastion_behemoth_spawn_egg": ("1a120a", "3a2a18", "ff9a2a")}
# Tempest Eye: the Tempest Wyvern's trophy - a storm caught in a crystal eye.
EYE = ["................", "................", "....aaaaaaaa....", "...abbbbbbbba...", "..abccccccccba..", ".abccddddddccba.",
       ".abcddeeeeddcba.", ".abcdeeffeedcba.", ".abcdeeffeedcba.", ".abcddeeeeddcba.", ".abccddddddccba.", "..abccccccccba..",
       "...abbbbbbbba...", "....aaaaaaaa....", "................", "................"]
# Behemoth Core: the Bastion Behemoth's trophy - a molten heart in a stone shell.
CORE = ["................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbccccccbba..", "..abcddddddcba..", ".abcddeeeeddcba.",
        ".abcdeeffeedcba.", ".abcdeffffedcba.", ".abcdeffffedcba.", ".abcdeeffeedcba.", ".abcddeeeeddcba.", "..abcddddddcba..",
        "..abbccccccbba..", "...aabbbbbbaa...", ".....aaaaaa.....", "................"]
# Storm feather: the Tempest Wyvern's projectile.
FEATHER = ["................", "............aa..", "...........abba.", "..........abcba.", ".........abcba..", "........abcba...",
           ".......abcba....", "......abcba.....", ".....abcba......", "....abcba.......", "...abcba........", "..abcba.........",
           "..dcba..........", ".dd.............", "d...............", "................"]
# Wyrm Heart: the crystalline heart the Shard Dragon leaves behind (3 open the Void Altar).
HEART = ["................", "..aa......aa....", ".abba....abba...", "abccba..abccba..", "abcdcbaabcddcba.", "abcddcbbcddddca.",
         "abcdddccdddddca.", ".abcdddddddcba..", "..abcdddddcba...", "...abcdddcba....", "....abcdcba.....", ".....abcba......",
         "......aba.......", ".......a........", "................", "................"]
# Void Orb: the Void Sovereign's homing projectile.
ORB = ["................", "................", ".....aaaaaa.....", "....abbbbbba....", "...abccccccba...", "..abcddddddcba..",
       "..abcdeeeedcba..", "..abcdeeeedcba..", "..abcdeeeedcba..", "..abcdeeeedcba..", "..abcddddddcba..", "...abccccccba...",
       "....abbbbbba....", ".....aaaaaa.....", "................", "................"]


def export_item_textures(item_dir):
    save_array(pixel_art(EYE, {"a": srgb("0a2233"), "b": srgb("1f5a80"), "c": srgb("3fb8e8"), "d": srgb("aef2ff"), "e": srgb("0a3048"),
                               "f": srgb("ffffff")}), os.path.join(item_dir, "tempest_eye.png"))
    save_array(pixel_art(CORE, {"a": srgb("2a2018"), "b": srgb("4a3a2a"), "c": srgb("7a3a10"), "d": srgb("d86a14"), "e": srgb("ffb040"),
                                "f": srgb("fff0b0")}), os.path.join(item_dir, "behemoth_core.png"))
    save_array(pixel_art(FEATHER, {"a": srgb("1a3a5a"), "b": srgb("6ad8ff"), "c": srgb("e8faff"), "d": srgb("3a6a8a")}),
               os.path.join(item_dir, "storm_feather.png"))
    save_array(pixel_art(HEART, {"a": srgb("2a0612"), "b": srgb("7a0f2e"), "c": srgb("d6245a"), "d": srgb("ff8fb0")}),
               os.path.join(item_dir, "wyrm_heart.png"))
    save_array(pixel_art(ORB, {"a": srgb("ff2d6a"), "b": srgb("8a0f3a"), "c": srgb("2a0716"), "d": srgb("0a0206"), "e": srgb("000000")}),
               os.path.join(item_dir, "void_orb.png"))
    save_array(pixel_art(SHARD, {"a": srgb("3a1f5c"), "b": srgb("7b3fc4"), "c": srgb("b57cff"), "d": srgb("e7d2ff"), "e": srgb("241238")}),
               os.path.join(item_dir, "wyrm_shard.png"))
    for name, (edge, body, spot) in EGGS.items():
        save_array(pixel_art(EGG, {"a": srgb(edge), "b": srgb(body), "c": srgb(spot)}), os.path.join(item_dir, name + ".png"))


# ------------------------------------------------------------------------------------------------ main
def convert(name, args, dirs):
    model = importlib.import_module("model_" + name)
    obj_path = os.path.join(args.source, model.OBJ)
    if not os.path.isfile(obj_path):
        raise SystemExit("%s not found in %s" % (model.OBJ, args.source))
    positions, uvs, normals, faces = parse_obj(obj_path)
    materials = parse_mtl(os.path.splitext(obj_path)[0] + ".mtl")
    corner_faces = [[c[0] for c in f] for f in faces]
    bone_of_triangle, bone_of_vertex, fragment_of, fragments = rig_common.assign(positions, corner_faces, model.SEGMENTS, model.SPLIT_THRESHOLD)
    print("[convert] %s: %d vertices, %d triangles, %d fragments, MTL %s" % (name, len(positions), len(faces), len(fragments), list(materials)))

    mesh_obj = import_mesh(obj_path, name)
    if len(mesh_obj.data.vertices) != len(positions):
        raise SystemExit("Blender import changed the vertex count - cannot map rig weights")
    size_hint = max(max(abs(p[i]) for p in positions) for i in (0, 2)) * 1.2 + 1
    scale = 30.0 / max(p[1] for p in positions)  # texture pattern density relative to the dragon (50 units tall)
    diffuse = next((m.get("map_Kd") for m in materials.values() if m.get("map_Kd")), None)
    first_texture = None
    for texture_name, palette in model.TEXTURES.items():
        albedo = os.path.join(dirs["tex"], texture_name + ".png")
        glow = os.path.join(dirs["tex"], texture_name + "_glowmask.png")
        first_texture = first_texture or albedo
        if diffuse and os.path.isfile(os.path.join(args.source, diffuse)):
            src = bpy.data.images.load(os.path.join(args.source, diffuse))
            src.filepath_raw = albedo; src.file_format = 'PNG'; src.save()
            continue
        if args.skip_bake:
            continue
        fill_attributes(mesh_obj.data, palette, bone_of_vertex, fragment_of, fragments, positions, model.SPLIT_THRESHOLD)
        mat = build_material(mesh_obj, palette, min(1.6, max(0.6, scale * 1.67)))
        print("[convert]   baking %s ..." % texture_name)
        bake_textures(mesh_obj, mat, args.texture_size, albedo, glow)

    build_armature(model, mesh_obj, bone_of_vertex)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(dirs["out"], name + "_rigged.blend"), check_existing=False)
    counts = export_geo(os.path.join(dirs["geo"], name + ".geo.json"), model, positions, uvs, normals, faces, bone_of_triangle, args.texture_size)
    print("[convert]   triangles per bone:", counts)
    export_animations(os.path.join(dirs["anim"], name + ".animation.json"), model)
    print("[convert]   %d animations: %s" % (len(model.ANIMATIONS), ", ".join(model.ANIMATIONS)))
    render_images(model, mesh_obj, first_texture, dirs["out"], dirs["gui"], size_hint)


def main():
    args = parse_args()
    assets = os.path.join(args.project, "src", "main", "resources", "assets", MOD_ID)
    dirs = {"geo": os.path.join(assets, "geckolib", "models", "entity"), "anim": os.path.join(assets, "geckolib", "animations", "entity"),
            "tex": os.path.join(assets, "textures", "entity"), "item": os.path.join(assets, "textures", "item"),
            "gui": os.path.join(assets, "textures", "gui"), "out": os.path.join(HERE, "output")}
    for d in dirs.values():
        os.makedirs(d, exist_ok=True)
    for name in (MODELS if args.model == "all" else [args.model]):
        convert(name, args, dirs)
    export_item_textures(dirs["item"])
    print("[convert] done")


main()
