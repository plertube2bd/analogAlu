package com.example.analogalu;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class ModEffects {
   private static final Identifier STUNNED_ID = Identifier.fromNamespaceAndPath("analog_alu", "stunned");
   public static final Holder<MobEffect> STUNNED = Registry.registerForHolder(
      BuiltInRegistries.MOB_EFFECT,
      STUNNED_ID,
      new StunnedEffect()
         .addAttributeModifier(Attributes.MOVEMENT_SPEED, STUNNED_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL)
         .addAttributeModifier(Attributes.JUMP_STRENGTH, STUNNED_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL)
   );

   private ModEffects() {
   }

   public static void init() {
   }
}
