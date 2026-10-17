package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.LegacyThrowableMovement;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the throwable projectile tick of Minecraft 1.21.1 for the projectiles chosen by legacyThrowableMovement.
 * <p>
 * 1.21.2 rewrote {@code ThrowableProjectile.tick()}. Besides applying gravity and drag before moving instead of
 * after, it looks for collisions with the new velocity, moves the projectile to the hit point, and only then runs
 * the base entity tick and the hit. That order cannot be restored by moving a single call, so for those projectiles
 * the 1.21.1 tick runs instead of the current one, reusing the current helpers where they behave the same.
 */
@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin extends Entity {
    // Projectile's constructor is not accessible, so the mixin extends Entity. Mixin binds the super.tick() call
    // below to the closest implementation above ThrowableProjectile, which is Projectile.tick().
    private ThrowableProjectileMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cba$tickLikeMinecraft1211(CallbackInfo ci) {
        if (LegacyThrowableMovement.appliesTo((ThrowableProjectile) (Object) this)) {
            ci.cancel();
            this.cba$legacyTick();
        }
    }

    /**
     * {@code ProjectileUtil.getHitResultOnMoveVector()} with the entity hit margin of 1.21.1. Since 1.21.6 the margin
     * grows from 0 to 0.3 over a projectile's first ticks ({@code ProjectileUtil.computeMargin()}), so entities right
     * next to a fresh projectile are no longer hit. 1.21.1 always used 0.3.
     */
    @Unique
    private HitResult cba$hitResultOnMoveVector(Predicate<Entity> canHit) {
        Vec3 from = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 to = from.add(motion);
        HitResult hitResult = this.level().clipIncludingBorder(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hitResult.getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }
        HitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                this.level(), this, from, to, this.getBoundingBox().expandTowards(motion).inflate(1.0), canHit, 0.3F
        );
        return entityHitResult != null ? entityHitResult : hitResult;
    }

    @Unique
    private void cba$legacyTick() {
        // Projectile.tick(): shoot game event, owner check and the base entity tick (fire, water state, portals)
        // come first, so the drag below uses the water state at the current position.
        super.tick();

        // Collisions are searched along last tick's velocity from where the projectile is now, and the hit is
        // handled right away, before it moves. An ender pearl teleports its owner to this position, which is also
        // the oldPosition() that the current ThrownEnderpearl.onHit() uses.
        ProjectileInvoker projectile = (ProjectileInvoker) this;
        HitResult hitResult = this.cba$hitResultOnMoveVector(projectile::cba$canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            projectile.cba$hitTargetOrDeflectSelf(hitResult);
        }

        // 1.21.1 called checkInsideBlocks() here for the blocks the projectile is in before moving. The current
        // equivalent is the block effect pass: the projectile has not moved yet, so it covers oldPosition() to
        // position(), which is the same point, plus any movement recorded this tick, e.g. from a piston push.
        this.applyEffectsFromBlocks();

        // Move by last tick's velocity, then apply drag and gravity for the next tick:
        //   1.21.1: position += v; v = v * drag - gravity
        //   1.21.2: v = (v - gravity) * drag; position += v
        Vec3 motion = this.getDeltaMovement();
        double x = this.getX() + motion.x;
        double y = this.getY() + motion.y;
        double z = this.getZ() + motion.z;
        projectile.cba$updateRotation();
        float drag;
        if (this.isInWater()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.BUBBLE, x - motion.x * 0.25, y - motion.y * 0.25, z - motion.z * 0.25, motion.x, motion.y, motion.z);
            }
            drag = 0.8F;
        } else {
            drag = 0.99F;
        }
        this.setDeltaMovement(motion.scale(drag));
        this.applyGravity();
        this.setPos(x, y, z);
    }
}
