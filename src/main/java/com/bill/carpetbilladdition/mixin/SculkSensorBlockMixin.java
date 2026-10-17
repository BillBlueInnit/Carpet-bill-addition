package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.BlockEntityRemovalOrder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @see BlockEntityRemovalOrder
 */
@Mixin(SculkSensorBlock.class)
public abstract class SculkSensorBlockMixin {
    @WrapOperation(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/BaseEntityBlock;onRemove(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"
            )
    )
    private void cba$postponeBlockEntityRemoval(
            SculkSensorBlock block, BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston,
            Operation<Void> original, @Share("removal") LocalRef<Runnable> removal
    ) {
        BlockEntityRemovalOrder.removeOrPostpone(() -> original.call(block, state, level, pos, newState, movedByPiston), removal);
    }

    @Inject(method = "onRemove", at = @At("TAIL"))
    private void cba$removeBlockEntityAfterUpdates(
            BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston,
            CallbackInfo ci, @Share("removal") LocalRef<Runnable> removal
    ) {
        BlockEntityRemovalOrder.runPostponed(removal);
    }
}
