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

/**
 * 실리콘 웨이퍼 코드 편집기 (우클릭으로 열림).
 *
 * 화면 높이에 맞춰 레이아웃이 달라진다 (GUI 배율이 커서 화면이 작아져도 잘리지 않게):
 *   tier 0: 포트 도식 + 설명 4줄 + 편집기 (넉넉한 화면)
 *   tier 1: 도식을 숨기고 설명 4줄 + 편집기
 *   tier 2: 제목/오류 줄 + 편집기만
 * 저장하지 않고 닫으면 내용이 {@link WaferDraftStore}에 임시 저장되어 다음에 다시 열 때 이어서 쓸 수 있다.
 */
@Environment(EnvType.CLIENT)
public class WaferEditorScreen extends Screen {
    private static final int MARGIN = 6;
    private static final int FOOTER = 40;
    private static final int MAX_PANEL_HEIGHT = 402;
    private static final int INFO_FULL = 174;
    private static final int INFO_COMPACT = 92;
    private static final int INFO_MINIMAL = 38;

    private final InteractionHand hand;
    /** 웨이퍼에 실제로 저장되어 있는 코드. 임시 저장(초안)의 기준이 된다. */
    private final String savedCode;
    /** 편집기의 현재 내용. 창 크기가 바뀌어 init()이 다시 불려도 유지된다. */
    private String currentText;
    /** 열 때 임시 저장본을 불러왔고 아직 수정하지 않았으면 true */
    private boolean restoredNotice;
    private boolean saved;
    private MultiLineEditBox codeEditor;
    private String validationError = "";
    private int tier;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int editorHeight;

    protected WaferEditorScreen(InteractionHand hand, String code) {
        super(Component.translatable("screen.analog_alu.wafer.title"));
        this.hand = hand;
        this.savedCode = code;
        var draft = WaferDraftStore.get(code);
        this.currentText = draft.orElse(code);
        this.restoredNotice = draft.isPresent();
    }

    private int infoHeight() {
        return tier == 0 ? INFO_FULL : tier == 1 ? INFO_COMPACT : INFO_MINIMAL;
    }

    private void layout() {
        int available = Math.max(60, height - MARGIN * 2);
        if (available >= INFO_FULL + FOOTER + 80) {
            tier = 0;
        } else if (available >= INFO_COMPACT + FOOTER + 60) {
            tier = 1;
        } else {
            tier = 2;
        }
        panelHeight = Math.min(MAX_PANEL_HEIGHT, available);
        editorHeight = Math.max(24, panelHeight - infoHeight() - FOOTER);
        panelWidth = Math.min(520, width - 24);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(MARGIN, (height - panelHeight) / 2);
    }

    @Override
    protected void init() {
        layout();

        codeEditor = addRenderableWidget(MultiLineEditBox.builder()
                .setX(panelLeft + 12)
                .setY(panelTop + infoHeight())
                .setPlaceholder(Component.translatable("screen.analog_alu.wafer.placeholder"))
                .setShowBackground(true)
                .setShowDecorations(true)
                .build(font, panelWidth - 24, editorHeight, Component.translatable("screen.analog_alu.wafer.program")));
        codeEditor.setCharacterLimit(4096);
        codeEditor.setLineLimit(80);
        codeEditor.setValue(currentText);
        // 값을 채운 뒤에 리스너를 달아야 열기만 한 것이 수정으로 기록되지 않는다.
        codeEditor.setValueListener(text -> {
            currentText = text;
            restoredNotice = false;
            WaferDraftStore.put(savedCode, text);
        });
        codeEditor.setFocused(true);

        int buttonY = panelTop + panelHeight - 30;
        int right = panelLeft + panelWidth - 12;
        addRenderableWidget(Button.builder(Component.translatable("screen.analog_alu.wafer.close"), button -> close())
                .pos(right - 76, buttonY).size(76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.analog_alu.wafer.revert"), button -> revert())
                .pos(right - 76 * 2 - 6, buttonY).size(76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.analog_alu.wafer.save"), button -> save())
                .pos(right - 76 * 3 - 12, buttonY).size(76, 20).build());
        setInitialFocus(codeEditor);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xF0181D22);
        graphics.outline(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF687985);
        graphics.text(font, Component.translatable("screen.analog_alu.wafer.header"),
            panelLeft + 12, panelTop + (tier == 0 ? 12 : 10), 0xFFE7EEF2);
        if (tier == 0) drawPortDiagram(graphics);
        if (tier <= 1) drawInstructions(graphics);
        if (!validationError.isEmpty()) {
            int errorY = tier == 0 ? 162 : tier == 1 ? 76 : 24;
            graphics.text(font, validationError, panelLeft + 12, panelTop + errorY, 0xFFFF7777);
        }
        drawDraftStatus(graphics);
    }

    private void drawPortDiagram(GuiGraphicsExtractor graphics) {
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
    }

    private void drawInstructions(GuiGraphicsExtractor graphics) {
        int y = panelTop + (tier == 0 ? 102 : 26);
        int step = tier == 0 ? 15 : 12;
        String[] keys = {"instruction_order", "instruction_width", "example", "instruction_later"};
        int[] colors = {0xFFB4BEC4, 0xFFB4BEC4, 0xFFF0C878, 0xFFB4BEC4};
        for (int i = 0; i < keys.length; i++) {
            graphics.text(font, Component.translatable("screen.analog_alu.wafer." + keys[i]),
                panelLeft + 12, y + i * step, colors[i]);
        }
    }

    /** 푸터 왼쪽에 임시 저장 상태를 표시한다 (저장된 코드와 다를 때만). 좁은 화면에서는 버튼과 겹치므로 생략. */
    private void drawDraftStatus(GuiGraphicsExtractor graphics) {
        if (panelWidth < 380 || currentText.equals(savedCode)) return;
        String key = restoredNotice ? "screen.analog_alu.wafer.draft_restored" : "screen.analog_alu.wafer.draft";
        graphics.text(font, Component.translatable(key), panelLeft + 12, panelTop + panelHeight - 24, 0xFF8FB8A8);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            close();
            return true;
        }
        return super.keyPressed(event);
    }

    /** 화면이 닫히거나 교체될 때 호출된다. 저장하지 않고 닫았다면 현재 내용을 임시 저장한다. */
    @Override
    public void removed() {
        if (!saved) WaferDraftStore.put(savedCode, currentText);
        WaferDraftStore.flush();
        super.removed();
    }

    private void save() {
        String code = codeEditor.getValue();
        var error = WaferProgramValidator.validate(code);
        if (error.isPresent()) {
            validationError = error.get();
            return;
        }
        saved = true;
        WaferDraftStore.clear(savedCode);
        WaferEditorClient.save(hand, code);
        close();
    }

    /** 임시 저장본을 버리고 웨이퍼에 저장된 코드로 되돌린다. */
    private void revert() {
        validationError = "";
        codeEditor.setValue(savedCode);
        currentText = savedCode;
        restoredNotice = false;
        WaferDraftStore.clear(savedCode);
    }

    private void close() {
        minecraft.setScreenAndShow(null);
    }
}
