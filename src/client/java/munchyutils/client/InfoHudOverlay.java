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
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import munchyutils.client.MunchyUtilsClient;
import munchyutils.client.Utils;

public class InfoHudOverlay extends BaseHudOverlay {
    public static final InfoHudSession session = new InfoHudSession();
    public static final FishingHudSession fishingSession = new FishingHudSession();
    private static Map<String, String> rewardCategoryMap = new LinkedHashMap<>();
    private static Map<String, String> categoryColorMap = new LinkedHashMap<>();
    private static int lastOverlayWidth = 120;
    private static int lastOverlayHeight = 48;
    private static final int HANDLE_SIZE = 12;
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

    // Drag/resize state for edit mode
    private static boolean dragging = false;
    private static boolean resizing = false;
    private static double dragOffsetX = 0, dragOffsetY = 0;
    private static double origScale = 1.0;
    private static double origMouseX = 0, origMouseY = 0;

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (isEditScreenActive()) return; // Hide real HUD in edit mode
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
            boolean hasPickaxeInHotbar = false;
            int pickaxeHotbarSlot = -1;
            // Only check hotbar slots 0-8 for pickaxe
            for (int i = 0; i < 9; i++) {
                ItemStack stack = client.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                if (Utils.isPickaxe(stack)) {
                    hasPickaxeInHotbar = true;
                    if (pickaxeHotbarSlot == -1) pickaxeHotbarSlot = i;
                }
            }

            boolean hasFishingRodInHotbar = false;
            int fishingRodHotbarSlot = -1;
            // Only check hotbar slots 0-8 for fishing rod
            for (int i = 0; i < 9; i++) {
                ItemStack stack = client.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() == Items.FISHING_ROD) {
                    hasFishingRodInHotbar = true;
                    if (fishingRodHotbarSlot == -1) fishingRodHotbarSlot = i;
                }
            }

            boolean hasJabbaTheHuttsBellyInInventory = Utils.hasJabbaTheHuttsBelly(client.player.getInventory());

            boolean showMining = false;
            boolean showFishing = false;

            // Determine which HUD to show
            if (hasPickaxeInHotbar && hasFishingRodInHotbar) {
                // If both are in hotbar, prioritize based on slot
                if (pickaxeHotbarSlot != -1 && fishingRodHotbarSlot != -1) {
                    showMining = pickaxeHotbarSlot <= fishingRodHotbarSlot;
                    showFishing = !showMining;
                } else if (pickaxeHotbarSlot != -1) {
                    showMining = true;
                } else if (fishingRodHotbarSlot != -1) {
                    showFishing = true;
                } else {
                    // Fallback if somehow slots are -1 but hasItem is true
                    showMining = true;
                }
            } else if (hasPickaxeInHotbar) {
                showMining = true;
            } else if (hasFishingRodInHotbar) {
                showFishing = true;
            } else if (hasJabbaTheHuttsBellyInInventory) {
                // Show mining if Jabba's Belly is in inventory and no pickaxe/fishing rod in hotbar
                showMining = true;
            }

            // If neither mining nor fishing HUD should be shown, return early
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
                boolean statsLoaded = munchyutils.client.MunchyUtilsClient.isFishingStatsLoaded();
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
                    String timeToNext = fishingSession.getTimeToNextLevelString(fishingSession.getXPPerHour());
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
                x = Math.round((float)x / 5) * 5;
                y = Math.round((float)y / 5) * 5;
                x = Math.max(4, Math.min(x, winW - overlayWidth - 4));
                y = Math.max(4, Math.min(y, winH - overlayHeight - 4));
                int iconRadius = 3;
                int iconPadding = 4;
                int textColor = 0xFFA0E0FF;
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
            if (!munchyutils.client.HudInputHandler.isMoveMode) {
                if (!showMining || !FeatureManager.isEnabled(FeatureManager.ModFeature.INFO_HUD)) return;
            }
            TextRenderer textRenderer = client.textRenderer;
            int[] movePos = MunchyUtilsClient.getMoveHudPosition(FeatureManager.ModFeature.INFO_HUD);
            int[] pos = (movePos != null) ? movePos : munchyutils.client.FeatureManager.getHudPosition(munchyutils.client.FeatureManager.ModFeature.INFO_HUD);
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
            int porgColor = 0xFFA0522D; // brown for Porg buff
            // Calculate overlay size based on content
            int maxTextWidth = 0;
            int numLines = 4;
            String incomeLine, totalLine, sessionLine, afkLine, porgBuffLine = null;
            int incomeWidth, totalWidth, sessionWidth, afkWidth, porgBuffWidth = 0;
            if (session.isActive) {
                incomeLine = "Income: " + session.getHourlyIncomeString();
                totalLine = "Total: " + Utils.formatMoney(session.getTotalEarnings());
                sessionLine = session.getSessionLengthString();
                afkLine = session.isAfk ? "AFK" : "Active";
                if (session.isPorgBuffActive()) {
                    long ms = session.getPorgBuffRemainingMs();
                    long sec = ms / 1000;
                    porgBuffLine = "Porg Buff: " + sec + "s left";
                    numLines++;
                }
            } else {
                incomeLine = "Income: $0/hr";
                totalLine = "Total: $0";
                sessionLine = "Session: 0m 0s";
                afkLine = "Active";
            }
            incomeWidth = textRenderer.getWidth(incomeLine) + 18;
            if (incomeWidth > maxTextWidth) maxTextWidth = incomeWidth;
            totalWidth = textRenderer.getWidth(totalLine) + 18;
            if (totalWidth > maxTextWidth) maxTextWidth = totalWidth;
            sessionWidth = textRenderer.getWidth(sessionLine) + 18;
            if (sessionWidth > maxTextWidth) maxTextWidth = sessionWidth;
            afkWidth = textRenderer.getWidth(afkLine) + 18;
            if (afkWidth > maxTextWidth) maxTextWidth = afkWidth;
            if (porgBuffLine != null) {
                porgBuffWidth = textRenderer.getWidth(porgBuffLine) + 18;
                if (porgBuffWidth > maxTextWidth) maxTextWidth = porgBuffWidth;
            }
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
            int lineIdx = 0;
            // Draw hourly income line
            int dotY = (int)(y + 8 * scale - 3 * scale);
            int dotX = (int)x;
            context.fill(dotX, dotY, (int)(dotX + 6 * scale), (int)(dotY + 6 * scale), incomeColor & 0xAAFFFFFF); // semi-transparent
            context.drawText(textRenderer, incomeLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale), textColor, false);
            lineIdx++;
            // Draw total earnings line
            int totalDotY = (int)(y + 8 * scale + 16 * scale - 3 * scale);
            context.fill(dotX, totalDotY, (int)(dotX + 6 * scale), (int)(totalDotY + 6 * scale), sessionColor & 0xAAFFFFFF); // match session color
            context.drawText(textRenderer, totalLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale + 16 * scale), textColor, false);
            lineIdx++;
            // Draw session time line
            int sessionDotY = (int)(y + 8 * scale + 32 * scale - 3 * scale);
            context.fill(dotX, sessionDotY, (int)(dotX + 6 * scale), (int)(sessionDotY + 6 * scale), sessionColor & 0xAAFFFFFF);
            context.drawText(textRenderer, sessionLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale + 32 * scale), textColor, false);
            lineIdx++;
            // Draw Porg buff line if active
            if (session.isPorgBuffActive() && porgBuffLine != null) {
                int porgDotY = (int)(y + 8 * scale + 48 * scale - 3 * scale);
                context.fill(dotX, porgDotY, (int)(dotX + 6 * scale), (int)(porgDotY + 6 * scale), porgColor & 0xAAFFFFFF);
                context.drawText(textRenderer, porgBuffLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale + 48 * scale), porgColor, false);
                lineIdx++;
            }
            // Draw AFK status line
            int afkColor = session.isAfk ? neutralColor : posColor;
            int afkDotY = (int)(y + 8 * scale + (16 * (numLines - 1)) - 3 * scale);
            context.fill(dotX, afkDotY, (int)(dotX + 6 * scale), (int)(afkDotY + 6 * scale), afkColor & 0xAAFFFFFF);
            context.drawText(textRenderer, afkLine, (int)(x + 6 * scale + 4 * scale), (int)(y + 4 * scale + (16 * (numLines - 1)) * scale / 1.0f), textColor, false);
            // After calculating overlayWidth/overlayHeight:
            int[] posSize = Utils.getClampedPositionAndSize(x, y, overlayWidth, overlayHeight, winW, winH);
            x = posSize[0];
            y = posSize[1];
            overlayWidth = posSize[2];
            overlayHeight = posSize[3];
            // Wrap all drawing code in context.getMatrices().push()/scale()/pop()
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1.0f);
            // ... draw overlay content here ...
            context.getMatrices().pop();
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

    // Render the Info HUD overlay for edit mode (from a Screen)
    public static void renderForEdit(DrawContext context, int mouseX, int mouseY, boolean isSelected) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        MunchyConfig config = MunchyConfig.get();
        float scale = Math.min(config.getInfoHudScale(), 2.0f);
        int x = config.getInfoHudX();
        int y = config.getInfoHudY();
        Window window = client.getWindow();
        int winW = window.getScaledWidth();
        int winH = window.getScaledHeight();
        // Example HUD: mimic the full-length fishing HUD
        String[] lines = new String[] {
            "Example HUD", // Title
            "Rewards: 123 (456/h)",
            "XP: 789 (1.2k/h)",
            "Level: 42 (99.9%)",
            "Session: 1h 23m 45s",
            "XP to next: 1234",
            "Time to next: 1.2h",
            "Casts: 99",
            "Active"
        };
        int[] dotColors = new int[] {
            0xFF00BFFF, // blue for title
            0xFF7ED957, // green for rewards
            0xFF6EC6FF, // blue for xp
            0xFFFFD966, // gold for level
            0xFFB4B4B4, // gray for session
            0xFF6EC6FF, // blue for xp to next
            0xFFB4B4B4, // gray for time to next
            0xFFB4B4B4, // gray for casts
            0xFF7ED957  // green for active
        };
        int numLines = lines.length;
        TextRenderer textRenderer = client.textRenderer;
        int maxTextWidth = 0;
        for (String line : lines) {
            int w = textRenderer.getWidth(line) + 18;
            if (w > maxTextWidth) maxTextWidth = w;
        }
        int overlayWidth = (int)((maxTextWidth + 12) * scale);
        int overlayHeight = (int)((numLines * 16 + 12) * scale);
        lastOverlayWidth = overlayWidth;
        lastOverlayHeight = overlayHeight;
        if (x == -1) x = 10;
        if (y == -1) y = 10;
        x = Math.max(0, Math.min(x, winW - overlayWidth));
        y = Math.max(0, Math.min(y, winH - overlayHeight));
        // --- EDIT MODE LOGIC ---
        boolean editMode = isSelected;
        double mouseXd = mouseX;
        double mouseYd = mouseY;
        boolean mouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        int handleSize = (int)(HANDLE_SIZE * scale);
        int handleX0 = x + overlayWidth - handleSize;
        int handleY0 = y + overlayHeight - handleSize;
        int handleX1 = x + overlayWidth;
        int handleY1 = y + overlayHeight;
        boolean overHandle = mouseXd >= handleX0 && mouseXd <= handleX1 && mouseYd >= handleY0 && mouseYd <= handleY1;
        boolean overHud = mouseXd >= x && mouseXd <= x + overlayWidth && mouseYd >= y && mouseYd <= y + overlayHeight;
        if (editMode) {
            if (mouseDown) {
                if (!dragging && !resizing) {
                    if (overHandle) {
                        resizing = true;
                        origScale = scale;
                        origMouseX = mouseXd;
                        origMouseY = mouseYd;
                    } else if (overHud) {
                        dragging = true;
                        dragOffsetX = mouseXd - x;
                        dragOffsetY = mouseYd - y;
                    }
                }
            } else {
                if (dragging || resizing) {
                    MunchyConfig.get().save();
                }
                dragging = false;
                resizing = false;
            }
            if (resizing) {
                double dx = mouseXd - origMouseX;
                double dy = mouseYd - origMouseY;
                double d = Math.max(dx, dy);
                float newScale = (float)Math.max(0.5, Math.min(origScale + d / 100.0, 2.0));
                config.setInfoHudScale(newScale);
                scale = newScale;
                // Recalculate overlay size with new scale using existing variables
                maxTextWidth = 0;
                for (String line : lines) {
                    int w = textRenderer.getWidth(line) + 18;
                    if (w > maxTextWidth) maxTextWidth = w;
                }
                overlayWidth = (int)((maxTextWidth + 12) * scale);
                overlayHeight = (int)((numLines * 16 + 12) * scale);
                lastOverlayWidth = overlayWidth;
                lastOverlayHeight = overlayHeight;
            } else if (dragging) {
                int newX = (int)(mouseXd - dragOffsetX);
                int newY = (int)(mouseYd - dragOffsetY);
                config.setInfoHudX(Math.max(0, Math.min(newX, winW - overlayWidth)));
                config.setInfoHudY(Math.max(0, Math.min(newY, winH - overlayHeight)));
            }
        }
        // Draw the example HUD
        for (int i = 0; i < numLines; i++) {
            int lineY = y + 4 + i * 16;
            int dotY = lineY + 4 - 3;
            int dotX = x;
            context.fill(dotX, dotY, dotX + 6, dotY + 6, dotColors[i] & 0xAAFFFFFF);
            int msgX = x + 6 + 4;
            int color = (i == 0) ? 0xFF00BFFF : 0xFFE0E0E0;
            context.drawText(textRenderer, lines[i], msgX, lineY, color, false);
        }
        int borderColor = isSelected ? 0xFF00BFFF : 0xFF888888;
        context.drawBorder(x - 2, y - 2, overlayWidth + 4, overlayHeight + 4, borderColor);
        int handleColor = isSelected ? 0xFF00BFFF : 0xFF888888;
        // Draw a triangle handle in the bottom-right corner
        int[] triX = {handleX1, handleX0, handleX1};
        int[] triY = {handleY0, handleY1, handleY1};
        context.fill(triX[0], triY[0], triX[1], triY[1], handleColor);
        context.fill(triX[1], triY[1], triX[2], triY[2], handleColor);
    }

    public static boolean isMouseOver(double mouseX, double mouseY) {
        MunchyConfig config = MunchyConfig.get();
        float scale = config.getInfoHudScale();
        int x = config.getInfoHudX();
        int y = config.getInfoHudY();
        int overlayWidth = (int)(lastOverlayWidth * scale);
        int overlayHeight = (int)(lastOverlayHeight * scale);
        if (x == -1) x = 10;
        if (y == -1) y = 10;
        return mouseX >= x && mouseX <= x + overlayWidth && mouseY >= y && mouseY <= y + overlayHeight;
    }

    public static boolean isEditScreenActive() {
        return MinecraftClient.getInstance().currentScreen instanceof HudEditScreen;
    }

    // Helper to get clamped/scaled position and size
    private static int[] getClampedPositionAndSize(int x, int y, int width, int height, int winW, int winH) {
        x = Math.max(0, Math.min(x, winW - width));
        y = Math.max(0, Math.min(y, winH - height));
        x = Math.round((float)x / 5) * 5;
        y = Math.round((float)y / 5) * 5;
        x = Math.max(4, Math.min(x, winW - width - 4));
        y = Math.max(4, Math.min(y, winH - height - 4));
        return new int[]{x, y, width, height};
    }
} 