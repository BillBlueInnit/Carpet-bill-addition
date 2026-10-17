package com.bill.carpetbilladdition.testmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development-only update suppressor: a barrier placed on a target block throws when it receives a neighbor update.
 * It stands in for any real update suppressor in the automated server test.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class TestUpdateSuppressorMixin {
    @Inject(method = "handleNeighborChanged", at = @At("HEAD"))
    private void cbaTest$suppress(Level level, BlockPos pos, Block block, Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        if (!level.isClientSide() && ((BlockState) (Object) this).is(Blocks.BARRIER) && level.getBlockState(pos.below()).is(Blocks.TARGET)) {
            throw new IllegalStateException("cba test update suppressor at " + pos.toShortString());
        }
    }
}
