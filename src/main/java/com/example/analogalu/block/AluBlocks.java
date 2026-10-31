package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 4비트 부호 없는 정수(0~15) 연산 블록들. 입력 신호 강도 = 데이터 값. */
public final class AluBlocks {
    private AluBlocks() {}

    /** 비트 단위 논리 게이트. 우클릭으로 반전(NOR/NAND/XNOR). NOT은 4비트 마스크로 제한한다. */
    public abstract static class Gate extends AluBlock {
        protected Gate(BlockBehaviour.Properties p) { super(p); }
        @Override protected boolean hasInvert() { return true; }
        protected abstract int op(int a, int b);
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            int r = op(inputA(level, pos, state), inputB(level, pos, state)) & 15;
            if (state.getValue(INVERTED)) r = ~r & 15;
            return new Out(r, false);
        }
    }

    public static class OrGate extends Gate {
        public OrGate(BlockBehaviour.Properties p) { super(p); }
        @Override protected int op(int a, int b) { return a | b; }
    }

    public static class AndGate extends Gate {
        public AndGate(BlockBehaviour.Properties p) { super(p); }
        @Override protected int op(int a, int b) { return a & b; }
    }

    public static class XorGate extends Gate {
        public XorGate(BlockBehaviour.Properties p) { super(p); }
        @Override protected int op(int a, int b) { return a ^ b; }
    }

    /** Bitwise NOT: R = ~A (4비트 마스크). 6(0110) -> 9(1001) */
    public static class BitwiseNot extends AluBlock {
        public BitwiseNot(BlockBehaviour.Properties p) { super(p); }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            return new Out(~inputBack(level, pos, state) & 15, false);
        }
    }

    /** Logical NOT: 입력이 0이면 15, 1 이상이면 0 */
    public static class LogicalNot extends AluBlock {
        public LogicalNot(BlockBehaviour.Properties p) { super(p); }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            return new Out(inputBack(level, pos, state) == 0 ? 15 : 0, false);
        }
    }

    /**
     * CRAM: 4비트 메모리 (정육면체). 저장값은 POWER 상태에 들어 있고 월드와 함께 저장된다.
     * 위에서 본 배치 (정면=위쪽):  B(출력) / D(초기화) E(CRAM) / H(입력). 나머지 면은 사용하지 않음.
     * D >= 1 이면 0으로 초기화(쓰기보다 우선), D = 0 이고 I >= 1 이면 I를 기록, I = 0 이면 유지.
     */
    public static class Cram extends AluBlock {
        public Cram(BlockBehaviour.Properties p) { super(p); }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            int d = inputA(level, pos, state);    // D: 초기화 (정면 기준 왼쪽)
            int i = inputBack(level, pos, state); // I: 데이터 입력 (뒷면)
            int cur = state.getValue(POWER);
            return new Out(d > 0 ? 0 : (i > 0 ? i : cur), false);
        }
        @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return true; }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return Shapes.block();
        }
    }

    /** R = (A + B) mod 16, C = A + B >= 16 */
    public static class Adder extends AluBlock {
        public Adder(BlockBehaviour.Properties p) { super(p); }
        @Override protected boolean hasCarry() { return true; }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            int sum = inputA(level, pos, state) + inputB(level, pos, state);
            return new Out(sum & 15, sum >= 16);
        }
    }

    /** R = (A - B) mod 16, C(Borrow) = A < B */
    public static class Subtractor extends AluBlock {
        public Subtractor(BlockBehaviour.Properties p) { super(p); }
        @Override protected boolean hasCarry() { return true; }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            int a = inputA(level, pos, state);
            int b = inputB(level, pos, state);
            return new Out((a - b) & 15, a < b);
        }
    }

    /**
     * 구리선 연장기: 입력 강도를 그대로 1틱 뒤 출력한다. 출력 쪽 구리선에게는 새 신호원이므로
     * 구리선 거리가 0부터 다시 시작해 32블록을 추가로 전달한다. 입력이 0이면 출력도 0.
     */
    public static class Extender extends AluBlock {
        public Extender(BlockBehaviour.Properties p) { super(p); }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            return new Out(inputBack(level, pos, state), false);
        }
    }

    /** R = A >> 1 */
    public static class ShiftRight extends AluBlock {
        public ShiftRight(BlockBehaviour.Properties p) { super(p); }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            return new Out(inputBack(level, pos, state) >> 1, false);
        }
    }

    /** R = (A << 1) mod 16 */
    public static class ShiftLeft extends AluBlock {
        public ShiftLeft(BlockBehaviour.Properties p) { super(p); }
        @Override protected Out compute(Level level, BlockPos pos, BlockState state) {
            return new Out((inputBack(level, pos, state) << 1) & 15, false);
        }
    }
}
