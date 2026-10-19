package com.bill.carpetbilladdition.mixin;

import java.util.List;
import java.util.Set;
import com.bill.carpetbilladdition.MinecraftVersions;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Minecraft 1.21 still has illegal block entities in vanilla, so the patches only apply from 1.21.1 on.
 */
public final class CbaMixinPlugin implements IMixinConfigPlugin {
    // The mixins that undo the 1.21.1 changes; everything else applies to 1.21 as well
    private static final Set<String> ILLEGAL_BLOCK_ENTITY_PATCHES = Set.of(
            "com.bill.carpetbilladdition.mixin.BlockEntityMixin",
            "com.bill.carpetbilladdition.mixin.LevelChunkMixin",
            "com.bill.carpetbilladdition.mixin.ChiseledBookShelfBlockMixin",
            "com.bill.carpetbilladdition.mixin.LecternBlockMixin",
            "com.bill.carpetbilladdition.mixin.SculkSensorBlockMixin",
            "com.bill.carpetbilladdition.mixin.ShulkerBoxBlockMixin"
    );

    private boolean vanillaHasIllegalBlockEntities;

    @Override
    public void onLoad(String mixinPackage) {
        this.vanillaHasIllegalBlockEntities = !MinecraftVersions.atLeast("1.21.1");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !this.vanillaHasIllegalBlockEntities || !ILLEGAL_BLOCK_ENTITY_PATCHES.contains(mixinClassName);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
