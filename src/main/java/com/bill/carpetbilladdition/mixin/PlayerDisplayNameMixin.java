package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.PlayerPrefixes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds the /playerprefix prefix to the name used in chat and messages of a player who is in another team than their
 * prefix team. In their prefix team the team prefix shows it already.
 */
@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Component cba$addPlayerPrefix(Component displayName) {
        return (Object) this instanceof ServerPlayer player ? PlayerPrefixes.decorate(player, displayName) : displayName;
    }
}
