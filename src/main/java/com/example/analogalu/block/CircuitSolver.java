package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CircuitSolver {
    private static final double CABLE_EDGE_FACTOR = 0.5;
    private static final int MAX_CABLES_PER_CIRCUIT = 128;
    private static final Map<Level, Map<Long, Long>> SOLVED_NETWORKS = new WeakHashMap<>();

    private CircuitSolver() {}

    public static void recomputeAround(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockEntity(neighbor) instanceof CopperCableBlockEntity) {
                recomputeFrom(level, neighbor);
            }
        }
    }

    public static void solveFrom(Level level, BlockPos start) {
        solveFrom(level, start, false);
    }

    public static void recomputeFrom(Level level, BlockPos start) {
        solveFrom(level, start, true);
    }

    private static void solveFrom(Level level, BlockPos start, boolean force) {
        if (level.isClientSide() || !(level.getBlockEntity(start) instanceof CopperCableBlockEntity)) return;
        Network network = discover(level, start);
        if (network.cables.isEmpty()) return;
        if (network.overflow) {
            clearState(level, network);
            return;
        }

        long gameTime = level.getGameTime();
        Map<Long, Long> solvedThisTick = SOLVED_NETWORKS.computeIfAbsent(level, ignored -> new HashMap<>());
        solvedThisTick.entrySet().removeIf(entry -> entry.getValue() != gameTime);
        if (force) {
            solvedThisTick.put(network.root.asLong(), gameTime);
        } else if (solvedThisTick.putIfAbsent(network.root.asLong(), gameTime) != null) {
            return;
        }

        // 전원 선택: 발전기/일반 건전지는 회로당 하나만 허용한다. 그런 전원이 있으면 충전식 건전지는 충전 부하가 되고,
        // 없으면 에너지가 가장 많은 충전식 건전지 하나가 전원이 된다(나머지는 회로에서 빠진다).
        List<Branch> sources = network.branches.stream().filter(branch -> branch.kind == BranchKind.SOURCE).toList();
        List<Branch> fixedSources = sources.stream().filter(branch -> !isRechargeable(level, branch)).toList();
        List<Branch> rechargeables = sources.stream().filter(branch -> isRechargeable(level, branch)).toList();
        Branch source;
        if (fixedSources.size() > 1) {
            clearState(level, network);
            return;
        } else if (fixedSources.size() == 1) {
            source = fixedSources.getFirst();
        } else if (!rechargeables.isEmpty()) {
            source = rechargeables.stream().max(Comparator
                    .comparingDouble((Branch branch) -> storedEnergy(level, branch))
                    .thenComparingLong(branch -> -branch.devicePos.asLong())).orElseThrow();
        } else {
            clearState(level, network);
            return;
        }
        List<Branch> chargers = new ArrayList<>();
        if (rechargeables.size() > (source.kind == BranchKind.SOURCE && isRechargeable(level, source) ? 1 : 0)) {
            List<Branch> branches = new ArrayList<>(network.branches);
            for (Branch battery : rechargeables) {
                if (battery == source) continue;
                branches.remove(battery);
                CircuitDeviceBlockEntity entity = blockEntity(level, battery.devicePos);
                if (entity == null) continue;
                entity.setSourceTerminalVoltage(0.0);
                entity.setLoadState(0.0, 0.0);
                if (fixedSources.isEmpty() || entity.isFullyCharged()) continue;
                Branch charger = Branch.passive(battery.devicePos, battery.positive, battery.negative,
                        battery.resistance);
                branches.add(charger);
                chargers.add(charger);
            }
            network = new Network(network.cables, branches, network.root, network.overflow);
        }
        CircuitDeviceBlock.Kind sourceKind = ((CircuitDeviceBlock) level.getBlockState(source.devicePos).getBlock()).kind();
        CircuitDeviceBlockEntity sourceEntity = blockEntity(level, source.devicePos);
        boolean available = sourceEntity != null && switch (sourceKind) {
            case BATTERY, RECHARGEABLE_BATTERY -> sourceEntity.getStoredEnergyJoules() > 0.0;
            case STEP_UP, STEP_DOWN -> sourceEntity.converterAvailable(gameTime);
            case COAL_GENERATOR -> sourceEntity.hasFuel();
            case DIESEL_GENERATOR -> sourceEntity.hasFuel();
            case WIND_GENERATOR, SOLAR_GENERATOR ->
                    CircuitDeviceBlock.hasNaturalInput(level, source.devicePos, sourceKind);
            default -> false;
        };
        if (!available || !hasPassivePath(network, source.positive, source.negative)) {
            if (sourceEntity != null) sourceEntity.accountSourcePower(0.0, gameTime, sourceKind);
            clearState(level, network);
            return;
        }

        double sourceVoltage = CircuitDeviceBlock.isConverter(sourceKind)
                ? sourceEntity.converterOutputVoltage()
                : CircuitDeviceBlock.sourceVoltage(level, source.devicePos, sourceKind);
        Solution solution = solveNetwork(level, network, source, sourceVoltage,
                CircuitDeviceBlock.sourceResistance(sourceKind));
        if (solution == null) {
            clearState(level, network);
            sourceEntity.accountSourcePower(0.0, gameTime, sourceKind);
            return;
        }

        for (BlockPos cablePos : network.cables) {
            if (level.getBlockEntity(cablePos) instanceof CopperCableBlockEntity cable) {
                cable.setCircuitState(solution.voltages.getOrDefault(cablePos, 0.0),
                        solution.currents.getOrDefault(cablePos, 0.0));
            }
        }
        sourceEntity.setSourceTerminalVoltage(solution.sourceTerminalVoltage);
        sourceEntity.setLoadState(0.0, 0.0);
        if (CircuitDeviceBlock.isConverter(sourceKind)) {
            sourceEntity.setConverterOutput(sourceVoltage, solution.sourceWatts, gameTime);
        } else {
            sourceEntity.accountSourcePower(solution.sourceWatts, gameTime, sourceKind);
        }
        for (Branch charger : chargers) {
            CircuitDeviceBlockEntity battery = blockEntity(level, charger.devicePos);
            if (battery == null) continue;
            double current = battery.getLoadCurrent();
            battery.accountCharge(current * current * CircuitDeviceBlock.RECHARGE_RESISTANCE, gameTime);
        }
    }

    private static boolean isRechargeable(Level level, Branch branch) {
        return level.getBlockState(branch.devicePos).getBlock() instanceof CircuitDeviceBlock device
                && device.kind() == CircuitDeviceBlock.Kind.RECHARGEABLE_BATTERY;
    }

    private static double storedEnergy(Level level, Branch branch) {
        CircuitDeviceBlockEntity entity = blockEntity(level, branch.devicePos);
        return entity == null ? 0.0 : entity.getStoredEnergyJoules();
    }

    private static Network discover(Level level, BlockPos start) {
        Set<BlockPos> cables = new HashSet<>();
        Set<BlockPos> devices = new HashSet<>();
        Set<String> branchKeys = new HashSet<>();
        List<Branch> branches = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty() && cables.size() < MAX_CABLES_PER_CIRCUIT) {
            BlockPos cablePos = queue.removeFirst();
            if (!cables.add(cablePos)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = cablePos.relative(direction);
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(ModBlocks.COPPER_CABLE)) {
                    BlockState cableState = level.getBlockState(cablePos);
                    if (!CopperCableBlock.allowsConnection(cableState, direction)
                            || !CopperCableBlock.allowsConnection(neighborState, direction.getOpposite())) {
                        continue;
                    }
                    addBranch(branches, branchKeys, Branch.wire(cablePos, neighbor,
                            wireResistance(level, cablePos, neighbor)));
                    queue.addLast(neighbor);
                    continue;
                }
                if (!(neighborState.getBlock() instanceof CircuitDeviceBlock device)
                    || !isPortDirection(neighborState, direction, device.kind())) continue;
                if (!CopperCableBlock.allowsConnection(level.getBlockState(cablePos), direction)) continue;

                PortPair ports = devicePorts(level, cablePos, neighbor, neighborState, device.kind());
                if (ports == null) continue;
                BlockPos positive = ports.positive;
                BlockPos negative = ports.negative;
                if (!isCable(level, positive) || !isCable(level, negative)) continue;
                if (!devices.add(neighbor)) continue;

                CircuitDeviceBlock.Kind kind = device.kind();
                if (kind == CircuitDeviceBlock.Kind.SWITCH && !neighborState.getValue(CircuitDeviceBlock.CLOSED)) {
                    continue;
                }
                // 승압기/변압기: 앞뒤 단자는 입력(부하), 위아래 단자는 출력(전원). 두 쪽은 서로 다른 회로다.
                boolean converterOutput = CircuitDeviceBlock.isConverter(kind) && positive.equals(neighbor.above());
                double resistance = switch (kind) {
                    case RESISTOR -> CircuitDeviceBlock.LOAD_RESISTANCE;
                    case MINER -> CircuitDeviceBlock.MINER_RESISTANCE;
                    case RECHARGEABLE_BATTERY -> CircuitDeviceBlock.RECHARGE_RESISTANCE;
                    case STEP_UP, STEP_DOWN -> converterInputResistance(level, neighbor);
                    default -> 0.0;
                };
                if (kind != CircuitDeviceBlock.Kind.BATTERY) {
                    resistance += wireResistance(level, positive, neighbor)
                            + wireResistance(level, negative, neighbor);
                }
                Branch branch = (CircuitDeviceBlock.isConverter(kind) ? converterOutput : isPowerSource(kind))
                    ? Branch.source(neighbor, positive, negative, Math.max(resistance, 1.0e-9))
                        : Branch.passive(neighbor, positive, negative, Math.max(resistance, 1.0e-9));
                addBranch(branches, branchKeys, branch);
                queue.addLast(positive);
                queue.addLast(negative);
            }
        }

        BlockPos root = cables.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElse(start);
        boolean overflow = queue.stream().anyMatch(pos -> !cables.contains(pos));
        return new Network(cables, branches, root, overflow);
    }

    private static double converterInputResistance(Level level, BlockPos pos) {
        CircuitDeviceBlockEntity entity = blockEntity(level, pos);
        return entity == null ? 1.0e6 : entity.converterInputResistance();
    }

    private static boolean isPortDirection(BlockState deviceState, Direction direction,
            CircuitDeviceBlock.Kind kind) {
        if (CircuitDeviceBlock.isConverter(kind)) {
            Direction facing = deviceState.getValue(CircuitDeviceBlock.FACING);
            return direction == Direction.UP || direction == Direction.DOWN
                    || direction == facing || direction == facing.getOpposite();
        }
        if (kind == CircuitDeviceBlock.Kind.MINER) {
            Direction facing = deviceState.getValue(CircuitDeviceBlock.FACING);
            return direction == Direction.UP || direction == Direction.DOWN
                    || direction == facing.getClockWise() || direction == facing.getCounterClockWise();
        }
        Direction facing = deviceState.getValue(CircuitDeviceBlock.FACING);
        return direction == facing || direction == facing.getOpposite();
    }

        private static PortPair devicePorts(Level level, BlockPos cablePos, BlockPos pos,
            BlockState state, CircuitDeviceBlock.Kind kind) {
        if (CircuitDeviceBlock.isConverter(kind)) {
            // 케이블이 위/아래에 닿아 있으면 출력 단자(위 +, 아래 -), 앞/뒤면 입력 단자(앞 +, 뒤 -).
            if (cablePos.equals(pos.above()) || cablePos.equals(pos.below())) {
                return new PortPair(pos.above(), pos.below());
            }
            Direction facing = state.getValue(CircuitDeviceBlock.FACING);
            return new PortPair(pos.relative(facing), pos.relative(facing.getOpposite()));
        }
        if (kind != CircuitDeviceBlock.Kind.MINER) {
            Direction facing = state.getValue(CircuitDeviceBlock.FACING);
            return new PortPair(pos.relative(facing), pos.relative(facing.getOpposite()));
        }

        BlockPos top = pos.above();
        BlockPos bottom = pos.below();
        if (isCable(level, top) && isCable(level, bottom) && (cablePos.equals(top) || cablePos.equals(bottom))) {
            return new PortPair(top, bottom);
        }

        Direction side = state.getValue(CircuitDeviceBlock.FACING).getClockWise();
        BlockPos right = pos.relative(side);
        BlockPos left = pos.relative(side.getOpposite());
        if (isCable(level, right) && isCable(level, left) && (cablePos.equals(right) || cablePos.equals(left))) {
            return new PortPair(right, left);
        }
        return null;
    }

    private static boolean isPowerSource(CircuitDeviceBlock.Kind kind) {
        return kind == CircuitDeviceBlock.Kind.BATTERY
                || kind == CircuitDeviceBlock.Kind.RECHARGEABLE_BATTERY
                || CircuitDeviceBlock.isConverter(kind)
                || kind == CircuitDeviceBlock.Kind.COAL_GENERATOR
            || kind == CircuitDeviceBlock.Kind.DIESEL_GENERATOR
                || kind == CircuitDeviceBlock.Kind.WIND_GENERATOR
                || kind == CircuitDeviceBlock.Kind.SOLAR_GENERATOR;
    }

    private static boolean isCable(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.COPPER_CABLE);
    }

    private static double wireResistance(Level level, BlockPos cablePos, BlockPos otherPos) {
        double resistance = 0.0;
        if (level.getBlockEntity(cablePos) instanceof CopperCableBlockEntity cable) {
            resistance += cable.getResistanceOhms() * CABLE_EDGE_FACTOR;
        }
        if (level.getBlockEntity(otherPos) instanceof CopperCableBlockEntity cable) {
            resistance += cable.getResistanceOhms() * CABLE_EDGE_FACTOR;
        }
        return Math.max(resistance, 1.0e-9);
    }

    private static void addBranch(List<Branch> branches, Set<String> keys, Branch branch) {
        if (keys.add(branch.key())) branches.add(branch);
    }

    private static boolean hasPassivePath(Network network, BlockPos start, BlockPos target) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            BlockPos node = queue.removeFirst();
            if (node.equals(target)) return true;
            for (Branch branch : network.branches) {
                if (branch.kind == BranchKind.SOURCE) continue;
                BlockPos next = branch.otherEnd(node);
                if (next != null && visited.add(next)) queue.addLast(next);
            }
        }
        return false;
    }

    private static Solution solveNetwork(Level level, Network network, Branch source,
            double sourceVoltage, double internalResistance) {
        List<BlockPos> nodes = new ArrayList<>(network.cables);
        nodes.sort(Comparator.comparingLong(BlockPos::asLong));
        BlockPos ground = source.negative;
        Map<BlockPos, Integer> variables = new HashMap<>();
        int nextVariable = 0;
        for (BlockPos node : nodes) {
            if (!node.equals(ground)) variables.put(node, nextVariable++);
        }
        int sourcePositive = nextVariable++;
        int sourceCurrent = nextVariable++;
        double[][] matrix = new double[nextVariable][nextVariable + 1];

        for (Branch branch : network.branches) {
            if (branch.kind == BranchKind.SOURCE) continue;
            stampResistor(matrix, variables, ground, branch.first, branch.second, branch.resistance);
        }
        stampResistorToVariable(matrix, variables, ground, source.positive, sourcePositive,
                internalResistance + wireResistance(level, source.positive, source.devicePos)
                        + wireResistance(level, source.negative, source.devicePos));
        matrix[sourcePositive][sourceCurrent] += 1.0;
        matrix[sourceCurrent][sourcePositive] += 1.0;
        matrix[sourceCurrent][nextVariable] = sourceVoltage;

        double[] values = gaussianSolve(matrix);
        if (values == null) return null;

        Map<BlockPos, Double> voltages = new HashMap<>();
        Map<BlockPos, Double> currents = new HashMap<>();
        for (BlockPos node : nodes) {
            int variable = variables.getOrDefault(node, -1);
            voltages.put(node, variable < 0 ? 0.0 : values[variable]);
        }
        for (Branch branch : network.branches) {
            if (branch.kind == BranchKind.SOURCE) continue;
            double current = Math.abs((voltages.getOrDefault(branch.first, 0.0)
                    - voltages.getOrDefault(branch.second, 0.0)) / branch.resistance);
            currents.merge(branch.first, current, Math::max);
            currents.merge(branch.second, current, Math::max);
            if (branch.devicePos != null && level.getBlockEntity(branch.devicePos)
                instanceof CircuitDeviceBlockEntity device) {
            CircuitDeviceBlock.Kind kind = ((CircuitDeviceBlock) level.getBlockState(branch.devicePos).getBlock())
                .kind();
            if (CircuitDeviceBlock.isConverter(kind)) {
                device.setConverterInput(current * device.converterInputResistance(), current, level.getGameTime(),
                        sourceVoltage);
                continue;
            }
            double deviceResistance = switch (kind) {
                case MINER -> CircuitDeviceBlock.MINER_RESISTANCE;
                case RESISTOR -> CircuitDeviceBlock.LOAD_RESISTANCE;
                case RECHARGEABLE_BATTERY -> CircuitDeviceBlock.RECHARGE_RESISTANCE;
                default -> 0.0;
            };
            device.setLoadState(current * deviceResistance, current);
            }
        }
        double sourceCurrentValue = Math.abs(values[sourceCurrent]);
        double terminalVoltage = voltages.getOrDefault(source.positive, 0.0)
            - voltages.getOrDefault(source.negative, 0.0);
        return new Solution(voltages, currents, sourceCurrentValue * sourceVoltage, terminalVoltage);
    }

    private static void stampResistor(double[][] matrix, Map<BlockPos, Integer> variables, BlockPos ground,
            BlockPos first, BlockPos second, double resistance) {
        if (resistance <= 0.0 || !Double.isFinite(resistance)) return;
        double conductance = 1.0 / resistance;
        Integer firstVariable = first.equals(ground) ? null : variables.get(first);
        Integer secondVariable = second.equals(ground) ? null : variables.get(second);
        if (firstVariable != null) matrix[firstVariable][firstVariable] += conductance;
        if (secondVariable != null) matrix[secondVariable][secondVariable] += conductance;
        if (firstVariable != null && secondVariable != null) {
            matrix[firstVariable][secondVariable] -= conductance;
            matrix[secondVariable][firstVariable] -= conductance;
        }
    }

    private static void stampResistorToVariable(double[][] matrix, Map<BlockPos, Integer> variables,
            BlockPos ground, BlockPos first, int secondVariable, double resistance) {
        double conductance = 1.0 / resistance;
        Integer firstVariable = first.equals(ground) ? null : variables.get(first);
        matrix[secondVariable][secondVariable] += conductance;
        if (firstVariable != null) {
            matrix[firstVariable][firstVariable] += conductance;
            matrix[firstVariable][secondVariable] -= conductance;
            matrix[secondVariable][firstVariable] -= conductance;
        }
    }

    private static double[] gaussianSolve(double[][] matrix) {
        int size = matrix.length;
        for (int pivot = 0; pivot < size; pivot++) {
            int best = pivot;
            for (int row = pivot + 1; row < size; row++) {
                if (Math.abs(matrix[row][pivot]) > Math.abs(matrix[best][pivot])) best = row;
            }
            if (Math.abs(matrix[best][pivot]) < 1.0e-12) return null;
            double[] swap = matrix[pivot];
            matrix[pivot] = matrix[best];
            matrix[best] = swap;

            double divisor = matrix[pivot][pivot];
            for (int column = pivot; column <= size; column++) matrix[pivot][column] /= divisor;
            for (int row = 0; row < size; row++) {
                if (row == pivot) continue;
                double factor = matrix[row][pivot];
                if (factor == 0.0) continue;
                for (int column = pivot; column <= size; column++) {
                    matrix[row][column] -= factor * matrix[pivot][column];
                }
            }
        }
        double[] result = new double[size];
        for (int row = 0; row < size; row++) result[row] = matrix[row][size];
        return result;
    }

    private static void clearState(Level level, Network network) {
        for (BlockPos cablePos : network.cables) {
            if (level.getBlockEntity(cablePos) instanceof CopperCableBlockEntity cable) {
                cable.setCircuitState(0.0, 0.0);
            }
        }
        for (Branch branch : network.branches) {
            if (branch.devicePos != null && level.getBlockEntity(branch.devicePos)
                    instanceof CircuitDeviceBlockEntity device) {
                if (branch.kind == BranchKind.SOURCE) {
                    device.setSourceTerminalVoltage(0.0);
                } else if (CircuitDeviceBlock.isConverter(device.kindOrNull())) {
                    device.setConverterInput(0.0, 0.0, level.getGameTime(), 0.0);
                    continue;
                }
                device.setLoadState(0.0, 0.0);
            }
        }
    }

    private static CircuitDeviceBlockEntity blockEntity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CircuitDeviceBlockEntity entity ? entity : null;
    }

    private enum BranchKind { WIRE, PASSIVE, SOURCE }

    private record Branch(BranchKind kind, BlockPos first, BlockPos second, double resistance,
            BlockPos devicePos, BlockPos positive, BlockPos negative) {
        static Branch wire(BlockPos first, BlockPos second, double resistance) {
            return new Branch(BranchKind.WIRE, first, second, resistance, null, null, null);
        }

        static Branch passive(BlockPos device, BlockPos positive, BlockPos negative, double resistance) {
            return new Branch(BranchKind.PASSIVE, positive, negative, resistance, device, null, null);
        }

        /** resistance 는 충전식 건전지가 충전 부하로 바뀔 때만 쓰인다. */
        static Branch source(BlockPos device, BlockPos positive, BlockPos negative, double resistance) {
            return new Branch(BranchKind.SOURCE, positive, negative, resistance, device, positive, negative);
        }

        String key() {
            if (kind != BranchKind.WIRE) return kind + ":" + devicePos.asLong();
            return "W:" + Math.min(first.asLong(), second.asLong()) + ":"
                + Math.max(first.asLong(), second.asLong());
        }

        BlockPos otherEnd(BlockPos node) {
            if (first.equals(node)) return second;
            if (second.equals(node)) return first;
            return null;
        }
    }

    private record Network(Set<BlockPos> cables, List<Branch> branches, BlockPos root, boolean overflow) {}
    private record PortPair(BlockPos positive, BlockPos negative) {}
    private record Solution(Map<BlockPos, Double> voltages, Map<BlockPos, Double> currents, double sourceWatts,
            double sourceTerminalVoltage) {}
}