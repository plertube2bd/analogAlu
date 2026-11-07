package com.bossmode.registry;

import com.bossmode.BossMode;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events. The files they play are declared in {@code assets/boss_mode/sounds.json} (vanilla sound files,
 * re-pitched; drop custom .ogg files in and repoint sounds.json to replace them).
 */
public final class ModSounds {
	public static final SoundEvent SHARD_DRAGON_AMBIENT = register("entity.shard_dragon.ambient");
	public static final SoundEvent SHARD_DRAGON_HURT = register("entity.shard_dragon.hurt");
	public static final SoundEvent SHARD_DRAGON_DEATH = register("entity.shard_dragon.death");
	public static final SoundEvent SHARD_DRAGON_SPAWN = register("entity.shard_dragon.spawn");
	public static final SoundEvent SHARD_DRAGON_ROAR = register("entity.shard_dragon.roar");
	public static final SoundEvent SHARD_DRAGON_BITE = register("entity.shard_dragon.bite");
	public static final SoundEvent SHARD_DRAGON_TAIL = register("entity.shard_dragon.tail");
	public static final SoundEvent SHARD_DRAGON_SLAM = register("entity.shard_dragon.slam");
	public static final SoundEvent SHARD_DRAGON_VOLLEY = register("entity.shard_dragon.volley");
	public static final SoundEvent SHARD_DRAGON_TEMPEST = register("entity.shard_dragon.tempest");
	public static final SoundEvent SHARD_DRAGON_CHARGE = register("entity.shard_dragon.charge");
	public static final SoundEvent SHARD_DRAGON_SUMMON = register("entity.shard_dragon.summon");
	public static final SoundEvent SHARD_DRAGON_IMPACT = register("entity.shard_dragon.impact");
	public static final SoundEvent SHARD_DRAGON_SPIKES = register("entity.shard_dragon.spikes");
	public static final SoundEvent SHARD_DRAGON_BEAM_CHARGE = register("entity.shard_dragon.beam_charge");
	public static final SoundEvent SHARD_DRAGON_BEAM = register("entity.shard_dragon.beam");
	public static final SoundEvent SHARD_DRAGON_VOID_ROAR = register("entity.shard_dragon.void_roar");
	public static final SoundEvent SHARD_DRAGON_CATACLYSM_CHARGE = register("entity.shard_dragon.cataclysm_charge");
	public static final SoundEvent SHARD_DRAGON_CATACLYSM = register("entity.shard_dragon.cataclysm");
	public static final SoundEvent SHARD_DRAGON_DEFLECT = register("entity.shard_dragon.deflect");
	public static final SoundEvent WYRM_SHARD_IMPACT = register("entity.wyrm_shard.impact");

	public static final SoundEvent SHARD_HOUND_AMBIENT = register("entity.shard_hound.ambient");
	public static final SoundEvent SHARD_HOUND_HURT = register("entity.shard_hound.hurt");
	public static final SoundEvent SHARD_HOUND_DEATH = register("entity.shard_hound.death");
	public static final SoundEvent SHARD_SPITTER_SPIT = register("entity.shard_spitter.spit");
	public static final SoundEvent VOLATILE_HOUND_FUSE = register("entity.volatile_hound.fuse");

	public static final SoundEvent SHARD_WARLORD_AMBIENT = register("entity.shard_warlord.ambient");
	public static final SoundEvent SHARD_WARLORD_HURT = register("entity.shard_warlord.hurt");
	public static final SoundEvent SHARD_WARLORD_DEATH = register("entity.shard_warlord.death");
	public static final SoundEvent SHARD_WARLORD_SWING = register("entity.shard_warlord.swing");
	public static final SoundEvent SHARD_WARLORD_CAST = register("entity.shard_warlord.cast");
	public static final SoundEvent SHARD_WARLORD_WARCRY = register("entity.shard_warlord.warcry");
	public static final SoundEvent SHARD_WARLORD_BLINK = register("entity.shard_warlord.blink");

	public static final SoundEvent VOID_SOVEREIGN_AMBIENT = register("entity.void_sovereign.ambient");
	public static final SoundEvent VOID_SOVEREIGN_HURT = register("entity.void_sovereign.hurt");
	public static final SoundEvent VOID_SOVEREIGN_DEATH = register("entity.void_sovereign.death");
	public static final SoundEvent VOID_SOVEREIGN_SPAWN = register("entity.void_sovereign.spawn");
	public static final SoundEvent VOID_SOVEREIGN_ROAR = register("entity.void_sovereign.roar");
	public static final SoundEvent VOID_SOVEREIGN_CLAW = register("entity.void_sovereign.claw");
	public static final SoundEvent VOID_SOVEREIGN_LASH = register("entity.void_sovereign.lash");
	public static final SoundEvent VOID_SOVEREIGN_POUND = register("entity.void_sovereign.pound");
	public static final SoundEvent VOID_SOVEREIGN_RIFT = register("entity.void_sovereign.rift");
	public static final SoundEvent VOID_SOVEREIGN_GRASP = register("entity.void_sovereign.grasp");
	public static final SoundEvent VOID_SOVEREIGN_HOLE = register("entity.void_sovereign.black_hole");
	public static final SoundEvent VOID_SOVEREIGN_HOLE_COLLAPSE = register("entity.void_sovereign.black_hole_collapse");
	public static final SoundEvent VOID_SOVEREIGN_STEP = register("entity.void_sovereign.step");
	public static final SoundEvent VOID_SOVEREIGN_ECLIPSE = register("entity.void_sovereign.eclipse");
	public static final SoundEvent VOID_SOVEREIGN_METEOR = register("entity.void_sovereign.meteor");
	public static final SoundEvent VOID_SOVEREIGN_DRAIN = register("entity.void_sovereign.drain");
	public static final SoundEvent VOID_SOVEREIGN_SUMMON = register("entity.void_sovereign.summon");
	public static final SoundEvent VOID_SOVEREIGN_SHIELD = register("entity.void_sovereign.shield");
	public static final SoundEvent VOID_SOVEREIGN_COUNTER = register("entity.void_sovereign.counter");
	public static final SoundEvent VOID_SOVEREIGN_GRAVITY = register("entity.void_sovereign.gravity");
	public static final SoundEvent VOID_SOVEREIGN_CARAPACE = register("entity.void_sovereign.carapace");
	public static final SoundEvent VOID_SOVEREIGN_SWEEP = register("entity.void_sovereign.sweep");
	public static final SoundEvent VOID_SOVEREIGN_CHAINS = register("entity.void_sovereign.chains");
	public static final SoundEvent VOID_SOVEREIGN_OBLIVION = register("entity.void_sovereign.oblivion");
	public static final SoundEvent VOID_ORB_CAST = register("entity.void_orb.cast");
	public static final SoundEvent VOID_ORB_BURST = register("entity.void_orb.burst");

	public static final SoundEvent TEMPEST_WYVERN_AMBIENT = register("entity.tempest_wyvern.ambient");
	public static final SoundEvent TEMPEST_WYVERN_HURT = register("entity.tempest_wyvern.hurt");
	public static final SoundEvent TEMPEST_WYVERN_DEATH = register("entity.tempest_wyvern.death");
	public static final SoundEvent TEMPEST_WYVERN_SPAWN = register("entity.tempest_wyvern.spawn");
	public static final SoundEvent TEMPEST_WYVERN_ROAR = register("entity.tempest_wyvern.roar");
	public static final SoundEvent TEMPEST_WYVERN_STRIKE = register("entity.tempest_wyvern.strike");
	public static final SoundEvent TEMPEST_WYVERN_FLAP = register("entity.tempest_wyvern.flap");
	public static final SoundEvent TEMPEST_WYVERN_GUST = register("entity.tempest_wyvern.gust");
	public static final SoundEvent TEMPEST_WYVERN_BREATH = register("entity.tempest_wyvern.breath");
	public static final SoundEvent TEMPEST_WYVERN_THUNDER = register("entity.tempest_wyvern.thunder");
	public static final SoundEvent TEMPEST_WYVERN_DIVE = register("entity.tempest_wyvern.dive");
	public static final SoundEvent TEMPEST_WYVERN_CYCLONE = register("entity.tempest_wyvern.cyclone");
	public static final SoundEvent TEMPEST_WYVERN_FEATHERS = register("entity.tempest_wyvern.feathers");
	public static final SoundEvent TEMPEST_WYVERN_STATIC = register("entity.tempest_wyvern.static");
	public static final SoundEvent TEMPEST_WYVERN_SUMMON = register("entity.tempest_wyvern.summon");

	public static final SoundEvent BASTION_BEHEMOTH_AMBIENT = register("entity.bastion_behemoth.ambient");
	public static final SoundEvent BASTION_BEHEMOTH_HURT = register("entity.bastion_behemoth.hurt");
	public static final SoundEvent BASTION_BEHEMOTH_DEATH = register("entity.bastion_behemoth.death");
	public static final SoundEvent BASTION_BEHEMOTH_SPAWN = register("entity.bastion_behemoth.spawn");
	public static final SoundEvent BASTION_BEHEMOTH_ROAR = register("entity.bastion_behemoth.roar");
	public static final SoundEvent BASTION_BEHEMOTH_STEP = register("entity.bastion_behemoth.step");
	public static final SoundEvent BASTION_BEHEMOTH_MAUL = register("entity.bastion_behemoth.maul");
	public static final SoundEvent BASTION_BEHEMOTH_QUAKE = register("entity.bastion_behemoth.quake");
	public static final SoundEvent BASTION_BEHEMOTH_THROW = register("entity.bastion_behemoth.throw");
	public static final SoundEvent BASTION_BEHEMOTH_ROLL = register("entity.bastion_behemoth.roll");
	public static final SoundEvent BASTION_BEHEMOTH_FORTIFY = register("entity.bastion_behemoth.fortify");
	public static final SoundEvent BASTION_BEHEMOTH_CLANG = register("entity.bastion_behemoth.clang");
	public static final SoundEvent BASTION_BEHEMOTH_SPIKES = register("entity.bastion_behemoth.spikes");
	public static final SoundEvent BASTION_BEHEMOTH_MAGMA = register("entity.bastion_behemoth.magma");
	public static final SoundEvent BASTION_BEHEMOTH_SUMMON = register("entity.bastion_behemoth.summon");
	public static final SoundEvent BOULDER_IMPACT = register("entity.boulder.impact");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = BossMode.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
		// class loading registers the fields above
	}
}
