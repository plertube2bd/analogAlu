package com.bossmode.client;

import com.bossmode.client.hud.DecoratedBossBarHud;
import com.bossmode.client.model.PolyMeshGeoLoader;
import com.bossmode.client.renderer.RaidBossRenderer;
import com.bossmode.client.renderer.ShardDragonRenderer;
import com.bossmode.client.renderer.ShardHoundRenderer;
import com.bossmode.client.renderer.ShardWarlordRenderer;
import com.bossmode.client.renderer.VoidPylonRenderer;
import com.bossmode.client.renderer.VoidSovereignRenderer;
import com.bossmode.registry.ModEntities;
import com.geckolib.util.GeckoLibUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Client-only entrypoint: model loading, renderers and the decorated boss bar (never loaded on a dedicated server). */
public class BossModeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// must happen before the first resource reload: lets GeckoLib bake the Blender poly meshes
		GeckoLibUtil.addResourceLoader(PolyMeshGeoLoader::handles, new PolyMeshGeoLoader());

		// Fabric API 0.161 (26.3) deprecates EntityRendererRegistry; vanilla's registry is made accessible by
		// fabric-transitive-access-wideners-v1 instead.
		EntityRenderers.register(ModEntities.SHARD_DRAGON, ShardDragonRenderer::new);
		EntityRenderers.register(ModEntities.SHARD_WARLORD, ShardWarlordRenderer::new);
		EntityRenderers.register(ModEntities.SHARD_HOUND, context -> new ShardHoundRenderer(context, "shard_hound"));
		EntityRenderers.register(ModEntities.SHARD_SPITTER, context -> new ShardHoundRenderer(context, "shard_spitter"));
		EntityRenderers.register(ModEntities.VOLATILE_HOUND, context -> new ShardHoundRenderer(context, "volatile_hound"));
		EntityRenderers.register(ModEntities.WYRM_SHARD, context -> new ThrownItemRenderer<>(context, 1.75F, true));
		EntityRenderers.register(ModEntities.VOID_SOVEREIGN, VoidSovereignRenderer::new);
		EntityRenderers.register(ModEntities.VOID_PYLON, VoidPylonRenderer::new);
		EntityRenderers.register(ModEntities.VOID_ORB, context -> new ThrownItemRenderer<>(context, 2.2F, true));
		EntityRenderers.register(ModEntities.TEMPEST_WYVERN, context -> new RaidBossRenderer<>(context, "tempest_wyvern", 1.6F, 0.6));
		EntityRenderers.register(ModEntities.BASTION_BEHEMOTH, context -> new RaidBossRenderer<>(context, "bastion_behemoth", 2.4F, 0.5));
		EntityRenderers.register(ModEntities.BOULDER, context -> new ThrownItemRenderer<>(context, 3.5F, true));

		// decorated boss bars for the dragon, the raid bosses, the Sovereign and the mid-bosses (other boss bars keep the vanilla look)
		HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, vanilla -> new DecoratedBossBarHud());
	}
}
