package com.bill.carpetbilladdition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

/**
 * Player name prefixes set with /playerprefix, shown as "[prefix]name".
 * <p>
 * Vanilla clients only draw a team prefix above a player's head, so a player with a prefix is put into a team of
 * their own, {@code cba_prefix_<name>}, whose prefix is "[prefix]". The team also shows it in chat, the player list
 * and /list. A player who is already in another team keeps that team; their prefix is then added to the name used in
 * chat and the player list instead (see the display name mixins), and cannot be shown above the head.
 * <p>
 * Prefixes are saved per world in {@code carpet-bill-addition/player_prefixes.json} and applied again when a player
 * logs in, so they also survive teams being removed by hand.
 */
public final class PlayerPrefixes {
    public static final int MAX_LENGTH = 32;
    private static final String TEAM_PREFIX = "cba_prefix_";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Prefix> PREFIXES = new LinkedHashMap<>();
    private static Path file;

    private PlayerPrefixes() {
    }

    public record Prefix(String player, String text, @Nullable ChatFormatting color) {
        public MutableComponent component() {
            MutableComponent component = Component.literal("[" + this.text + "]");
            return this.color != null ? component.withStyle(this.color) : component;
        }
    }

    public static void load(MinecraftServer server) {
        PREFIXES.clear();
        file = server.getWorldPath(LevelResource.ROOT).resolve(CarpetBillAddition.MOD_ID).resolve("player_prefixes.json");
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            for (JsonElement element : GSON.fromJson(reader, JsonArray.class)) {
                JsonObject entry = element.getAsJsonObject();
                ChatFormatting color = entry.has("color") ? ChatFormatting.getByName(entry.get("color").getAsString()) : null;
                Prefix prefix = new Prefix(entry.get("player").getAsString(), entry.get("prefix").getAsString(), color);
                PREFIXES.put(key(prefix.player()), prefix);
            }
        } catch (IOException | RuntimeException e) {
            CarpetBillAddition.LOGGER.error("Could not read player prefixes from {}", file, e);
        }
    }

    public static void unload() {
        PREFIXES.clear();
        file = null;
    }

    @Nullable
    public static Prefix get(String player) {
        return PREFIXES.get(key(player));
    }

    public static void set(MinecraftServer server, String player, String text, @Nullable ChatFormatting color) {
        PREFIXES.put(key(player), new Prefix(player, text, color));
        save();
        apply(server, player);
    }

    public static boolean remove(MinecraftServer server, String player) {
        boolean removed = PREFIXES.remove(key(player)) != null;
        if (removed) {
            save();
        }
        apply(server, player);
        return removed;
    }

    /**
     * Puts the player into their prefix team, or takes the team away, and refreshes their name in the player list.
     */
    public static void apply(MinecraftServer server, String player) {
        ServerScoreboard scoreboard = server.getScoreboard();
        String teamName = teamName(player);
        PlayerTeam ownTeam = scoreboard.getPlayerTeam(teamName);
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(player);
        Prefix prefix = get(player);
        if (prefix != null && (currentTeam == null || currentTeam == ownTeam)) {
            if (ownTeam == null) {
                ownTeam = scoreboard.addPlayerTeam(teamName);
            }
            ownTeam.setPlayerPrefix(prefix.component());
            if (currentTeam != ownTeam) {
                scoreboard.addPlayerToTeam(player, ownTeam);
            }
        } else if (ownTeam != null) {
            scoreboard.removePlayerTeam(ownTeam);
        }
        ServerPlayer online = server.getPlayerList().getPlayerByName(player);
        if (online != null) {
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, online));
        }
    }

    /**
     * The other team a prefixed player is in, if any: the prefix is then not shown above their head.
     */
    @Nullable
    public static PlayerTeam otherTeam(MinecraftServer server, String player) {
        PlayerTeam team = server.getScoreboard().getPlayersTeam(player);
        return team != null && !team.getName().equals(teamName(player)) ? team : null;
    }

    /**
     * Whether the prefix has to be added to the display name, because the player is not in their prefix team.
     */
    public static boolean needsDisplayNamePrefix(ServerPlayer player) {
        if (PREFIXES.isEmpty() || get(player.getScoreboardName()) == null) {
            return false;
        }
        PlayerTeam team = player.getTeam();
        return team == null || !team.getName().equals(teamName(player.getScoreboardName()));
    }

    public static Component decorate(ServerPlayer player, Component displayName) {
        if (!needsDisplayNamePrefix(player)) {
            return displayName;
        }
        return Component.empty().append(get(player.getScoreboardName()).component()).append(displayName);
    }

    private static void save() {
        if (file == null) {
            return;
        }
        JsonArray array = new JsonArray();
        for (Prefix prefix : PREFIXES.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("player", prefix.player());
            entry.addProperty("prefix", prefix.text());
            if (prefix.color() != null) {
                entry.addProperty("color", prefix.color().getName());
            }
            array.add(entry);
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
        } catch (IOException e) {
            CarpetBillAddition.LOGGER.error("Could not save player prefixes to {}", file, e);
        }
    }

    private static String teamName(String player) {
        return TEAM_PREFIX + key(player);
    }

    private static String key(String player) {
        return player.toLowerCase(Locale.ROOT);
    }
}
