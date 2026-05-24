package com.piecemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles reading and writing {@link ModConfig} to {@code config/piecemod.json}.
 * Gson is used because it ships with Minecraft and needs no extra dependency.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "piecemod.json";

    private ConfigManager() {}

    /** Loads config from disk into the {@link ModConfig} singleton. Silent on missing file. */
    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;

            ModConfig c = ModConfig.getInstance();
            if (json.has("hudX"))            c.setHudX(json.get("hudX").getAsInt());
            if (json.has("hudY"))            c.setHudY(json.get("hudY").getAsInt());
            if (json.has("backgroundColor")) c.setBackgroundColor(json.get("backgroundColor").getAsInt());
            if (json.has("autoFetchEnabled"))       c.setAutoFetchEnabled(json.get("autoFetchEnabled").getAsBoolean());
            if (json.has("autoFetchIntervalSeconds")) c.setAutoFetchIntervalSeconds(json.get("autoFetchIntervalSeconds").getAsInt());
            if (json.has("bossEnabled")) {
                json.getAsJsonObject("bossEnabled")
                    .entrySet()
                    .forEach(e -> c.setBossEnabled(e.getKey(), e.getValue().getAsBoolean()));
            }
            if (json.has("customCommands")) {
                json.getAsJsonArray("customCommands")
                    .forEach(el -> c.addCustomCommand(el.getAsString()));
            }
        } catch (IOException ignored) {
            // Silently skip; default config will be used
        }
    }

    /** Persists {@code config} to disk. Creates parent directories as needed. */
    public static void save(ModConfig config) {
        JsonObject json = new JsonObject();
        json.addProperty("hudX",            config.getHudX());
        json.addProperty("hudY",            config.getHudY());
        json.addProperty("backgroundColor", config.getBackgroundColor());
        json.addProperty("autoFetchEnabled",        config.isAutoFetchEnabled());
        json.addProperty("autoFetchIntervalSeconds", config.getAutoFetchIntervalSeconds());

        JsonObject bosses = new JsonObject();
        config.getBossEnabled().forEach(bosses::addProperty);
        json.add("bossEnabled", bosses);

        JsonArray customCmds = new JsonArray();
        config.getCustomCommands().forEach(customCmds::add);
        json.add("customCommands", customCmds);

        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException ignored) {
            // Save failure is non-fatal; settings will be reset on next launch
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}
