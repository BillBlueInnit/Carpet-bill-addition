package com.bill.carpetbilladdition.testmod;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Development-only commands for the server tests.
 * <p>
 * /cbatest as &lt;player&gt; &lt;command&gt; runs a command as that player, with the player's own permission level
 * (unlike /execute as, which keeps the caller's), and replies with everything the command said.
 */
public final class CbaTestMod implements ModInitializer, CarpetExtension {
    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        dispatcher.register(Commands.literal("cbatest")
                .requires(source -> CommandHelper.canUseCommand(source, "4"))
                .then(Commands.literal("as")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(context -> runAs(context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "command")))))));
    }

    private static int runAs(CommandSourceStack source, String name, String command) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(name);
        if (player == null) {
            source.sendFailure(Component.literal("No player " + name));
            return 0;
        }
        StringBuilder output = new StringBuilder();
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                output.append(message.getString()).append(" | ");
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        // Straight through the dispatcher: performPrefixedCommand would only queue it behind this command
        try {
            source.getServer().getCommands().getDispatcher().execute(command, player.createCommandSourceStack().withSource(capture));
        } catch (CommandSyntaxException e) {
            output.append("ERROR: ").append(e.getMessage());
        }
        source.sendSuccess(() -> Component.literal("as " + name + ": " + output), false);
        return 1;
    }
}
