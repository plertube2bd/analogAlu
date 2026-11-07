package com.example.modernizationworld.redcoat;

import com.example.analogalu.gun.GunFiring;
import com.example.analogalu.gun.GunType;
import com.example.analogalu.gun.ModGuns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;


/** Shared humanoid soldier implementation; the registered entity type determines its uniform and role. */
public final class RedcoatSoldierEntity extends Monster {
    private int nextShot;
    private int drummerPulse;
    private BlockPos marchTarget;

    public RedcoatSoldierEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        RedcoatKind kind = kind();
        switch (kind) {
            case MUSKETEER, RIFLEMAN -> setItemSlot(EquipmentSlot.HEAD, new ItemStack(RedcoatContent.SHAKO));
            case GRENADIER -> setItemSlot(EquipmentSlot.HEAD, new ItemStack(RedcoatContent.GRENADIER_MITRE));
            case SERGEANT -> setItemSlot(EquipmentSlot.HEAD, new ItemStack(RedcoatContent.SERGEANT_SHAKO));
            case OFFICER, DRAGOON -> setItemSlot(EquipmentSlot.HEAD, new ItemStack(RedcoatContent.OFFICER_BICORNE));
            case DRUMMER -> setItemSlot(EquipmentSlot.HEAD, new ItemStack(RedcoatContent.SHAKO));
        }
        switch (kind) {
            case MUSKETEER, GRENADIER, RIFLEMAN -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModGuns.MUSKET));
            case OFFICER, DRAGOON -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModGuns.REVOLVER));
            case SERGEANT -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            case DRUMMER -> setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.08D);
    }

    public void setMarchTarget(BlockPos target) { this.marchTarget = target.immutable(); }

    public RedcoatKind kind() {
        EntityType<?> type = getType();
        if (type == RedcoatEntities.GRENADIER) return RedcoatKind.GRENADIER;
        if (type == RedcoatEntities.SERGEANT) return RedcoatKind.SERGEANT;
        if (type == RedcoatEntities.OFFICER) return RedcoatKind.OFFICER;
        if (type == RedcoatEntities.RIFLEMAN) return RedcoatKind.RIFLEMAN;
        if (type == RedcoatEntities.DRUMMER) return RedcoatKind.DRUMMER;
        if (type == RedcoatEntities.DRAGOON) return RedcoatKind.DRAGOON;
        return RedcoatKind.MUSKETEER;
    }

    @Override
    protected void registerGoals() {
        if (kind().melee()) goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.12D, false));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        RedcoatKind role = kind();
        LivingEntity target = getTarget();
        if (role == RedcoatKind.DRUMMER) {
            if (++drummerPulse >= 100) {
                drummerPulse = 0;
                for (RedcoatSoldierEntity ally : server.getEntitiesOfClass(RedcoatSoldierEntity.class,
                        getBoundingBox().inflate(16.0D), e -> e.isAlive() && e != this)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0, false, true));
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 140, 0, false, true));
                }
            }
            if (target != null) {
                Vec3 away = position().subtract(target.position()).normalize();
                getNavigation().moveTo(getX() + away.x * 12.0D, getY(), getZ() + away.z * 12.0D, 1.25D);
            } else if (marchTarget != null) {
                getNavigation().moveTo(marchTarget.getX() + 0.5D, marchTarget.getY(), marchTarget.getZ() + 0.5D, 0.85D);
            }
            return;
        }

        if (target == null || !target.isAlive()) {
            if (marchTarget != null && position().distanceToSqr(Vec3.atCenterOf(marchTarget)) > 9.0D) {
                getNavigation().moveTo(marchTarget.getX() + 0.5D, marchTarget.getY(), marchTarget.getZ() + 0.5D, 0.9D);
            } else {
                getNavigation().stop();
            }
            return;
        }
        // Keep the soldier visibly aimed at the target while approaching or firing.
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (role == RedcoatKind.OFFICER || role == RedcoatKind.DRAGOON) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(distanceToSqr(target) <= 16.0D
                    ? Items.IRON_SWORD : ModGuns.REVOLVER));
        }
        if (role == RedcoatKind.GRENADIER && distanceToSqr(target) < 13.0D * 13.0D
                && distanceToSqr(target) > 5.0D * 5.0D && tickCount >= nextShot) {
            Vec3 grenadeAt = target.position().add(0.0D, 0.5D, 0.0D);
            server.explode(this, null, null, grenadeAt.x, grenadeAt.y, grenadeAt.z,
                    1.6F, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            nextShot = tickCount + 160;
            return;
        }
        if (role == RedcoatKind.MUSKETEER || role == RedcoatKind.GRENADIER
                || role == RedcoatKind.RIFLEMAN || role == RedcoatKind.OFFICER || role == RedcoatKind.DRAGOON) {
            double distance = distanceToSqr(target);
            double preferredRange = Math.max(5.0D, role.range() * 0.75D);
            if (distance > preferredRange * preferredRange) getNavigation().moveTo(target, 1.0D);
            else getNavigation().stop();
            if (tickCount >= nextShot && distance > 16.0D && distance <= role.range() * role.range()) {
                fireAt(server, target, role);
                nextShot = tickCount + role.cooldown();
            }
        }
    }

    public static boolean rollAccurate(net.minecraft.util.RandomSource random) {
        return random.nextBoolean();
    }

    private void fireAt(ServerLevel server, LivingEntity target, RedcoatKind role) {
        Vec3 eye = getEyePosition();
        Vec3 centre = target.getBoundingBox().getCenter();
        Vec3 direction = centre.subtract(eye).normalize();
        // Exactly half of the attempts are deliberately aimed away from the target.
        boolean accurate = rollAccurate(getRandom());
        if (!accurate) {
            Vec3 side = direction.cross(Math.abs(direction.y) > 0.95D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            direction = direction.add(side.scale(0.42D)).add(0.0D, 0.18D, 0.0D).normalize();
        }
        double range = role.range();
        GunType gun = role == RedcoatKind.OFFICER || role == RedcoatKind.DRAGOON
                ? GunType.REVOLVER : GunType.MUSKET;
        GunFiring.fire(server, this, gun, direction, 0.0D, range,
                entity -> GunFiring.canHit(this, entity) && !(entity instanceof RedcoatSoldierEntity));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof RedcoatSoldierEntity) return false;
        return super.canAttack(target);
    }

}
