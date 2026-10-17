package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.CbaSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    @Final
    Level level;

    /**
     * 1.21.1 started to delete a block entity whose type does not match the block placed at its position.
     */
    @ModifyExpressionValue(
            method = "setBlockState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;isValidBlockState(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean cba$keepIllegalBlockEntity(boolean valid) {
        return valid || CbaSettings.reintroduceIllegalBlockEntity;
    }

    /**
     * Since 1.21.5 the block entity of a removed block is deleted before {@code affectNeighborsAfterRemoval}
     * (formerly part of {@code onRemove}) sends its neighbor updates. Until 1.21 those updates came first, so
     * suppressing them left the block entity behind under the new block, which is how illegal block entities are made.
     * <p>
     * With the rule on, the neighbor updates are sent first again and the block entity is deleted afterwards.
     */
    @WrapOperation(
            method = "setBlockState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V",
                    ordinal = 0
            )
    )
    private void cba$updateNeighborsBeforeRemovingBlockEntity(
            LevelChunk chunk, BlockPos pos, Operation<Void> original,
            @Local(argsOnly = true) int flags, @Local(ordinal = 1) BlockState oldState,
            @Share("neighborsUpdated") LocalBooleanRef neighborsUpdated
    ) {
        boolean movedByPiston = (flags & 64) != 0;
        if (CbaSettings.reintroduceIllegalBlockEntity
                && this.level instanceof ServerLevel serverLevel
                && ((flags & 1) != 0 || movedByPiston)) {
            neighborsUpdated.set(true);
            oldState.affectNeighborsAfterRemoval(serverLevel, pos, movedByPiston);
        }
        original.call(chunk, pos);
    }

    @WrapWithCondition(
            method = "setBlockState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;affectNeighborsAfterRemoval(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Z)V"
            )
    )
    private boolean cba$skipNeighborUpdatesSentEarlier(
            BlockState oldState, ServerLevel level, BlockPos pos, boolean movedByPiston,
            @Share("neighborsUpdated") LocalBooleanRef neighborsUpdated
    ) {
        return !neighborsUpdated.get();
    }
}
