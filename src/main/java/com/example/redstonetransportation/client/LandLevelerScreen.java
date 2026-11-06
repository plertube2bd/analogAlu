package com.example.redstonetransportation.client;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.machine.LandLevelerBlockEntity;
import com.example.redstonetransportation.machine.LandLevelerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Storage on top, then the fuel slot with its gauge, the two buttons and a line saying what it is doing. */
public final class LandLevelerScreen extends AbstractContainerScreen<LandLevelerMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "textures/gui/container/land_leveler.png");
    private static final String KEY = "gui.redstone_transportation.land_leveler.";
    private static final int WIDTH = 176;
    private static final int HEIGHT = 202;
    private static final int GAUGE_LEFT = 28;
    private static final int GAUGE_HEIGHT = 16;
    private static final int STATUS_TOP = 97;
    private static final int TEXT = 0xFF404040;
    private static final int GOOD = 0xFF1E7A34;
    private static final int BAD = 0xFFB0261E;

    private Button runButton;
    private Button rubbleButton;

    public LandLevelerScreen(LandLevelerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        inventoryLabelY = LandLevelerMenu.INVENTORY_TOP - 11;
    }

    @Override
    protected void init() {
        super.init();
        int top = topPos + LandLevelerMenu.CONTROL_TOP - 2;
        runButton = addRenderableWidget(Button.builder(runLabel(), button -> press(LandLevelerBlockEntity.BUTTON_RUN))
                .bounds(leftPos + 74, top, 34, 20).build());
        rubbleButton = addRenderableWidget(Button.builder(rubbleLabel(), button -> press(LandLevelerBlockEntity.BUTTON_DISCARD))
                .bounds(leftPos + 110, top, 58, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + "rubble_tooltip", LandLevelerBlockEntity.RUBBLE_RESERVE)))
                .build());
    }

    private void press(int button) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private Component runLabel() {
        return Component.translatable(KEY + (menu.isRunning() ? "stop" : "start"));
    }

    private Component rubbleLabel() {
        return Component.translatable(KEY + (menu.discardsRubble() ? "rubble_discard" : "rubble_keep"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        runButton.setMessage(runLabel());
        rubbleButton.setMessage(rubbleLabel());
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        int filled = Math.round(menu.fuelLeft() * GAUGE_HEIGHT);
        if (filled > 0) {
            int bottom = topPos + LandLevelerMenu.CONTROL_TOP + GAUGE_HEIGHT;
            graphics.fill(leftPos + GAUGE_LEFT, bottom - filled, leftPos + GAUGE_LEFT + 4, bottom, 0xFFF08A24);
        }
    }

    /** Empty fuel and fill slots say what goes in them. */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot == null || hoveredSlot.hasItem() || !menu.getCarried().isEmpty()) return;
        String hint = switch (hoveredSlot.getContainerSlot()) {
            case LandLevelerBlockEntity.FUEL_SLOT -> "fuel_slot";
            case LandLevelerBlockEntity.FILL_SLOT -> "fill_slot";
            case LandLevelerBlockEntity.LIGHTER_SLOT -> "lighter_slot";
            default -> null;
        };
        if (hint != null && hoveredSlot.container != minecraft.player.getInventory()) {
            graphics.setTooltipForNextFrame(font, Component.translatable(KEY + hint), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        LandLevelerBlockEntity.Problem problem = menu.problem();
        Component status;
        int color;
        if (!menu.isRunning()) {
            status = Component.translatable(KEY + "status.stopped", menu.travelled());
            color = TEXT;
        } else if (problem == LandLevelerBlockEntity.Problem.NONE) {
            status = Component.translatable(KEY + (menu.isBurning() ? "status.burning" : "status.running"), menu.travelled());
            color = GOOD;
        } else {
            status = Component.translatable(KEY + "status." + problem.name().toLowerCase(java.util.Locale.ROOT));
            color = problem == LandLevelerBlockEntity.Problem.WAITING ? TEXT : BAD;
        }
        graphics.text(font, status, LandLevelerMenu.GRID_LEFT, STATUS_TOP, color, false);
        if (menu.hasCargo()) {
            Component cargo = Component.translatable(KEY + "cargo");
            graphics.text(font, cargo, imageWidth - 8 - font.width(cargo), titleLabelY, TEXT, false);
        }
    }
}
