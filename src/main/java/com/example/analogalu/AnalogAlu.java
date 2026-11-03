package com.example.analogalu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.network.chat.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public class AnalogAlu implements ModInitializer {
    public static final String MOD_ID = "analog_alu";

    @Override
    public void onInitialize() {
        ModComponents.init();
        ModBlocks.init();
        ModItems.init();
        com.example.analogalu.fluid.OilDrowning.init();
        com.example.analogalu.fluid.OilBucketing.init();
        PayloadTypeRegistry.serverboundPlay().register(SaveWaferCodePayload.TYPE, SaveWaferCodePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RunWaferProgramPayload.TYPE, RunWaferProgramPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CircuitStatusPayload.TYPE, CircuitStatusPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SaveWaferCodePayload.TYPE, (payload, context) ->
            context.server().execute(() -> saveWaferCode(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(RunWaferProgramPayload.TYPE, (payload, context) ->
            context.server().execute(() -> runWaferProgram(payload, context.player())));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(
                net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            ModBlocks.ITEMS.forEach(output::accept);
            ModItems.ITEMS.forEach(output::accept);
        });
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
            player.sendSystemMessage(Component.translatable("message.analog_alu.wafer_no_program"));
            return;
        }

        var result = WaferProgramRuntime.execute(bytecode, Map.of(
                "front", 0, "back", 0, "left", 0, "right", 0));
        if (!result.successful()) {
            player.sendSystemMessage(Component.translatable("message.analog_alu.wafer_error", result.error()));
            return;
        }
        for (String line : result.output()) {
            player.sendSystemMessage(Component.literal("show: " + line));
        }
        player.sendSystemMessage(Component.translatable("message.analog_alu.wafer_result",
            String.valueOf(result.ports().get("front")), String.valueOf(result.elapsedMillis())));
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
