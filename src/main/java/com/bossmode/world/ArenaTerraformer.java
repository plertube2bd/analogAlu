package com.bossmode.world;

import com.bossmode.BossMode;
import com.bossmode.block.ShardAltarBlock;
import com.bossmode.block.VoidAltarBlock;
import com.bossmode.config.BastionConfig;
import com.bossmode.config.TempestConfig;
import com.bossmode.config.SovereignConfig;
import com.bossmode.config.BossConfig;
import com.bossmode.entity.boss.ShardDragonEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the land around a Shard Altar into the dragon's arena when the ritual completes.
 * <p>
 * A circle of {@link BossConfig#ARENA_RADIUS} blocks around the altar is levelled to the arena floor: everything above
 * it (hills, trees, water, buildings) is cleared, holes below it are filled, and the surface becomes a corrupted
 * crystal waste - blackstone and basalt with obsidian veins, amethyst patches and a rune circle where the dragon rose.
 * A {@link BossConfig#ARENA_BLEND}-block ring slopes back to the original terrain, obsidian spires are left standing
 * as cover (the dragon cannot crush obsidian; they matter for Cataclysm), and the biome becomes Shard Wastes (violet
 * sky and fog, no rain, no natural spawns). The altar and its dais ({@link BossConfig#ARENA_PROTECT_RADIUS}) and
 * unbreakable blocks are never touched.
 * <p>
 * The work spreads outward from the altar as a visible shockwave, limited to {@link BossConfig#ARENA_BLOCKS_PER_TICK}
 * block changes per tick so even a mountain is flattened without a lag spike. Unfinished jobs are completed when the
 * server stops, so an arena is never left half-made.
 */
public final class ArenaTerraformer {
	public static final ResourceKey<Biome> SHARD_WASTES = ResourceKey.create(Registries.BIOME, BossMode.id("shard_wastes"));
	public static final ResourceKey<Biome> VOID_EXPANSE = ResourceKey.create(Registries.BIOME, BossMode.id("void_expanse"));
	public static final ResourceKey<Biome> STORM_REACH = ResourceKey.create(Registries.BIOME, BossMode.id("storm_reach"));
	public static final ResourceKey<Biome> EMBER_WASTE = ResourceKey.create(Registries.BIOME, BossMode.id("ember_waste"));

	/** One arena per boss: the Shard Dragon's crystal waste, the Wyvern's storm plateau, the Behemoth's scorched waste, the Void. */
	public enum Theme {
		/** Blackstone and basalt, amethyst patches, obsidian veins, amethyst-crowned spires; Shard Wastes biome. */
		SHARD(BossConfig.ARENA_RADIUS, SHARD_WASTES, Blocks.BLACKSTONE.defaultBlockState()),
		/** Deepslate and sculk, crying-obsidian veins, spires tipped with end rods; Void Expanse biome. */
		VOID(SovereignConfig.ARENA_RADIUS, VOID_EXPANSE, Blocks.COBBLED_DEEPSLATE.defaultBlockState()),
		/** Andesite and tuff swept bare, calcite and oxidized-copper rune rings, stone spires carrying lightning rods; Storm Reach biome. */
		STORM(TempestConfig.ARENA_RADIUS, STORM_REACH, Blocks.ANDESITE.defaultBlockState()),
		/** Basalt and blackstone, thin magma veins, shroomlight rune rings, basalt spires crowned with shroomlight; Ember Waste biome. */
		EMBER(BastionConfig.ARENA_RADIUS, EMBER_WASTE, Blocks.BASALT.defaultBlockState());

		final int radius;
		final ResourceKey<Biome> biome;
		final BlockState filler;

		Theme(int radius, ResourceKey<Biome> biome, BlockState filler) {
			this.radius = radius;
			this.biome = biome;
			this.filler = filler;
		}
	}
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final Map<ServerLevel, List<Job>> JOBS = new IdentityHashMap<>();

	private ArenaTerraformer() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(ArenaTerraformer::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			JOBS.forEach((level, jobs) -> jobs.forEach(job -> job.work(level, Integer.MAX_VALUE)));
			JOBS.clear();
		});
	}

	/**
	 * Starts levelling the land around {@code altar}. {@code floorY} is the y of the new surface blocks (one below the
	 * dragon's feet); the rune circle is drawn around {@code arenaCenter}.
	 */
	public static void start(ServerLevel level, BlockPos altar, Vec3 arenaCenter, int floorY) {
		start(level, altar, arenaCenter, floorY, Theme.SHARD);
	}

	public static void start(ServerLevel level, BlockPos altar, Vec3 arenaCenter, int floorY, Theme theme) {
		Job job = new Job(level, altar, arenaCenter, floorY, theme);
		JOBS.computeIfAbsent(level, l -> new ArrayList<>()).add(job);
		job.begin(level);
	}

	public static boolean isBusy(ServerLevel level) {
		List<Job> jobs = JOBS.get(level);
		return jobs != null && !jobs.isEmpty();
	}

	private static void tick(ServerLevel level) {
		List<Job> jobs = JOBS.get(level);
		if (jobs == null || jobs.isEmpty()) {
			return;
		}
		Job job = jobs.getFirst();
		job.work(level, BossConfig.ARENA_BLOCKS_PER_TICK);
		if (job.done()) {
			jobs.removeFirst();
			job.finish(level);
		}
	}

	// ================================================================================================ job
	private static final class Job {
		private final BlockPos altar;
		private final Vec3 arenaCenter;
		private final int floorY;
		private final Theme theme;
		private final int radius;
		private final int outer;
		/** Column offsets from the altar, nearest first - the wave front. */
		private final int[][] columns;
		private final Map<Long, int[]> spires = new HashMap<>();
		private final SimplexNoise patches;
		private final SimplexNoise veins;
		private final long seed;
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		private int next;
		private int ticks;
		private long changed;
		private long slowestNanos;

		Job(ServerLevel level, BlockPos altar, Vec3 arenaCenter, int floorY, Theme theme) {
			this.theme = theme;
			this.radius = theme.radius;
			this.outer = theme.radius + BossConfig.ARENA_BLEND;
			this.altar = altar.immutable();
			this.arenaCenter = arenaCenter;
			this.floorY = floorY;
			this.seed = level.getSeed() ^ altar.asLong();
			RandomSource random = RandomSource.create(this.seed);
			this.patches = new SimplexNoise(random);
			this.veins = new SimplexNoise(random);
			List<int[]> list = new ArrayList<>();
			for (int dx = -outer; dx <= outer; dx++) {
				for (int dz = -outer; dz <= outer; dz++) {
					if (dx * dx + dz * dz <= outer * outer) {
						list.add(new int[]{dx, dz});
					}
				}
			}
			list.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
			this.columns = list.toArray(int[][]::new);
			planSpires(random);
		}

		/** Scatters obsidian spires (cover) across the arena, away from the altar, the rune circle and each other. */
		private void planSpires(RandomSource random) {
			List<int[]> placed = new ArrayList<>();
			int count = BossConfig.ARENA_SPIRES * this.radius / BossConfig.ARENA_RADIUS;
			for (int i = 0; i < count; i++) {
				for (int attempt = 0; attempt < 40; attempt++) {
					double angle = random.nextDouble() * Mth.TWO_PI;
					double distance = 16 + random.nextDouble() * (this.radius - 21);
					int x = altar.getX() + Mth.floor(Math.cos(angle) * distance);
					int z = altar.getZ() + Mth.floor(Math.sin(angle) * distance);
					if (Math.hypot(x + 0.5 - arenaCenter.x, z + 0.5 - arenaCenter.z) < 16
							|| placed.stream().anyMatch(p -> Math.hypot(p[0] - x, p[1] - z) < 10)) {
						continue;
					}
					placed.add(new int[]{x, z});
					int height = 5 + random.nextInt(5);
					int size = 2 + random.nextInt(2);
					for (int ox = 0; ox < size; ox++) {
						for (int oz = 0; oz < size; oz++) {
							// tallest at the core, stepping down to the edges; the tallest column carries the crystal
							int edge = Math.max(Math.abs(2 * ox - (size - 1)), Math.abs(2 * oz - (size - 1)));
							int h = Math.max(2, height - edge * (1 + random.nextInt(2)) - random.nextInt(2));
							boolean capped = ox == size / 2 && oz == size / 2;
							spires.put(BlockPos.asLong(x + ox, 0, z + oz), new int[]{capped ? height : Math.min(h, height - 1), capped ? 1 : 0});
						}
					}
					break;
				}
			}
		}

		void begin(ServerLevel level) {
			Vec3 center = Vec3.atBottomCenterOf(altar);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 6.0F, 0.6F);
			// nobody standing on a hill that is about to disappear should die from the fall
			double range = (outer + 8) * (outer + 8);
			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(center.x, player.getY(), center.z) <= range) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, true));
				}
			}
			applyBiome(level);
		}

		boolean done() {
			return this.next >= this.columns.length;
		}

		void work(ServerLevel level, int budget) {
			long start = System.nanoTime();
			int spent = 0;
			while (this.next < this.columns.length && spent < budget) {
				int[] c = this.columns[this.next++];
				spent += column(level, this.altar.getX() + c[0], this.altar.getZ() + c[1], Math.sqrt(c[0] * c[0] + c[1] * c[1]));
			}
			this.changed += spent;
			this.ticks++;
			this.slowestNanos = Math.max(this.slowestNanos, System.nanoTime() - start);
			if (this.next > 0) {
				int[] front = this.columns[Math.min(this.next, this.columns.length) - 1];
				double radius = Math.sqrt(front[0] * front[0] + front[1] * front[1]);
				waveEffects(level, radius);
				rescueEntities(level, radius);
			}
		}

		void finish(ServerLevel level) {
			BossMode.LOGGER.info("{} arena around {} levelled at y={}: ~{} block changes in {} ticks (slowest tick {} ms)", this.theme,
					this.altar.toShortString(), this.floorY, this.changed, this.ticks, this.slowestNanos / 1_000_000);
		}

		// ------------------------------------------------------------------------------------------ one column
		/** Rebuilds one column; returns its cost (block changes, at least 1). */
		private int column(ServerLevel level, int x, int z, double distance) {
			if (distance <= BossConfig.ARENA_PROTECT_RADIUS) {
				// Around the altar only nature is levelled - rock, soil, trees, water, snow; whatever was built (the shrine's
				// dais, obelisks and chest, or a player's platform) stays, and so does the altar's footing.
				int cost = 1;
				for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y > this.floorY; y--) {
					if (isNatural(level.getBlockState(cursor.set(x, y, z)))) {
						cost += set(level, x, y, z, Blocks.AIR.defaultBlockState());
					}
				}
				for (int y = this.floorY; y > this.floorY - BossConfig.ARENA_FILL_DEPTH && !isGround(level.getBlockState(cursor.set(x, y, z))); y--) {
					cost += set(level, x, y, z, this.theme.filler);
				}
				return cost;
			}
			int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
			int ground = findGround(level, x, z, top);
			int cost = 1;
			if (distance <= this.radius) {
				cost += clearAbove(level, x, z, top, this.floorY, true);
				cost += set(level, x, this.floorY, z, surface(x, z));
				for (int y = this.floorY - 1; y >= this.floorY - 3; y--) {
					cost += set(level, x, y, z, this.theme.filler);
				}
				for (int y = this.floorY - 4; y > this.floorY - BossConfig.ARENA_FILL_DEPTH && !isGround(level.getBlockState(cursor.set(x, y, z))); y--) {
					cost += set(level, x, y, z, this.theme.filler);
				}
				cost += decorate(level, x, z);
				return cost;
			}
			// blend ring: slope from the arena floor back to the original ground
			double t = (distance - this.radius) / BossConfig.ARENA_BLEND;
			t = t * t * (3 - 2 * t);
			int height = (int) Math.round(Mth.lerp(t, this.floorY, ground));
			BlockState top0 = level.getBlockState(cursor.set(x, ground, z));
			BlockState skin = isGround(top0) ? top0 : Blocks.GRASS_BLOCK.defaultBlockState();
			BlockState fill = subsoil(skin);
			cost += clearAbove(level, x, z, top, height, false);
			for (int y = ground + 1; y < height; y++) {
				cost += set(level, x, y, z, fill);
			}
			if (height != ground) {
				cost += set(level, x, height, z, skin);
			}
			return cost;
		}

		/** Clears every block from {@code top} down to {@code floor + 1}; the blend ring keeps water so lakes stay lakes. */
		private int clearAbove(ServerLevel level, int x, int z, int top, int floor, boolean removeFluids) {
			int cost = 0;
			for (int y = top; y > floor; y--) {
				BlockState state = level.getBlockState(cursor.set(x, y, z));
				if (state.isAir() || (!removeFluids && !state.getFluidState().isEmpty())) {
					continue;
				}
				cost += set(level, x, y, z, Blocks.AIR.defaultBlockState());
			}
			return cost;
		}

		private int set(ServerLevel level, int x, int y, int z, BlockState state) {
			cursor.set(x, y, z);
			if (level.isOutsideBuildHeight(y)) {
				return 0;
			}
			BlockState old = level.getBlockState(cursor);
			if (old == state || old.getDestroySpeed(level, cursor) < 0 || old.getBlock() instanceof ShardAltarBlock || old.getBlock() instanceof VoidAltarBlock) {
				return 0;
			}
			return level.setBlock(cursor, state, FLAGS) ? 1 : 0;
		}

		/** The new surface: a rune circle around the boss's rising point, the theme's waste everywhere else. */
		private BlockState surface(int x, int z) {
			switch (this.theme) {
				case VOID -> {
					return voidSurface(x, z);
				}
				case STORM -> {
					return stormSurface(x, z);
				}
				case EMBER -> {
					return emberSurface(x, z);
				}
				default -> {
				}
			}
			double dx = x + 0.5 - this.arenaCenter.x, dz = z + 0.5 - this.arenaCenter.z;
			double ring = Math.sqrt(dx * dx + dz * dz);
			if (ring < 1.5) {
				return Blocks.AMETHYST_BLOCK.defaultBlockState();
			}
			if (ring < 2.6) {
				return Blocks.CRYING_OBSIDIAN.defaultBlockState();
			}
			if (Math.abs(ring - 7.0) < 0.55 || Math.abs(ring - 13.0) < 0.55) {
				return Blocks.OBSIDIAN.defaultBlockState();
			}
			if (ring < 13.0) {
				double angle = Math.atan2(dz, dx);
				double spoke = Math.abs(Math.sin(angle * 4.0)) * ring; // distance to the nearest of 8 spokes
				return spoke < 0.6 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
			}
			double vein = this.veins.get(x * 0.09, z * 0.09);
			if (Math.abs(vein) < 0.045) {
				return hash(x, z) % 5 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
			}
			double patch = this.patches.get(x * 0.055, z * 0.055);
			if (patch > 0.62) {
				return Blocks.AMETHYST_BLOCK.defaultBlockState();
			}
			if (patch > 0.3) {
				return Blocks.SMOOTH_BASALT.defaultBlockState();
			}
			if (patch < -0.5) {
				return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
			}
			return hash(x, z) % 40 == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
		}

		/** The Void Sovereign's floor: three rune rings around its rising point, deepslate and sculk everywhere else. */
		private BlockState voidSurface(int x, int z) {
			double dx = x + 0.5 - this.arenaCenter.x, dz = z + 0.5 - this.arenaCenter.z;
			double ring = Math.sqrt(dx * dx + dz * dz);
			if (ring < 2.6) {
				return Blocks.CRYING_OBSIDIAN.defaultBlockState();
			}
			if (Math.abs(ring - 8.0) < 0.55 || Math.abs(ring - 15.0) < 0.55 || Math.abs(ring - 22.0) < 0.55) {
				return Blocks.CRYING_OBSIDIAN.defaultBlockState();
			}
			if (ring < 22.0) {
				double angle = Math.atan2(dz, dx);
				double spoke = Math.abs(Math.sin(angle * 3.0)) * ring; // six spokes
				return spoke < 0.7 ? Blocks.OBSIDIAN.defaultBlockState() : ring < 15.0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
						: Blocks.DEEPSLATE_TILES.defaultBlockState();
			}
			double vein = this.veins.get(x * 0.08, z * 0.08);
			if (Math.abs(vein) < 0.05) {
				return hash(x, z) % 3 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
			}
			double patch = this.patches.get(x * 0.05, z * 0.05);
			if (patch > 0.5) {
				return Blocks.SCULK.defaultBlockState();
			}
			if (patch > 0.2) {
				return Blocks.DEEPSLATE_TILES.defaultBlockState();
			}
			if (patch < -0.5) {
				return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
			}
			return hash(x, z) % 7 == 0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
		}

		/** The Tempest Wyvern's plateau: calcite and weathered-copper rune rings, andesite, tuff and stone swept bare. */
		private BlockState stormSurface(int x, int z) {
			double dx = x + 0.5 - this.arenaCenter.x, dz = z + 0.5 - this.arenaCenter.z;
			double ring = Math.sqrt(dx * dx + dz * dz);
			if (ring < 1.5) {
				return Blocks.CUT_COPPER.waxed().oxidized().defaultBlockState();
			}
			if (ring < 2.6 || Math.abs(ring - 8.0) < 0.55) {
				return Blocks.CALCITE.defaultBlockState();
			}
			if (Math.abs(ring - 14.0) < 0.55) {
				return Blocks.CUT_COPPER.waxed().oxidized().defaultBlockState();
			}
			if (ring < 14.0) {
				double angle = Math.atan2(dz, dx);
				double spoke = Math.abs(Math.sin(angle * 4.0)) * ring;
				return spoke < 0.6 ? Blocks.POLISHED_TUFF.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState();
			}
			double vein = this.veins.get(x * 0.09, z * 0.09);
			if (Math.abs(vein) < 0.04) {
				return Blocks.GRAVEL.defaultBlockState();
			}
			double patch = this.patches.get(x * 0.055, z * 0.055);
			if (patch > 0.55) {
				return Blocks.CALCITE.defaultBlockState();
			}
			if (patch > 0.25) {
				return Blocks.TUFF.defaultBlockState();
			}
			if (patch < -0.5) {
				return Blocks.STONE.defaultBlockState();
			}
			return hash(x, z) % 30 == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
		}

		/** The Bastion Behemoth's scorched waste: shroomlight rune rings, basalt and blackstone, thin magma veins. */
		private BlockState emberSurface(int x, int z) {
			double dx = x + 0.5 - this.arenaCenter.x, dz = z + 0.5 - this.arenaCenter.z;
			double ring = Math.sqrt(dx * dx + dz * dz);
			if (ring < 2.6) {
				return Blocks.SHROOMLIGHT.defaultBlockState();
			}
			if (Math.abs(ring - 9.0) < 0.55 || Math.abs(ring - 16.0) < 0.55) {
				return Blocks.SHROOMLIGHT.defaultBlockState();
			}
			if (ring < 16.0) {
				double angle = Math.atan2(dz, dx);
				double spoke = Math.abs(Math.sin(angle * 3.0)) * ring;
				return spoke < 0.7 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BASALT.defaultBlockState();
			}
			double vein = this.veins.get(x * 0.08, z * 0.08);
			if (Math.abs(vein) < 0.025) {
				return Blocks.MAGMA_BLOCK.defaultBlockState();
			}
			double patch = this.patches.get(x * 0.05, z * 0.05);
			if (patch > 0.5) {
				return Blocks.BLACKSTONE.defaultBlockState();
			}
			if (patch > 0.2) {
				return Blocks.SMOOTH_BASALT.defaultBlockState();
			}
			if (patch < -0.5) {
				return Blocks.COARSE_DIRT.defaultBlockState();
			}
			return hash(x, z) % 40 == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
		}

		/** A spire block: {@code cap} is the crowned top of the tallest column. */
		private BlockState spireBlock(int i, int x, int z, boolean cap) {
			boolean alt = (i + x + z) % 3 == 0;
			return switch (this.theme) {
				case SHARD -> cap ? Blocks.AMETHYST_BLOCK.defaultBlockState() : alt ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
				case VOID -> cap || alt ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
				case STORM -> cap ? Blocks.COPPER_BLOCK.waxed().oxidized().defaultBlockState()
						: alt ? Blocks.TUFF_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
				case EMBER -> cap ? Blocks.SHROOMLIGHT.defaultBlockState() : alt ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
			};
		}

		/** What crowns a capped spire. */
		private BlockState spireTop() {
			return switch (this.theme) {
				case SHARD -> Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
				case VOID -> Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP);
				case STORM -> Blocks.LIGHTNING_ROD.waxed().oxidized().defaultBlockState();
				case EMBER -> Blocks.CAMPFIRE.defaultBlockState();
			};
		}

		/** Spires and the odd amethyst bud on the new floor. */
		private int decorate(ServerLevel level, int x, int z) {
			int[] spire = this.spires.get(BlockPos.asLong(x, 0, z));
			if (spire != null) {
				int cost = 0;
				for (int i = 1; i <= spire[0]; i++) {
					cost += set(level, x, this.floorY + i, z, spireBlock(i, x, z, i == spire[0] && spire[1] == 1));
				}
				if (spire[1] == 1) {
					cost += set(level, x, this.floorY + spire[0] + 1, z, spireTop());
				}
				return cost;
			}
			double ring = Math.hypot(x + 0.5 - this.arenaCenter.x, z + 0.5 - this.arenaCenter.z);
			if (this.theme == Theme.SHARD && ring > 14 && hash(x, z) % 90 == 0) {
				BlockState bud = (hash(z, x) & 1) == 0 ? Blocks.SMALL_AMETHYST_BUD.defaultBlockState() : Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState();
				return set(level, x, this.floorY + 1, z, bud.setValue(AmethystClusterBlock.FACING, Direction.UP));
			}
			return 0;
		}

		private int hash(int x, int z) {
			long h = this.seed + x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL;
			h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
			return (int) ((h ^ (h >>> 29)) & Integer.MAX_VALUE);
		}

		/** Highest block of real terrain in the column (skipping trees, plants, snow and water). */
		private int findGround(ServerLevel level, int x, int z, int top) {
			for (int y = top; y > top - 96 && y > level.getMinY(); y--) {
				if (isGround(level.getBlockState(cursor.set(x, y, z)))) {
					return y;
				}
			}
			return this.floorY;
		}

		private static boolean isGround(BlockState state) {
			return !state.isAir() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)
					&& !state.canBeReplaced() && state.isSolidRender();
		}

		/** Terrain and vegetation, as opposed to anything built. */
		private static boolean isNatural(BlockState state) {
			return !state.isAir() && (state.canBeReplaced() || !state.getFluidState().isEmpty() || state.is(BlockTags.DIRT)
					|| state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND) || state.is(BlockTags.SNOW) || state.is(BlockTags.ICE)
					|| state.is(BlockTags.TERRACOTTA) || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.FLOWERS)
					|| state.is(BlockTags.SAPLINGS) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SANDSTONE)
					|| state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.CALCITE) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD));
		}

		private static BlockState subsoil(BlockState skin) {
			if (skin.is(Blocks.SAND) || skin.is(Blocks.SANDSTONE)) {
				return Blocks.SANDSTONE.defaultBlockState();
			}
			if (skin.is(Blocks.RED_SAND) || skin.is(Blocks.RED_SANDSTONE)) {
				return Blocks.RED_SANDSTONE.defaultBlockState();
			}
			if (skin.is(BlockTags.DIRT) || skin.is(Blocks.SNOW_BLOCK)) {
				return Blocks.DIRT.defaultBlockState();
			}
			return skin;
		}

		// ------------------------------------------------------------------------------------------ effects / safety
		private void waveEffects(ServerLevel level, double radius) {
			if (radius <= BossConfig.ARENA_PROTECT_RADIUS) {
				return;
			}
			int points = Math.min(48, 8 + (int) radius);
			for (int i = 0; i < points; i++) {
				double a = (i + (this.ticks % 2) * 0.5) * Mth.TWO_PI / points;
				double x = this.altar.getX() + 0.5 + Math.cos(a) * radius, z = this.altar.getZ() + 0.5 + Math.sin(a) * radius;
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, this.floorY + 1.2, z, 2, 0.3, 0.4, 0.3, 0.05);
				if (i % 4 == 0) {
					level.sendParticles(ParticleTypes.EXPLOSION, x, this.floorY + 1.0, z, 1, 0.5, 0.3, 0.5, 0.0);
				}
			}
			if (this.ticks % 6 == 1) {
				level.playSound(null, this.altar.getX() + 0.5, this.floorY + 1, this.altar.getZ() + 0.5, SoundEvents.GENERIC_EXPLODE.value(),
						SoundSource.BLOCKS, 3.0F, 0.4F + level.getRandom().nextFloat() * 0.2F);
			}
		}

		/** Anything the new floor or a filled slope closed over is lifted back into the open. */
		private void rescueEntities(ServerLevel level, double radius) {
			AABB area = new AABB(this.altar).inflate(radius, 64, radius);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && !(e instanceof ShardDragonEntity))) {
				if (!entity.isInWall()) {
					continue;
				}
				BlockPos.MutableBlockPos p = entity.blockPosition().mutable();
				for (int i = 0; i < 48; i++) {
					p.move(Direction.UP);
					if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()
							&& level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
						entity.teleportTo(entity.getX(), p.getY(), entity.getZ());
						break;
					}
				}
			}
		}

		// ------------------------------------------------------------------------------------------ biome
		private void applyBiome(ServerLevel level) {
			Holder<Biome> wastes = level.registryAccess().lookupOrThrow(Registries.BIOME).get(this.theme.biome).orElse(null);
			if (wastes == null) {
				return;
			}
			double reach = this.radius + BossConfig.ARENA_BLEND * 0.5;
			int minY = this.floorY - 16;
			List<ChunkAccess> changed = new ArrayList<>();
			int minChunkX = (this.altar.getX() - outer) >> 4, maxChunkX = (this.altar.getX() + outer) >> 4;
			int minChunkZ = (this.altar.getZ() - outer) >> 4, maxChunkZ = (this.altar.getZ() + outer) >> 4;
			for (int cx = minChunkX; cx <= maxChunkX; cx++) {
				for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
					LevelChunk chunk = level.getChunk(cx, cz);
					chunk.fillBiomesFromNoise((qx, qy, qz) -> {
						double bx = QuartPos.toBlock(qx) + 2 - this.altar.getX(), bz = QuartPos.toBlock(qz) + 2 - this.altar.getZ();
						return bx * bx + bz * bz <= reach * reach && QuartPos.toBlock(qy) + 4 >= minY ? wastes : chunk.getNoiseBiome(qx, qy, qz);
					});
					chunk.markUnsaved();
					changed.add(chunk);
				}
			}
			level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
		}
	}
}
