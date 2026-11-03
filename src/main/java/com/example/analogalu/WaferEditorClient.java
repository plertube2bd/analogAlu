package com.example.analogalu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@Environment(EnvType.CLIENT)
public final class WaferEditorClient implements ClientModInitializer {
    public static final String DEFAULT_CODE = "module main;\nport front: 4;\nport back: 4;\nport left: 4;\nport right: 4;\ncram scratch: 256;\nff flags: 2048;\n\nlet a = left;\nlet b = right;\nfront = +(a, b);\nlet carry = (a ++ b).carry;\nshow(a ++ b);";

    public WaferEditorClient() {}

    @Override
    public void onInitializeClient() {
        var oilMaterial = new Material(Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "block/oil_still"));
        var flowingOilMaterial = new Material(Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "block/oil_flow"));
        var oilModel = new FluidModel.Unbaked(oilMaterial, flowingOilMaterial, flowingOilMaterial,
            BlockTintSources.constant(0xFF4A3C22));
        FluidRenderingRegistry.register(ModFluids.STILL, oilModel);
        FluidRenderingRegistry.register(ModFluids.FLOWING, oilModel);
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide()) return net.minecraft.world.InteractionResult.PASS;
            if (stack.is(ModItems.SILICON_BOULE)) {
                Minecraft.getInstance().setScreenAndShow(new ProcessGuideScreen("screen.analog_alu.boule"));
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown()
                    && (stack.is(ModItems.PROGRAMMED_SILICON_WAFER)
                    || stack.is(ModItems.SEMICONDUCTOR)
                    || stack.is(ModBlocks.SEMICONDUCTOR_BLOCK_ITEM)
                    || stack.is(ModBlocks.WATERPROOF_SEMICONDUCTOR_BLOCK_ITEM))) {
                ClientPlayNetworking.send(new RunWaferProgramPayload(hand));
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            if (!(stack.is(ModItems.SILICON_WAFER) || stack.is(ModItems.PROGRAMMED_SILICON_WAFER))) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            String code = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getStringOr("wafer_code", DEFAULT_CODE);
            open(stack, hand, code);
            return net.minecraft.world.InteractionResult.SUCCESS;
        });
        CircuitStatusTooltips.init();
        ClientPlayNetworking.registerGlobalReceiver(CircuitStatusPayload.TYPE, (payload, context) ->
            context.client().execute(() -> CircuitStatusTooltips.update(payload.itemId(), payload.text())));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() && level.getBlockState(hit.getBlockPos()).is(ModBlocks.MASS_ANALYZER)) {
                Minecraft.getInstance().setScreenAndShow(new ProcessGuideScreen("screen.analog_alu.analyzer"));
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
    }

    public static void open(ItemStack stack, InteractionHand hand, String code) {
        Minecraft.getInstance().setScreenAndShow(new WaferEditorScreen(hand, code));
    }

    public static void save(InteractionHand hand, String code) {
        ClientPlayNetworking.send(new SaveWaferCodePayload(hand, code));
    }

    public static Component text(String value) {
        return Component.literal(value);
    }
}