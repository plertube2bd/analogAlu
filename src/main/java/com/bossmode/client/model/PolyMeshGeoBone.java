package com.bossmode.client.model;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A GeckoLib bone that renders an arbitrary triangle mesh (a Bedrock {@code poly_mesh}) instead of cubes.
 * <p>
 * Vertex data is baked once at resource-load time into flat arrays already converted to GeckoLib render space
 * (block units, X negated like GeckoLib's own cube baking, texture V flipped), so a frame only multiplies each
 * corner by the bone's current pose. GeckoLib positions the {@link PoseStack} for this bone (pivot, animated
 * rotation / translation / scale, parent chain) before {@link #render} is called, exactly as for cube bones.
 */
public final class PolyMeshGeoBone extends GeoBone {
	/** x, y, z per triangle corner (3 corners per triangle). */
	private final float[] positions;
	/** nx, ny, nz per triangle corner. */
	private final float[] normals;
	/** u, v per triangle corner. */
	private final float[] uvs;
	private final int triangleCount;

	public PolyMeshGeoBone(@Nullable GeoBone parent, String name, GeoBone[] children, GeoLocator[] locators,
			float pivotX, float pivotY, float pivotZ, float rotX, float rotY, float rotZ,
			float[] positions, float[] normals, float[] uvs) {
		super(parent, name, children, locators, pivotX, pivotY, pivotZ, rotX, rotY, rotZ);
		this.positions = positions;
		this.normals = normals;
		this.uvs = uvs;
		this.triangleCount = positions.length / 9;
	}

	public int triangleCount() {
		return this.triangleCount;
	}

	@Override
	public <R extends GeoRenderState> void render(RenderPassInfo<R> renderPassInfo, PoseStack poseStack, VertexConsumer vertexConsumer,
			int packedLight, int packedOverlay, int renderColor) {
		if (this.triangleCount == 0 || (this.frameSnapshot != null && this.frameSnapshot.isHidden())) {
			return;
		}
		PoseStack.Pose last = poseStack.last();
		Matrix4f pose = last.pose();
		Matrix3f normalMatrix = last.normal();
		Vector3f position = new Vector3f();
		Vector3f normal = new Vector3f();

		for (int tri = 0; tri < this.triangleCount; tri++) {
			// entity render types draw quads: emit the triangle as a degenerate quad (last corner repeated)
			for (int corner = 0; corner < 4; corner++) {
				int c = tri * 3 + Math.min(corner, 2);
				pose.transformPosition(this.positions[c * 3], this.positions[c * 3 + 1], this.positions[c * 3 + 2], position);
				normalMatrix.transform(this.normals[c * 3], this.normals[c * 3 + 1], this.normals[c * 3 + 2], normal);
				if (normal.lengthSquared() > 1.0E-8F) {
					normal.normalize();
				}
				vertexConsumer.addVertex(position.x, position.y, position.z, renderColor,
						this.uvs[c * 2], this.uvs[c * 2 + 1], packedOverlay, packedLight, normal.x, normal.y, normal.z);
			}
		}
	}
}
