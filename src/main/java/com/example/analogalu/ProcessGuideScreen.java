package com.example.analogalu;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 공정 안내 화면 (실리콘 원형/질량 분석기 우클릭). 화면 높이에 맞춰 패널 높이와 줄 간격을 줄여 잘리지 않게 한다. */
@Environment(EnvType.CLIENT)
public final class ProcessGuideScreen extends Screen {
    private static final int MARGIN = 6;
    private static final int LINES = 6;
    private static final int LINES_TOP = 54;
    private static final int FOOTER = 46;

    private final String prefix;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int lineSpacing;

    public ProcessGuideScreen(String prefix) {
        super(Component.translatable(prefix + ".title"));
        this.prefix = prefix;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(430, width - 24);
        panelLeft = (width - panelWidth) / 2;
        panelHeight = Math.max(120, Math.min(300, height - MARGIN * 2));
        panelTop = Math.max(MARGIN, (height - panelHeight) / 2);
        lineSpacing = Math.max(11, Math.min(30, (panelHeight - LINES_TOP - FOOTER) / LINES));
        addRenderableWidget(Button.builder(Component.translatable("screen.analog_alu.guide.close"),
                button -> minecraft.setScreenAndShow(null))
            .pos(panelLeft + panelWidth - 92, panelTop + panelHeight - 36).size(80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xF0181D22);
        graphics.outline(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF687985);
        graphics.text(font, Component.translatable(prefix + ".title"), panelLeft + 16, panelTop + 16, 0xFFE7EEF2);
        graphics.fill(panelLeft + 16, panelTop + 40, panelLeft + panelWidth - 16, panelTop + 41, 0xFF52636B);
        List<Integer> colors = List.of(0xFFB4BEC4, 0xFFB4BEC4, 0xFFF0C878, 0xFF79D4C1, 0xFFB4BEC4, 0xFFB4BEC4);
        for (int index = 0; index < LINES; index++) {
            graphics.text(font, Component.translatable(prefix + ".line" + (index + 1)), panelLeft + 16,
                panelTop + LINES_TOP + index * lineSpacing, colors.get(index));
        }
    }
}
