package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.CbaSettings;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 started to throw when a block entity is constructed with, or given, a block state of another block.
 * That kills illegal block entities when their chunk is loaded and when their block is replaced.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "validateBlockState", at = @At("HEAD"), cancellable = true)
    private void cba$allowIllegalBlockState(BlockState blockState, CallbackInfo ci) {
        if (CbaSettings.reintroduceIllegalBlockEntity) {
            ci.cancel();
        }
    }
}
