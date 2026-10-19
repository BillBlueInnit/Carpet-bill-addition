package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.CbaSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Until 1.21.4 a lectern with a book that was removed dropped the book and then cleared itself, which updates the
 * comparators next to it before its block entity is deleted. Update suppression reached through such a comparator
 * (for example one that breaks because nothing is under it) kept the lectern block entity, the usual way to swap it
 * under a shulker box for a CCE box. Since 1.21.5 the book is only copied out, so that update is gone.
 * <p>
 * With reintroduceIllegalBlockEntity on, the lectern clears itself again after dropping the book.
 */
@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin {
    @Shadow
    public abstract void clearContent();

    @Inject(method = "preRemoveSideEffects", at = @At("TAIL"))
    private void cba$clearBookLikeBefore1215(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (CbaSettings.reintroduceIllegalBlockEntity && state.getValue(LecternBlock.HAS_BOOK)) {
            this.clearContent();
        }
    }
}
