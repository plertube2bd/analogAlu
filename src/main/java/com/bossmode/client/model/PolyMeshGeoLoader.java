package com.bossmode.client.model;

import com.bossmode.BossMode;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.ModelProperties;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GeckoLib resource loader for Bedrock geometry files whose bones carry {@code poly_mesh} data.
 * <p>
 * GeckoLib 5.5.7 parses {@code poly_mesh} but does not render it ("This information isn't used by GeckoLib
 * natively" - GeometryPolyMesh), so the Blender-exported Shard Dragon mesh would otherwise be dropped. This loader is
 * registered through GeckoLib's public {@code GeckoLibUtil.addResourceLoader} hook for this mod's model files only; it
 * builds the same {@link BakedGeoModel} / bone hierarchy GeckoLib would, but with {@link PolyMeshGeoBone}s, so the
 * GeckoLib animation system (controllers, keyframes, triggers) drives the mesh unchanged. Animation files are
 * delegated to GeckoLib's default Gson handling.
 * <p>
 * Expected file format (written by tools/blender/convert_boss.py): Bedrock geometry 1.21.0, absolute model-space
 * positions in pixels, {@code normalized_uvs: true} with a bottom-left UV origin (OBJ / Blockbench convention),
 * triangles as {@code [[position, normal, uv] x 3]}.
 */
public final class PolyMeshGeoLoader extends GeckoLibGsonLoader {
	private static final float PIXEL = 1.0F / 16.0F;

	/** Only handle this mod's geo models; everything else keeps GeckoLib's default loader. */
	public static boolean handles(Identifier id, Resource resource) {
		return BossMode.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("geckolib/models/");
	}

	@Override
	public BakedGeoModel bakeGeckoLibModelFile(Identifier resourcePath, JsonObject raw) {
		JsonArray geometries = GsonHelper.getAsJsonArray(raw, "minecraft:geometry");
		if (geometries.isEmpty()) {
			throw new IllegalStateException(resourcePath + ": no geometry definitions");
		}
		JsonObject geometry = geometries.get(0).getAsJsonObject();
		JsonObject description = GsonHelper.getAsJsonObject(geometry, "description", new JsonObject());
		JsonArray bonesJson = GsonHelper.getAsJsonArray(geometry, "bones", new JsonArray());

		boolean hasMesh = false;
		for (JsonElement element : bonesJson) {
			if (element.getAsJsonObject().has("poly_mesh")) {
				hasMesh = true;
				break;
			}
		}
		if (!hasMesh) {
			return super.bakeGeckoLibModelFile(resourcePath, raw); // plain cube model: GeckoLib's own baking
		}

		int textureWidth = GsonHelper.getAsInt(description, "texture_width", 16);
		int textureHeight = GsonHelper.getAsInt(description, "texture_height", 16);

		Map<String, JsonObject> byName = new LinkedHashMap<>();
		Map<String, List<String>> children = new LinkedHashMap<>();
		List<String> roots = new ArrayList<>();
		for (JsonElement element : bonesJson) {
			JsonObject bone = element.getAsJsonObject();
			String name = GsonHelper.getAsString(bone, "name");
			if (byName.put(name, bone) != null) {
				throw new IllegalStateException(resourcePath + ": duplicate bone " + name);
			}
			if (bone.has("cubes") && !GsonHelper.getAsJsonArray(bone, "cubes").isEmpty()) {
				BossMode.LOGGER.warn("{}: cubes on bone '{}' are ignored by the poly mesh loader", resourcePath, name);
			}
		}
		for (JsonObject bone : byName.values()) {
			String name = GsonHelper.getAsString(bone, "name");
			String parent = GsonHelper.getAsString(bone, "parent", null);
			if (parent == null || !byName.containsKey(parent) || parent.equals(name)) {
				if (parent != null) {
					BossMode.LOGGER.warn("{}: bone '{}' has invalid parent '{}', treating it as a root bone", resourcePath, name, parent);
				}
				roots.add(name);
			} else {
				children.computeIfAbsent(parent, k -> new ArrayList<>()).add(name);
			}
		}

		GeoBone[] topLevel = new GeoBone[roots.size()];
		for (int i = 0; i < roots.size(); i++) {
			topLevel[i] = bakeBone(resourcePath, null, roots.get(i), byName, children, textureWidth, textureHeight);
		}

		ModelProperties properties = new ModelProperties(resourcePath,
				GsonHelper.getAsString(description, "identifier", "geometry.unknown"),
				description.has("visible_bounds_width") ? GsonHelper.getAsFloat(description, "visible_bounds_width") : null,
				description.has("visible_bounds_height") ? GsonHelper.getAsFloat(description, "visible_bounds_height") : null,
				readVec3(description, "visible_bounds_offset"),
				textureWidth, textureHeight);
		return new BakedGeoModel(topLevel, Map.<String, GeoLocator>of(), properties);
	}

	private GeoBone bakeBone(Identifier resourcePath, GeoBone parent, String name, Map<String, JsonObject> byName,
			Map<String, List<String>> childMap, int textureWidth, int textureHeight) {
		JsonObject json = byName.get(name);
		Vec3 pivot = readVec3(json, "pivot");
		Vec3 rotation = readVec3(json, "rotation");
		List<String> childNames = childMap.getOrDefault(name, List.of());
		GeoBone[] childBones = new GeoBone[childNames.size()];

		float[][] mesh = json.has("poly_mesh")
				? bakeMesh(resourcePath, name, GsonHelper.getAsJsonObject(json, "poly_mesh"), textureWidth, textureHeight)
				: new float[][] {new float[0], new float[0], new float[0]};

		// Same conventions as GeckoLib's GeometryBone#bake: pivot X and rotation X/Y are negated
		PolyMeshGeoBone bone = new PolyMeshGeoBone(parent, name, childBones, new GeoLocator[0],
				(float) -pivot.x, (float) pivot.y, (float) pivot.z,
				(float) Math.toRadians(-rotation.x), (float) Math.toRadians(-rotation.y), (float) Math.toRadians(rotation.z),
				mesh[0], mesh[1], mesh[2]);
		for (int i = 0; i < childBones.length; i++) {
			childBones[i] = bakeBone(resourcePath, bone, childNames.get(i), byName, childMap, textureWidth, textureHeight);
		}
		return bone;
	}

	/** @return {positions, normals, uvs} unrolled per triangle corner, in GeckoLib render space */
	private static float[][] bakeMesh(Identifier resourcePath, String boneName, JsonObject polyMesh, int textureWidth, int textureHeight) {
		boolean normalizedUvs = GsonHelper.getAsBoolean(polyMesh, "normalized_uvs", true);
		JsonArray positions = GsonHelper.getAsJsonArray(polyMesh, "positions");
		JsonArray normals = GsonHelper.getAsJsonArray(polyMesh, "normals", new JsonArray());
		JsonArray uvs = GsonHelper.getAsJsonArray(polyMesh, "uvs", new JsonArray());
		JsonArray polys = GsonHelper.getAsJsonArray(polyMesh, "polys");

		List<int[]> corners = new ArrayList<>();
		for (JsonElement polyElement : polys) {
			JsonArray poly = polyElement.getAsJsonArray();
			if (poly.size() < 3) {
				continue;
			}
			// fan-triangulate (quads are allowed by the Bedrock format)
			for (int i = 1; i < poly.size() - 1; i++) {
				corners.add(readCorner(poly.get(0)));
				corners.add(readCorner(poly.get(i)));
				corners.add(readCorner(poly.get(i + 1)));
			}
		}

		float[] outPositions = new float[corners.size() * 3];
		float[] outNormals = new float[corners.size() * 3];
		float[] outUvs = new float[corners.size() * 2];
		for (int i = 0; i < corners.size(); i++) {
			int[] corner = corners.get(i);
			JsonArray p = positions.get(corner[0]).getAsJsonArray();
			outPositions[i * 3] = -p.get(0).getAsFloat() * PIXEL;
			outPositions[i * 3 + 1] = p.get(1).getAsFloat() * PIXEL;
			outPositions[i * 3 + 2] = p.get(2).getAsFloat() * PIXEL;

			if (corner[1] >= 0 && corner[1] < normals.size()) {
				JsonArray n = normals.get(corner[1]).getAsJsonArray();
				outNormals[i * 3] = -n.get(0).getAsFloat();
				outNormals[i * 3 + 1] = n.get(1).getAsFloat();
				outNormals[i * 3 + 2] = n.get(2).getAsFloat();
			} else {
				outNormals[i * 3 + 1] = 1.0F;
			}

			if (corner[2] >= 0 && corner[2] < uvs.size()) {
				JsonArray uv = uvs.get(corner[2]).getAsJsonArray();
				float u = uv.get(0).getAsFloat();
				float v = uv.get(1).getAsFloat();
				if (!normalizedUvs) {
					u /= textureWidth;
					v /= textureHeight;
				}
				outUvs[i * 2] = u;
				outUvs[i * 2 + 1] = 1.0F - v;
			}
		}
		if (corners.isEmpty()) {
			BossMode.LOGGER.warn("{}: poly_mesh of bone '{}' has no polygons", resourcePath, boneName);
		}
		return new float[][] {outPositions, outNormals, outUvs};
	}

	private static int[] readCorner(JsonElement element) {
		JsonArray corner = element.getAsJsonArray();
		return new int[] {corner.get(0).getAsInt(), corner.size() > 1 ? corner.get(1).getAsInt() : -1, corner.size() > 2 ? corner.get(2).getAsInt() : -1};
	}

	private static Vec3 readVec3(JsonObject json, String key) {
		if (!json.has(key)) {
			return Vec3.ZERO;
		}
		JsonArray array = GsonHelper.getAsJsonArray(json, key);
		return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
	}
}
