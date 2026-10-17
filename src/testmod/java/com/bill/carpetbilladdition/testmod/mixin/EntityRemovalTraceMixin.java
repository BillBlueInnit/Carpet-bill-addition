package com.bill.carpetbilladdition.testmod.mixin;

import com.bill.carpetbilladdition.testmod.MovementTrace;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRemovalTraceMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void cbaTest$traceRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        MovementTrace.log((Entity) (Object) this, "removed:" + reason.name());
    }
}
