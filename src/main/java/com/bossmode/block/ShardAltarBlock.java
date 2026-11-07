package com.bossmode.block;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModEntities;
import com.bossmode.registry.ModItems;
import com.bossmode.world.ArenaTerraformer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Shard Altar: the only way to call the Shard Dragon in survival. Generated in the world as the centrepiece of the
 * Shard Shrine structure.
 * <p>
 * Right-click with a Wyrm Shard to offer one (sneak to offer as many as still needed). Every shard lights one of the
 * twelve crystal studs around the basin ({@link #CHARGE}). The last shard starts a five-second ritual ({@link #RITUAL},
 * driven by scheduled block ticks so it survives chunk unloads) and the dragon rises {@link BossConfig#ALTAR_SUMMON_DISTANCE}
 * blocks in front of the altar while the land around is levelled into its arena ({@link ArenaTerraformer}). The final shard is refused while a Shard Dragon is already alive nearby. Breaking the
 * altar returns the shards offered so far. Everything is kept in the block state, so the client sees the charge
 * without any extra syncing.
 */
public class ShardAltarBlock extends Block {
	public static final int COST = BossConfig.ALTAR_SHARD_COST;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, COST);
	public static final IntegerProperty RITUAL = IntegerProperty.create("ritual", 0, BossConfig.ALTAR_RITUAL_STEPS);

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(0, 0, 0, 16, 3, 16),
			Block.box(2, 3, 2, 14, 5, 14),
			Block.box(4, 5, 4, 12, 11, 12),
			Block.box(1, 11, 1, 15, 14, 15),
			Block.box(6, 14, 6, 10, 23, 10));
	/** Tip of the central crystal, relative to the block origin. */
	private static final Vec3 CRYSTAL_TIP = new Vec3(0.5, 23.0 / 16.0, 0.5);
	private static final DustParticleOptions RITUAL_DUST = new DustParticleOptions(0xB77BFF, 1.6F);

	public ShardAltarBlock(Properties properties) {
		super(properties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(CHARGE, 0).setValue(RITUAL, 0));
	}

	/** Block light: a faint glow at rest, brighter with every shard, full during the ritual. */
	public static int lightLevel(BlockState state) {
		if (state.getValue(RITUAL) > 0) {
			return 15;
		}
		int charge = state.getValue(CHARGE);
		return charge == 0 ? 4 : 7 + charge * 8 / COST;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CHARGE, RITUAL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// ================================================================================================ offering
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(ModItems.WYRM_SHARD)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		int charge = state.getValue(CHARGE);
		if (state.getValue(RITUAL) > 0 || charge >= COST) {
			player.sendOverlayMessage(message("busy").withStyle(ChatFormatting.LIGHT_PURPLE));
			return InteractionResult.FAIL;
		}
		int amount = player.isShiftKeyDown() ? Math.min(stack.getCount(), COST - charge) : 1;
		if (charge + amount >= COST && findBoss(server, pos) != null) {
			player.sendOverlayMessage(message("boss_alive").withStyle(ChatFormatting.RED));
			server.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, SoundSource.BLOCKS, 1.0F, 0.7F);
			return InteractionResult.FAIL;
		}
		stack.consume(amount, player);
		offer(server, pos, state, charge + amount, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			Component message = state.getValue(RITUAL) > 0
					? message("busy")
					: message("status", state.getValue(CHARGE), COST);
			player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return InteractionResult.SUCCESS;
	}

	/** Server: stores the new charge and, when the altar is full, starts the ritual. Public for the game test. */
	public void offer(ServerLevel level, BlockPos pos, BlockState state, int charge, @Nullable Player player) {
		boolean complete = charge >= COST;
		level.setBlock(pos, state.setValue(CHARGE, Math.min(charge, COST)).setValue(RITUAL, complete ? 1 : 0), Block.UPDATE_ALL);
		float progress = Math.min(charge, COST) / (float) COST;
		level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.6F + progress);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.5F + progress);
		Vec3 tip = Vec3.atLowerCornerOf(pos).add(CRYSTAL_TIP);
		level.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 10, 0.25, 0.35, 0.25, 0.04);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, tip.x, tip.y - 0.6, tip.z, 20, 0.5, 0.2, 0.5, 0.05);
		if (player != null) {
			player.sendOverlayMessage(message("offered", Math.min(charge, COST), COST)
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		if (complete) {
			level.scheduleTick(pos, this, BossConfig.ALTAR_RITUAL_STEP_TICKS);
			level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
			level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2.0F, 1.4F);
			announce(level, pos, message("ritual").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
		}
	}

	// ================================================================================================ ritual
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int step = state.getValue(RITUAL);
		if (step == 0) {
			return;
		}
		Vec3 summon = summonPosition(level, pos, state, summonDistance());
		if (step < BossConfig.ALTAR_RITUAL_STEPS) {
			level.setBlock(pos, state.setValue(RITUAL, step + 1), Block.UPDATE_CLIENTS);
			ritualEffects(level, pos, summon, step + 1, random);
			level.scheduleTick(pos, this, BossConfig.ALTAR_RITUAL_STEP_TICKS);
			return;
		}
		level.setBlock(pos, state.setValue(CHARGE, 0).setValue(RITUAL, 0), Block.UPDATE_ALL);
		if (findBoss(level, pos) != null) {
			// Another boss of this kind appeared while the ritual ran (spawn egg, second altar): give the offering back.
			popResource(level, pos.above(), new ItemStack(ModItems.WYRM_SHARD, COST));
			level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.5F, 1.0F);
			announce(level, pos, message("fizzle").withStyle(ChatFormatting.GRAY));
			return;
		}
		summon(level, pos, summon, state.getValue(FACING));
	}

	private void ritualEffects(ServerLevel level, BlockPos pos, Vec3 summon, int step, RandomSource random) {
		float progress = step / (float) BossConfig.ALTAR_RITUAL_STEPS;
		Vec3 tip = Vec3.atLowerCornerOf(pos).add(CRYSTAL_TIP);
		// Arc of crystal dust from the altar to the summoning point, rising higher as the ritual builds.
		Vec3 span = summon.subtract(tip);
		for (int i = 0; i <= 20; i++) {
			double t = i / 20.0;
			double lift = Math.sin(t * Math.PI) * (1.5 + 3.0 * progress);
			level.sendParticles(ritualDust(), tip.x + span.x * t, tip.y + span.y * t + lift, tip.z + span.z * t, 1, 0.05, 0.05, 0.05, 0.0);
		}
		// A widening rune circle on the ground where the dragon will appear, and a pillar of light at its centre.
		double radius = 1.5 + 4.5 * progress;
		int points = 12 + step * 2;
		double spin = step * 0.35;
		for (int i = 0; i < points; i++) {
			double a = spin + i * Mth.TWO_PI / points;
			level.sendParticles(ritualRingParticle(), summon.x + Math.cos(a) * radius, summon.y + 0.15, summon.z + Math.sin(a) * radius,
					1, 0.0, 0.05, 0.0, 0.0);
		}
		level.sendParticles(ritualPillarParticle(), summon.x, summon.y + 0.5, summon.z, 4 + step, 0.3, 1.5 + progress * 4.0, 0.3, 0.02);
		level.sendParticles(ParticleTypes.PORTAL, tip.x, tip.y, tip.z, 12, 0.3, 0.3, 0.3, 0.6);
		if (step % 4 == 0) {
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.5F, 0.5F + progress);
			level.playSound(null, summon.x, summon.y, summon.z, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE, 2.0F, 0.6F + progress * 0.6F);
		}
		if (step == BossConfig.ALTAR_RITUAL_STEPS - 8) {
			level.playSound(null, summon.x, summon.y, summon.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0F, 0.5F);
		}
		if (step > BossConfig.ALTAR_RITUAL_STEPS / 2) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, summon.x, summon.y + 0.2, summon.z, 6, radius * 0.5, 0.1, radius * 0.5, 0.02);
		}
	}

	private void summon(ServerLevel level, BlockPos altar, Vec3 ground, Direction facing) {
		// With the arena coming, the dragon stands on the future floor, which never strays far from the altar's own height.
		Vec3 at = BossConfig.ALTAR_TERRAFORM ? new Vec3(ground.x, arenaFloor(altar, ground) + 1, ground.z) : ground;
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(at);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.0, at.z, 2, 1.0, 0.5, 1.0, 0.0);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 2.0, at.z, 80, 2.5, 2.5, 2.5, 0.25);
		Mob boss = createBoss(level);
		if (boss == null) {
			return;
		}
		float yaw = facing.getOpposite().toYRot();
		boss.snapTo(at.x, at.y, at.z, yaw, 0.0F);
		boss.setYHeadRot(yaw);
		boss.setYBodyRot(yaw);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.TRIGGERED, null);
		level.addFreshEntity(boss);
		if (BossConfig.ALTAR_TERRAFORM) {
			// the boss's arrival flattens and reshapes the surroundings into its arena
			ArenaTerraformer.start(level, altar, at, Mth.floor(at.y) - 1, arenaTheme());
		}
		announce(level, altar, message("summoned").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
	}

	// ================================================================================================ per-altar hooks
	/** The boss this altar calls (the Tempest and Bastion altars override the hooks below). */
	protected @Nullable Mob createBoss(ServerLevel level) {
		return ModEntities.SHARD_DRAGON.create(level, EntitySpawnReason.TRIGGERED);
	}

	/** A living boss of this altar's kind near {@code pos}: the last offering is refused while one exists. */
	public @Nullable Entity findBoss(ServerLevel level, BlockPos pos) {
		return findDragon(level, pos);
	}

	protected ArenaTerraformer.Theme arenaTheme() {
		return ArenaTerraformer.Theme.SHARD;
	}

	protected int summonDistance() {
		return BossConfig.ALTAR_SUMMON_DISTANCE;
	}

	/** Translation key prefix of this altar's messages ({@code <prefix>busy}, {@code status}, {@code offered} ...). */
	protected String messagePrefix() {
		return "message.boss_mode.altar.";
	}

	protected DustParticleOptions ritualDust() {
		return RITUAL_DUST;
	}

	protected ParticleOptions ritualRingParticle() {
		return ParticleTypes.REVERSE_PORTAL;
	}

	protected ParticleOptions ritualPillarParticle() {
		return ParticleTypes.END_ROD;
	}

	/** Client: motes rising from the lit studs. */
	protected ParticleOptions moteParticle() {
		return ParticleTypes.END_ROD;
	}

	protected final MutableComponent message(String key, Object... args) {
		return Component.translatable(messagePrefix() + key, args);
	}

	/**
	 * Where the dragon rises: {@link BossConfig#ALTAR_SUMMON_DISTANCE} blocks in front of the altar, settled onto the ground
	 * within a few blocks of the altar's own height (the shrine's arena sits one step below its dais).
	 */
	public static Vec3 summonPosition(BlockGetter level, BlockPos altar, BlockState state) {
		return summonPosition(level, altar, state, BossConfig.ALTAR_SUMMON_DISTANCE);
	}

	public static Vec3 summonPosition(BlockGetter level, BlockPos altar, BlockState state, int distance) {
		BlockPos column = altar.relative(state.getValue(FACING), distance);
		int y = altar.getY();
		for (int i = 0; i < 6 && !isOpen(level, column.atY(y)); i++) {
			y++;
		}
		for (int i = 0; i < 6 && isOpen(level, column.atY(y - 1)); i++) {
			y--;
		}
		return Vec3.atBottomCenterOf(column.atY(y));
	}

	private static boolean isOpen(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/**
	 * y of the arena's surface blocks: the ground under the rising point, kept within 1-3 blocks below the altar's base
	 * (the shrine's arena lies two below its altar, one step down from the dais), so the altar never ends up in a pit or
	 * on a pillar whatever the ground in front of it looked like.
	 */
	public static int arenaFloor(BlockPos altar, Vec3 summon) {
		return Mth.clamp(Mth.floor(summon.y) - 1, altar.getY() - 3, altar.getY() - 1);
	}

	public static @Nullable ShardDragonEntity findDragon(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(ShardDragonEntity.class, new AABB(pos).inflate(BossConfig.ALTAR_DRAGON_EXCLUSION_RADIUS),
				ShardDragonEntity::isAlive).stream().findFirst().orElse(null);
	}

	protected static void announce(ServerLevel level, BlockPos pos, Component message) {
		Vec3 center = Vec3.atCenterOf(pos);
		double range = BossConfig.ALTAR_ANNOUNCE_RADIUS * BossConfig.ALTAR_ANNOUNCE_RADIUS;
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(center) <= range) {
				player.sendSystemMessage(message);
			}
		}
	}

	// ================================================================================================ removal / looks
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		int charge = state.getValue(CHARGE);
		if (!level.isClientSide() && charge > 0 && !player.hasInfiniteMaterials()) {
			popResource(level, pos, new ItemStack(ModItems.WYRM_SHARD, charge));
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int charge = state.getValue(CHARGE);
		double x = pos.getX() + 0.5, y = pos.getY() + 14.5 / 16.0, z = pos.getZ() + 0.5;
		if (charge == 0) {
			if (random.nextInt(3) == 0) {
				level.addParticle(ParticleTypes.REVERSE_PORTAL, x + (random.nextDouble() - 0.5) * 0.6, y, z + (random.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
			}
			return;
		}
		// Motes drift up from every lit stud; a full altar pulls portal particles into its crystal.
		for (int i = 0; i < 1 + charge / 3; i++) {
			double a = random.nextDouble() * Mth.TWO_PI;
			level.addParticle(moteParticle(), x + Math.cos(a) * 0.4, y + 0.1, z + Math.sin(a) * 0.4, 0, 0.015 + random.nextDouble() * 0.02, 0);
		}
		if (charge >= COST || state.getValue(RITUAL) > 0) {
			for (int i = 0; i < 6; i++) {
				level.addParticle(ParticleTypes.PORTAL, x, y + 0.5, z, (random.nextDouble() - 0.5) * 2.5, random.nextDouble() * 1.5, (random.nextDouble() - 0.5) * 2.5);
			}
		}
	}
}
