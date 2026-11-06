package com.example.analogalu.fluid;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OilFluid extends FlowingFluid {
    private final boolean source;

    public OilFluid(boolean source) {
        this.source = source;
        registerDefaultState(getStateDefinition().any().setValue(LEVEL, source ? 8 : 7).setValue(FALLING, false));
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
        builder.add(LEVEL);
    }

    @Override
    public Fluid getFlowing() {
        return ModFluids.FLOWING;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.STILL;
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.STILL || fluid == ModFluids.FLOWING;
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(net.minecraft.world.level.LevelAccessor level, BlockPos pos,
            BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 2;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 2;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 30;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.OIL.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSource(FluidState state) {
        return source;
    }

    @Override
    public int getAmount(FluidState state) {
        return source ? 8 : state.getValue(LEVEL);
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid,
            Direction direction) {
        return fluid != this;
    }

    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return source ? 1.0F : state.getValue(LEVEL) / 8.0F;
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return source ? 1.0F : state.getValue(LEVEL) / 8.0F;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    /** 눈 높이가 석유 속에 잠겨 있는지. 서버(익사)와 클라이언트(안개) 모두에서 쓴다. */
    public static boolean isInOil(BlockGetter level, net.minecraft.world.phys.Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        FluidState fluid = level.getFluidState(pos);
        if (!(fluid.getType() instanceof OilFluid)) return false;
        return point.y < pos.getY() + fluid.getHeight(level, pos);
    }

    @Override
    public Item getBucket() {
        // 석유 양동이는 없다. 빈 양동이 우클릭은 OilBucketing 이 처리한다.
        return net.minecraft.world.item.Items.AIR;
    }

    /** How deep an entity stands in oil, from the bottom of its box up to the surface. */
    public static double submergedDepth(net.minecraft.world.entity.Entity entity) {
        net.minecraft.world.phys.AABB box = entity.getBoundingBox().deflate(0.001);
        BlockGetter level = entity.level();
        int minX = net.minecraft.util.Mth.floor(box.minX);
        int maxX = net.minecraft.util.Mth.ceil(box.maxX);
        int minY = net.minecraft.util.Mth.floor(box.minY);
        int maxY = net.minecraft.util.Mth.ceil(box.maxY);
        int minZ = net.minecraft.util.Mth.floor(box.minZ);
        int maxZ = net.minecraft.util.Mth.ceil(box.maxZ);
        double depth = 0.0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.getType() instanceof OilFluid) {
                        double surface = y + fluid.getHeight(level, pos);
                        if (surface > box.minY) depth = Math.max(depth, surface - box.minY);
                    }
                }
            }
        }
        return depth;
    }
}
