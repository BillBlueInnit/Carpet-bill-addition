package com.bill.carpetbilladdition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import carpet.utils.Translations;
import com.bill.carpetbilladdition.CbaSettings;
import com.bill.carpetbilladdition.PlayerPrefixes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TeamColorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

/**
 * /playerprefix &lt;player&gt; [&lt;prefix&gt; [&lt;color&gt;]], enabled by commandPlayerPrefix.
 * <p>
 * Operators can set the prefix of anyone. Other players can only set their own prefix and the prefix of Carpet fake
 * players. An empty prefix ("") removes it.
 */
public final class PlayerPrefixCommand {
    private PlayerPrefixCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playerprefix")
                .requires(source -> CommandHelper.canUseCommand(source, CbaSettings.commandPlayerPrefix))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), builder))
                        .executes(context -> show(context.getSource(), StringArgumentType.getString(context, "player")))
                        .then(Commands.argument("prefix", StringArgumentType.string())
                                .executes(context -> set(context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "prefix"),
                                        null))
                                .then(Commands.argument("color", TeamColorArgument.teamColor())
                                        .executes(context -> set(context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "prefix"),
                                                TeamColorArgument.getTeamColor(context, "color")))))));
    }

    private static int show(CommandSourceStack source, String name) {
        String player = realName(source, name);
        PlayerPrefixes.Prefix prefix = PlayerPrefixes.get(player);
        if (prefix == null) {
            source.sendSuccess(() -> Component.literal(tr("none", player)), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal(tr("show", player)).append(prefix.component())
                .append(Component.literal(" (" + (prefix.color() != null ? prefix.color().getSerializedName() : "default") + ")")), false);
        return 1;
    }

    private static int set(CommandSourceStack source, String name, String text, @Nullable TeamColor color) throws CommandSyntaxException {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
        String player = target != null ? target.getScoreboardName() : name;
        if (!CommandHelper.canUseCommand(source, "ops")) {
            boolean self = source.getEntity() == target && target != null;
            if (!self && !(target instanceof EntityPlayerMPFake)) {
                throw error("not_allowed");
            }
        }
        String prefix = text.replace("§", "");
        if (prefix.isEmpty()) {
            boolean removed = PlayerPrefixes.remove(source.getServer(), player);
            source.sendSuccess(() -> Component.literal(tr(removed ? "removed" : "none", player)), false);
            return removed ? 1 : 0;
        }
        if (prefix.length() > PlayerPrefixes.MAX_LENGTH) {
            throw new SimpleCommandExceptionType(Component.literal(tr("too_long", PlayerPrefixes.MAX_LENGTH))).create();
        }
        PlayerPrefixes.set(source.getServer(), player, prefix, color);
        PlayerPrefixes.Prefix stored = PlayerPrefixes.get(player);
        source.sendSuccess(() -> Component.literal(tr("set", player)).append(stored.component()).append(Component.literal(player)), false);
        PlayerTeam otherTeam = PlayerPrefixes.otherTeam(source.getServer(), player);
        if (otherTeam != null) {
            source.sendSuccess(() -> Component.literal(tr("other_team", player, otherTeam.getName())).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static String realName(CommandSourceStack source, String name) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(name);
        return player != null ? player.getScoreboardName() : name;
    }

    private static CommandSyntaxException error(String key) {
        return new SimpleCommandExceptionType(Component.literal(tr(key))).create();
    }

    private static String tr(String key, Object... args) {
        return String.format(Translations.tr("carpet.bill.playerprefix." + key), args);
    }
}
