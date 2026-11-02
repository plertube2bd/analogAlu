package com.example.analogalu.fluid;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModFluids;
import com.example.analogalu.ModItems;
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
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(net.minecraft.world.level.LevelAccessor level, BlockPos pos,
            BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 3;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 2;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 12;
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

    @Override
    public Item getBucket() {
        return ModItems.OIL_BUCKET;
    }
}