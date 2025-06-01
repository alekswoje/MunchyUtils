package munchyutils.client;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import java.util.List;
import java.io.InputStreamReader;

public class FishingHudSession extends HudSessionBase {
    public long startTime = 0;
    public boolean isActive = false;
    public int totalRewards = 0;
    public int totalXP = 0;
    public Map<String, Integer> rewardCounts = new HashMap<>();
    public Map<String, Integer> categoryCounts = new HashMap<>();
    public int casts = 0;
    private boolean lastRewardWasNewCast = false;
    public int playerLevel = -1;
    public int playerXP = -1;
    private String playerName = null;
    private static final String STATS_FILE = "fishing_stats.json";
    private static final Gson GSON = new Gson();
    private static Map<String, Integer> xpTable = null;
    private long lastCatchTime = 0;

    public void reset() {
        startTime = 0;
        isActive = false;
        totalRewards = 0;
        totalXP = 0;
        rewardCounts.clear();
        categoryCounts.clear();
        resetAfk();
    }

    public void addReward(String reward, String category) {
        if (!isActive) {
            startTime = System.currentTimeMillis();
            isActive = true;
        }
        lastCatchTime = System.currentTimeMillis();
        if (!lastRewardWasNewCast) {
            casts++;
            lastRewardWasNewCast = true;
        }
        totalRewards++;
        rewardCounts.put(reward, rewardCounts.getOrDefault(reward, 0) + 1);
        if (category != null) {
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }
    }

    public void endCast() {
        lastRewardWasNewCast = false;
    }

    public int getCasts() {
        return casts;
    }

    public int getRewards() {
        return totalRewards;
    }

    public void addXP(int xp) {
        if (!isActive) {
            startTime = System.currentTimeMillis();
            isActive = true;
        }
        lastCatchTime = System.currentTimeMillis();
        totalXP += xp;
        // If we have playerXP loaded, increment it and handle level up
        if (playerXP >= 0 && playerLevel >= 0) {
            playerXP += xp;
            boolean leveled = false;
            while (playerLevel < 99 && playerXP >= getXPForLevel(playerLevel + 1)) {
                playerLevel++;
                leveled = true;
            }
            if (leveled) saveStats();
        }
    }

    public double getRewardsPerHour() {
        if (!isActive) return 0;
        long duration = getActiveSessionSeconds(startTime);
        if (duration <= 0) return 0;
        return (totalRewards / (double)duration) * 3600;
    }

    public double getXPPerHour() {
        if (!isActive) return 0;
        long duration = getActiveSessionSeconds(startTime);
        if (duration <= 0) return 0;
        return (totalXP / (double)duration) * 3600;
    }

    public String getSessionLengthString() {
        if (!isActive) return "Session: 0m 0s";
        long sessionLength = getActiveSessionSeconds(startTime);
        if (sessionLength < 0) sessionLength = 0;
        return String.format("Session: %dm %ds", sessionLength / 60, sessionLength % 60);
    }

    public void setPlayerName(String name) { this.playerName = name; }
    public String getPlayerName() { return playerName; }
    public void setLevelAndXP(int level, int xp) {
        this.playerLevel = level;
        this.playerXP = xp;
        saveStats();
    }
    public void updateXP(int delta) {
        if (playerXP >= 0) {
            playerXP += delta;
            // Level up if needed
            while (playerLevel < 99 && playerXP >= getXPForLevel(playerLevel + 1)) {
                playerLevel++;
            }
            saveStats();
        }
    }
    public int getXPForLevel(int level) {
        if (xpTable == null) loadXPTable();
        return xpTable.getOrDefault(String.valueOf(level), 0);
    }
    public int getXPToNextLevel() {
        if (playerLevel < 0 || playerXP < 0) return -1;
        if (playerLevel >= 99) return 0;
        int xpToNext = getXPForLevel(playerLevel) - playerXP;
        return Math.max(0, xpToNext);
    }
    public double getPercentToNextLevel() {
        if (playerLevel < 0 || playerXP < 0) return 0.0;
        if (playerLevel >= 99) return 100.0;
        int req = getXPForLevel(playerLevel);
        if (req <= 0) return 100.0;
        return Math.max(0, Math.min(100, (playerXP * 100.0) / req));
    }
    public double getTimeToNextLevelHours(double xpPerHour) {
        int toNext = getXPToNextLevel();
        if (toNext <= 0 || xpPerHour <= 0) return -1;
        return toNext / xpPerHour;
    }
    public String getTimeToNextLevelString(double xpPerHour) {
        double hours = getTimeToNextLevelHours(xpPerHour);
        if (hours < 0) return null;
        int h = (int) hours;
        int m = (int) Math.round((hours - h) * 60);
        if (h > 0) {
            return String.format("Time to next: %dh %dm", h, m);
        } else {
            return String.format("Time to next: %dm", m);
        }
    }
    public void saveStats() {
        try {
            File file = getStatsFile();
            FileWriter writer = new FileWriter(file);
            GSON.toJson(new PlayerFishingStats(playerName, playerLevel, playerXP), writer);
            writer.close();
        } catch (IOException e) {
            // ignore
        }
    }
    public void loadStats() {
        try {
            File file = getStatsFile();
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                PlayerFishingStats stats = GSON.fromJson(reader, PlayerFishingStats.class);
                reader.close();
                if (stats != null) {
                    this.playerName = stats.playerName;
                    this.playerLevel = stats.playerLevel;
                    this.playerXP = stats.playerXP;
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }
    private File getStatsFile() {
        File dir = MinecraftClient.getInstance().runDirectory;
        return new File(dir, STATS_FILE);
    }
    private void loadXPTable() {
        try {
            InputStreamReader reader = new InputStreamReader(FishingHudSession.class.getClassLoader().getResourceAsStream("fishing_xp_table.json"));
            xpTable = new Gson().fromJson(reader, new com.google.gson.reflect.TypeToken<Map<String, Integer>>(){}.getType());
            reader.close();
        } catch (Exception e) {
            xpTable = new java.util.HashMap<>();
        }
    }
    // Helper class for JSON
    private static class PlayerFishingStats {
        String playerName;
        int playerLevel;
        int playerXP;
        PlayerFishingStats(String n, int l, int x) { playerName = n; playerLevel = l; playerXP = x; }
    }
    public void saveStatsToFile(String playerName, int level, int xp) {
        try {
            File dir = MinecraftClient.getInstance().runDirectory;
            File file = new File(dir, STATS_FILE);
            Map<String, Object> data = new HashMap<>();
            data.put("playerName", playerName);
            data.put("level", level);
            data.put("xp", xp);
            FileWriter writer = new FileWriter(file);
            GSON.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[FishingHudSession] Failed to save stats: " + e.getMessage());
        }
    }
    public void tickTimeout() {
        if (!isActive) return;
        MunchyConfig config = munchyutils.client.MunchyConfig.get();
        if (!config.isFishingHudSessionTimeoutEnabled()) return;
        int timeout = config.getFishingHudSessionTimeoutMs();
        long now = System.currentTimeMillis();
        if (now - lastCatchTime > timeout) { // session reset
            // Reset session stats, but not playerLevel/playerXP
            startTime = 0;
            isActive = false;
            totalRewards = 0;
            totalXP = 0;
            rewardCounts.clear();
            categoryCounts.clear();
            casts = 0;
            lastRewardWasNewCast = false;
        }
    }
} 