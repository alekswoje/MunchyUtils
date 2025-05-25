package munchyutils.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.function.Consumer;

public class MunchyConfig {
    private static final Path CONFIG_PATH = Paths.get("config/munchyutils.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MunchyConfig instance;

    public int infoHudX = -1;
    public int infoHudY = -1;
    public float infoHudScale = 1.0f;
    public int cooldownHudX = -1;
    public int cooldownHudY = -1;
    public float cooldownHudScale = 1.0f;

    private transient Consumer<MunchyConfig> onChange;

    public static MunchyConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public void setOnChange(Consumer<MunchyConfig> listener) {
        this.onChange = listener;
    }

    public void notifyChange() {
        if (onChange != null) onChange.accept(this);
    }

    public static MunchyConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    return GSON.fromJson(reader, MunchyConfig.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new MunchyConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters and setters for Info HUD
    public int getInfoHudX() { return infoHudX; }
    public void setInfoHudX(int x) { this.infoHudX = x; notifyChange(); save(); }
    public int getInfoHudY() { return infoHudY; }
    public void setInfoHudY(int y) { this.infoHudY = y; notifyChange(); save(); }
    public float getInfoHudScale() { return infoHudScale; }
    public void setInfoHudScale(float scale) { this.infoHudScale = scale; notifyChange(); save(); }

    // Getters and setters for Cooldown HUD
    public int getCooldownHudX() { return cooldownHudX; }
    public void setCooldownHudX(int x) { this.cooldownHudX = x; notifyChange(); save(); }
    public int getCooldownHudY() { return cooldownHudY; }
    public void setCooldownHudY(int y) { this.cooldownHudY = y; notifyChange(); save(); }
    public float getCooldownHudScale() { return cooldownHudScale; }
    public void setCooldownHudScale(float scale) { this.cooldownHudScale = scale; notifyChange(); save(); }
} 