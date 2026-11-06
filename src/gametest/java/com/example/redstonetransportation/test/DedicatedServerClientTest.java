package com.example.redstonetransportation.test;

import com.example.redstonetransportation.block.StructureCoreBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.structure.MovingStructureEntity;
import com.example.redstonetransportation.structure.MovingStructures;
import com.example.redstonetransportation.structure.StructureAssembler;
import com.example.redstonetransportation.structure.StructurePose;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;

/**
 * A real dedicated server with a real client connected. Here the server does not trust the client
 * the way singleplayer does: it kicks a player who hangs in the air for four seconds, and it sends
 * back a player whose position it disagrees with.
 */
public final class DedicatedServerClientTest implements FabricClientGameTest {
    private static final int RIDE_TICKS = 160;
    private static final int DECK_HEIGHT = 30;

    @Override
    public void runTest(ClientGameTestContext context) {
        Properties properties = new Properties();
        properties.setProperty("allow-flight", "false");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            server.runCommand("time set 6000");
            server.runCommand("gamerule advance_time false");

            BlockPos core = server.computeOnServer(minecraft -> {
                ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
                BlockPos deckCore = player.blockPosition().above(DECK_HEIGHT);
                buildAirship(minecraft.overworld(), deckCore);
                return deckCore;
            });
            context.waitTicks(10);
            if (server.computeOnServer(MinecraftServer::allowFlight)) throw new AssertionError("this server would never kick for flying");
            boolean assembled = server.computeOnServer(minecraft ->
                    StructureAssembler.assemble(minecraft.overworld(), core, null).structure() != null);
            if (!assembled) throw new AssertionError("the airship did not assemble on a dedicated server");
            connection.waitForClientboundPackets();

            // Off to one side of the core: the player has to stand on the deck, not on the seat.
            double deckTop = core.getY();
            server.runCommand("tp @a " + (core.getX() + 2.5) + " " + deckTop + " " + (core.getZ() + 0.5));
            context.waitTicks(20);
            double settled = context.computeOnClient(client -> client.player.getY() - deckTop);
            if (Math.abs(settled) > 0.2) throw new AssertionError("the player did not land on the deck: " + settled);

            Vec3 start = context.computeOnClient(client -> client.player.position());
            server.runOnServer(minecraft -> structure(minecraft.overworld()).steerForTest(new Input(true, false, false, true, false, false, false)));
            context.waitTicks(RIDE_TICKS);

            // Long past the 80 ticks in the air that get a player kicked, with no block under the deck.
            if (context.computeOnClient(client -> client.player == null || client.getConnection() == null)) {
                throw new AssertionError("the player was disconnected from the server");
            }
            double above = context.computeOnClient(client -> client.player.getY() - deckTop);
            if (Math.abs(above) > 0.3) throw new AssertionError("the player fell off the moving deck: " + above);
            double travelled = context.computeOnClient(client -> client.player.position().distanceTo(start));
            if (travelled < 10.0) throw new AssertionError("the deck left the player behind: moved " + travelled);

            Vec3 onClient = context.computeOnClient(client -> client.player.position());
            Vec3 onServer = server.computeOnServer(minecraft -> minecraft.getPlayerList().getPlayers().getFirst().position());
            double apart = onClient.distanceTo(onServer);
            if (apart > 3.0) throw new AssertionError("server and client disagree about the player by " + apart);

            testClicksOverTheNetwork(context, server);
        }
    }

    /** Aiming and clicking through a real connection, while standing on the airship. */
    private static void testClicksOverTheNetwork(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runOnServer(minecraft -> structure(minecraft.overworld()).steerForTest(null));
        context.waitTicks(40);
        server.runCommand("give @a oak_planks 4");
        context.waitTicks(4);

        Vec3 chest = cellOnClient(context, new BlockPos(0, 0, 3), 0.4D);
        context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, chest));
        context.waitTicks(3);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(8);
        boolean opened = context.computeOnClient(client -> client.player.containerMenu instanceof ChestMenu);
        if (!opened) throw new AssertionError("the chest aboard did not open over the network");
        context.setScreen(() -> null);
        context.waitTicks(4);

        int before = server.computeOnServer(minecraft -> structure(minecraft.overworld()).blocks().size());
        Vec3 deck = cellOnClient(context, new BlockPos(0, -1, -2), 1.0D);
        context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, deck));
        context.waitTicks(3);
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(8);
        int placed = server.computeOnServer(minecraft -> structure(minecraft.overworld()).blocks().size());
        if (placed != before + 1) throw new AssertionError("placing a block aboard did nothing: " + before + " to " + placed);

        Vec3 added = cellOnClient(context, new BlockPos(0, 0, -2), 0.5D);
        context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, added));
        context.waitTicks(3);
        context.getInput().pressKey(options -> options.keyAttack);
        context.waitTicks(8);
        int broken = server.computeOnServer(minecraft -> structure(minecraft.overworld()).blocks().size());
        if (broken != before) throw new AssertionError("breaking a block aboard did nothing: " + placed + " to " + broken);
    }

    /** Where a cell of the structure is, on the client, at this height within it. */
    private static Vec3 cellOnClient(ClientGameTestContext context, BlockPos cell, double height) {
        return context.computeOnClient(client -> {
            StructurePose pose = MovingStructures.in(client.level).getFirst().pose(1.0F);
            return new Vec3(pose.worldX(cell.getX(), cell.getZ()), pose.position().y + cell.getY() + height, pose.worldZ(cell.getX(), cell.getZ()));
        });
    }

    private static MovingStructureEntity structure(ServerLevel level) {
        return MovingStructures.in(level).getFirst();
    }

    private static void buildAirship(ServerLevel level, BlockPos core) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlock(core.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int x = -3; x <= 3; x++) {
            level.setBlock(core.offset(x, -2, 0), ModContent.LIFT_BALLOON.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(core.offset(0, 0, 3), Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(core, ModContent.AIRSHIP_HELM.defaultBlockState().setValue(StructureCoreBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
    }
}
