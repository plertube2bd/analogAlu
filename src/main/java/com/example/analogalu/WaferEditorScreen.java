package com.example.analogalu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

@Environment(EnvType.CLIENT)
public class WaferEditorScreen extends Screen {
    private final InteractionHand hand;
    private final String initialCode;
    private MultiLineEditBox codeEditor;
    private String validationError = "";
    private int panelLeft;
    private int panelTop;
    private int panelWidth;

    protected WaferEditorScreen(InteractionHand hand, String code) {
        super(Component.translatable("screen.analog_alu.wafer.title"));
        this.hand = hand;
        this.initialCode = code;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 24);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(12, (height - 420) / 2);

        codeEditor = addRenderableWidget(MultiLineEditBox.builder()
                .setX(panelLeft + 12)
                .setY(panelTop + 174)
                .setPlaceholder(Component.translatable("screen.analog_alu.wafer.placeholder"))
                .setShowBackground(true)
                .setShowDecorations(true)
                .build(font, panelWidth - 24, 188, Component.translatable("screen.analog_alu.wafer.program")));
        codeEditor.setCharacterLimit(4096);
        codeEditor.setLineLimit(80);
        codeEditor.setValue(initialCode);
        codeEditor.setFocused(true);

        int buttonY = panelTop + 374;
        addRenderableWidget(Button.builder(Component.translatable("screen.analog_alu.wafer.save"), button -> save())
                .pos(panelLeft + panelWidth - 172, buttonY).size(76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.analog_alu.wafer.cancel"), button -> close())
                .pos(panelLeft + panelWidth - 88, buttonY).size(76, 20).build());
        setInitialFocus(codeEditor);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 410, 0xF0181D22);
        graphics.outline(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 410, 0xFF687985);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.header"),
            panelLeft + 12, panelTop + 12, 0xFFE7EEF2);
        int center = panelLeft + panelWidth / 2;
        graphics.fill(center - 44, panelTop + 32, center + 44, panelTop + 49, 0xFF183B3B);
        graphics.fill(panelLeft + 12, panelTop + 54, panelLeft + 126, panelTop + 72, 0xFF183B3B);
        graphics.fill(center - 36, panelTop + 53, center + 36, panelTop + 73, 0xFF343E45);
        graphics.fill(panelLeft + panelWidth - 126, panelTop + 54,
            panelLeft + panelWidth - 12, panelTop + 72, 0xFF183B3B);
        graphics.fill(center - 44, panelTop + 77, center + 44, panelTop + 94, 0xFF183B3B);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.port_front"),
            center - 40, panelTop + 37, 0xFF79D4C1);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.port_left"),
            panelLeft + 17, panelTop + 59, 0xFF79D4C1);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.port_center"),
            center - 32, panelTop + 59, 0xFFE7EEF2);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.port_right"),
            panelLeft + panelWidth - 121, panelTop + 59, 0xFF79D4C1);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.port_back"),
            center - 40, panelTop + 82, 0xFF79D4C1);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.instruction_order"),
            panelLeft + 12, panelTop + 102, 0xFFB4BEC4);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.instruction_width"),
            panelLeft + 12, panelTop + 117, 0xFFB4BEC4);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.example"),
            panelLeft + 12, panelTop + 132, 0xFFF0C878);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.instruction_later"),
            panelLeft + 12, panelTop + 147, 0xFFB4BEC4);
        if (!validationError.isEmpty()) {
            graphics.text(font, validationError, panelLeft + 12, panelTop + 162, 0xFFFF7777);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            close();
            return true;
        }
        return super.keyPressed(event);
    }

    private void save() {
        var error = WaferProgramValidator.validate(codeEditor.getValue());
        if (error.isPresent()) {
            validationError = error.get();
            return;
        }
        WaferEditorClient.save(hand, codeEditor.getValue());
        close();
    }

    private void close() {
        minecraft.setScreenAndShow(null);
    }
}