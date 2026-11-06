package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChemicalMachineBlockEntity extends CircuitDeviceBlockEntity {
   public static final int INPUT_CAPACITY = 64;
   private static final int TICK_INTERVAL = 10;
   private final Map<Item, Integer> buffer = new LinkedHashMap<>();
   private int recipeIndex;
   private double progressJoules;
   private double lastWatts;
   private int lastSync = Integer.MIN_VALUE;
   private static final int[] SLOTS = new int[]{0};

   public ChemicalMachineBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlocks.CHEMICAL_MACHINE_ENTITY, pos, state);
   }

   public CircuitDeviceBlock.Kind machineKind() {
      return this.getBlockState().getBlock() instanceof CircuitDeviceBlock block ? block.kind() : CircuitDeviceBlock.Kind.CHEMICAL_REACTOR;
   }

   public List<ChemicalRecipes.Recipe> recipes() {
      return ChemicalRecipes.forKind(this.machineKind());
   }

   public ChemicalRecipes.Recipe recipe() {
      List<ChemicalRecipes.Recipe> recipes = this.recipes();
      return recipes.get(Math.floorMod(this.recipeIndex, recipes.size()));
   }

   public static double minimumVoltage(CircuitDeviceBlock.Kind kind) {
      return kind == CircuitDeviceBlock.Kind.CHLORINE_GENERATOR ? 4.0 : 12.0;
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.recipeIndex = Math.max(0, input.getIntOr("Recipe", 0));
      this.progressJoules = Math.max(0.0, input.getDoubleOr("ProgressJoules", 0.0));
      this.lastWatts = Math.max(0.0, input.getDoubleOr("Watts", 0.0));
      this.buffer.clear();

      for (String entry : input.getStringOr("Buffer", "").split(";")) {
         int split = entry.lastIndexOf(61);
         if (split > 0) {
            Identifier id = Identifier.tryParse(entry.substring(0, split));
            if (id != null) {
               Item item = (Item)BuiltInRegistries.ITEM.getValue(id);
               if (item != null && item != Items.AIR) {
                  try {
                     int count = Integer.parseInt(entry.substring(split + 1));
                     if (count > 0) {
                        this.buffer.put(item, Math.min(count, 256));
                     }
                  } catch (NumberFormatException var10) {
                  }
               }
            }
         }
      }
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("Recipe", this.recipeIndex);
      output.putDouble("ProgressJoules", this.progressJoules);
      output.putDouble("Watts", this.lastWatts);
      StringBuilder encoded = new StringBuilder();
      this.buffer.forEach((item, count) -> {
         if (count > 0) {
            if (!encoded.isEmpty()) {
               encoded.append(';');
            }

            encoded.append(BuiltInRegistries.ITEM.getKey(item)).append('=').append(count);
         }
      });
      output.putString("Buffer", encoded.toString());
   }

   private void sync() {
      int signature = Objects.hash(
         this.buffer, this.recipeIndex, (int)this.progressJoules / 20, Math.round(this.lastWatts * 10.0), Math.round(this.getLoadVoltage() * 100.0)
      );
      if (signature != this.lastSync) {
         this.lastSync = signature;
         BlockEntitySync.send(this);
      }
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      super.preRemoveSideEffects(pos, state);
      if (this.level != null && !this.level.isClientSide()) {
         this.buffer.forEach((item, count) -> {
            if (item != Items.WATER_BUCKET) {
               for (int left = count; left > 0; left -= item.getDefaultMaxStackSize()) {
                  MachineOutput.emit(this.level, pos, new ItemStack(item, Math.min(left, item.getDefaultMaxStackSize())));
               }
            }
         });
         this.buffer.clear();
      }
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ChemicalMachineBlockEntity machine) {
      if (!level.isClientSide() && level.getGameTime() % 10L == 0L) {
         double voltage = machine.getLoadVoltage();
         double watts = voltage >= minimumVoltage(machine.machineKind()) ? Math.max(0.0, voltage * Math.abs(machine.getLoadCurrent())) : 0.0;
         machine.lastWatts = watts;
         ChemicalRecipes.Recipe recipe = machine.recipe();
         boolean ready = machine.hasInputs(recipe);
         boolean active = ready && watts > 0.0;
         if ((Boolean)state.getValue(CircuitDeviceBlock.CLOSED) != active) {
            level.setBlock(pos, (BlockState)state.setValue(CircuitDeviceBlock.CLOSED, active), 3);
         }

         if (active) {
            machine.progressJoules += watts * 10.0 / 20.0;
            if (machine.progressJoules >= recipe.energyJoules()) {
               machine.progressJoules = 0.0;

               for (ChemicalRecipes.Input input : recipe.inputs()) {
                  machine.take(input.item(), input.count());
               }

               for (ItemStack output : recipe.outputs()) {
                  MachineOutput.emit(level, pos, output.copy());
               }
            }

            machine.setChanged();
         } else if (!ready && machine.progressJoules > 0.0) {
            machine.progressJoules = 0.0;
            machine.setChanged();
         }

         machine.sync();
      }
   }

   private boolean hasInputs(ChemicalRecipes.Recipe recipe) {
      for (ChemicalRecipes.Input input : recipe.inputs()) {
         if (this.buffer.getOrDefault(input.item(), 0) < input.count()) {
            return false;
         }
      }

      return true;
   }

   private void take(Item item, int count) {
      int left = this.buffer.getOrDefault(item, 0) - count;
      if (left > 0) {
         this.buffer.put(item, left);
      } else {
         this.buffer.remove(item);
      }
   }

   public int stored(Item item) {
      return this.buffer.getOrDefault(item, 0);
   }

   public boolean accepts(ItemStack stack) {
      return !stack.isEmpty() && this.recipe().uses(stack.getItem()) && this.stored(stack.getItem()) < 64;
   }

   public int insert(ItemStack stack, int maxCount) {
      if (!this.accepts(stack)) {
         return 0;
      }

      int inserted = Math.min(maxCount, Math.min(stack.getCount(), 64 - this.stored(stack.getItem())));
      if (inserted <= 0) {
         return 0;
      }

      this.buffer.merge(stack.getItem(), inserted, Integer::sum);
      this.setChanged();
      this.sync();
      return inserted;
   }

   public ChemicalRecipes.Recipe cycleRecipe(boolean forward) {
      List<ChemicalRecipes.Recipe> recipes = this.recipes();
      this.recipeIndex = Math.floorMod(this.recipeIndex + (forward ? 1 : -1), recipes.size());
      this.progressJoules = 0.0;
      if (this.level != null) {
         BlockPos pos = this.worldPosition;
         this.buffer.forEach((item, count) -> {
            if (item != Items.WATER_BUCKET) {
               MachineOutput.emit(this.level, pos, new ItemStack(item, count));
            }
         });
      }

      this.buffer.clear();
      this.setChanged();
      this.sync();
      return this.recipe();
   }

   public static MutableComponent recipeName(ChemicalRecipes.Recipe recipe) {
      return Component.translatable("recipe.analog_alu." + recipe.id());
   }

   @Override
   public List<Component> hoverInfo(Level level) {
      CircuitDeviceBlock.Kind kind = this.machineKind();
      ChemicalRecipes.Recipe recipe = this.recipe();
      double minVoltage = minimumVoltage(kind);
      double voltage = this.getLoadVoltage();
      List<Component> lines = new ArrayList<>();
      lines.add(this.getBlockState().getBlock().getName().withStyle(ChatFormatting.GOLD));
      if (kind == CircuitDeviceBlock.Kind.CHEMICAL_REACTOR) {
         lines.add(HoverText.line("process", recipeName(recipe), ChatFormatting.LIGHT_PURPLE));
      }

      lines.add(
         HoverText.line(
            "load_voltage",
            String.format(Locale.ROOT, "%.2f V (min %.0f V)", voltage, minVoltage),
            voltage >= minVoltage ? ChatFormatting.GREEN : ChatFormatting.RED
         )
      );
      lines.add(HoverText.line("power", String.format(Locale.ROOT, "%.1f W", this.lastWatts), ChatFormatting.WHITE));

      for (ChemicalRecipes.Input input : recipe.inputs()) {
         int have = this.stored(input.item());
         lines.add(
            Component.literal(" ")
               .append(new ItemStack(input.item()).getItemName().copy().withStyle(ChatFormatting.GRAY))
               .append(
                  Component.literal(String.format(Locale.ROOT, ": %d / %d", have, input.count()))
                     .withStyle(have >= input.count() ? ChatFormatting.GREEN : ChatFormatting.RED)
               )
         );
      }

      lines.add(
         HoverText.line(
            "progress",
            String.format(
               Locale.ROOT, "%.0f / %.0f J (%.0f%%)", this.progressJoules, recipe.energyJoules(), 100.0 * this.progressJoules / recipe.energyJoules()
            ),
            ChatFormatting.AQUA
         )
      );
      MutableComponent products = Component.empty();

      for (int i = 0; i < recipe.outputs().size(); i++) {
         ItemStack output = recipe.outputs().get(i);
         if (i > 0) {
            products.append(", ");
         }

         products.append(output.getItemName().copy()).append(" x" + output.getCount());
      }

      lines.add(HoverText.line("products", products, ChatFormatting.YELLOW));
      lines.add(HoverText.hint(kind == CircuitDeviceBlock.Kind.CHEMICAL_REACTOR ? "chemical_reactor" : "chlorine_generator"));
      return lines;
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return SLOTS;
   }

   @Override
   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
      return this.canPlaceItem(slot, stack);
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
      return false;
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return this.accepts(stack);
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      if (!stack.isEmpty()) {
         boolean water = stack.is(Items.WATER_BUCKET);
         int inserted = this.insert(stack, stack.getCount());
         if (water && inserted > 0 && this.level != null) {
            MachineOutput.emit(this.level, this.worldPosition, new ItemStack(Items.BUCKET, inserted));
         }
      }
   }

   @Override
   public boolean stillValid(Player player) {
      return false;
   }
}
