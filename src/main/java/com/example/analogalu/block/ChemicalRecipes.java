package com.example.analogalu.block;

import com.example.analogalu.ModItems;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ChemicalRecipes {
   private static List<ChemicalRecipes.Recipe> reactor;
   private static List<ChemicalRecipes.Recipe> chlorine;

   private ChemicalRecipes() {
   }

   public static List<ChemicalRecipes.Recipe> forKind(CircuitDeviceBlock.Kind kind) {
      if (reactor == null) {
         build();
      }

      return kind == CircuitDeviceBlock.Kind.CHLORINE_GENERATOR ? chlorine : reactor;
   }

   private static ChemicalRecipes.Input in(Item item, int count) {
      return new ChemicalRecipes.Input(item, count);
   }

   private static ItemStack out(Item item, int count) {
      return new ItemStack(item, count);
   }

   private static ChemicalRecipes.Recipe recipe(String id, double joules, List<ChemicalRecipes.Input> inputs, List<ItemStack> outputs) {
      return new ChemicalRecipes.Recipe(id, inputs, outputs, joules);
   }

   private static void build() {
      chlorine = List.of(recipe("electrolysis", 1600.0, List.of(in(Items.WATER_BUCKET, 1)), List.of(out(ModItems.CHLORINE, 1), out(ModItems.HYDROGEN, 1))));
      reactor = List.of(
         recipe(
            "naphtha_cracking",
            3000.0,
            List.of(in(ModItems.NAPHTHA, 2)),
            List.of(out(ModItems.ETHYLENE, 2), out(ModItems.PROPYLENE, 1), out(ModItems.C4_FRACTION, 1), out(ModItems.PYROLYSIS_GASOLINE, 1))
         ),
         recipe("aromatics_extraction", 2000.0, List.of(in(ModItems.PYROLYSIS_GASOLINE, 2)), List.of(out(ModItems.BENZENE, 1), out(ModItems.P_XYLENE, 1))),
         recipe("butadiene_extraction", 1500.0, List.of(in(ModItems.C4_FRACTION, 1)), List.of(out(ModItems.BUTADIENE, 1))),
         recipe("ammonia_synthesis", 2500.0, List.of(in(ModItems.HYDROGEN, 3)), List.of(out(ModItems.AMMONIA, 2))),
         recipe("polyethylene", 2000.0, List.of(in(ModItems.ETHYLENE, 2)), List.of(out(ModItems.POLYETHYLENE, 1))),
         recipe("polypropylene", 2000.0, List.of(in(ModItems.PROPYLENE, 2)), List.of(out(ModItems.POLYPROPYLENE, 1))),
         recipe("vinyl_chloride", 1500.0, List.of(in(ModItems.ETHYLENE, 1), in(ModItems.CHLORINE, 1)), List.of(out(ModItems.VINYL_CHLORIDE, 1))),
         recipe("polyvinyl_chloride", 2000.0, List.of(in(ModItems.VINYL_CHLORIDE, 2)), List.of(out(ModItems.POLYVINYL_CHLORIDE, 1))),
         recipe("ethylene_glycol", 1500.0, List.of(in(ModItems.ETHYLENE, 1), in(Items.WATER_BUCKET, 1)), List.of(out(ModItems.ETHYLENE_GLYCOL, 1))),
         recipe("terephthalic_acid", 2000.0, List.of(in(ModItems.P_XYLENE, 1)), List.of(out(ModItems.TEREPHTHALIC_ACID, 1))),
         recipe(
            "polyethylene_terephthalate",
            2500.0,
            List.of(in(ModItems.TEREPHTHALIC_ACID, 1), in(ModItems.ETHYLENE_GLYCOL, 1)),
            List.of(out(ModItems.POLYETHYLENE_TEREPHTHALATE, 1))
         ),
         recipe("acrylonitrile", 2000.0, List.of(in(ModItems.PROPYLENE, 1), in(ModItems.AMMONIA, 1)), List.of(out(ModItems.ACRYLONITRILE, 1))),
         recipe("polyacrylonitrile", 2000.0, List.of(in(ModItems.ACRYLONITRILE, 2)), List.of(out(ModItems.POLYACRYLONITRILE, 1))),
         recipe("adipic_acid", 2000.0, List.of(in(ModItems.BENZENE, 1), in(ModItems.HYDROGEN, 2)), List.of(out(ModItems.ADIPIC_ACID, 1))),
         recipe("hexamethylenediamine", 2500.0, List.of(in(ModItems.ADIPIC_ACID, 1), in(ModItems.AMMONIA, 2)), List.of(out(ModItems.HEXAMETHYLENEDIAMINE, 1))),
         recipe("polyamide", 2500.0, List.of(in(ModItems.ADIPIC_ACID, 1), in(ModItems.HEXAMETHYLENEDIAMINE, 1)), List.of(out(ModItems.POLYAMIDE, 1))),
         recipe("styrene", 2000.0, List.of(in(ModItems.BENZENE, 1), in(ModItems.ETHYLENE, 1)), List.of(out(ModItems.STYRENE, 1))),
         recipe("synthetic_rubber", 3000.0, List.of(in(ModItems.BUTADIENE, 3), in(ModItems.STYRENE, 1)), List.of(out(ModItems.SYNTHETIC_RUBBER, 2)))
      );
   }

   public record Input(Item item, int count) {
   }

   public record Recipe(String id, List<ChemicalRecipes.Input> inputs, List<ItemStack> outputs, double energyJoules) {
      public boolean uses(Item item) {
         return this.inputs.stream().anyMatch(input -> input.item() == item);
      }

      public int required(Item item) {
         return this.inputs.stream().filter(input -> input.item() == item).mapToInt(ChemicalRecipes.Input::count).sum();
      }
   }
}
