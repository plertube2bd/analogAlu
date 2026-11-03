package com.example.analogalu.block;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 조준 정보 창(HoverInfo)에 쓰는 번역 가능한 줄들. 키는 lang/*.json 의 hover.analog_alu.* 에 있다. */
public final class HoverText {
    private static final String PREFIX = "hover.analog_alu.";

    private HoverText() {}

    /** "라벨: 값" 줄. 라벨은 번역 키, 값은 숫자 등 그대로 보여줄 문자열. */
    public static Component line(String labelKey, String value, ChatFormatting valueColor) {
        return line(labelKey, Component.literal(value), valueColor);
    }

    public static Component line(String labelKey, Component value, ChatFormatting valueColor) {
        return Component.translatable(PREFIX + "label." + labelKey).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value.copy().withStyle(valueColor));
    }

    /** 상태 값(잠김, 열림, 발전 중 등). */
    public static MutableComponent value(String key) {
        return Component.translatable(PREFIX + "value." + key);
    }

    /** 맨 아래 회색 안내 문구. */
    public static Component hint(String key) {
        return Component.translatable(PREFIX + "hint." + key).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component seconds(double seconds) {
        return Component.translatable(PREFIX + "seconds", String.format(Locale.ROOT, "%.0f", seconds));
    }

    public static Component direction(Direction direction) {
        return Component.translatable(PREFIX + "direction." + direction.getSerializedName());
    }
}
