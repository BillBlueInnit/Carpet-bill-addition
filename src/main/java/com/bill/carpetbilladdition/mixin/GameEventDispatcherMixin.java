package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.SoundSuppressors;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * soundSuppressionRange: offers game events to sound suppressors in sections vanilla did not look at,
 * see {@link SoundSuppressors}.
 */
@Mixin(GameEventDispatcher.class)
public abstract class GameEventDispatcherMixin {
    @Shadow
    @Final
    private ServerLevel level;

    // Right after the section scan, before the listeners that want events by distance get theirs
    @Inject(method = "post", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private void cba$postToFarSoundSuppressors(Holder<GameEvent> event, Vec3 source, GameEvent.Context context, CallbackInfo ci) {
        SoundSuppressors.postToSuppressorsOutsideScannedSections(this.level, event, source, context);
    }
}
