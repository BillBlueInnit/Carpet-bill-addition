package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.SoundSuppressors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A sculk sensor block entity that keeps its place under another block becomes a sound suppressor when it gets that
 * block's state, see {@link SoundSuppressors}.
 */
@Mixin(BlockEntity.class)
public abstract class SoundSuppressorBlockEntityMixin {
    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void cba$updateSoundSuppressor(BlockState blockState, CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity instanceof SculkSensorBlockEntity sensor && blockEntity.getLevel() instanceof ServerLevel level && !blockEntity.isRemoved()) {
            SoundSuppressors.update(level, sensor);
        }
    }
}
