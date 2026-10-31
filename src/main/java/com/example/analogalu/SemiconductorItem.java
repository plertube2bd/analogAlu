package com.example.analogalu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SemiconductorItem extends Item {
    public SemiconductorItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.analog_alu.semiconductor").withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}