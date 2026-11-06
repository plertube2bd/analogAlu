package com.example.redstonetransportation.furnace;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.rail.DriveInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * A furnace, blast furnace or smoker that cooks several times faster and burns less fuel per item.
 * Any signal, ALU output included, pauses it, the same way a signal stops a conveyor.
 */
public final class UpgradedFurnaceBlock extends AbstractFurnaceBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private final FurnaceKind kind;
    private final FurnaceTier tier;

    public UpgradedFurnaceBlock(Properties properties, FurnaceKind kind, FurnaceTier tier) {
        super(properties);
        this.kind = kind;
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    public FurnaceKind kind() {
        return kind;
    }

    public FurnaceTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpgradedFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModContent.UPGRADED_FURNACE_ENTITY,
                (tickLevel, pos, tickState, furnace) -> furnace.tick(serverLevel, pos, tickState));
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof MenuProvider furnace) player.openMenu(furnace);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock,
                                   Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        boolean powered = DriveInput.readSignals(level, pos).speed() > 0;
        if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.1D) {
            level.playLocalSound(x, y, z, kind.sound(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction front = state.getValue(FACING);
        double across = random.nextDouble() * 0.6D - 0.3D;
        double offsetX = front.getAxis() == Direction.Axis.X ? front.getStepX() * 0.52D : across;
        double offsetZ = front.getAxis() == Direction.Axis.Z ? front.getStepZ() * 0.52D : across;
        double height = random.nextDouble() * 6.0D / 16.0D;
        level.addParticle(ParticleTypes.SMOKE, x + offsetX, y + height, z + offsetZ, 0.0D, 0.0D, 0.0D);
        if (kind == FurnaceKind.FURNACE) {
            level.addParticle(ParticleTypes.FLAME, x + offsetX, y + height, z + offsetZ, 0.0D, 0.0D, 0.0D);
        }
    }
}
