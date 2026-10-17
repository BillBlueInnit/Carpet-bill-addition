package com.bill.carpetbilladdition.testmod.mixin;

import com.bill.carpetbilladdition.testmod.MovementTrace;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTraceMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void cbaTest$applyMotionTags(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        for (Entity entity : ((ServerLevel) (Object) this).getAllEntities()) {
            MovementTrace.applyMotionTag(entity);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cbaTest$traceEntities(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        for (Entity entity : ((ServerLevel) (Object) this).getAllEntities()) {
            MovementTrace.log(entity, "tick");
        }
    }
}
