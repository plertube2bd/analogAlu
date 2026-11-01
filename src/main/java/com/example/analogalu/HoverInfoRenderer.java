package com.example.analogalu;

import com.example.analogalu.block.HoverInfo;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 십자선이 가리키는 전선/회로 장치의 정보를, 인벤토리 아이템 툴팁과 같은 모양의 창으로 십자선 옆에 그린다.
 * (예전에는 우클릭할 때마다 채팅창에 출력하던 내용이다.)
 */
@Environment(EnvType.CLIENT)
public final class HoverInfoRenderer {
    private static final int BACKGROUND = 0xF0100010;
    private static final int BORDER = 0xF05000FF;
    private static final int BORDER_INNER = 0xF028007F;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;

    private HoverInfoRenderer() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockEntity blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
        if (!(blockEntity instanceof HoverInfo info)) return;
        List<Component> lines = info.hoverInfo(minecraft.level);
        if (lines.isEmpty()) return;

        Font font = minecraft.font;
        int textWidth = 0;
        for (Component line : lines) textWidth = Math.max(textWidth, font.width(line));
        int boxWidth = textWidth + PADDING * 2;
        int boxHeight = lines.size() * LINE_HEIGHT + PADDING * 2 - 1 + 2; // 제목 줄 아래 여백 2px

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int x = screenWidth / 2 + 14;
        int y = screenHeight / 2 - boxHeight / 2;
        if (x + boxWidth + 4 > screenWidth) x = screenWidth / 2 - 14 - boxWidth; // 오른쪽이 모자라면 왼쪽으로
        x = Math.max(4, x);
        y = Math.clamp(y, 4, Math.max(4, screenHeight - boxHeight - 4));

        drawWindow(graphics, x, y, boxWidth, boxHeight);
        int textY = y + PADDING;
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, lines.get(i), x + PADDING, textY, 0xFFFFFFFF);
            textY += LINE_HEIGHT + (i == 0 ? 2 : 0);
        }
    }

    /** 바닐라 툴팁과 같은 어두운 배경 + 보라색 테두리 */
    private static void drawWindow(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y, x + width + 1, y + height, BACKGROUND);
        graphics.fill(x, y - 1, x + width, y + height + 1, BACKGROUND);
        // 테두리 (바깥 한 줄 + 안쪽 한 줄)
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_INNER);
        graphics.fill(x, y + 1, x + 1, y + height - 1, BORDER);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, BORDER_INNER);
    }
}
