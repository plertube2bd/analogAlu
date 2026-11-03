package com.example.analogalu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.network.chat.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.example.analogalu.block.CircuitDeviceBlock;
import com.example.analogalu.block.CircuitDeviceBlockEntity;
import com.example.analogalu.block.CopperCableBlockEntity;

public class AnalogAlu implements ModInitializer {
    public static final String MOD_ID = "analog_alu";
    private static final Map<UUID, Long> LAST_CIRCUIT_INFO_REQUEST = new HashMap<>();

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        com.example.analogalu.fluid.OilDrowning.init();
        PayloadTypeRegistry.serverboundPlay().register(SaveWaferCodePayload.TYPE, SaveWaferCodePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RunWaferProgramPayload.TYPE, RunWaferProgramPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CircuitInfoRequestPayload.TYPE, CircuitInfoRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CircuitStatusPayload.TYPE, CircuitStatusPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CircuitLookInfoPayload.TYPE, CircuitLookInfoPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SaveWaferCodePayload.TYPE, (payload, context) ->
            context.server().execute(() -> saveWaferCode(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(RunWaferProgramPayload.TYPE, (payload, context) ->
            context.server().execute(() -> runWaferProgram(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(CircuitInfoRequestPayload.TYPE, (payload, context) ->
            context.server().execute(() -> sendCircuitInfo(payload, context.player())));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
            .register(output -> {
                ModBlocks.ITEMS.forEach(output::accept);
                ModItems.ITEMS.forEach(output::accept);
            });
    }

        private static void sendCircuitInfo(CircuitInfoRequestPayload payload, ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = BlockPos.of(payload.blockPos());
        long gameTime = level.getGameTime();
        Long previousRequest = LAST_CIRCUIT_INFO_REQUEST.put(player.getUUID(), gameTime);
        LAST_CIRCUIT_INFO_REQUEST.entrySet().removeIf(entry -> gameTime - entry.getValue() > 1200L);
        if (previousRequest != null && gameTime - previousRequest < 5L) return;
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0
            || !level.hasChunkAt(pos)) return;

        var state = level.getBlockState(pos);
        String message;
        if (state.is(ModBlocks.COPPER_CABLE)
            && level.getBlockEntity(pos) instanceof CopperCableBlockEntity cable) {
            message = String.format(java.util.Locale.ROOT,
                "R %.5f Ω  |  V %.2f V  |  I %.3f A  |  %.1f °C  |  HP %.0f/100%s",
                cable.getResistanceOhms(), cable.getVoltage(), cable.getCurrent(), cable.getTemperatureCelsius(),
                cable.getHealth(), state.getValue(com.example.analogalu.block.CopperCableBlock.LOCK) == 0
                    ? "" : "  |  잠김");
        } else if (state.getBlock() instanceof CircuitDeviceBlock device) {
            message = circuitDeviceInfo(level, pos, state, device);
        } else {
            return;
        }
        CircuitLookInfoPayload.send(player, pos, message);
        }

        private static String circuitDeviceInfo(ServerLevel level, BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, CircuitDeviceBlock device) {
        CircuitDeviceBlockEntity entity = level.getBlockEntity(pos) instanceof CircuitDeviceBlockEntity value
            ? value : null;
        return switch (device.kind()) {
            case BATTERY -> String.format(java.util.Locale.ROOT, "건전지  공칭 %.1f V  |  단자 %.2f V  |  %.0f/%.0f J",
                CircuitDeviceBlock.BATTERY_VOLTAGE,
                entity == null ? 0.0 : entity.getSourceTerminalVoltage(),
                entity == null ? 0.0 : entity.getStoredEnergyJoules(),
                CircuitDeviceBlock.BATTERY_CAPACITY_JOULES);
            case COAL_GENERATOR -> String.format(java.util.Locale.ROOT, "석탄 발전기  공칭 %.1f V  |  단자 %.2f V  |  연료 %.0f초",
                CircuitDeviceBlock.COAL_GENERATOR_VOLTAGE,
                entity == null ? 0.0 : entity.getSourceTerminalVoltage(),
                entity == null ? 0.0 : entity.getFuelTicks() / 20.0);
            case DIESEL_GENERATOR -> String.format(java.util.Locale.ROOT, "디젤 발전기  공칭 %.1f V  |  단자 %.2f V  |  연료 %.0f초",
                CircuitDeviceBlock.DIESEL_GENERATOR_VOLTAGE,
                entity == null ? 0.0 : entity.getSourceTerminalVoltage(),
                entity == null ? 0.0 : entity.getFuelTicks() / 20.0);
            case WIND_GENERATOR -> CircuitDeviceBlock.hasNaturalInput(level, pos, device.kind())
                ? String.format(java.util.Locale.ROOT, "풍력 발전기  발전 중  |  공칭 %.1f V  |  단자 %.2f V",
                    CircuitDeviceBlock.sourceVoltage(level, pos, device.kind()),
                    entity == null ? 0.0 : entity.getSourceTerminalVoltage())
                : "풍력 발전기  대기  |  Y=80 이상, 하늘 필요";
            case SOLAR_GENERATOR -> CircuitDeviceBlock.hasNaturalInput(level, pos, device.kind())
                ? String.format(java.util.Locale.ROOT, "태양광 발전기  발전 중  |  공칭 %.1f V  |  단자 %.2f V",
                    CircuitDeviceBlock.sourceVoltage(level, pos, device.kind()),
                    entity == null ? 0.0 : entity.getSourceTerminalVoltage())
                : "태양광 발전기  대기  |  낮과 열린 하늘 필요";
            case SWITCH -> state.getValue(CircuitDeviceBlock.CLOSED)
                ? "전기 스위치  닫힘" : "전기 스위치  열림";
            case RESISTOR -> String.format(java.util.Locale.ROOT, "저항 부하  %.1f Ω",
                CircuitDeviceBlock.LOAD_RESISTANCE);
                case MINER -> String.format(java.util.Locale.ROOT, "채굴기  %.1f Ω  |  %.1f/%.1f V  |  %s",
                    CircuitDeviceBlock.MINER_RESISTANCE,
                    entity == null ? 0.0 : entity.getLoadVoltage(), CircuitDeviceBlock.MINER_REQUIRED_VOLTAGE,
                    level.hasNeighborSignal(pos) ? "채굴 신호 켜짐" : "레드스톤 신호 대기");
        };
        }

    private static void saveWaferCode(SaveWaferCodePayload payload, net.minecraft.server.level.ServerPlayer player) {
        ItemStack held = player.getItemInHand(payload.hand());
        if (!(held.is(ModItems.SILICON_WAFER) || held.is(ModItems.PROGRAMMED_SILICON_WAFER))
            || payload.code().length() > 4096) return;
        WaferProgramCompiler.Compilation compilation = WaferProgramCompiler.compile(payload.code());
        if (!compilation.successful()) return;

        ItemStack remaining = held.copyWithCount(held.getCount() - 1);
        ItemStack programmed = new ItemStack(ModItems.PROGRAMMED_SILICON_WAFER);
        WaferProgramData.write(programmed, payload.code(), compilation.bytecode(),
            sha256(payload.code()), compilation.statementCount());
        player.setItemInHand(payload.hand(), programmed);
        if (!remaining.isEmpty() && !player.getInventory().add(remaining)) {
            player.drop(remaining, false, Prediction.SERVER_ONLY);
        }
    }

    private static void runWaferProgram(RunWaferProgramPayload payload,
            net.minecraft.server.level.ServerPlayer player) {
        ItemStack held = player.getItemInHand(payload.hand());
        if (!(held.is(ModItems.PROGRAMMED_SILICON_WAFER)
            || held.is(ModItems.SEMICONDUCTOR)
            || held.is(ModBlocks.SEMICONDUCTOR_BLOCK_ITEM)
            || held.is(ModBlocks.WATERPROOF_SEMICONDUCTOR_BLOCK_ITEM))) return;
        var data = WaferProgramData.get(held);
        String bytecode = data.getStringOr(WaferProgramData.BYTECODE_KEY, "");
        if (!WaferProgramCompiler.isBytecode(bytecode) || bytecode.length() > 32768) {
            player.sendSystemMessage(Component.literal("No valid compiled wafer program is stored."));
            return;
        }

        var result = WaferProgramRuntime.execute(bytecode, Map.of(
                "front", 0, "back", 0, "left", 0, "right", 0));
        if (!result.successful()) {
            player.sendSystemMessage(Component.literal("Program error: " + result.error()));
            return;
        }
        for (String line : result.output()) {
            player.sendSystemMessage(Component.literal("show: " + line));
        }
        player.sendSystemMessage(Component.literal("Front output: " + result.ports().get("front")
            + " | " + result.elapsedMillis() + " ms"));
    }

    private static String sha256(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
