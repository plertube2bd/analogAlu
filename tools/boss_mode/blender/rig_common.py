"""Shared rigging helpers: attach mesh geometry to bones of a hand-fitted rig.

Meshy exports are made of many disconnected fragments. Small fragments are attached whole to the bone whose axis
segment is closest to their centroid (rigid, nothing tears). Fragments above `split_threshold` vertices (e.g. the
connected torso of Mob2) are split per triangle instead, so each limb of a connected body can still move.
"""


def seg_dist(p, a, b):
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    length_sq = dx * dx + dy * dy + dz * dz
    t = 0.0 if length_sq == 0 else max(0.0, min(1.0, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy + (p[2] - a[2]) * dz) / length_sq))
    cx, cy, cz = a[0] + dx * t, a[1] + dy * t, a[2] + dz * t
    return ((p[0] - cx) ** 2 + (p[1] - cy) ** 2 + (p[2] - cz) ** 2) ** 0.5


def find_fragments(vertex_count, faces):
    parent = list(range(vertex_count))

    def find(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a

    for face in faces:
        for v in face[1:]:
            ra, rb = find(face[0]), find(v)
            if ra != rb:
                parent[ra] = rb
    groups = {}
    for i in range(vertex_count):
        groups.setdefault(find(i), []).append(i)
    return list(groups.values())


def nearest_bone(point, segments):
    return min(segments, key=lambda name: seg_dist(point, *segments[name]))


def assign(positions, faces, segments, split_threshold=10 ** 9):
    """Returns (bone per triangle, bone per vertex, fragment index per vertex, fragments)."""
    fragments = find_fragments(len(positions), faces)
    fragment_of = [0] * len(positions)
    bone_of_vertex = [None] * len(positions)
    split = set()
    for index, members in enumerate(fragments):
        for i in members:
            fragment_of[i] = index
        if len(members) > split_threshold:
            split.add(index)
            continue
        centroid = [sum(positions[i][k] for i in members) / len(members) for k in range(3)]
        bone = nearest_bone(centroid, segments)
        for i in members:
            bone_of_vertex[i] = bone

    bone_of_triangle = []
    votes = {}
    for face in faces:
        if fragment_of[face[0]] in split:
            centroid = [sum(positions[v][k] for v in face) / len(face) for k in range(3)]
            bone = nearest_bone(centroid, segments)
            for v in face:
                votes.setdefault(v, {}).setdefault(bone, 0)
                votes[v][bone] += 1
        else:
            bone = bone_of_vertex[face[0]]
        bone_of_triangle.append(bone)
    for v, counts in votes.items():
        bone_of_vertex[v] = max(counts, key=counts.get)
    return bone_of_triangle, bone_of_vertex, fragment_of, fragments
