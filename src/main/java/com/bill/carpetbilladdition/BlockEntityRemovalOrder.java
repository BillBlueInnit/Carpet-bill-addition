package com.bill.carpetbilladdition;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;

/**
 * 1.21.1 moved {@code super.onRemove}, which removes the block entity, in front of the neighbor updates that
 * sculk sensors, lecterns, shulker boxes and chiseled bookshelves send when they are removed. Until 1.21 the
 * updates came first, so suppressing them left the block entity behind under the new block. That is how illegal
 * sculk sensor (sound suppression), shulker box (CCE suppression), lectern and chiseled bookshelf block entities
 * were made.
 * <p>
 * With the rule on, the block entity removal is postponed to the end of {@code onRemove}, after the updates.
 */
public final class BlockEntityRemovalOrder {
    private BlockEntityRemovalOrder() {
    }

    public static void removeOrPostpone(Runnable removal, LocalRef<Runnable> postponed) {
        if (CbaSettings.reintroduceIllegalBlockEntity) {
            postponed.set(removal);
        } else {
            removal.run();
        }
    }

    public static void runPostponed(LocalRef<Runnable> postponed) {
        Runnable removal = postponed.get();
        if (removal != null) {
            removal.run();
        }
    }
}
