package com.bill.carpetbilladdition.mixin;

import com.bill.carpetbilladdition.PlayerPrefixes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Shows the /playerprefix prefix in the player list for a player who is in another team than their prefix team.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTabListMixin {
    @ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN"))
    private Component cba$addPlayerPrefix(Component tabListName) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        return tabListName == null && PlayerPrefixes.needsDisplayNamePrefix(player) ? player.getDisplayName() : tabListName;
    }
}
