package com.example.redstonetransportation.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Works out the rail line between two points, climbing one block per step. Slopes keep off bends
 * and the two end rails, because a rail block cannot both turn and climb, and an end rail has to
 * stay free to turn into whatever comes next.
 */
public final class TrackPlanner {
    /** How to cover the distance when the two points are not in line. */
    public enum Style {
        /** A staircase of bends, which is as diagonal as rails go, with any remainder laid straight. */
        DIAGONAL,
        /** An L with one bend. */
        CORNER
    }

    public record Cell(BlockPos pos, RailShape shape, boolean bend) {}

    private TrackPlanner() {}

    /**
     * @param startJoin the side the first rail already runs off to, or null for a free end
     * @param endJoin   the same for the last rail
     * @return the cells from start to end, or null when the height difference is too steep
     */
    public static List<Cell> plan(BlockPos start, BlockPos end, Direction startJoin, Direction endJoin, Style style) {
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();
        boolean xIsLong = Math.abs(dx) >= Math.abs(dz);
        Direction longWay = xIsLong ? direction(Direction.Axis.X, dx) : direction(Direction.Axis.Z, dz);
        Direction shortWay = xIsLong ? direction(Direction.Axis.Z, dz) : direction(Direction.Axis.X, dx);
        int longSteps = Math.max(Math.abs(dx), Math.abs(dz));
        int shortSteps = Math.min(Math.abs(dx), Math.abs(dz));

        // Stairs of single steps leave nowhere for a slope, so widen them only as far as the climb needs.
        int mostFlights = style == Style.DIAGONAL ? shortSteps / 2 : 0;
        for (int flights = 0; flights <= mostFlights; flights++) {
            List<Direction> steps = style == Style.DIAGONAL
                    ? diagonal(longWay, longSteps, shortWay, shortSteps, startJoin, flights)
                    : corner(longWay, longSteps, shortWay, shortSteps, startJoin);
            List<BlockPos> columns = walk(start, steps);
            int[] heights = climb(columns, end.getY() - start.getY(), start.getY());
            if (heights != null) return cells(columns, heights, startJoin, endJoin);
        }
        return null;
    }

    /** True when the only way to the end runs back over the rail the line came from. */
    public static boolean doublesBack(BlockPos start, BlockPos end, Direction startJoin) {
        boolean inLine = start.getX() == end.getX() || start.getZ() == end.getZ();
        return inLine && towards(start, end) == startJoin;
    }

    private static List<Cell> cells(List<BlockPos> columns, int[] heights, Direction startJoin, Direction endJoin) {
        List<Cell> cells = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            BlockPos column = columns.get(i);
            boolean first = i == 0;
            boolean last = i + 1 == columns.size();
            Direction back = first ? startJoin : towards(column, columns.get(i - 1));
            Direction ahead = last ? endJoin : towards(column, columns.get(i + 1));

            RailShape shape;
            if (!last && heights[i + 1] > heights[i]) {
                shape = ascending(ahead);
            } else if (!first && heights[i - 1] > heights[i]) {
                shape = ascending(back);
            } else {
                shape = connect(back, ahead);
            }
            cells.add(new Cell(new BlockPos(column.getX(), heights[i], column.getZ()), shape, isBend(back, ahead)));
        }
        return cells;
    }

    /** The long leg comes first, which leaves it the most room for slopes, unless that runs back down the line. */
    private static List<Direction> corner(Direction longWay, int longSteps, Direction shortWay, int shortSteps, Direction startJoin) {
        List<Direction> steps = new ArrayList<>();
        if (longWay == startJoin) {
            steps.addAll(Collections.nCopies(shortSteps, shortWay));
            steps.addAll(Collections.nCopies(longSteps, longWay));
        } else {
            steps.addAll(Collections.nCopies(longSteps, longWay));
            steps.addAll(Collections.nCopies(shortSteps, shortWay));
        }
        return steps;
    }

    /** The straight remainder comes first for the same reason, again unless that runs back down the line. */
    private static List<Direction> diagonal(Direction longWay, int longSteps, Direction shortWay, int shortSteps,
                                            Direction startJoin, int flights) {
        boolean backwards = longWay == startJoin;
        Direction first = backwards ? shortWay : longWay;
        Direction second = backwards ? longWay : shortWay;
        List<Direction> stairs = new ArrayList<>();
        for (int flight = 0; flight < flights; flight++) {
            stairs.addAll(List.of(first, first, second, second));
        }
        for (int step = flights * 2; step < shortSteps; step++) {
            stairs.addAll(List.of(first, second));
        }

        List<Direction> straight = Collections.nCopies(longSteps - shortSteps, longWay);
        List<Direction> steps = new ArrayList<>(backwards ? stairs : straight);
        steps.addAll(backwards ? straight : stairs);
        return steps;
    }

    private static List<BlockPos> walk(BlockPos start, List<Direction> steps) {
        List<BlockPos> columns = new ArrayList<>(steps.size() + 1);
        BlockPos.MutableBlockPos cursor = start.mutable().setY(0);
        columns.add(cursor.immutable());
        for (Direction step : steps) {
            cursor.move(step);
            columns.add(cursor.immutable());
        }
        return columns;
    }

    /** Spends the height difference on the earliest steps that allow a slope. */
    private static int[] climb(List<BlockPos> columns, int rise, int startY) {
        int[] heights = new int[columns.size()];
        heights[0] = startY;
        int left = rise;
        for (int i = 0; i + 1 < columns.size(); i++) {
            // The sloped rail is the lower cell of the step: this one going up, the next one going down.
            int sloped = left > 0 ? i : i + 1;
            boolean canClimb = left != 0 && sloped > 0 && sloped + 1 < columns.size() && !isBendAt(columns, sloped);
            int step = canClimb ? Integer.signum(left) : 0;
            heights[i + 1] = heights[i] + step;
            left -= step;
        }
        return left == 0 ? heights : null;
    }

    private static boolean isBendAt(List<BlockPos> columns, int index) {
        if (index <= 0 || index + 1 >= columns.size()) return false;
        return towards(columns.get(index - 1), columns.get(index)) != towards(columns.get(index), columns.get(index + 1));
    }

    private static boolean isBend(Direction back, Direction ahead) {
        return back != null && ahead != null && back.getAxis() != ahead.getAxis();
    }

    private static Direction direction(Direction.Axis axis, int distance) {
        return Direction.fromAxisAndDirection(axis, distance > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    private static Direction towards(BlockPos from, BlockPos to) {
        return Direction.getApproximateNearest(to.getX() - from.getX(), 0.0D, to.getZ() - from.getZ());
    }

    private static RailShape ascending(Direction direction) {
        return switch (direction) {
            case EAST -> RailShape.ASCENDING_EAST;
            case WEST -> RailShape.ASCENDING_WEST;
            case NORTH -> RailShape.ASCENDING_NORTH;
            default -> RailShape.ASCENDING_SOUTH;
        };
    }

    private static RailShape connect(Direction back, Direction ahead) {
        Direction first = back != null ? back : ahead;
        Direction second = ahead != null ? ahead : back;
        if (first == null) return RailShape.NORTH_SOUTH;
        if (first.getAxis() == second.getAxis()) {
            return first.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }
        boolean north = first == Direction.NORTH || second == Direction.NORTH;
        boolean east = first == Direction.EAST || second == Direction.EAST;
        if (north) return east ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
        return east ? RailShape.SOUTH_EAST : RailShape.SOUTH_WEST;
    }
}
