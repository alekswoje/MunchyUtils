package munchyutils.client;

import java.util.EnumMap;
import java.util.Map;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

public class FeatureManager {
    public enum ModFeature {
        COOLDOWN_HUD(true),
        INFO_HUD(true);

        private boolean enabled;
        ModFeature(boolean defaultEnabled) {
            this.enabled = defaultEnabled;
        }
        public boolean isEnabled() { return enabled; }
        public void toggle() { enabled = !enabled; }
        public void setEnabled(boolean value) { enabled = value; }
    }

    private static final String CONFIG_FILE = "munchyutils_features.json";
    private static final Gson GSON = new Gson();
    private static final String POSITIONS_FILE = "munchyutils_hud_positions.json";
    private static final Map<ModFeature, int[]> hudPositions = new EnumMap<>(ModFeature.class);

    static {
        // Set defaults
        hudPositions.put(ModFeature.COOLDOWN_HUD, new int[]{-1, -1});
        hudPositions.put(ModFeature.INFO_HUD, new int[]{-1, -1});
        for (ModFeature f : ModFeature.values()) f.setEnabled(f.isEnabled());
        load();
        loadPositions();
    }

    public static boolean isEnabled(ModFeature feature) {
        return feature.isEnabled();
    }

    public static void toggle(ModFeature feature) {
        feature.toggle();
        save();
    }

    public static Map<ModFeature, Boolean> getAll() {
        Map<ModFeature, Boolean> allFeatures = new EnumMap<>(ModFeature.class);
        for (ModFeature f : ModFeature.values()) allFeatures.put(f, f.isEnabled());
        return allFeatures;
    }

    public static int[] getHudPosition(ModFeature feature) {
        return hudPositions.getOrDefault(feature, new int[]{-1, -1});
    }

    public static void setHudPosition(ModFeature feature, int x, int y) {
        hudPositions.put(feature, new int[]{x, y});
        savePositions();
    }

    private static void load() {
        try {
            File file = getFile();
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                Map<String, Boolean> loaded = GSON.fromJson(reader, new TypeToken<Map<String, Boolean>>(){}.getType());
                reader.close();
                if (loaded != null) {
                    for (ModFeature f : ModFeature.values()) {
                        if (loaded.containsKey(f.name())) f.setEnabled(loaded.get(f.name()));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[FeatureManager] Failed to load features: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            File file = getFile();
            FileWriter writer = new FileWriter(file);
            Map<String, Boolean> toSave = new java.util.HashMap<>();
            for (ModFeature f : ModFeature.values()) toSave.put(f.name(), f.isEnabled());
            GSON.toJson(toSave, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[FeatureManager] Failed to save features: " + e.getMessage());
        }
    }

    private static void loadPositions() {
        try {
            File file = getPositionsFile();
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                Map<String, int[]> loaded = GSON.fromJson(reader, new TypeToken<Map<String, int[]>>(){}.getType());
                reader.close();
                if (loaded != null) {
                    for (ModFeature f : ModFeature.values()) {
                        if (loaded.containsKey(f.name())) hudPositions.put(f, loaded.get(f.name()));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[FeatureManager] Failed to load HUD positions: " + e.getMessage());
        }
    }

    private static void savePositions() {
        try {
            File file = getPositionsFile();
            FileWriter writer = new FileWriter(file);
            Map<String, int[]> toSave = new java.util.HashMap<>();
            for (ModFeature f : ModFeature.values()) toSave.put(f.name(), hudPositions.getOrDefault(f, new int[]{-1, -1}));
            GSON.toJson(toSave, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[FeatureManager] Failed to save HUD positions: " + e.getMessage());
        }
    }

    private static File getFile() {
        File dir = MinecraftClient.getInstance().runDirectory;
        return new File(dir, CONFIG_FILE);
    }

    private static File getPositionsFile() {
        File dir = MinecraftClient.getInstance().runDirectory;
        return new File(dir, POSITIONS_FILE);
    }
} 