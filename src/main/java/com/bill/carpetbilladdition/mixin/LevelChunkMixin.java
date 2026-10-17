package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.CbaSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.1 started to delete a block entity whose type does not match the block placed at its position.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
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
}
