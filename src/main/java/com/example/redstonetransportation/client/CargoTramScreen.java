package com.example.redstonetransportation.client;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.vehicle.CargoTramMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class CargoTramScreen extends AbstractContainerScreen<CargoTramMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "textures/gui/container/cargo_tram.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 114 + CargoTramMenu.ROWS * 18;

    public CargoTramScreen(CargoTramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }
}
