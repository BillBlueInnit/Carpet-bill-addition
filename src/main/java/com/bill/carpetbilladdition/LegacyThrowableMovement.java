package com.bill.carpetbilladdition;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;

/**
 * Decides which throwable projectiles move like they did in Minecraft 1.21.1, see legacyThrowableMovement.
 * <p>
 * Only {@link ThrowableProjectile} subclasses are affected: ender pearls, snowballs, eggs, thrown potions and
 * bottles of enchanting. Arrows, tridents, fireballs and wind charges have their own movement code and are left alone.
 */
public final class LegacyThrowableMovement {
    private LegacyThrowableMovement() {
    }

    public static boolean appliesTo(ThrowableProjectile projectile) {
        return switch (CbaSettings.legacyThrowableMovement) {
            case OFF -> false;
            case PEARL -> projectile instanceof ThrownEnderpearl;
            case ALL -> true;
        };
    }
}
