package munchyutils.client;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CooldownManager {
    private static final Map<String, Long> cooldowns = new HashMap<>();
    private static final List<CooldownTrigger> triggers = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static final String COOLDOWN_FILE = "munchyutils_cooldowns.json";
    private static final String TRIGGERS_FILE = "munchyutils_triggers.json";

    public static void startCooldown(String name, long cooldownMs) {
        cooldowns.put(name, System.currentTimeMillis() + cooldownMs);
        saveCooldowns();
    }

    public static long getRemaining(String name) {
        return Math.max(0, (cooldowns.getOrDefault(name, 0L) - System.currentTimeMillis()) / 1000);
    }

    public static Map<String, Long> getAllCooldowns() {
        Map<String, Long> all = new HashMap<>();
        long now = System.currentTimeMillis();
        for (CooldownTrigger trigger : triggers) {
            long remaining = Math.max(0, (cooldowns.getOrDefault(trigger.name, 0L) - now) / 1000);
            all.put(trigger.name, remaining);
        }
        return all;
    }

    public static void addTrigger(CooldownTrigger trigger) {
        triggers.removeIf(t -> t.name.equalsIgnoreCase(trigger.name));
        triggers.add(trigger);
        saveTriggers();
    }

    public static boolean removeTrigger(String name) {
        boolean removed = triggers.removeIf(t -> t.name.equalsIgnoreCase(name));
        if (removed) saveTriggers();
        return removed;
    }

    public static List<CooldownTrigger> getTriggers() {
        return new ArrayList<>(triggers);
    }

    public static CooldownTrigger getTriggerByName(String name) {
        for (CooldownTrigger t : triggers) {
            if (t.name.equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    public static void saveCooldowns() {
        try {
            File file = getFile(COOLDOWN_FILE);
            FileWriter writer = new FileWriter(file);
            GSON.toJson(cooldowns, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[CooldownManager] Failed to save cooldowns: " + e.getMessage());
        }
    }

    public static void saveTriggers() {
        try {
            File file = getFile(TRIGGERS_FILE);
            FileWriter writer = new FileWriter(file);
            GSON.toJson(triggers, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[CooldownManager] Failed to save triggers: " + e.getMessage());
        }
    }

    public static void load() {
        // Load cooldowns
        try {
            File file = getFile(COOLDOWN_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                Map<String, Double> loaded = GSON.fromJson(reader, new TypeToken<Map<String, Double>>(){}.getType());
                reader.close();
                if (loaded != null) {
                    cooldowns.clear();
                    for (var entry : loaded.entrySet()) {
                        cooldowns.put(entry.getKey(), entry.getValue().longValue());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[CooldownManager] Failed to load cooldowns: " + e.getMessage());
        }
        // Load triggers
        try {
            File file = getFile(TRIGGERS_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                List<CooldownTrigger> loaded = GSON.fromJson(reader, new TypeToken<List<CooldownTrigger>>(){}.getType());
                reader.close();
                if (loaded != null) {
                    triggers.clear();
                    triggers.addAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("[CooldownManager] Failed to load triggers: " + e.getMessage());
        }
        // Add default triggers if none exist
        if (triggers.isEmpty()) {
            // Try to load from resources
            try (InputStreamReader reader = new InputStreamReader(
                    CooldownManager.class.getClassLoader().getResourceAsStream("default_cooldown_triggers.json"),
                    StandardCharsets.UTF_8)) {
                if (reader != null) {
                    List<CooldownTrigger> defaults = GSON.fromJson(reader, new TypeToken<List<CooldownTrigger>>(){}.getType());
                    if (defaults != null) {
                        triggers.addAll(defaults);
                        saveTriggers();
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("[CooldownManager] Failed to load default triggers from resources: " + e.getMessage());
            }
            // Fallback: (optional) could log or add a warning here
        }
    }

    private static File getFile(String name) {
        File dir = MinecraftClient.getInstance().runDirectory;
        return new File(dir, name);
    }

    static {
        // Save on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> saveCooldowns());
    }

    // Poll for ready cooldowns (for HUD sound)
    private static final Map<String, Boolean> wasReady = new HashMap<>();
    public static String pollReadyCooldown() {
        Map<String, Long> all = getAllCooldowns();
        for (String key : all.keySet()) {
            boolean ready = all.get(key) == 0;
            boolean prev = wasReady.getOrDefault(key, true);
            wasReady.put(key, ready);
            if (ready && !prev) {
                return key;
            }
        }
        return null;
    }
} 