package com.example.analogalu.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 구리선: 신호 강도를 유지하며 연결된 경로 기준 최대 32블록까지 전달한다.
 *
 * 전달 방식: 이웃 업데이트가 오면 연결된 구리선 네트워크 전체를 한 번에 탐색하고,
 * 외부 신호원(레버, 중계기, 연장기 등)에서 BFS로 (강도, 경로 거리)를 다시 계산한다.
 * 네트워크 전체를 재계산하므로 신호원이 사라질 때 잔류 신호(ghost)가 생기지 않는다.
 *
 * 잠금 모드(우클릭): 잠긴 구리선은 다른 구리선과 연결/신호 교환을 하지 않고, 잠근 순간의 세기를 그대로 유지한다.
 * 이웃한 구리선이 아닌 블록(연산 블록, 레드스톤 부품 등)에는 유지 중인 세기를 계속 출력한다.
 */
public class CopperWireBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    public static final int MAX_DISTANCE = 32;
    private static final int MAX_NETWORK = 4096;

    private static final Map<Direction, EnumProperty<RedstoneSide>> SIDES = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH_REDSTONE,
            Direction.EAST, BlockStateProperties.EAST_REDSTONE,
            Direction.SOUTH, BlockStateProperties.SOUTH_REDSTONE,
            Direction.WEST, BlockStateProperties.WEST_REDSTONE);

    // 바닐라 레드스톤 가루와 동일한 히트박스
    private static final VoxelShape DOT = Block.box(3, 0, 3, 13, 1, 13);
    private static final Map<Direction, VoxelShape> ARM = Map.of(
            Direction.NORTH, Block.box(3, 0, 0, 13, 1, 13),
            Direction.SOUTH, Block.box(3, 0, 3, 13, 1, 16),
            Direction.WEST, Block.box(0, 0, 3, 13, 1, 13),
            Direction.EAST, Block.box(3, 0, 3, 16, 1, 13));
    private static final Map<Direction, VoxelShape> WALL = Map.of(
            Direction.NORTH, Block.box(3, 0, 0, 13, 16, 1),
            Direction.SOUTH, Block.box(3, 0, 15, 13, 16, 16),
            Direction.WEST, Block.box(0, 0, 3, 1, 16, 13),
            Direction.EAST, Block.box(15, 0, 3, 16, 16, 13));

    /** 현재 재계산 중인 네트워크의 좌표(재진입 방지). 서버 스레드 전용. */
    private static final Set<BlockPos> ACTIVE = new HashSet<>();
    /** 외부 신호를 읽는 동안 구리선의 출력을 0으로 만들어 자기 자신을 읽는 루프를 막는다. */
    private static boolean calculating = false;

    public CopperWireBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any().setValue(POWER, 0).setValue(LOCKED, false);
        for (EnumProperty<RedstoneSide> p : SIDES.values()) s = s.setValue(p, RedstoneSide.NONE);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, LOCKED);
        SIDES.values().forEach(builder::add);
    }

    // ---------------------------------------------------------------- 모양/설치

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = DOT;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            RedstoneSide side = state.getValue(SIDES.get(d));
            if (side != RedstoneSide.NONE) shape = Shapes.or(shape, ARM.get(d));
            if (side == RedstoneSide.UP) shape = Shapes.or(shape, WALL.get(d));
        }
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeState(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        return b.isFaceSturdy(level, below, Direction.UP) || b.is(Blocks.HOPPER);
    }

    // ---------------------------------------------------------------- 연결 판정

    /** 연결 가능한(= 잠기지 않은) 구리선인가 */
    private boolean isOpenWire(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.is(this) && !s.getValue(LOCKED);
    }

    private BlockPos neighborWire(BlockGetter level, BlockPos pos, Direction d) {
        BlockState self = level.getBlockState(pos);
        if (self.is(this) && self.getValue(LOCKED)) return null; // 잠긴 구리선은 아무와도 연결되지 않는다
        BlockPos np = pos.relative(d);
        BlockState ns = level.getBlockState(np);
        if (ns.is(this)) return ns.getValue(LOCKED) ? null : np;
        BlockPos above = pos.above();
        if (!level.getBlockState(above).isRedstoneConductor(level, above)
                && ns.isFaceSturdy(level, np, Direction.UP)
                && isOpenWire(level, np.above())) {
            return np.above();
        }
        if (!ns.isRedstoneConductor(level, np) && isOpenWire(level, np.below())) {
            return np.below();
        }
        return null;
    }

    private static boolean canConnectOther(BlockState ns, Direction d) {
        if (ns.getBlock() instanceof CopperWireBlock) return false; // 구리선끼리의 연결은 neighborWire가 담당
        if (ns.is(Blocks.REDSTONE_WIRE)) return true;
        if (ns.is(Blocks.REPEATER)) {
            Direction f = ns.getValue(DiodeBlock.FACING);
            return f == d || f == d.getOpposite();
        }
        if (ns.is(Blocks.OBSERVER)) return ns.getValue(ObserverBlock.FACING) == d;
        return ns.isSignalSource();
    }

    private RedstoneSide sideOf(BlockGetter level, BlockPos pos, Direction d) {
        BlockPos w = neighborWire(level, pos, d);
        if (w != null) return w.getY() > pos.getY() ? RedstoneSide.UP : RedstoneSide.SIDE;
        return canConnectOther(level.getBlockState(pos.relative(d)), d) ? RedstoneSide.SIDE : RedstoneSide.NONE;
    }

    private BlockState computeState(BlockGetter level, BlockPos pos, BlockState state) {
        Map<Direction, RedstoneSide> sides = new HashMap<>();
        int count = 0;
        Direction last = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            RedstoneSide s = sideOf(level, pos, d);
            sides.put(d, s);
            if (s != RedstoneSide.NONE) { count++; last = d; }
        }
        // 연결이 하나뿐이면 바닐라처럼 반대쪽도 직선으로 연장
        if (count == 1 && sides.get(last.getOpposite()) == RedstoneSide.NONE) {
            sides.put(last.getOpposite(), RedstoneSide.SIDE);
        }
        for (Direction d : Direction.Plane.HORIZONTAL) state = state.setValue(SIDES.get(d), sides.get(d));
        return state;
    }

    private List<BlockPos> wireNeighbors(BlockGetter level, BlockPos pos) {
        List<BlockPos> list = new ArrayList<>(4);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos w = neighborWire(level, pos, d);
            if (w != null) list.add(w);
        }
        return list;
    }

    // ---------------------------------------------------------------- 신호 출력

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (calculating || direction == Direction.DOWN) return 0;
        int p = state.getValue(POWER);
        if (p == 0) return 0;
        if (direction == Direction.UP) return p;
        Direction toward = direction.getOpposite(); // 신호를 받는 쪽
        if (state.getValue(LOCKED) && level.getBlockState(pos.relative(toward)).is(this)) return 0;
        // 바닐라 가루로는 출력하지 않음 (가루 <-> 구리선 사이 잔류 신호 루프 방지). 바닐라 -> 구리선은 가능.
        if (level.getBlockState(pos.relative(toward)).is(Blocks.REDSTONE_WIRE)) return 0;
        return state.getValue(SIDES.get(toward)) != RedstoneSide.NONE ? p : 0;
    }

    // ---------------------------------------------------------------- 잠금 모드

    /** 우클릭으로 잠금/해제. 잠그면 현재 세기를 유지한 채 다른 구리선과 분리되고, 해제하면 다시 연결되어 재계산된다. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        boolean lock = !state.getValue(LOCKED);
        // 먼저 잠금 값을 반영해야 연결 판정(neighborWire)이 새 상태를 보고 모양을 계산한다
        level.setBlock(pos, state.setValue(LOCKED, lock), Block.UPDATE_CLIENTS);
        BlockState shaped = computeState(level, pos, level.getBlockState(pos));
        if (shaped != level.getBlockState(pos)) level.setBlock(pos, shaped, Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, lock ? 0.55F : 0.6F);
        for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
        refreshAround(level, pos);
        if (!lock) recompute(level, pos);
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- 업데이트

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(this) || level.isClientSide()) return;
        refreshAround(level, pos);
        recompute(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (movedByPiston) return;
        for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
        refreshAround(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide() || ACTIVE.contains(pos)) return;
        if (!state.canSurvive(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }
        BlockState updated = computeState(level, pos, state);
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        recompute(level, pos);
    }

    /** pos 주변(수평 + 계단 높이)의 구리선 연결 모양과 신호를 갱신한다. */
    private void refreshAround(Level level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos np = pos.relative(d);
            for (BlockPos q : new BlockPos[] {np, np.above(), np.below()}) {
                BlockState s = level.getBlockState(q);
                if (!s.is(this)) continue;
                BlockState updated = computeState(level, q, s);
                if (updated != s) level.setBlock(q, updated, Block.UPDATE_CLIENTS);
                recompute(level, q);
            }
        }
    }

    private void recompute(Level level, BlockPos start) {
        if (level.isClientSide() || ACTIVE.contains(start) || !isOpenWire(level, start)) return;

        // 1) 연결된 네트워크 수집
        Map<BlockPos, List<BlockPos>> adj = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        adj.put(start, wireNeighbors(level, start));
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (BlockPos n : adj.get(p)) {
                if (adj.size() >= MAX_NETWORK) break;
                if (!adj.containsKey(n)) {
                    adj.put(n, wireNeighbors(level, n));
                    queue.add(n);
                }
            }
        }

        // 2) 각 구리선에 인접한 외부 신호 읽기
        Map<BlockPos, Integer> ext = new HashMap<>();
        calculating = true;
        try {
            for (BlockPos p : adj.keySet()) {
                int m = 0;
                for (Direction d : Direction.values()) m = Math.max(m, level.getSignal(p.relative(d), d));
                ext.put(p, m);
            }
        } finally {
            calculating = false;
        }

        // 3) 강한 신호부터 BFS. 신호원 바로 옆 구리선이 거리 1, 거리 32까지 유지
        Map<BlockPos, Integer> power = new HashMap<>();
        for (int lvl = 15; lvl >= 1; lvl--) {
            Map<BlockPos, Integer> dist = new HashMap<>();
            ArrayDeque<BlockPos> cur = new ArrayDeque<>();
            for (BlockPos p : adj.keySet()) {
                if (ext.get(p) == lvl && !power.containsKey(p)) {
                    power.put(p, lvl);
                    dist.put(p, 1);
                    cur.add(p);
                }
            }
            while (!cur.isEmpty()) {
                BlockPos p = cur.poll();
                int d = dist.get(p);
                if (d >= MAX_DISTANCE) continue;
                for (BlockPos n : adj.get(p)) {
                    if (adj.containsKey(n) && !power.containsKey(n)) {
                        power.put(n, lvl);
                        dist.put(n, d + 1);
                        cur.add(n);
                    }
                }
            }
        }

        // 4) 결과 반영 (변경된 것만) 후 주변 알림
        ACTIVE.addAll(adj.keySet());
        try {
            List<BlockPos> changed = new ArrayList<>();
            for (BlockPos p : adj.keySet()) {
                BlockState s = level.getBlockState(p);
                int np = power.getOrDefault(p, 0);
                if (s.is(this) && !s.getValue(LOCKED) && s.getValue(POWER) != np) {
                    level.setBlock(p, s.setValue(POWER, np), Block.UPDATE_CLIENTS);
                    changed.add(p);
                }
            }
            for (BlockPos p : changed) {
                level.updateNeighborsAt(p, this);
                for (Direction d : Direction.values()) level.updateNeighborsAt(p.relative(d), this);
            }
        } finally {
            ACTIVE.removeAll(adj.keySet());
        }
    }
}
