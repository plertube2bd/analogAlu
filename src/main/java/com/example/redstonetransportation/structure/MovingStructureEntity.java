package com.example.redstonetransportation.structure;

import com.example.redstonetransportation.block.ElevatorCallBlock;
import com.example.redstonetransportation.block.RouteBeaconBlock;
import com.example.redstonetransportation.content.ModAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A built structure, lifted out of the world so it can move. The entity itself is the size of the
 * core block; the other blocks exist as collision boxes and a drawn mesh, and are aimed at through
 * {@link StructureRay} rather than through the entity's hitbox.
 */
public final class MovingStructureEntity extends Entity {
    /** How far from an elevator its call blocks may stand. */
    public static final int FLOOR_RADIUS = 8;
    private static final int FLOOR_HEIGHT = 96;
    private static final int FLOOR_SCAN_INTERVAL = 40;

    private static final double ELEVATOR_SPEED = 0.2D;
    private static final double SHIP_SPEED = 0.4D;
    private static final double AIRSHIP_SPEED = 0.5D;
    private static final double AIRSHIP_CLIMB = 0.2D;
    /** Each ship motor on a ship, or airship motor on an airship, adds this much of the top speed... */
    public static final double MOTOR_BOOST = 0.25D;
    /** ...for up to this many, which makes it twice as fast. */
    public static final int MAX_MOTORS = 4;
    /** And each one burns this much more diesel. */
    private static final double MOTOR_FUEL = 0.25D;
    /** About 43 km/h flat out. */
    private static final double CAR_SPEED = 0.6D;
    private static final double CAR_ACCELERATION = 0.02D;
    private static final double CAR_BRAKING = 0.04D;
    private static final float CAR_TURN_RATE = 3.5F;
    private static final double GRAVITY = 0.08D;
    private static final double TERMINAL_FALL = 1.0D;
    /** A car moving this slowly burns no fuel. */
    private static final double IDLING = 0.01D;
    /** The top of a car seat's cushion, where the driver's hips go. */
    private static final double SEAT_HEIGHT = 7.0D / 16.0D;
    private static final double ACCELERATION = 0.015D;
    private static final double STOPPED = 0.001D;
    private static final float TURN_RATE = 1.5F;
    private static final float TURN_ACCELERATION = 0.15F;
    private static final float STOPPED_TURN = 0.01F;

    /** How far a route beacon reaches for a vessel. */
    private static final int ROUTE_RANGE = 128;
    private static final double ARRIVAL_DISTANCE = 1.0D;
    private static final double ARRIVAL_HEIGHT = 0.05D;
    /** A ship stopped by land this close to the beacon counts as docked, for a beacon on the quay. */
    private static final double DOCK_REACH = 4.0D;
    private static final double SLOWDOWN_DISTANCE = 10.0D;
    private static final double SLOWEST_THROTTLE = 0.15D;
    private static final float STEERING_SOFTNESS = 10.0F;
    private static final float ON_COURSE = 20.0F;
    private static final double OFF_COURSE_THROTTLE = 0.2D;
    private static final int ROUTE_GIVE_UP_TICKS = 100;
    /** Whoever is this close to a vessel when it docks gets the credit: a route has no one player behind it. */
    private static final int ROUTE_WITNESS_RANGE = 64;

    /** Keeps the corners of a block inside the cells it really overlaps. */
    private static final double SIDE_INSET = 0.49D;
    private static final double HEIGHT_INSET = 0.01D;
    /** Steps of a fifth of a block add up to a hair under a whole block. */
    private static final double WHOLE_BLOCK = 1.0E-6D;
    /** Keeps something merely touching a wall from counting as pinned against it. */
    private static final double TOUCHING = 1.0E-4D;
    /** How far above a block something still counts as aboard, so a jump does not leave the deck behind. */
    private static final int ABOARD_HEIGHT = 3;

    private static final EntityDataAccessor<StructureBlocks> DATA_BLOCKS =
            SynchedEntityData.defineId(MovingStructureEntity.class, StructureBlocks.SERIALIZER);
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BUILT_YAW =
            SynchedEntityData.defineId(MovingStructureEntity.class, EntityDataSerializers.INT);

    /** What the helm asks for this tick, each from -1 to 1 of the vessel's top rate. */
    private record Helm(double throttle, float steering, double lift) {
        private static final Helm IDLE = new Helm(0.0D, 0.0F, 0.0D);

        private static Helm from(Input input) {
            return new Helm((input.forward() ? 1.0D : 0.0D) - (input.backward() ? 0.5D : 0.0D),
                    (input.right() ? 1.0F : 0.0F) - (input.left() ? 1.0F : 0.0F),
                    (input.jump() ? 1.0D : 0.0D) - (input.sprint() ? 1.0D : 0.0D));
        }

        private boolean isIdle() {
            return throttle == 0.0D && steering == 0.0F && lift == 0.0D;
        }
    }

    private double speed;
    private double climb;
    private double fall;
    /** Diesel owed in fractions of a tick, from motors that each burn a quarter more. */
    private double fuelOwed;
    private @Nullable StructureBlocks motorsCountedIn;
    private int motorCount;
    private float turn;
    private double targetY = Double.NaN;
    /** Height of this elevator's call blocks above its core, taken from the floor it was built on. */
    private int floorOffset;
    private int[] floors = new int[0];
    private int nextFloorScan;

    private @Nullable BlockPos route;
    private double routeSpeed;
    private int routeBlockedTicks;
    private boolean blocked;
    private @Nullable BlockPos dockedAt;

    private Vec3 settledPosition;
    private float settledYaw;
    private Input testInput;
    /** Block entities being used while aboard. They have no level, so nothing they do reaches the world. */
    private final Map<BlockPos, BlockEntity> liveBlockEntities = new HashMap<>();

    public MovingStructureEntity(EntityType<? extends MovingStructureEntity> type, Level level) {
        super(type, level);
    }

    /** Sets what this entity carries. The core stays at (0, 0, 0) and the structure's front lies along {@code facing}. */
    void setup(StructureKind kind, StructureBlocks blocks, BlockPos core, Direction facing, int floorOffset) {
        entityData.set(DATA_KIND, kind.ordinal());
        entityData.set(DATA_BLOCKS, blocks);
        int yaw = Mth.floor(facing.toYRot());
        entityData.set(DATA_BUILT_YAW, yaw);
        this.floorOffset = floorOffset;
        setPos(Vec3.atBottomCenterOf(core));
        setYRot(yaw);
        yRotO = yaw;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCKS, StructureBlocks.EMPTY);
        builder.define(DATA_KIND, 0);
        builder.define(DATA_BUILT_YAW, 0);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    public StructureBlocks blocks() {
        return entityData.get(DATA_BLOCKS);
    }

    public StructureKind kind() {
        return StructureKind.values()[entityData.get(DATA_KIND)];
    }

    /** How far this structure has turned from the way it was built, in degrees. */
    public float turned(float yaw) {
        return yaw - entityData.get(DATA_BUILT_YAW);
    }

    public StructurePose pose() {
        return StructurePose.of(position(), turned(getYRot()));
    }

    /** Where this structure is drawn in a frame. */
    public StructurePose pose(float partialTicks) {
        return StructurePose.of(getPosition(partialTicks), turned(Mth.rotLerp(partialTicks, yRotO, getYRot())));
    }

    private StructurePose settledPose() {
        return settledPosition != null ? StructurePose.of(settledPosition, turned(settledYaw)) : pose();
    }

    public boolean isMoving() {
        return route != null || !Double.isNaN(targetY) || Math.abs(speed) > STOPPED
                || Math.abs(climb) > STOPPED || Math.abs(turn) > STOPPED_TURN || fall > 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (blocks().isEmpty()) {
            discard();
            return;
        }
        switch (kind()) {
            case ELEVATOR -> tickElevator(level);
            case SHIP -> tickVessel(level, SHIP_SPEED * motorBoost(), 0.0D);
            case AIRSHIP -> tickVessel(level, AIRSHIP_SPEED * motorBoost(), AIRSHIP_CLIMB * motorBoost());
            case CAR -> tickCar(level);
        }
    }

    /**
     * Drives like a car: it falls when nothing holds it up, only accelerates with its wheels on the
     * ground and fuel in the tank, steers only while rolling, and climbs a step one block high.
     */
    private void tickCar(ServerLevel level) {
        Vec3 position = position();
        boolean grounded = !fits(position.add(0.0D, -0.05D, 0.0D), getYRot());
        if (!grounded) {
            fall = Math.min(fall + GRAVITY, TERMINAL_FALL);
            Vec3 lower = position.add(0.0D, -fall, 0.0D);
            if (fits(lower, getYRot()) && !wouldCrush(lower, getYRot())) {
                position = lower;
            } else {
                // Land on the whole block below rather than hovering a fraction above it.
                Vec3 landed = new Vec3(position.x, Math.floor(position.y + WHOLE_BLOCK), position.z);
                if (fits(landed, getYRot())) position = landed;
                fall = 0.0D;
                grounded = true;
            }
        } else {
            fall = 0.0D;
        }

        Helm helm = Helm.from(driverInput());
        boolean fuelled = fuel() > 0;
        if (!fuelled && !helm.isIdle()) warnNoFuel();
        double wanted = grounded && fuelled ? helm.throttle() * CAR_SPEED : 0.0D;
        speed = approach(speed, wanted, wanted == 0.0D ? CAR_BRAKING : CAR_ACCELERATION);
        // Steering turns the wheels, which only turns the car while it rolls, and the other way in reverse.
        float rolling = (float) Math.clamp(Math.abs(speed) / (CAR_SPEED * 0.3D), 0.0D, 1.0D);
        turn = helm.steering() * CAR_TURN_RATE * rolling * (speed < 0.0D ? -1.0F : 1.0F);
        blocked = false;

        float yaw = getYRot() + turn;
        float radians = yaw * Mth.DEG_TO_RAD;
        Vec3 ahead = position.add(-Mth.sin(radians) * speed, 0.0D, Mth.cos(radians) * speed);
        if (Math.abs(speed) <= STOPPED) {
            place(position, getYRot());
        } else if (canStand(ahead, yaw)) {
            place(ahead, yaw);
        } else if (grounded && canStand(ahead.add(0.0D, 1.0D, 0.0D), yaw)) {
            place(ahead.add(0.0D, 1.0D, 0.0D), yaw);
        } else {
            blocked = true;
            speed = 0.0D;
            place(position, canStand(position, yaw) ? yaw : getYRot());
        }
        if (Math.abs(speed) > IDLING) burnFuel(1);
        reportFuel(fuelled);
    }

    /** Every 20 ticks, the driver sees the diesel left and the speed; with an empty tank, that it is empty. */
    private void reportFuel(boolean fuelled) {
        if (tickCount % 20 != 0 || !fuelled || !(getFirstPassenger() instanceof ServerPlayer driver)) return;
        driver.sendOverlayMessage(Component.translatable("message.redstone_transportation.car_fuel",
                (fuel() + com.example.redstonetransportation.block.FuelTankBlockEntity.TICKS_PER_DIESEL - 1)
                        / com.example.redstonetransportation.block.FuelTankBlockEntity.TICKS_PER_DIESEL,
                String.format("%.0f", Math.abs(speed) * 20.0D * 3.6D)));
    }

    private void warnNoFuel() {
        if (tickCount % 20 == 0 && getFirstPassenger() instanceof ServerPlayer driver) {
            driver.sendOverlayMessage(Component.translatable("message.redstone_transportation.no_fuel"));
        }
    }

    /** Motors of the right kind aboard that count: ship motors on a ship, airship motors on an airship. */
    public int motors() {
        StructureBlocks blocks = blocks();
        if (blocks != motorsCountedIn) {
            net.minecraft.world.level.block.Block motor = kind() == StructureKind.SHIP ? com.example.redstonetransportation.content.ModContent.SHIP_MOTOR
                    : kind() == StructureKind.AIRSHIP ? com.example.redstonetransportation.content.ModContent.AIRSHIP_MOTOR : null;
            int count = 0;
            if (motor != null) {
                for (StructureBlocks.Cell cell : blocks.cells()) {
                    if (cell.state().is(motor)) count++;
                }
            }
            motorCount = Math.min(count, MAX_MOTORS);
            motorsCountedIn = blocks;
        }
        return motorCount;
    }

    /** How many times its plain top speed a ship or airship goes, with its motors. */
    public double motorBoost() {
        return 1.0D + MOTOR_BOOST * motors();
    }

    /** The fuel tanks aboard (a car, ship or airship), loaded for use. */
    private List<com.example.redstonetransportation.block.FuelTankBlockEntity> fuelTanks() {
        List<com.example.redstonetransportation.block.FuelTankBlockEntity> tanks = new ArrayList<>();
        for (StructureBlocks.Cell cell : blocks().cells()) {
            if (!cell.state().is(com.example.redstonetransportation.content.ModContent.FUEL_TANK)) continue;
            if (blockEntityAt(cell.pos()) instanceof com.example.redstonetransportation.block.FuelTankBlockEntity tank) tanks.add(tank);
        }
        return tanks;
    }

    /** Ticks of driving left in all the tanks aboard together. */
    public int fuel() {
        return fuelTanks().stream().mapToInt(com.example.redstonetransportation.block.FuelTankBlockEntity::fuel).sum();
    }

    private void burnFuel(int ticks) {
        int left = ticks;
        for (com.example.redstonetransportation.block.FuelTankBlockEntity tank : fuelTanks()) {
            left -= tank.drain(left);
            if (left <= 0) return;
        }
    }

    /** Diesel poured into a car, ship or airship goes into the first tank with room for it. */
    private boolean refuel(Player player, net.minecraft.world.item.ItemStack diesel) {
        for (com.example.redstonetransportation.block.FuelTankBlockEntity tank : fuelTanks()) {
            if (tank.fuel() + com.example.redstonetransportation.block.FuelTankBlockEntity.TICKS_PER_DIESEL
                    > com.example.redstonetransportation.block.FuelTankBlockEntity.CAPACITY) continue;
            com.example.redstonetransportation.block.FuelTankBlock.fill(tank, diesel, player);
            return true;
        }
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.fuel_tank_full",
                com.example.redstonetransportation.block.FuelTankBlockEntity.CAPACITY_DIESEL));
        return false;
    }

    /** The driver of a car sits on its first seat rather than on the steering wheel. */
    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        if (kind() != StructureKind.CAR) return super.getPassengerRidingPosition(passenger);
        for (StructureBlocks.Cell cell : blocks().cells()) {
            if (!cell.state().is(com.example.redstonetransportation.content.ModContent.CAR_SEAT)) continue;
            StructurePose pose = pose();
            return new Vec3(pose.worldX(cell.pos().getX(), cell.pos().getZ()), getY() + cell.pos().getY() + SEAT_HEIGHT,
                    pose.worldZ(cell.pos().getX(), cell.pos().getZ()));
        }
        return super.getPassengerRidingPosition(passenger);
    }

    private Input driverInput() {
        if (testInput != null) return testInput;
        return getFirstPassenger() instanceof ServerPlayer driver ? driver.getLastClientInput() : Input.EMPTY;
    }

    /** Stands in for a second player at the helm, which the client tests do not have. */
    public void steerForTest(Input input) {
        testInput = input;
    }

    /**
     * Ships and airships run on diesel from the fuel tanks aboard. With none left the engines stop: a ship
     * slows to a halt, an airship hangs where its balloons hold it, and a route it was on is dropped.
     */
    private void tickVessel(ServerLevel level, double topSpeed, double topClimb) {
        Helm helm = helm(level);
        boolean fuelled = fuel() > 0;
        if (!fuelled) {
            if (!helm.isIdle()) warnNoFuel();
            route = null;
            helm = Helm.IDLE;
        }
        speed = approach(speed, helm.throttle() * topSpeed, ACCELERATION);
        climb = approach(climb, helm.lift() * topClimb, ACCELERATION);
        turn = (float) approach(turn, helm.steering() * TURN_RATE, TURN_ACCELERATION);
        blocked = false;
        reportFuel(fuelled);
        if (!isMoving()) return;
        if (Math.abs(speed) > IDLING || Math.abs(climb) > IDLING || Math.abs(turn) > STOPPED_TURN) {
            fuelOwed += 1.0D + MOTOR_FUEL * motors();
            int whole = (int) fuelOwed;
            fuelOwed -= whole;
            burnFuel(whole);
        }

        Vec3 position = position();
        if (climb != 0.0D) {
            Vec3 risen = position.add(0.0D, climb, 0.0D);
            if (canStand(risen, getYRot())) {
                position = risen;
            } else {
                climb = 0.0D;
            }
        }

        float yaw = getYRot() + turn;
        float radians = yaw * Mth.DEG_TO_RAD;
        Vec3 ahead = position.add(-Mth.sin(radians) * speed, 0.0D, Mth.cos(radians) * speed);
        if (canStand(ahead, yaw)) {
            place(ahead, yaw);
        } else if (canStand(ahead, getYRot())) {
            turn = 0.0F;
            place(ahead, getYRot());
        } else if (canStand(position, yaw)) {
            blocked = speed != 0.0D;
            speed = 0.0D;
            place(position, yaw);
        } else {
            blocked = true;
            speed = 0.0D;
            turn = 0.0F;
            place(position, getYRot());
        }
    }

    /** A hand on the helm always wins over a route. */
    private Helm helm(ServerLevel level) {
        Helm manual = Helm.from(driverInput());
        if (!manual.isIdle()) {
            route = null;
            return manual;
        }
        return route != null ? routeHelm(level) : Helm.IDLE;
    }

    /** Steers for the beacon. An airship flies over what is in the way and comes down onto it. */
    private Helm routeHelm(ServerLevel level) {
        double toX = route.getX() + 0.5D - getX();
        double toZ = route.getZ() + 0.5D - getZ();
        double distance = Math.sqrt(toX * toX + toZ * toZ);
        boolean flying = kind() == StructureKind.AIRSHIP;
        double height = flying ? route.getY() + 1.0D - blocks().minY() - getY() : 0.0D;
        boolean overBeacon = distance < ARRIVAL_DISTANCE;

        if (overBeacon && Math.abs(height) < ARRIVAL_HEIGHT || blocked && !flying && distance <= blocks().reach() + DOCK_REACH) {
            arrive(level);
            return Helm.IDLE;
        }
        routeBlockedTicks = blocked ? routeBlockedTicks + 1 : 0;
        if (routeBlockedTicks > ROUTE_GIVE_UP_TICKS) {
            route = null;
            return Helm.IDLE;
        }

        if (overBeacon) return new Helm(0.0D, 0.0F, Math.clamp(height / 2.0D, -1.0D, 1.0D));
        float wanted = (float) (Mth.atan2(toZ, toX) * Mth.RAD_TO_DEG) - 90.0F;
        float error = Mth.wrapDegrees(wanted - getYRot());
        double slowing = Math.clamp(distance / SLOWDOWN_DISTANCE, SLOWEST_THROTTLE, 1.0D);
        double throttle = routeSpeed * slowing * (Math.abs(error) < ON_COURSE ? 1.0D : OFF_COURSE_THROTTLE);
        return new Helm(throttle, Mth.clamp(error / STEERING_SOFTNESS, -1.0F, 1.0F), flying && blocked ? 1.0D : 0.0D);
    }

    private void arrive(ServerLevel level) {
        dockedAt = route;
        route = null;
        speed = 0.0D;
        climb = 0.0D;
        turn = 0.0F;
        RouteBeaconBlock.dock(level, dockedAt);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) <= ROUTE_WITNESS_RANGE * ROUTE_WITNESS_RANGE) ModAdvancements.award(player, ModAdvancements.ROUTE);
        }
    }

    /** Sends the nearest ship or airship to this beacon. Returns false when there is none in reach. */
    public static boolean sendTo(ServerLevel level, BlockPos beacon, double speedFraction) {
        AABB area = new AABB(beacon).inflate(ROUTE_RANGE);
        return level.getEntitiesOfClass(MovingStructureEntity.class, area,
                        structure -> (structure.kind() == StructureKind.SHIP || structure.kind() == StructureKind.AIRSHIP)
                                && structure.fuel() > 0).stream()
                .min(Comparator.comparingDouble(structure -> structure.position().distanceToSqr(Vec3.atCenterOf(beacon))))
                .map(structure -> {
                    structure.route = beacon.immutable();
                    structure.routeSpeed = speedFraction;
                    structure.routeBlockedTicks = 0;
                    structure.dockedAt = null;
                    return true;
                })
                .orElse(false);
    }

    /** Stops whatever is on its way to this beacon. */
    public static void cancelRouteTo(ServerLevel level, BlockPos beacon) {
        for (MovingStructureEntity structure : MovingStructures.in(level)) {
            if (beacon.equals(structure.route)) structure.route = null;
        }
    }

    public static boolean isDockedAt(ServerLevel level, BlockPos beacon) {
        for (MovingStructureEntity structure : MovingStructures.in(level)) {
            if (beacon.equals(structure.dockedAt)) {
                double reach = (structure.blocks().reach() + DOCK_REACH) * Mth.SQRT_OF_TWO;
                if (structure.position().horizontal().distanceToSqr(Vec3.atCenterOf(beacon).horizontal()) <= reach * reach) return true;
            }
        }
        return false;
    }

    private void tickElevator(ServerLevel level) {
        Input input = driverInput();
        boolean up = input.jump() || input.forward();
        boolean down = input.sprint() || input.backward();
        if (Double.isNaN(targetY) && up != down) targetY = nextFloor(level, up);
        if (Double.isNaN(targetY)) return;

        double distance = targetY - getY();
        double step = Math.clamp(distance, -ELEVATOR_SPEED, ELEVATOR_SPEED);
        Vec3 next = position().add(0.0D, step, 0.0D);
        if (!fits(next, getYRot()) || wouldCrush(next, getYRot())) {
            // Back onto a whole block, so the structure can always be set down again.
            setPos(getX(), step > 0.0D ? Math.floor(getY() + WHOLE_BLOCK) : Math.ceil(getY() - WHOLE_BLOCK), getZ());
            targetY = Double.NaN;
            return;
        }
        setPos(next);
        if (Math.abs(distance) <= ELEVATOR_SPEED) targetY = Double.NaN;
    }

    /** The next floor that way, or the next block when no call block stands there. */
    private double nextFloor(ServerLevel level, boolean up) {
        if (tickCount >= nextFloorScan) {
            floors = scanFloors(level, blockPosition());
            nextFloorScan = tickCount + FLOOR_SCAN_INTERVAL;
        }
        int here = Mth.floor(getY() + 0.5D);
        int nearest = up ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        for (int floor : floors) {
            int y = floor - floorOffset;
            if (up ? y > here && y < nearest : y < here && y > nearest) nearest = y;
        }
        boolean found = nearest != Integer.MAX_VALUE && nearest != Integer.MIN_VALUE;
        return found ? nearest : here + (up ? 1 : -1);
    }

    private static int[] scanFloors(ServerLevel level, BlockPos center) {
        BlockPos from = center.offset(-FLOOR_RADIUS, -FLOOR_HEIGHT, -FLOOR_RADIUS);
        BlockPos to = center.offset(FLOOR_RADIUS, FLOOR_HEIGHT, FLOOR_RADIUS);
        Set<Integer> heights = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (level.getBlockState(pos).getBlock() instanceof ElevatorCallBlock) heights.add(pos.getY());
        }
        return heights.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Sends the nearest elevator to the floor of this call block. Returns false when there is none in reach. */
    public static boolean callTo(ServerLevel level, BlockPos call) {
        AABB area = new AABB(call).inflate(FLOOR_RADIUS, FLOOR_HEIGHT, FLOOR_RADIUS);
        return level.getEntitiesOfClass(MovingStructureEntity.class, area, structure -> structure.kind() == StructureKind.ELEVATOR).stream()
                .min(Comparator.comparingDouble(structure -> structure.position().horizontal().distanceToSqr(Vec3.atCenterOf(call).horizontal())))
                .map(structure -> {
                    structure.targetY = call.getY() - structure.floorOffset;
                    return true;
                })
                .orElse(false);
    }

    private static double approach(double value, double target, double step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    private void place(Vec3 position, float yaw) {
        setPos(position);
        setYRot(yaw);
    }

    /** A ship may start out half on land, but it never moves further onto it. */
    private boolean canStand(Vec3 position, float yaw) {
        if (!fits(position, yaw) || wouldCrush(position, yaw)) return false;
        if (kind() != StructureKind.SHIP) return true;
        int wet = wetCells(position, yaw);
        return wet == blocks().bottom().size() || wet >= wetCells(position(), getYRot());
    }

    /** True when nothing solid stands in the way of the structure at this position. */
    boolean fits(Vec3 position, float yaw) {
        StructurePose pose = StructurePose.of(position, turned(yaw));
        double half = SIDE_INSET * (Math.abs(pose.cos()) + Math.abs(pose.sin()));
        Level level = level();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (BlockPos cell : blocks().shell()) {
            double x = pose.worldX(cell.getX(), cell.getZ());
            double z = pose.worldZ(cell.getX(), cell.getZ());
            double y = position.y + cell.getY();
            int top = Mth.floor(y + 1.0D - HEIGHT_INSET);
            int right = Mth.floor(x + half);
            int far = Mth.floor(z + half);
            for (int blockY = Mth.floor(y + HEIGHT_INSET); blockY <= top; blockY++) {
                for (int blockX = Mth.floor(x - half); blockX <= right; blockX++) {
                    for (int blockZ = Mth.floor(z - half); blockZ <= far; blockZ++) {
                        probe.set(blockX, blockY, blockZ);
                        if (!level.isLoaded(probe) || !level.isInWorldBounds(probe)) return false;
                        BlockState state = level.getBlockState(probe);
                        if (!state.isAir() && !state.getCollisionShape(level, probe).isEmpty()) return false;
                    }
                }
            }
        }
        return true;
    }

    /** Counts the blocks of the bottom layer that are in or right above water. */
    private int wetCells(Vec3 position, float yaw) {
        StructurePose pose = StructurePose.of(position, turned(yaw));
        Level level = level();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        int wet = 0;
        for (BlockPos cell : blocks().bottom()) {
            probe.set(Mth.floor(pose.worldX(cell.getX(), cell.getZ())),
                    Mth.floor(position.y + cell.getY() + HEIGHT_INSET),
                    Mth.floor(pose.worldZ(cell.getX(), cell.getZ())));
            if (level.getFluidState(probe).is(FluidTags.WATER) || level.getFluidState(probe.move(Direction.DOWN)).is(FluidTags.WATER)) wet++;
        }
        return wet;
    }

    /**
     * True when moving here would pin something living against the world, which stops the structure
     * instead. That goes for what rides on it too: a low bridge stops the ship, not the people on deck.
     */
    private boolean wouldCrush(Vec3 position, float yaw) {
        StructurePose now = settledPose();
        StructurePose next = StructurePose.of(position, turned(yaw));
        StructureBlocks blocks = blocks();
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, reachAt(position), this::canBePushed)) {
            if (!isAboard(entity, now) && !StructureBoxes.overlap(blocks, next, entity.getBoundingBox())) continue;
            Vec3 moved = carried(entity, now, next).subtract(entity.position());
            if (!level().noBlockCollision(entity, entity.getBoundingBox().deflate(TOUCHING).move(moved))) return true;
        }
        return false;
    }

    private boolean canBePushed(Entity entity) {
        boolean flying = entity instanceof Player player && player.getAbilities().flying;
        return !entity.isPassenger() && !entity.isSpectator() && !flying;
    }

    /** See {@link MovingStructures#finishTick}. */
    void settle() {
        Vec3 now = position();
        float yaw = getYRot();
        if (settledPosition == null) {
            settledPosition = now;
            settledYaw = yaw;
            return;
        }
        if (now.equals(settledPosition) && yaw == settledYaw) return;

        StructurePose from = settledPose();
        StructurePose to = pose();
        AABB around = reachAt(settledPosition);
        if (level() instanceof ServerLevel level) {
            // A player's own client moves it; the server only has to keep it from counting as flying.
            for (ServerPlayer player : level.players()) {
                if (around.intersects(player.getBoundingBox())) player.connection.resetFlyingTicks();
            }
        }

        List<Entity> inTheWay = new ArrayList<>();
        for (Entity entity : level().getEntities(this, around, this::canBeMoved)) {
            if (isAboard(entity, from)) {
                carry(entity, from, to);
            } else if (StructureBoxes.overlap(blocks(), to, entity.getBoundingBox())) {
                inTheWay.add(entity);
            }
        }
        settledPosition = now;
        settledYaw = yaw;
        for (Entity entity : inTheWay) {
            entity.move(MoverType.SHULKER, carried(entity, from, to).subtract(entity.position()));
        }
    }

    private boolean canBeMoved(Entity entity) {
        if (entity instanceof Player player) return player.isLocalPlayer() && canBePushed(player);
        return !level().isClientSide() && canBePushed(entity) && !(entity instanceof MovingStructureEntity);
    }

    private AABB reachAt(Vec3 position) {
        StructureBlocks blocks = blocks();
        double reach = (blocks.reach() + 1.0D) * Mth.SQRT_OF_TWO + 1.0D;
        return new AABB(position.x - reach, position.y + blocks.minY() - 2.0D, position.z - reach,
                position.x + reach, position.y + blocks.maxY() + 1.0D + ABOARD_HEIGHT, position.z + reach);
    }

    private boolean isAboard(Entity entity, StructurePose pose) {
        double localX = pose.localX(entity.getX(), entity.getZ());
        double localZ = pose.localZ(entity.getX(), entity.getZ());
        int top = Mth.ceil(entity.getY() - pose.position().y) - 1;
        double half = entity.getBbWidth() / 2.0D;
        StructureBlocks blocks = blocks();
        for (double side : new double[]{-half, half}) {
            for (double along : new double[]{-half, half}) {
                int x = Mth.floor(localX + side + 0.5D);
                int z = Mth.floor(localZ + along + 0.5D);
                for (int y = top; y >= top - ABOARD_HEIGHT; y--) {
                    if (blocks.isSolidAt(x, y, z)) return true;
                }
            }
        }
        return false;
    }

    /** Where something fixed to the structure at {@code from} ends up at {@code to}. */
    private static Vec3 carried(Entity entity, StructurePose from, StructurePose to) {
        double localX = from.localX(entity.getX(), entity.getZ());
        double localZ = from.localZ(entity.getX(), entity.getZ());
        return new Vec3(to.worldX(localX, localZ), entity.getY() + to.position().y - from.position().y, to.worldZ(localX, localZ));
    }

    private static void carry(Entity entity, StructurePose from, StructurePose to) {
        entity.setPos(carried(entity, from, to));
        float turned = to.turned() - from.turned();
        if (turned == 0.0F) return;
        entity.setYRot(entity.getYRot() + turned);
        entity.setYHeadRot(entity.getYHeadRot() + turned);
        if (entity instanceof LivingEntity living) living.setYBodyRot(living.yBodyRot + turned);
    }

    /** Only what really meets {@code area}: the game takes any shape in this list to mean the area is occupied. */
    void collectCollisions(AABB area, List<VoxelShape> shapes) {
        StructureBoxes.near(blocks(), settledPose(), area, box -> {
            if (box.intersects(area)) shapes.add(Shapes.create(box));
        });
    }

    boolean isInHull(double x, double y, double z) {
        StructurePose pose = pose();
        return blocks().isHull(Mth.floor(pose.localX(x, z) + 0.5D), Mth.floor(y - pose.position().y), Mth.floor(pose.localZ(x, z) + 0.5D));
    }

    /** The block entity at this cell, loaded for use. Call {@link #storeBlockEntities} to keep what changed. */
    @Nullable BlockEntity blockEntityAt(BlockPos cell) {
        BlockEntity live = liveBlockEntities.get(cell);
        if (live != null) return live;
        StructureBlocks.Cell stored = blocks().cell(cell);
        if (stored == null || !(stored.state().getBlock() instanceof EntityBlock block)) return null;
        BlockEntity loaded = stored.blockEntity()
                .map(tag -> BlockEntity.loadStatic(cell, stored.state(), tag, registryAccess()))
                .orElseGet(() -> block.newBlockEntity(cell, stored.state()));
        if (loaded != null) liveBlockEntities.put(cell, loaded);
        return loaded;
    }

    void storeBlockEntities() {
        if (liveBlockEntities.isEmpty()) return;
        StructureBlocks blocks = blocks();
        for (Map.Entry<BlockPos, BlockEntity> entry : liveBlockEntities.entrySet()) {
            StructureBlocks.Cell cell = blocks.cell(entry.getKey());
            if (cell == null) continue;
            blocks = blocks.with(cell.pos(), new StructureBlocks.Cell(cell.pos(), cell.state(),
                    Optional.of(entry.getValue().saveWithFullMetadata(registryAccess())),
                    Optional.of(entry.getValue().getUpdateTag(registryAccess()))));
        }
        entityData.set(DATA_BLOCKS, blocks);
    }

    /** Puts a block on the structure, or takes one off when {@code cell} is null. */
    void setCell(BlockPos pos, @Nullable StructureBlocks.Cell cell) {
        storeBlockEntities();
        liveBlockEntities.remove(pos);
        entityData.set(DATA_BLOCKS, blocks().with(pos, cell));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (kind() != StructureKind.ELEVATOR && player.getItemInHand(hand).is(com.example.analogalu.ModItems.DIESEL)) {
            refuel(player, player.getItemInHand(hand));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            Component result = isMoving()
                    ? Component.translatable("message.redstone_transportation.structure_moving")
                    : StructureAssembler.restore(level, this);
            player.sendOverlayMessage(result);
        } else if (getPassengers().isEmpty()) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        float turned = getYRot() - yRotO;
        if (turned != 0.0F && passenger.isLocalInstanceAuthoritative()) {
            passenger.setYRot(passenger.getYRot() + turned);
            passenger.setYHeadRot(passenger.getYHeadRot() + turned);
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 40000.0D;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        storeBlockEntities();
        output.store("blocks", StructureBlocks.CODEC, blocks());
        output.putInt("kind", kind().ordinal());
        output.putInt("built_yaw", entityData.get(DATA_BUILT_YAW));
        output.putInt("floor_offset", floorOffset);
        if (!Double.isNaN(targetY)) output.putDouble("target_y", targetY);
        output.storeNullable("route", BlockPos.CODEC, route);
        output.putDouble("route_speed", routeSpeed);
        output.storeNullable("docked_at", BlockPos.CODEC, dockedAt);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(DATA_BLOCKS, input.read("blocks", StructureBlocks.CODEC).orElse(StructureBlocks.EMPTY));
        entityData.set(DATA_KIND, input.getIntOr("kind", 0));
        entityData.set(DATA_BUILT_YAW, input.getIntOr("built_yaw", 0));
        floorOffset = input.getIntOr("floor_offset", 0);
        targetY = input.getDoubleOr("target_y", Double.NaN);
        route = input.read("route", BlockPos.CODEC).orElse(null);
        routeSpeed = input.getDoubleOr("route_speed", 1.0D);
        dockedAt = input.read("docked_at", BlockPos.CODEC).orElse(null);
    }
}
