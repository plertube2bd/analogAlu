package com.example.analogalu;

import com.example.analogalu.block.CircuitDeviceBlock;
import com.example.analogalu.block.CircuitDeviceBlockEntity;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class ConverterScreen extends Screen {
   private static final int PANEL_WIDTH = 260;
   private static final int PANEL_HEIGHT = 168;
   private static final int[] STEPS = new int[]{-100, -10, -1, 1, 10, 100};
   private final BlockPos pos;
   private final boolean stepUp;
   private final double min;
   private final double max;
   private double value;
   private EditBox input;
   private String error = "";
   private int left;
   private int top;

   public ConverterScreen(OpenConverterScreenPayload payload) {
      super(Component.translatable(payload.stepUp() ? "block.analog_alu.step_up_converter" : "block.analog_alu.step_down_transformer"));
      this.pos = payload.pos();
      this.stepUp = payload.stepUp();
      this.min = payload.min();
      this.max = payload.max();
      this.value = payload.setpoint();
   }

   protected void init() {
      this.left = (this.width - 260) / 2;
      this.top = Math.max(6, (this.height - 168) / 2);
      this.input = new EditBox(this.font, this.left + 16, this.top + 52, 228, 20, Component.translatable("screen.analog_alu.converter.target"));
      this.input.setMaxLength(8);
      this.input.setValue(format(this.value));
      this.input.setResponder(text -> this.error = "");
      this.addRenderableWidget(this.input);
      this.setInitialFocus(this.input);
      int buttonWidth = 34;

      for (int i = 0; i < STEPS.length; i++) {
         int step = STEPS[i];
         this.addRenderableWidget(
            Button.builder(Component.literal((step > 0 ? "+" : "") + step), button -> this.nudge(step))
               .pos(this.left + 16 + i * (buttonWidth + 4), this.top + 80)
               .size(buttonWidth, 20)
               .build()
         );
      }

      this.addRenderableWidget(
         Button.builder(Component.translatable("screen.analog_alu.converter.apply"), button -> this.apply())
            .pos(this.left + 260 - 16 - 160 - 6, this.top + 168 - 32)
            .size(80, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("screen.analog_alu.guide.close"), button -> this.onClose())
            .pos(this.left + 260 - 16 - 80, this.top + 168 - 32)
            .size(80, 20)
            .build()
      );
   }

   private static String format(double volts) {
      return volts == Math.rint(volts) ? String.format(Locale.ROOT, "%.0f", volts) : String.format(Locale.ROOT, "%.1f", volts);
   }

   private Double parse() {
      try {
         double parsed = Double.parseDouble(this.input.getValue().trim().replace(',', '.'));
         return Double.isFinite(parsed) ? parsed : null;
      } catch (NumberFormatException exception) {
         return null;
      }
   }

   private void nudge(int step) {
      Double current = this.parse();
      this.value = Math.clamp((current == null ? this.value : current) + step, this.min, this.max);
      this.input.setValue(format(this.value));
   }

   private void apply() {
      Double parsed = this.parse();
      if (parsed != null && !(parsed < this.min) && !(parsed > this.max)) {
         this.value = parsed;
         ClientPlayNetworking.send(new SetConverterSetpointPayload(this.pos, this.value));
         this.onClose();
      } else {
         this.error = Component.translatable("screen.analog_alu.converter.range", new Object[]{format(this.min), format(this.max)}).getString();
      }
   }

   public boolean keyPressed(KeyEvent event) {
      if (event.key() != 257 && event.key() != 335) {
         return super.keyPressed(event);
      }

      this.apply();
      return true;
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      graphics.fill(this.left, this.top, this.left + 260, this.top + 168, -266855134);
      graphics.outline(this.left, this.top, this.left + 260, this.top + 168, -9930363);
      graphics.text(this.font, this.title, this.left + 16, this.top + 14, -1577230);
      graphics.text(
         this.font,
         Component.translatable("screen.analog_alu.converter.target_range", new Object[]{format(this.min), format(this.max)}),
         this.left + 16,
         this.top + 36,
         -4931900
      );
      double inputVolts = 0.0;
      double outputVolts = 0.0;
      if (this.minecraft.level != null && this.minecraft.level.getBlockEntity(this.pos) instanceof CircuitDeviceBlockEntity device) {
         inputVolts = device.getConverterInputVoltage();
         outputVolts = device.getSourceTerminalVoltage();
      }

      graphics.text(
         this.font,
         Component.translatable(
            "screen.analog_alu.converter.status", new Object[]{CircuitDeviceBlock.formatVolts(inputVolts), CircuitDeviceBlock.formatVolts(outputVolts)}
         ),
         this.left + 16,
         this.top + 108,
         -8792895
      );
      graphics.text(
         this.font,
         Component.translatable(this.stepUp ? "screen.analog_alu.converter.hint_up" : "screen.analog_alu.converter.hint_down"),
         this.left + 16,
         this.top + 120,
         -7694692
      );
      if (!this.error.isEmpty()) {
         graphics.text(this.font, this.error, this.left + 16, this.top + 168 - 44, -34953);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
