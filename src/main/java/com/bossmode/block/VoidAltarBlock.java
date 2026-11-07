package com.bossmode.block;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModEntities;
import com.bossmode.registry.ModItems;
import com.bossmode.world.ArenaTerraformer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 * Void Altar: where the Void Sovereign is called. Generated as the heart of the Void Sanctum structure.
 * <p>
 * Its three prongs each hold one boss trophy: a Wyrm Heart (Shard Dragon), a Tempest Eye (Tempest Wyvern) and a
 * Behemoth Core (Bastion Behemoth) - right-click with one to set it ({@link #HEART}, {@link #EYE}, {@link #CORE}). The
 * third trophy starts a seven-second ritual ({@link #RITUAL}, scheduled block ticks) that ends with the Sovereign rising
 * {@link SovereignConfig#ALTAR_SUMMON_DISTANCE} blocks in front of the altar while the land around becomes its arena
 * ({@link ArenaTerraformer.Theme#VOID}). The last trophy is refused while a Sovereign is alive nearby; breaking the altar
 * returns the trophies set in it. All state lives in the block state.
 */
public class VoidAltarBlock extends Block {
	/** One trophy of each raid boss. */
	public static final int COST = 3;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty HEART = BooleanProperty.create("heart");
	public static final BooleanProperty EYE = BooleanProperty.create("eye");
	public static final BooleanProperty CORE = BooleanProperty.create("core");
	public static final IntegerProperty RITUAL = IntegerProperty.create("ritual", 0, SovereignConfig.ALTAR_RITUAL_STEPS);

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(0, 0, 0, 16, 3, 16),
			Block.box(2, 3, 2, 14, 5, 14),
			Block.box(4, 5, 4, 12, 12, 12),
			Block.box(2, 12, 2, 14, 14, 14),
			Block.box(3, 14, 3, 13, 22, 13));
	/** The dark orb between the prongs, relative to the block origin. */
	private static final Vec3 ORB = new Vec3(0.5, 16.0 / 16.0, 0.5);
	private static final DustParticleOptions RITUAL_DUST = new DustParticleOptions(0xFF2D6A, 1.8F);
	private static final DustParticleOptions DARK_DUST = new DustParticleOptions(0x1A0612, 2.4F);

	public VoidAltarBlock(Properties properties) {
		super(properties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(HEART, false).setValue(EYE, false)
				.setValue(CORE, false).setValue(RITUAL, 0));
	}

	public static int lightLevel(BlockState state) {
		return state.getValue(RITUAL) > 0 ? 15 : 3 + trophies(state) * 4;
	}

	/** How many of the three trophies are set. */
	public static int trophies(BlockState state) {
		return (state.getValue(HEART) ? 1 : 0) + (state.getValue(EYE) ? 1 : 0) + (state.getValue(CORE) ? 1 : 0);
	}

	/** The slot a trophy item goes into, or null if it is no trophy. */
	public static @Nullable BooleanProperty slotFor(ItemStack stack) {
		if (stack.is(ModItems.WYRM_HEART)) {
			return HEART;
		}
		if (stack.is(ModItems.TEMPEST_EYE)) {
			return EYE;
		}
		return stack.is(ModItems.BEHEMOTH_CORE) ? CORE : null;
	}

	public static Item trophyItem(BooleanProperty slot) {
		return slot == HEART ? ModItems.WYRM_HEART : slot == EYE ? ModItems.TEMPEST_EYE : ModItems.BEHEMOTH_CORE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HEART, EYE, CORE, RITUAL);
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
		BooleanProperty slot = slotFor(stack);
		if (slot == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (state.getValue(RITUAL) > 0 || trophies(state) >= COST) {
			player.sendOverlayMessage(Component.translatable("message.boss_mode.void_altar.busy").withStyle(ChatFormatting.DARK_RED));
			return InteractionResult.FAIL;
		}
		if (state.getValue(slot)) {
			player.sendOverlayMessage(Component.translatable("message.boss_mode.void_altar.already_set", stack.getHoverName()).withStyle(ChatFormatting.DARK_RED));
			server.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, SoundSource.BLOCKS, 1.0F, 0.7F);
			return InteractionResult.FAIL;
		}
		if (trophies(state) + 1 >= COST && findSovereign(server, pos) != null) {
			player.sendOverlayMessage(Component.translatable("message.boss_mode.void_altar.sovereign_alive").withStyle(ChatFormatting.RED));
			server.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, SoundSource.BLOCKS, 1.0F, 0.5F);
			return InteractionResult.FAIL;
		}
		stack.consume(1, player);
		offer(server, pos, state, slot, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			Component message = state.getValue(RITUAL) > 0
					? Component.translatable("message.boss_mode.void_altar.busy")
					: Component.translatable("message.boss_mode.void_altar.status", mark(state, HEART), mark(state, EYE), mark(state, CORE));
			player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.DARK_RED));
		}
		return InteractionResult.SUCCESS;
	}

	private static Component mark(BlockState state, BooleanProperty slot) {
		return state.getValue(slot)
				? Component.literal("\u25C6 ").append(Component.translatable(trophyItem(slot).getDescriptionId())).withStyle(ChatFormatting.GOLD)
				: Component.literal("\u25C7 ").append(Component.translatable(trophyItem(slot).getDescriptionId())).withStyle(ChatFormatting.DARK_GRAY);
	}

	/** Server: sets one trophy and, when all three are set, starts the ritual. Public for the game test. */
	public void offer(ServerLevel level, BlockPos pos, BlockState state, BooleanProperty slot, @Nullable Player player) {
		BlockState filled = state.setValue(slot, true);
		int count = trophies(filled);
		boolean complete = count >= COST;
		level.setBlock(pos, filled.setValue(RITUAL, complete ? 1 : 0), Block.UPDATE_ALL);
		float progress = count / (float) COST;
		level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.2F, 0.4F + progress * 0.5F);
		level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 2.0F, 0.6F + progress * 0.4F);
		Vec3 orb = Vec3.atLowerCornerOf(pos).add(ORB);
		level.sendParticles(RITUAL_DUST, orb.x, orb.y + 0.4, orb.z, 16, 0.3, 0.4, 0.3, 0);
		level.sendParticles(ParticleTypes.SQUID_INK, orb.x, orb.y, orb.z, 10, 0.3, 0.3, 0.3, 0.02);
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.boss_mode.void_altar.offered",
					Component.translatable(trophyItem(slot).getDescriptionId()), count, COST).withStyle(ChatFormatting.DARK_RED));
		}
		if (complete) {
			level.scheduleTick(pos, this, SovereignConfig.ALTAR_RITUAL_STEP_TICKS);
			level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.5F);
			announce(level, pos, Component.translatable("message.boss_mode.void_altar.ritual").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		}
	}

	// ================================================================================================ ritual
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int step = state.getValue(RITUAL);
		if (step == 0) {
			return;
		}
		Vec3 summon = summonPosition(level, pos, state);
		if (step < SovereignConfig.ALTAR_RITUAL_STEPS) {
			level.setBlock(pos, state.setValue(RITUAL, step + 1), Block.UPDATE_CLIENTS);
			ritualEffects(level, pos, summon, step + 1);
			level.scheduleTick(pos, this, SovereignConfig.ALTAR_RITUAL_STEP_TICKS);
			return;
		}
		level.setBlock(pos, state.setValue(HEART, false).setValue(EYE, false).setValue(CORE, false).setValue(RITUAL, 0), Block.UPDATE_ALL);
		if (findSovereign(level, pos) != null) {
			for (BooleanProperty slot : new BooleanProperty[]{HEART, EYE, CORE}) {
				popResource(level, pos.above(), new ItemStack(trophyItem(slot)));
			}
			level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.5F, 0.7F);
			announce(level, pos, Component.translatable("message.boss_mode.void_altar.fizzle").withStyle(ChatFormatting.GRAY));
			return;
		}
		summon(level, pos, summon, state.getValue(FACING));
	}

	private static void ritualEffects(ServerLevel level, BlockPos pos, Vec3 summon, int step) {
		float progress = step / (float) SovereignConfig.ALTAR_RITUAL_STEPS;
		Vec3 orb = Vec3.atLowerCornerOf(pos).add(ORB);
		// three streams, one from each prong, braid toward the rising point
		Vec3 span = summon.subtract(orb);
		for (int prong = 0; prong < 3; prong++) {
			double offset = prong * Mth.TWO_PI / 3 + step * 0.3;
			for (int i = 0; i <= 18; i++) {
				double t = i / 18.0;
				double wobble = Math.sin(t * Math.PI) * (1.0 + 2.0 * progress);
				double lift = Math.sin(t * Math.PI) * (2.0 + 4.0 * progress);
				level.sendParticles(RITUAL_DUST, orb.x + span.x * t + Math.cos(offset + t * 6) * wobble, orb.y + span.y * t + lift,
						orb.z + span.z * t + Math.sin(offset + t * 6) * wobble, 1, 0.03, 0.03, 0.03, 0.0);
			}
		}
		// a spreading pool of darkness where the Sovereign will rise
		double radius = 2.0 + 6.0 * progress;
		int points = 16 + step * 2;
		for (int i = 0; i < points; i++) {
			double a = -step * 0.25 + i * Mth.TWO_PI / points;
			level.sendParticles(ParticleTypes.SQUID_INK, summon.x + Math.cos(a) * radius, summon.y + 0.15, summon.z + Math.sin(a) * radius, 1, 0.0, 0.02, 0.0, 0.0);
		}
		level.sendParticles(DARK_DUST, summon.x, summon.y + 0.3, summon.z, 6 + step, radius * 0.5, 0.1, radius * 0.5, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, summon.x, summon.y + 1.0, summon.z, 6 + step, 0.6, 2.0 + progress * 5.0, 0.6, 0.03);
		if (step % 4 == 0) {
			level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0F, 0.5F + progress * 0.5F);
		}
		if (step == SovereignConfig.ALTAR_RITUAL_STEPS - 10) {
			level.playSound(null, summon.x, summon.y, summon.z, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 5.0F, 0.5F);
		}
	}

	private void summon(ServerLevel level, BlockPos altar, Vec3 ground, Direction facing) {
		int floor = ShardAltarBlock.arenaFloor(altar, ground);
		Vec3 at = new Vec3(ground.x, floor + 1, ground.z);
		for (int i = 0; i < 3; i++) {
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.snapTo(at.add((i - 1) * 3.0, 0, (i % 2) * 2.0));
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.0, at.z, 3, 1.5, 0.5, 1.5, 0.0);
		level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 1.0, at.z, 120, 3.0, 1.0, 3.0, 0.1);
		VoidSovereignEntity sovereign = ModEntities.VOID_SOVEREIGN.create(level, EntitySpawnReason.TRIGGERED);
		if (sovereign == null) {
			return;
		}
		float yaw = facing.getOpposite().toYRot();
		sovereign.snapTo(at.x, at.y, at.z, yaw, 0.0F);
		sovereign.setYHeadRot(yaw);
		sovereign.setYBodyRot(yaw);
		sovereign.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.TRIGGERED, null);
		level.addFreshEntity(sovereign);
		ArenaTerraformer.start(level, altar, at, floor, ArenaTerraformer.Theme.VOID);
		announce(level, altar, Component.translatable("message.boss_mode.void_altar.summoned").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
	}

	/** Where the Sovereign rises: {@link SovereignConfig#ALTAR_SUMMON_DISTANCE} blocks in front, settled onto the ground. */
	public static Vec3 summonPosition(BlockGetter level, BlockPos altar, BlockState state) {
		BlockPos column = altar.relative(state.getValue(FACING), SovereignConfig.ALTAR_SUMMON_DISTANCE);
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

	public static @Nullable VoidSovereignEntity findSovereign(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(VoidSovereignEntity.class, new AABB(pos).inflate(SovereignConfig.ALTAR_EXCLUSION_RADIUS),
				VoidSovereignEntity::isAlive).stream().findFirst().orElse(null);
	}

	private static void announce(ServerLevel level, BlockPos pos, Component message) {
		Vec3 center = Vec3.atCenterOf(pos);
		double range = SovereignConfig.ALTAR_ANNOUNCE_RADIUS * SovereignConfig.ALTAR_ANNOUNCE_RADIUS;
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(center) <= range) {
				player.sendSystemMessage(message);
			}
		}
	}

	// ================================================================================================ removal / looks
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && !player.hasInfiniteMaterials()) {
			for (BooleanProperty slot : new BooleanProperty[]{HEART, EYE, CORE}) {
				if (state.getValue(slot)) {
					popResource(level, pos, new ItemStack(trophyItem(slot)));
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int hearts = trophies(state);
		double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
		level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y + 0.1, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
		for (int i = 0; i < hearts; i++) {
			double a = random.nextDouble() * Mth.TWO_PI;
			level.addParticle(ParticleTypes.CRIMSON_SPORE, x + Math.cos(a) * 0.35, y + 0.3, z + Math.sin(a) * 0.35, 0, 0.02, 0);
		}
		if (hearts >= COST || state.getValue(RITUAL) > 0) {
			for (int i = 0; i < 6; i++) {
				level.addParticle(ParticleTypes.PORTAL, x, y + 0.2, z, (random.nextDouble() - 0.5) * 2.5, random.nextDouble() * 1.5, (random.nextDouble() - 0.5) * 2.5);
			}
		}
	}
}
