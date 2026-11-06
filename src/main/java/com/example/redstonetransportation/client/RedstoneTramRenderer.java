package com.example.redstonetransportation.client;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.vehicle.TramVehicle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Draws a tram from its own model instead of the vanilla minecart: an opaque body, a translucent
 * glass pass, and full-bright lamps that show which end leads.
 */
public final class RedstoneTramRenderer<T extends AbstractMinecart & TramVehicle>
        extends AbstractMinecartRenderer<T, TramRenderState> {
    public static final Look TRAM = new Look("redstone_tram", TramGeometry::tramBody, TramGeometry::tramGlass, TramGeometry::tramLights);
    public static final Look CARGO = new Look("cargo_tram", TramGeometry::cargoBody, null, TramGeometry::cargoLights);

    private static final int HEADLIGHT = ARGB.color(255, 255, 244, 214);
    private static final int TAIL_LIGHT = ARGB.color(255, 255, 40, 28);
    private static final int PARKED_LIGHT = ARGB.color(255, 150, 120, 72);
    private static final float MOVING_SPEED = 0.01F;
    private static final float FULL_GLOW_SPEED = 1.0F;

    /** Which model layers and texture make up one vehicle. */
    public static final class Look {
        private final Identifier texture;
        private final ModelLayerLocation body;
        private final ModelLayerLocation glass;
        private final ModelLayerLocation lights;
        private final Supplier<LayerDefinition> bodyLayer;
        private final Supplier<LayerDefinition> glassLayer;
        private final Supplier<LayerDefinition> lightsLayer;

        private Look(String name, Supplier<LayerDefinition> bodyLayer,
                     Supplier<LayerDefinition> glassLayer, Supplier<LayerDefinition> lightsLayer) {
            Identifier id = Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, name);
            this.texture = Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "textures/entity/" + name + ".png");
            this.body = new ModelLayerLocation(id, "main");
            this.glass = new ModelLayerLocation(id, "glass");
            this.lights = new ModelLayerLocation(id, "lights");
            this.bodyLayer = bodyLayer;
            this.glassLayer = glassLayer;
            this.lightsLayer = lightsLayer;
        }

        public void registerLayers() {
            ModelLayerRegistry.registerModelLayer(body, bodyLayer::get);
            ModelLayerRegistry.registerModelLayer(lights, lightsLayer::get);
            if (glassLayer != null) ModelLayerRegistry.registerModelLayer(glass, glassLayer::get);
        }
    }

    private final Identifier texture;
    private final TramModel body;
    private final ModelPart glass;
    private final ModelPart frontLamps;
    private final ModelPart rearLamps;
    private final ModelPart strip;

    public RedstoneTramRenderer(EntityRendererProvider.Context context, Look look) {
        super(context, ModelLayers.MINECART);
        this.texture = look.texture;
        this.body = new TramModel(context.bakeLayer(look.body));
        this.glass = look.glassLayer != null ? context.bakeLayer(look.glass) : null;
        ModelPart lights = context.bakeLayer(look.lights);
        this.frontLamps = lights.getChild("lamps_front");
        this.rearLamps = lights.getChild("lamps_rear");
        this.strip = lights.getChild("strip");
    }

    @Override
    public TramRenderState createRenderState() {
        return new TramRenderState();
    }

    @Override
    public void extractRenderState(T entity, TramRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.wheelAngle = entity.wheelAngle(partialTicks);

        Vec3 movement = entity.getDeltaMovement();
        state.speed = (float) movement.horizontalDistance();
        // After the renderer's flip, the model's +X axis lies along this world direction.
        float yaw = state.yRot * Mth.DEG_TO_RAD;
        double alongModel = movement.x * -Mth.cos(yaw) + movement.z * Mth.sin(yaw);
        state.leadingEnd = state.speed < MOVING_SPEED ? 0 : (alongModel > 0.0D ? 1 : -1);
    }

    @Override
    protected AABB getBoundingBoxForCulling(T entity, float partialTicks) {
        // The body is far taller than the hitbox, and its noses reach past it.
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.2D).expandTowards(0.0D, 1.5D, 0.0D);
    }

    @Override
    public void submit(TramRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        submitNameDisplay(state, poseStack, collector, camera);

        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, state.yRot);
        poseStack.rotateDegrees(Axis.ZP, -state.xRot);
        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        int light = state.lightCoords;
        collector.submitModel(body, state, poseStack, texture, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (glass != null) {
            collector.submitModelPart(glass, poseStack, RenderTypes.entityTranslucent(texture), light, OverlayTexture.NO_OVERLAY, null);
        }

        RenderType glow = RenderTypes.eyes(texture);
        int full = LightCoordsUtil.FULL_BRIGHT;
        collector.submitModelPart(frontLamps, poseStack, glow, full, OverlayTexture.NO_OVERLAY, null, lampColor(state.leadingEnd, 1));
        collector.submitModelPart(rearLamps, poseStack, glow, full, OverlayTexture.NO_OVERLAY, null, lampColor(state.leadingEnd, -1));
        collector.submitModelPart(strip, poseStack, glow, full, OverlayTexture.NO_OVERLAY, null, stripColor(state.speed));
        poseStack.popPose();
    }

    private static int lampColor(int leadingEnd, int end) {
        if (leadingEnd == 0) return PARKED_LIGHT;
        return leadingEnd == end ? HEADLIGHT : TAIL_LIGHT;
    }

    /** The redstone line brightens with speed. */
    private static int stripColor(float speed) {
        float level = 0.3F + 0.7F * Math.min(speed / FULL_GLOW_SPEED, 1.0F);
        return ARGB.color(255, Mth.floor(255 * level), Mth.floor(48 * level), Mth.floor(36 * level));
    }
}
