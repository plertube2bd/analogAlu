package com.example.redstonetransportation.client;

import com.example.redstonetransportation.structure.MovingStructureEntity;
import com.example.redstonetransportation.structure.MovingStructures;
import com.example.redstonetransportation.structure.StructureActions;
import com.example.redstonetransportation.structure.StructureRay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Which block of a structure the player is looking at. It takes the place of the game's own hit
 * result, so clicks reach the structure instead of whatever stands behind it.
 */
public final class StructureTargeting {
    private static MovingStructureEntity structure;
    private static StructureRay.Hit target;

    private StructureTargeting() {}

    public static void update(Minecraft client, float partialTicks) {
        structure = null;
        target = null;
        LocalPlayer player = client.player;
        if (player == null || client.level == null || player.isSpectator()) return;
        List<MovingStructureEntity> structures = MovingStructures.in(client.level);
        if (structures.isEmpty()) return;

        Vec3 eye = player.getEyePosition(partialTicks);
        Vec3 look = player.getViewVector(partialTicks);
        double range = player.blockInteractionRange();
        double nearest = range;
        HitResult hit = client.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) nearest = hit.getLocation().distanceTo(eye);

        for (MovingStructureEntity candidate : structures) {
            StructureRay.Hit found = StructureRay.cast(candidate.blocks(), candidate.pose(partialTicks), eye, look, range);
            if (found != null && found.distance() < nearest) {
                nearest = found.distance();
                structure = candidate;
                target = found;
            }
        }
        if (structure != null) {
            Vec3 end = eye.add(look.scale(range));
            client.hitResult = BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end));
            client.crosshairPickEntity = null;
        }
    }

    /** The cell of this structure under the crosshair, or null. */
    static BlockPos targetOn(MovingStructureEntity candidate) {
        return candidate == structure ? target.cell() : null;
    }

    /** Returns true when the click went to a structure, so the game should not act on it as well. */
    public static boolean send(Minecraft client, boolean attack) {
        if (structure == null || client.player == null) return false;
        ClientPlayNetworking.send(new StructureActions.Payload(structure.getId(), attack, target.cell(), target.face()));
        client.player.swing(InteractionHand.MAIN_HAND, client.player.getMainHandItem().getInteractAnimation(), false);
        return true;
    }
}
