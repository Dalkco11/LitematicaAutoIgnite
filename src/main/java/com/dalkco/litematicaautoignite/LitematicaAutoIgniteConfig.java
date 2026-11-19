package com.dalkco.litematicaautoignite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings stored in config/litematica-auto-ignite.json. */
public final class LitematicaAutoIgniteConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(LitematicaAutoIgniteMod.MOD_ID + ".json");

    private static Data data = new Data();

    public static final class Data {
        /** Light schematic nether portals automatically while holding flint and steel. */
        public boolean enabled = true;
        /** Only light portals inside Litematica's render layer range. */
        public boolean respectLayerRange = true;
        /** Play the hand swing animation when lighting a portal. */
        public boolean swingHand = true;
    }

    private LitematicaAutoIgniteConfig() {
    }

    public static Data get() {
        return data;
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                data = loaded;
            }
        } catch (Exception e) {
            LitematicaAutoIgniteMod.LOGGER.warn("Failed to read {}", FILE, e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            LitematicaAutoIgniteMod.LOGGER.warn("Failed to write {}", FILE, e);
        }
    }
}
