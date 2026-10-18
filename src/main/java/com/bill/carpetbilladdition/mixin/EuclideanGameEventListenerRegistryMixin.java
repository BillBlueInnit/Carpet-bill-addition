package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.SoundSuppressors;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * soundSuppressionRange: the radius sound suppressors hear vibrations in, see {@link SoundSuppressors}.
 */
@Mixin(EuclideanGameEventListenerRegistry.class)
public abstract class EuclideanGameEventListenerRegistryMixin {
    @ModifyExpressionValue(
            method = "getPostableListenerPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/GameEventListener;getListenerRadius()I")
    )
    private static int cba$soundSuppressorRadius(
            int radius, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) GameEventListener listener
    ) {
        return SoundSuppressors.listenerRadius(level, listener, radius);
    }
}
