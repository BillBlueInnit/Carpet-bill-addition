package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.SoundSuppressors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks sound suppressors as their game event listeners are registered (placement, chunk load) and unregistered,
 * see {@link SoundSuppressors}.
 */
@Mixin(LevelChunk.class)
public abstract class SoundSuppressorLevelChunkMixin {
    @Inject(method = "addGameEventListener", at = @At("TAIL"))
    private void cba$trackSoundSuppressor(BlockEntity blockEntity, ServerLevel level, CallbackInfo ci) {
        if (blockEntity instanceof SculkSensorBlockEntity sensor) {
            SoundSuppressors.update(level, sensor);
        }
    }

    @Inject(method = "removeGameEventListener", at = @At("TAIL"))
    private void cba$untrackSoundSuppressor(BlockEntity blockEntity, ServerLevel level, CallbackInfo ci) {
        if (blockEntity instanceof SculkSensorBlockEntity sensor) {
            SoundSuppressors.remove(level, sensor);
        }
    }
}
