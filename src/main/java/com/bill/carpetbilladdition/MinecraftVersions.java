package com.bill.carpetbilladdition;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

/**
 * Minecraft version checks; safe to use before game classes are loaded, e.g. from the mixin plugin.
 */
public final class MinecraftVersions {
    private MinecraftVersions() {
    }

    public static boolean atLeast(String version) {
        try {
            Version minecraft = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion();
            return minecraft.compareTo(Version.parse(version)) >= 0;
        } catch (VersionParsingException e) {
            throw new IllegalStateException(e);
        }
    }
}
