package munchyutils.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import munchyutils.client.FeatureManager;
import munchyutils.munchyutils.MunchyUtilsClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Items;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import munchyutils.client.MunchyConfig;

public class InfoHudOverlay extends BaseHudOverlay {
    public static final InfoHudSession session = new InfoHudSession();
    public static final FishingHudSession fishingSession = new FishingHudSession();
    private static Map<String, String> rewardCategoryMap = new LinkedHashMap<>();
    private static Map<String, String> categoryColorMap = new LinkedHashMap<>();
    private static int lastOverlayWidth = 120;
    private static int lastOverlayHeight = 48;
    static {
        try {
            InputStreamReader reader = new InputStreamReader(InfoHudOverlay.class.getClassLoader().getResourceAsStream("fishing_rewards.json"));
            rewardCategoryMap = new Gson().fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            reader.close();
        } catch (Exception e) {
            rewardCategoryMap = new LinkedHashMap<>();
        }
        try {
            InputStreamReader colorReader = new InputStreamReader(InfoHudOverlay.class.getClassLoader().getResourceAsStream("fishing_category_colors.json"));
            categoryColorMap = new Gson().fromJson(colorReader, new TypeToken<Map<String, String>>(){}.getType());
            colorReader.close();
        } catch (Exception e) {
            categoryColorMap = new LinkedHashMap<>();
        }
    }

    public static void register() {
        HudRenderCallback.EVENT.register((DrawContext context, RenderTickCounter tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) return;
            MunchyConfig config = MunchyConfig.get();
            float scale = config.getInfoHudScale();
            int x = config.getInfoHudX();
            int y = config.getInfoHudY();
            Window window = client.getWindow();
            int winW = window.getScaledWidth();
            int winH = window.getScaledHeight();
            int overlayWidth = (int)(lastOverlayWidth * scale);
            int overlayHeight = (int)(lastOverlayHeight * scale);
            // Clamp and default position logic
            if (x == -1) x = 10;
            if (y == -1) y = 10;
            x = Math.max(0, Math.min(x, winW - overlayWidth));
            y = Math.max(0, Math.min(y, winH - overlayHeight));
            // Tool detection logic FIRST
            boolean hasPickaxe = false, hasFishingRod = false;
            int pickaxeHotbarSlot = -1, fishingRodHotbarSlot = -1;
            for (int i = 0; i < client.player.getInventory().size(); i++) {
                ItemStack stack = client.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                if (isPickaxe(stack)) {
                    hasPickaxe = true;
                    if (i >= 0 && i < 9 && pickaxeHotbarSlot == -1) pickaxeHotbarSlot = i;
                }
                if (stack.getItem() == Items.FISHING_ROD) {
                    hasFishingRod = true;
                    if (i >= 0 && i < 9 && fishingRodHotbarSlot == -1) fishingRodHotbarSlot = i;
                }
            }
            boolean showMining = false, showFishing = false;
            if (hasPickaxe && hasFishingRod) {
                if (pickaxeHotbarSlot != -1 && fishingRodHotbarSlot != -1) {
                    showMining = pickaxeHotbarSlot <= fishingRodHotbarSlot;
                    showFishing = !showMining;
                } else if (pickaxeHotbarSlot != -1) {
                    showMining = true;
                } else if (fishingRodHotbarSlot != -1) {
                    showFishing = true;
                } else {
                    showMining = true;
                }
            } else if (hasPickaxe) {
                showMining = true;
            } else if (hasFishingRod) {
                showFishing = true;
            }
            if (!showMining && !showFishing) {
                return;
            }
            // If fishing is preferred, render only the fishing HUD (styled identically)
            if (showFishing) {
                TextRenderer textRenderer = client.textRenderer;
                int[] movePos = MunchyUtilsClient.getMoveHudPosition(FeatureManager.ModFeature.INFO_HUD);
                int[] pos = (movePos != null) ? movePos : munchyutils.client.FeatureManager.getHudPosition(munchyutils.client.FeatureManager.ModFeature.INFO_HUD);
                // Prepare lines for the overlay
                String rewardsHr = String.format("Rewards/hr: %.1f", fishingSession.getRewardsPerHour());
                String xpHr = String.format("XP/hr: %.1f", fishingSession.getXPPerHour());
                String sessionTime = fishingSession.getSessionLengthString();
                List<Map.Entry<String, Integer>> topRewards = fishingSession.rewardCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(2).collect(Collectors.toList());
                List<Map.Entry<String, Integer>> topCats = fishingSession.categoryCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(2).collect(Collectors.toList());
                String topRewardLine = topRewards.isEmpty() ? "" : topRewards.stream().map(e -> e.getKey() + "(" + e.getValue() + ")").collect(Collectors.joining(", ", "Top: ", ""));
                String topCatLine = topCats.isEmpty() ? "" : topCats.stream().map(e -> e.getKey() + "(" + e.getValue() + ")").collect(Collectors.joining(", ", "Cat: ", ""));
                // Build lines and colors
                String[] lines;
                int[] dotColors;
                boolean statsLoaded = munchyutils.munchyutils.MunchyUtilsClient.isFishingStatsLoaded();
                boolean statsOutOfSync = false;
                if (statsLoaded) {
                    int sessionXP = fishingSession.totalXP;
                    int storedXP = fishingSession.playerXP;
                    if (sessionXP > 0 && sessionXP > storedXP) {
                        statsOutOfSync = true;
                    }
                }
                if (fishingSession.isActive) {
                    String afkLine = fishingSession.isAfk ? "AFK" : "Active";
                    String rewardsLine = String.format("Rewards: %d (%.0f/h)", fishingSession.getRewards(), fishingSession.getRewardsPerHour());
                    String xpLine = String.format("XP: %d (%.1fk/h)", fishingSession.playerXP, fishingSession.getXPPerHour()/1000.0);
                    String levelLine = String.format("Level: %d (%.1f%%)", fishingSession.playerLevel, fishingSession.getPercentToNextLevel());
                    String sessionTimeStr = String.format("Session: %s", fishingSession.getSessionLengthString().replace("Session: ", ""));
                    String xpToNext = String.format("XP to next: %d", fishingSession.getXPToNextLevel());
                    String timeToNext = fishingSession.getTimeToNextLevelHours(fishingSession.getXPPerHour()) > 0 ? String.format("Time to next: %.1fh", fishingSession.getTimeToNextLevelHours(fishingSession.getXPPerHour())) : null;
                    String castsLine = String.format("Casts: %d", fishingSession.getCasts());
                    String warnLine = null;
                    if (!statsLoaded) {
                        warnLine = "Run /fish stats to enable XP tracking!";
                    } else if (statsOutOfSync) {
                        warnLine = "Out of sync, run /fish stats to update.";
                    }
                    java.util.List<String> lineList = new java.util.ArrayList<>();
                    lineList.add(rewardsLine);
                    lineList.add(xpLine);
                    lineList.add(levelLine);
                    lineList.add(sessionTimeStr);
                    lineList.add(xpToNext);
                    if (timeToNext != null) lineList.add(timeToNext);
                    lineList.add(castsLine);
                    lineList.add(afkLine);
                    if (warnLine != null) lineList.add(warnLine);
                    lines = lineList.stream().filter(s -> s != null && !s.isEmpty()).toArray(String[]::new);
                    dotColors = new int[lines.length];
                    for (int i = 0; i < lines.length; i++) {
                        if (warnLine != null && lines[i].equals(warnLine)) dotColors[i] = 0xFFFF6F61; // warning red/orange
                        else if (lines[i].startsWith("Rewards:")) dotColors[i] = 0xFF7ED957; // green for rewards
                        else if (lines[i].startsWith("XP:")) dotColors[i] = 0xFF6EC6FF; // blue for xp
                        else if (lines[i].startsWith("Level:")) dotColors[i] = 0xFFFFD966; // gold for level
                        else if (lines[i].startsWith("Session:")) dotColors[i] = 0xFFB4B4B4; // gray for session
                        else if (lines[i].startsWith("XP to next:")) dotColors[i] = 0xFF6EC6FF; // blue for xp to next
                        else if (lines[i].startsWith("Time to next:")) dotColors[i] = 0xFFB4B4B4; // gray for time to next
                        else if (lines[i].startsWith("Casts:")) dotColors[i] = 0xFFB4B4B4; // gray for casts
                        else if (lines[i].equals("AFK")) dotColors[i] = 0xFFFFD966; // gold for AFK
                        else if (lines[i].equals("Active")) dotColors[i] = 0xFF7ED957; // green for active
                        else dotColors[i] = 0xFFB4B4B4; // fallback gray
                    }
                } else {
                    lines = new String[] { "Catch a fish to start session!", "Session: 0m 0s" };
                    dotColors = new int[] { 0xFFA0E0FF, 0xFFE57373 };
                }
                // Filter out empty lines (except for placeholder, which is always one line)
                int numLines = lines.length;
                if (fishingSession.isActive) {
                    int nonEmptyCount = 0;
                    for (String line : lines) if (!line.isEmpty()) nonEmptyCount++;
                    String[] filteredLines = new String[nonEmptyCount];
                    int[] filteredColors = new int[nonEmptyCount];
                    int idx = 0;
                    for (int i = 0; i < lines.length; i++) {
                        if (!lines[i].isEmpty()) {
                            filteredLines[idx] = lines[i];
                            filteredColors[idx] = dotColors[i];
                            idx++;
                        }
                    }
                    lines = filteredLines;
                    dotColors = filteredColors;
                    numLines = lines.length;
                }
                int maxTextWidth = 0;
                for (String line : lines) {
                    int w = textRenderer.getWidth(line) + 18;
                    if (w > maxTextWidth) maxTextWidth = w;
                }
                if (munchyutils.munchyutils.MunchyUtilsClient.isMoveMode && munchyutils.munchyutils.MunchyUtilsClient.movingHudStatic == munchyutils.client.FeatureManager.ModFeature.INFO_HUD) {
                    x = munchyutils.munchyutils.MunchyUtilsClient.moveXStatic;
                    y = munchyutils.munchyutils.MunchyUtilsClient.moveYStatic;
                }
                x = Math.round((float)x / 5) * 5;
                y = Math.round((float)y / 5) * 5;
                x = Math.max(4, Math.min(x, winW - overlayWidth - 4));
                y = Math.max(4, Math.min(y, winH - overlayHeight - 4));
                int iconRadius = 3;
                int iconPadding = 4;
                int textColor = 0xFFA0E0FF;
                // Only draw background and border in move mode
                boolean moveMode = munchyutils.munchyutils.MunchyUtilsClient.isMoveMode;
                boolean isActive = moveMode && munchyutils.munchyutils.MunchyUtilsClient.movingHudStatic == munchyutils.client.FeatureManager.ModFeature.INFO_HUD;
                if (moveMode) {
                    int borderX = x - 6;
                    int borderY = y - 6;
                    int borderW = overlayWidth + 12;  // Added padding for resize handles
                    int borderH = overlayHeight + 12;
                    int bgTop = 0xAA44474A;
                    int bgBottom = 0xAA232526;
                    context.fillGradient(borderX, borderY, borderX + borderW, borderY + borderH, bgTop, bgBottom);
                    int borderColor = isActive ? 0xFF00BFFF : 0xFF6C6F72;
                    context.drawBorder(borderX, borderY, borderW, borderH, borderColor);
                    
                    // Add resize handles in corners
                    int handleSize = 6;
                    int handleColor = isActive ? 0xFF00BFFF : 0xFF6C6F72;
                    
                    // Top-left handle
                    context.fill(borderX - 2, borderY - 2, borderX + handleSize, borderY + handleSize, handleColor);
                    // Top-right handle
                    context.fill(borderX + borderW - handleSize, borderY - 2, borderX + borderW + 2, borderY + handleSize, handleColor);
                    // Bottom-left handle
                    context.fill(borderX - 2, borderY + borderH - handleSize, borderX + handleSize, borderY + borderH + 2, handleColor);
                    // Bottom-right handle
                    context.fill(borderX + borderW - handleSize, borderY + borderH - handleSize, borderX + borderW + 2, borderY + borderH + 2, handleColor);
                    
                    // Add a clean, semi-transparent overlay if this HUD is being moved
                    if (isActive) {
                        int shadeColor = 0x4420A0FF; // subtle blue shade
                        context.fill(borderX, borderY, borderX + borderW, borderY + borderH, shadeColor);
                        context.drawText(textRenderer, "MOVING INFO HUD", x + overlayWidth / 2 - textRenderer.getWidth("MOVING INFO HUD") / 2, y - 18, 0xFFD700, false);
                    }
                    context.drawText(textRenderer, "MOVE MODE", borderX + 6, borderY + 2, 0x80FFFFFF, false);
                }
                for (int i = 0; i < numLines; i++) {
                    int lineY = y + 4 + i * 16;
                    int dotY = lineY + 4 - iconRadius;
                    int dotX = x;
                    context.fill(dotX, dotY, dotX + iconRadius * 2, dotY + iconRadius * 2, dotColors[i] & 0xAAFFFFFF);
                    int msgWidth = textRenderer.getWidth(lines[i]);
                    int msgX = x + iconRadius * 2 + iconPadding;
                    context.drawText(textRenderer, lines[i], msgX, lineY, textColor, false);
                }
                return;
            }
            // Only render mining HUD if showMining is true, session is active, and feature is enabled
            if (!munchyutils.munchyutils.HudInputHandler.isMoveMode) {
                if (!showMining || !FeatureManager.isEnabled(FeatureManager.ModFeature.INFO_HUD) || !session.isActive) return;
                if (!FeatureManager.isEnabled(FeatureManager.ModFeature.INFO_HUD) || !session.isActive) return;
            }
            TextRenderer textRenderer = client.textRenderer;
            int[] movePos = MunchyUtilsClient.getMoveHudPosition(FeatureManager.ModFeature.INFO_HUD);
            int[] pos = (movePos != null) ? movePos : munchyutils.client.FeatureManager.getHudPosition(munchyutils.client.FeatureManager.ModFeature.INFO_HUD);
            // If in move mode and this is the active HUD, use the current move position
            if (munchyutils.munchyutils.MunchyUtilsClient.isMoveMode && munchyutils.munchyutils.MunchyUtilsClient.movingHudStatic == munchyutils.client.FeatureManager.ModFeature.INFO_HUD) {
                x = munchyutils.munchyutils.MunchyUtilsClient.moveXStatic;
                y = munchyutils.munchyutils.MunchyUtilsClient.moveYStatic;
            }
            x = Math.round((float)x / 5) * 5;
            y = Math.round((float)y / 5) * 5;
            x = Math.max(4, Math.min(x, winW - overlayWidth - 4));
            y = Math.max(4, Math.min(y, winH - overlayHeight - 4));
            int iconRadius = 3;
            int iconPadding = 4;
            int textColor = 0xFFE0E0E0; // soft light gray
            int posColor = 0xFF8FCB9B; // soft green
            int neutralColor = 0xFFF2C97D; // soft amber
            int negColor = 0xFFE57373; // soft red
            // Calculate overlay size based on content
            int maxTextWidth = 0;
            int numLines = 4;
            String incomeLine = "Income: " + session.getHourlyIncomeString();
            int incomeWidth = textRenderer.getWidth(incomeLine) + 18;
            if (incomeWidth > maxTextWidth) maxTextWidth = incomeWidth;
            String totalLine = "Total: " + InfoHudSession.formatMoney(session.getTotalEarnings());
            int totalWidth = textRenderer.getWidth(totalLine) + 18;
            if (totalWidth > maxTextWidth) maxTextWidth = totalWidth;
            String sessionLine = session.getSessionLengthString();
            int sessionWidth = textRenderer.getWidth(sessionLine) + 18;
            if (sessionWidth > maxTextWidth) maxTextWidth = sessionWidth;
            String afkLine = session.isAfk ? "AFK" : "Active";
            int afkWidth = textRenderer.getWidth(afkLine) + 18;
            if (afkWidth > maxTextWidth) maxTextWidth = afkWidth;
            overlayWidth = (int)((maxTextWidth + 12) * scale);
            overlayHeight = (int)((numLines * 16 + 12) * scale);
            lastOverlayWidth = overlayWidth;
            lastOverlayHeight = overlayHeight;
            // Calculate session color before drawing total and session lines
            int sessionColor;
            if (session.isAfk) {
                sessionColor = neutralColor; // yellow if AFK
            } else {
                long sessionLength = session.getActiveSessionSeconds(session.startTime);
                if (session.isActive && sessionLength > 0) {
                    sessionColor = posColor; // green if active and not afk
                } else {
                    sessionColor = negColor; // red if not counting up
                }
            }
            // Determine color for income
            int incomeColor = session.getHourlyIncome() > 0 ? posColor : (session.getHourlyIncome() < 0 ? negColor : neutralColor);
            // Draw hourly income line
            int dotY = (int)(y + 8 * scale - 3 * scale);
            int dotX = (int)x;
            context.fill(dotX, dotY, (int)(dotX + 6 * scale), (int)(dotY + 6 * scale), incomeColor & 0xAAFFFFFF); // semi-transparent
            context.drawText(textRenderer, incomeLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale), textColor, false);
            // Draw total earnings line
            int totalDotY = (int)(y + 8 * scale + 16 * scale - 3 * scale);
            context.fill(dotX, totalDotY, (int)(dotX + 6 * scale), (int)(totalDotY + 6 * scale), sessionColor & 0xAAFFFFFF); // match session color
            context.drawText(textRenderer, totalLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale + 16 * scale), textColor, false);
            // Draw session time line
            int sessionDotY = (int)(y + 8 * scale + 32 * scale - 3 * scale);
            context.fill(dotX, sessionDotY, (int)(dotX + 6 * scale), (int)(sessionDotY + 6 * scale), sessionColor & 0xAAFFFFFF);
            context.drawText(textRenderer, sessionLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale + 32 * scale), textColor, false);
            // Draw AFK status line
            int afkColor = session.isAfk ? neutralColor : posColor;
            int afkDotY = (int)(y + 8 * scale + 48 * scale - 3 * scale);
            context.fill(dotX, afkDotY, (int)(dotX + 6 * scale), (int)(afkDotY + 6 * scale), afkColor & 0xAAFFFFFF);
            context.drawText(textRenderer, afkLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale + 48 * scale), textColor, false);
            // Draw ghosted border in move mode
            boolean moveMode = munchyutils.munchyutils.MunchyUtilsClient.isMoveMode;
            boolean isActive = moveMode && munchyutils.munchyutils.MunchyUtilsClient.movingHudStatic == munchyutils.client.FeatureManager.ModFeature.INFO_HUD;
            if (moveMode) {
                int borderX = (int)(x - 6 * scale);
                int borderY = (int)(y - 6 * scale);
                int borderW = (int)(overlayWidth + 12 * scale);
                int borderH = (int)(overlayHeight + 12 * scale);
                int bgTop = 0xAA44474A; // semi-transparent dark stone
                int bgBottom = 0xAA232526; // semi-transparent even darker
                context.fillGradient(borderX, borderY, borderX + borderW, borderY + borderH, bgTop, bgBottom);
                int borderColor = isActive ? 0xFF00BFFF : 0xFF6C6F72;
                context.drawBorder(borderX, borderY, borderW, borderH, borderColor);
                // Add resize handles in corners
                int handleSize = (int)(6 * scale);
                int handleColor = isActive ? 0xFF00BFFF : 0xFF6C6F72;
                // Top-left handle
                context.fill(borderX - 2, borderY - 2, borderX + handleSize, borderY + handleSize, handleColor);
                // Top-right handle
                context.fill(borderX + borderW - handleSize, borderY - 2, borderX + borderW + 2, borderY + handleSize, handleColor);
                // Bottom-left handle
                context.fill(borderX - 2, borderY + borderH - handleSize, borderX + handleSize, borderY + borderH + 2, handleColor);
                // Bottom-right handle
                context.fill(borderX + borderW - handleSize, borderY + borderH - handleSize, borderX + borderW + 2, borderY + borderH + 2, handleColor);
                // Add a clean, semi-transparent overlay if this HUD is being moved
                if (isActive) {
                    int shadeColor = 0x4420A0FF; // subtle blue shade
                    context.fill(borderX, borderY, borderX + borderW, borderY + borderH, shadeColor);
                    context.drawText(textRenderer, "MOVING INFO HUD", (int)(x + overlayWidth / 2 - textRenderer.getWidth("MOVING INFO HUD") / 2), (int)(y - 18 * scale), 0xFFD700, false);
                }
                context.drawText(textRenderer, "MOVE MODE", borderX + 6, borderY + 2, 0x80FFFFFF, false);
            }
        });
        // Listen for config changes and update overlay in real time
        MunchyConfig.get().setOnChange(cfg -> {
            // No-op, overlay will re-read config on next render
        });
    }

    public static int[] getOverlaySize() {
        return new int[]{lastOverlayWidth, lastOverlayHeight};
    }

    public static void setOverlaySize(int width, int height) {
        lastOverlayWidth = width;
        lastOverlayHeight = height;
    }

    private static boolean isPickaxe(ItemStack stack) {
        return stack.getItem() == Items.WOODEN_PICKAXE || stack.getItem() == Items.STONE_PICKAXE ||
               stack.getItem() == Items.IRON_PICKAXE || stack.getItem() == Items.GOLDEN_PICKAXE ||
               stack.getItem() == Items.DIAMOND_PICKAXE || stack.getItem() == Items.NETHERITE_PICKAXE;
    }

    public static Map<String, String> getRewardCategoryMap() {
        return rewardCategoryMap;
    }

    public static Map<String, String> getCategoryColorMap() {
        return categoryColorMap;
    }
} 