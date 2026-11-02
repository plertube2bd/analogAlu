package com.example.analogalu;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class CircuitStatusTooltips {
    private static final Map<String, String> LAST_STATUS = new ConcurrentHashMap<>();

    private CircuitStatusTooltips() {}

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, tooltip) -> append(stack, tooltip));
    }

    public static void update(String itemId, String text) {
        LAST_STATUS.put(itemId, text);
    }

    private static void append(ItemStack stack, java.util.List<Component> tooltip) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String status = LAST_STATUS.get(itemId);
        if (status == null || status.isBlank()) return;

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.analog_alu.last_reading").withStyle(ChatFormatting.GOLD));
        for (String line : status.split("\\n")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }
}