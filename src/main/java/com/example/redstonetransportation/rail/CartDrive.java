package com.example.redstonetransportation.rail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * Saved drive state of any minecart, stored as a data attachment so vanilla carts carry it too.
 *
 * @param cruise   speed held along every rail, in blocks/tick; 0 means coasting
 * @param limit    speed cap above the vanilla rail limit, in blocks/tick
 * @param headingX departure direction used while the cart is standing still
 * @param headingZ departure direction used while the cart is standing still
 * @param follows  the car one step closer to the front of the train, if this one is not in front
 */
public record CartDrive(float cruise, float limit, float headingX, float headingZ, Optional<UUID> follows) {
    public static final Codec<CartDrive> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("cruise").forGetter(CartDrive::cruise),
            Codec.FLOAT.fieldOf("limit").forGetter(CartDrive::limit),
            Codec.FLOAT.fieldOf("heading_x").forGetter(CartDrive::headingX),
            Codec.FLOAT.fieldOf("heading_z").forGetter(CartDrive::headingZ),
            UUIDUtil.CODEC.optionalFieldOf("follows").forGetter(CartDrive::follows)
    ).apply(instance, CartDrive::new));

    /** A cart that keeps its current speed under {@code limit} and is not driven. */
    public static CartDrive coasting(float limit) {
        return new CartDrive(0.0F, limit, 0.0F, 0.0F, Optional.empty());
    }
}
