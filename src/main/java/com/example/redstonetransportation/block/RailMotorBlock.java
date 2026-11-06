package com.example.redstonetransportation.block;

import com.example.redstonetransportation.content.ModAdvancements;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import com.example.redstonetransportation.rail.DriveInput;
import com.example.redstonetransportation.rail.RailDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

/** Tiered accelerator for every minecart type. Analog ALU controller output is treated as a 4-bit speed setpoint. */
public class RailMotorBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final int tier;

    public RailMotorBlock(Properties properties, int tier) {
        super(properties);
        this.tier = Math.clamp(tier, 1, 4);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(ACTIVE, false));
    }

    public int getTier() { return tier; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Direction next = state.getValue(FACING).getClockWise();
        level.setBlock(pos, state.setValue(FACING, next), 3);
        level.scheduleTick(pos, this, 1);
        player.sendOverlayMessage(Component.literal("가속 방향: " + directionName(next)));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && oldState.getBlock() != this) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block sourceBlock, Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        DriveInput input = DriveInput.read(level, pos);
        int signal = input.speed();
        boolean commandPresent = signal > 0 || input.brake();
        updateIndicators(level, pos, commandPresent, false);

        AABB search = new AABB(
                pos.getX() - 1.0D, pos.getY() - 0.25D, pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 2.0D, pos.getZ() + 2.0D
        );
        var carts = level.getEntitiesOfClass(AbstractMinecart.class, search);
        if (carts.isEmpty()) {
            if (commandPresent) {
                level.scheduleTick(pos, this, 1);
            } else if (ElectricPower.isAvailable()) {
                level.scheduleTick(pos, this, ElectricPower.CHECK_TICKS);
            }
            return;
        }

        // The ALU brake channel is a digital emergency-stop input.
        if (input.brake()) {
            for (AbstractMinecart cart : carts) {
                Vec3 velocity = cart.getDeltaMovement();
                double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                double factor = horizontal < 0.025D ? 0.0D : 0.18D;
                cart.setDeltaMovement(new Vec3(velocity.x * factor, velocity.y, velocity.z * factor));
            }
            updateIndicators(level, pos, true, false);
            level.scheduleTick(pos, this, 1);
            return;
        }

        if (signal <= 0) {
            updateIndicators(level, pos, false, false);
            if (ElectricPower.isAvailable()) level.scheduleTick(pos, this, ElectricPower.CHECK_TICKS);
            return;
        }

        Direction facing = state.getValue(FACING);
        Direction driveDirection = input.reverse() ? facing.getOpposite() : facing;
        double throttle = signal / 15.0D;
        double acceleration = switch (tier) {
            case 2 -> 0.031D + (0.014D * throttle);
            case 3 -> 0.043D + (0.019D * throttle);
            case 4 -> 0.060D + (0.025D * throttle);
            default -> 0.004D + (0.018D * throttle);
        };
        double tierMaxSpeed = switch (tier) {
            case 2 -> 43.0D / 20.0D;
            case 3 -> 72.0D / 20.0D;
            case 4 -> 103.0D / 20.0D;
            default -> 21.0D / 20.0D;
        };
        // ALU output is a true 4-bit setpoint: 0 stops, 15 allows the tier's full speed.
        double targetMaxSpeed = tierMaxSpeed * throttle;

        int energyInterval = switch (tier) {
            case 2 -> 12;
            case 3 -> 8;
            case 4 -> 4;
            default -> 20;
        };
        BlockPos powerCellPos = findAdjacentPowerCell(level, pos);
        if (powerCellPos != null) {
            BlockState cellState = level.getBlockState(powerCellPos);
            if (cellState.getValue(PowerCellBlock.CHARGE) <= 0) {
                updateIndicators(level, pos, true, false);
                level.scheduleTick(pos, this, 10);
                return;
            }
            if (level.getGameTime() % energyInterval == 0) {
                PowerCellBlock.consumeOne(level, powerCellPos);
            }
        }

        updateIndicators(level, pos, true, true);
        for (AbstractMinecart entity : carts) {
            Vec3 velocity = entity.getDeltaMovement();
            double vx = velocity.x + driveDirection.getStepX() * acceleration;
            double vz = velocity.z + driveDirection.getStepZ() * acceleration;
            double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);
            if (horizontalSpeed > targetMaxSpeed) {
                double scale = targetMaxSpeed / horizontalSpeed;
                vx *= scale;
                vz *= scale;
            }
            RailDrive.allowSpeed(entity, targetMaxSpeed);
            entity.setDeltaMovement(new Vec3(vx, velocity.y, vz));
            if (Math.sqrt(vx * vx + vz * vz) >= TOP_SPEED_BRAG) {
                entity.getPassengers().forEach(passenger -> ModAdvancements.award(passenger, ModAdvancements.FULL_SPEED));
            }
        }
        level.scheduleTick(pos, this, 1);
    }

    private static BlockPos findAdjacentPowerCell(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos candidate = pos.relative(direction);
            if (level.getBlockState(candidate).is(ModContent.POWER_CELL)) return candidate;
        }
        return null;
    }

    private static void updateIndicators(Level level, BlockPos pos, boolean powered, boolean active) {
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof RailMotorBlock)) return;
        if (current.getValue(POWERED) != powered || current.getValue(ACTIVE) != active) {
            level.setBlock(pos, current.setValue(POWERED, powered).setValue(ACTIVE, active), 3);
        }
    }

    /** 100 m/s, in blocks per tick: only Motor IV gets there, at a strong signal. */
    private static final double TOP_SPEED_BRAG = 5.0D;

    private static String directionName(Direction direction) {
        return switch (direction) {
            case NORTH -> "북쪽";
            case SOUTH -> "남쪽";
            case EAST -> "동쪽";
            case WEST -> "서쪽";
            default -> "방향";
        };
    }

}
