package com.bossmode.entity.minion;

import com.bossmode.config.BossConfig;
import com.bossmode.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Minion types the bosses can call - see BossPhase / SovereignPhase / the raid bosses' phase tables. */
public enum MinionType {
	/** vanilla Vex bound to the dragon ("Shard Wraith"), expires after a while */
	WRAITH,
	/** Shard Hound - melee pack hunter */
	HOUND,
	/** Shard Spitter - keeps its distance and spits crystal shards */
	SPITTER,
	/** Volatile Hound - rushes in and detonates */
	VOLATILE,
	/** vanilla Breeze answering the Tempest Wyvern, tagged into the shard faction */
	BREEZE;

	/** Entity tag that makes any mob a member of the bosses' side (see ShardFaction#isMember). */
	public static final String FACTION_TAG = "boss_mode_minion";

	/** Spawned a little above the ground. */
	public boolean flies() {
		return this == WRAITH;
	}

	public @Nullable Mob spawn(ServerLevel level, Mob boss, Vec3 pos, @Nullable LivingEntity target) {
		BlockPos blockPos = BlockPos.containing(pos);
		Mob mob = switch (this) {
			case WRAITH -> EntityTypes.VEX.create(level, EntitySpawnReason.MOB_SUMMONED);
			case HOUND -> create(level, ModEntities.SHARD_HOUND);
			case SPITTER -> create(level, ModEntities.SHARD_SPITTER);
			case VOLATILE -> create(level, ModEntities.VOLATILE_HOUND);
			case BREEZE -> EntityTypes.BREEZE.create(level, EntitySpawnReason.MOB_SUMMONED);
		};
		if (mob == null) {
			return null;
		}
		mob.snapTo(pos.x, pos.y, pos.z, boss.getYRot(), 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPos), EntitySpawnReason.MOB_SUMMONED, null);
		if (mob instanceof Vex vex) {
			vex.setOwner(boss);
			vex.setBoundOrigin(blockPos);
			vex.setLimitedLife(BossConfig.MINION_LIFETIME);
		} else if (mob instanceof ShardHoundEntity hound) {
			hound.setSummoner(boss);
		} else if (mob instanceof Breeze) {
			mob.addTag(FACTION_TAG);
			mob.setPersistenceRequired();
		}
		if (target != null) {
			mob.setTarget(target);
			if (mob instanceof Breeze breeze) {
				breeze.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
			}
		}
		level.addFreshEntity(mob);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.5, pos.z, 16, 0.4, 0.5, 0.4, 0.05);
		return mob;
	}

	private static <T extends Mob> @Nullable T create(ServerLevel level, EntityType<T> type) {
		return type.create(level, EntitySpawnReason.MOB_SUMMONED);
	}
}
