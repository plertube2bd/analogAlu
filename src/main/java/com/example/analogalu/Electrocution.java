package com.example.analogalu;

import com.example.analogalu.block.CircuitDeviceBlock;
import com.example.analogalu.block.CircuitDeviceBlockEntity;
import com.example.analogalu.block.CopperCableBlock;
import com.example.analogalu.block.CopperCableBlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class Electrocution {
   public static final float DAMAGE_PER_WATT = 0.05F;
   public static final double MIN_SHOCK_VOLTAGE = 3.0;
   public static final double DEVICE_SHOCK_VOLTAGE = 60.0;
   private static final int INTERVAL_TICKS = 10;
   private static final Map<UUID, Long> LAST_SHOCK = new HashMap<>();
   private static final double REACH = 0.3;
   public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
      Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("analog_alu", "electrocution")
   );

   private Electrocution() {
   }

   public static void init() {
      ServerTickEvents.END_LEVEL_TICK.register(Electrocution::tick);
   }

   private static void tick(ServerLevel level) {
      long now = level.getGameTime();

      for (ServerPlayer player : level.players()) {
         if (player.isAlive() && !player.isSpectator() && !player.isCreative()) {
            Long last = LAST_SHOCK.get(player.getUUID());
            if ((last == null || now - last >= 10L || now < last) && check(level, player)) {
               LAST_SHOCK.put(player.getUUID(), now);
            }
         }
      }

      if (now % 1200L == 0L) {
         LAST_SHOCK.values().removeIf(tick -> now - tick > 10L);
      }
   }

   private static boolean check(ServerLevel level, ServerPlayer player) {
      AABB reach = player.getBoundingBox().inflate(0.3);
      double wireWatts = 0.0;
      double wireVolts = 0.0;
      double deviceWatts = 0.0;
      double deviceVolts = 0.0;
      BlockPos sparkAt = null;
      MutableBlockPos pos = new MutableBlockPos();

      for (int x = Mth.floor(reach.minX); x <= Mth.floor(reach.maxX); x++) {
         for (int y = Mth.floor(reach.minY); y <= Mth.floor(reach.maxY); y++) {
            for (int z = Mth.floor(reach.minZ); z <= Mth.floor(reach.maxZ); z++) {
               pos.set(x, y, z);
               BlockEntity blockEntity = level.getBlockEntity(pos);
               if (blockEntity instanceof CopperCableBlockEntity cable && cable.getBlockState().getBlock() instanceof CopperCableBlock block) {
                  double volts = Math.abs(cable.getVoltage());
                  double amps = Math.abs(cable.getCurrent());
                  if (!(volts < 3.0) && !(amps < 0.001) && !(volts <= block.insulationRatingVolts()) && volts * amps > wireWatts) {
                     wireWatts = volts * amps;
                     wireVolts = volts;
                     sparkAt = pos.immutable();
                  }
               } else if (blockEntity instanceof CircuitDeviceBlockEntity device && CircuitDeviceBlock.isConverter(device.kindOrNull())) {
                  double volts = device.getSourceTerminalVoltage();
                  if (!(volts < 60.0)) {
                     double watts = device.getConverterOutputWatts();
                     if (watts > deviceWatts) {
                        deviceWatts = watts;
                        deviceVolts = volts;
                        sparkAt = pos.immutable();
                     }
                  }
               }
            }
         }
      }

      double watts = wireWatts + deviceWatts;
      if (!(watts <= 0.0) && sparkAt != null) {
         ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
         if (boots.is(ModItems.INSULATED_BOOTS)) {
            boots.hurtAndBreak(1 + (int)(Math.max(wireVolts, deviceVolts) / 500.0), player, EquipmentSlot.FEET);
            return true;
         } else {
            float damage = Math.max(1.0F, (float)watts * 0.05F);
            player.hurtServer(level, level.damageSources().source(DAMAGE_TYPE), damage);
            level.playSound(null, sparkAt, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.8F, 1.6F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 12, 0.3, 0.5, 0.3, 0.2);
            return true;
         }
      } else {
         return false;
      }
   }
}
