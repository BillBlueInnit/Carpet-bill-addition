package com.bill.carpetbilladdition.testmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Prints the exact position and velocity of entities tagged {@code cba_case_<name>} so movement can be compared
 * tick by tick between Minecraft versions. {@link Double#toString} round-trips, so equal text means equal bits.
 */
public final class MovementTrace {
    private static final String TAG_PREFIX = "cba_case_";
    private static final String MOTION_TAG_PREFIX = "cba_motion=";

    private MovementTrace() {
    }

    /**
     * Sets the velocity from a {@code cba_motion=<x>,<y>,<z>} tag before the first tick. Unlike the Motion NBT,
     * which ignores components above 10, this allows the speeds TNT gives pearls in cannons.
     */
    public static void applyMotionTag(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(MOTION_TAG_PREFIX)) {
                String[] parts = tag.substring(MOTION_TAG_PREFIX.length()).split(",");
                entity.setDeltaMovement(new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                entity.removeTag(tag);
                return;
            }
        }
    }

    public static void log(Entity entity, String event) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                Vec3 motion = entity.getDeltaMovement();
                System.out.println("CBA-TRACE " + tag.substring(TAG_PREFIX.length()) + " " + event + " " + entity.tickCount
                        + " " + entity.getX() + " " + entity.getY() + " " + entity.getZ()
                        + " " + motion.x + " " + motion.y + " " + motion.z);
                return;
            }
        }
    }
}
