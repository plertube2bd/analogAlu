package com.example.analogalu;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class ProcessGuideScreen extends Screen {
    private final String prefix;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;

    public ProcessGuideScreen(String prefix) {
        super(Component.translatable(prefix + ".title"));
        this.prefix = prefix;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(430, width - 24);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(20, (height - 300) / 2);
        addRenderableWidget(Button.builder(Component.translatable("screen.analog_alu.guide.close"),
                button -> minecraft.setScreenAndShow(null))
            .pos(panelLeft + panelWidth - 92, panelTop + 264).size(80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 300, 0xF0181D22);
        graphics.outline(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 300, 0xFF687985);
        graphics.text(font, Component.translatable(prefix + ".title"), panelLeft + 16, panelTop + 16, 0xFFE7EEF2);
        graphics.fill(panelLeft + 16, panelTop + 40, panelLeft + panelWidth - 16, panelTop + 41, 0xFF52636B);
        List<Integer> colors = List.of(0xFFB4BEC4, 0xFFB4BEC4, 0xFFF0C878, 0xFF79D4C1, 0xFFB4BEC4, 0xFFB4BEC4);
        for (int index = 0; index < 6; index++) {
            graphics.text(font, Component.translatable(prefix + ".line" + (index + 1)), panelLeft + 16,
                panelTop + 54 + index * 30, colors.get(index));
        }
    }
}
