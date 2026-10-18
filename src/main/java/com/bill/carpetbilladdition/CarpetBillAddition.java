package com.bill.carpetbilladdition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.bill.carpetbilladdition.command.PlayerPrefixCommand;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarpetBillAddition implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "carpet-bill-addition";
    public static final String MOD_NAME = "Carpet Bill Addition";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static String version = "unknown";

    @Override
    public void onInitialize() {
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(version);
        CarpetServer.manageExtension(this);
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(CbaSettings.class);
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        PlayerPrefixes.load(server);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        PlayerPrefixes.unload();
    }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        PlayerPrefixes.apply(CarpetServer.minecraft_server, player.getScoreboardName());
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        PlayerPrefixCommand.register(dispatcher);
    }

    @Override
    public String version() {
        return version;
    }

    public static String getVersion() {
        return version;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        String path = "/assets/" + MOD_ID + "/lang/" + lang + ".json";
        try (InputStream stream = CarpetBillAddition.class.getResourceAsStream(path)) {
            if (stream == null) {
                return Collections.emptyMap();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> translations = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                return translations != null ? translations : Collections.emptyMap();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load {} translations", lang, e);
            return Collections.emptyMap();
        }
    }
}
