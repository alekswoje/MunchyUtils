package munchyutils.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.client.render.RenderTickCounter;

public class CooldownHudOverlay extends BaseHudOverlay {
    private static int lastOverlayWidth = 120;
    private static int lastOverlayHeight = 48;

    public static void register() {
        HudRenderCallback.EVENT.register((DrawContext context, RenderTickCounter tickCounter) -> {
            MunchyConfig config = MunchyConfig.get();
            float scale = config.getCooldownHudScale();
            int x = config.getCooldownHudX();
            int y = config.getCooldownHudY();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) return;
            Window window = client.getWindow();
            TextRenderer textRenderer = client.textRenderer;
            int[] movePos = munchyutils.munchyutils.MunchyUtilsClient.getMoveHudPosition(munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD);
            int[] pos = (movePos != null) ? movePos : munchyutils.client.FeatureManager.getHudPosition(munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD);
            int winW = window.getScaledWidth();
            int winH = window.getScaledHeight();
            int overlayWidth = (int)(lastOverlayWidth * scale);
            int overlayHeight = (int)(lastOverlayHeight * scale);
            System.out.println("[CooldownHudOverlay] Render: x=" + x + ", y=" + y + ", scale=" + scale + ", overlayWidth=" + overlayWidth + ", overlayHeight=" + overlayHeight + ", winW=" + winW + ", winH=" + winH);
            // Clamp and default position logic
            if (x == -1) x = 10;
            if (y == -1) y = 10;
            // X/Y now represent the final on-screen position (after scaling)
            x = Math.max(0, Math.min(x, winW - overlayWidth));
            y = Math.max(0, Math.min(y, winH - overlayHeight));
            // If in move mode and this is the active HUD, use the current move position
            if (munchyutils.munchyutils.MunchyUtilsClient.isMoveMode && munchyutils.munchyutils.MunchyUtilsClient.movingHudStatic == munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD) {
                x = munchyutils.munchyutils.MunchyUtilsClient.moveXStatic;
                y = munchyutils.munchyutils.MunchyUtilsClient.moveYStatic;
            }
            // Snap to grid and clamp with margin
            x = Math.round((float)x / 5) * 5;  // Updated grid size
            y = Math.round((float)y / 5) * 5;  // Updated grid size
            x = Math.max(4, Math.min(x, winW - overlayWidth - 4));
            y = Math.max(4, Math.min(y, winH - overlayHeight - 4));
            int cooldownY = y;
            int lineHeight = 12;
            java.util.Map<String, Long> cooldowns = munchyutils.client.CooldownManager.getAllCooldowns();
            if (cooldowns.isEmpty()) {
                System.out.println("[CooldownHudOverlay] Skipping render: cooldowns.isEmpty()");
                return;
            }
            // Only log once per render event
            if (!cooldowns.isEmpty()) {
                System.out.println("[CooldownHudOverlay] Rendering Cooldown HUD with " + cooldowns.size() + " cooldown(s)");
            }
            String justReady = munchyutils.client.CooldownManager.pollReadyCooldown();
            if (justReady != null) {
                java.time.LocalTime now = java.time.LocalTime.now();
                if ((now.getHour() == 4 || now.getHour() == 16) && now.getMinute() == 20) {
                    MinecraftClient.getInstance().player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                    MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("Nice"), false);
                } else {
                    MinecraftClient.getInstance().player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0F, 1.5F);
                }
            }
            int maxY = window.getScaledHeight();
            int iconRadius = 3;
            int iconPadding = 4;
            int textColor = 0xFFE0E0E0; // soft light gray
            int readyColor = 0xFF8FCB9B; // soft green
            int almostColor = 0xFFF2C97D; // soft amber
            int notReadyColor = 0xFFE57373; // soft red
            // Calculate overlay size based on content
            int maxTextWidth = 0;
            int numLines = 0;
            for (var entry : cooldowns.entrySet()) {
                CooldownTrigger trigger = munchyutils.client.CooldownManager.getTriggerByName(entry.getKey());
                if (trigger == null) continue;
                boolean found = false;
                if (entry.getValue() > 0) {
                    found = true; // Always show ongoing cooldowns
                } else if (client.player != null) {
                    ItemStack mainHand = client.player.getMainHandStack();
                    if (!mainHand.isEmpty() && mainHand.getName().getString().contains(trigger.itemNamePart)) found = true;
                    ItemStack offHand = client.player.getOffHandStack();
                    if (!offHand.isEmpty() && offHand.getName().getString().contains(trigger.itemNamePart)) found = true;
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        ItemStack stack = client.player.getEquippedStack(slot);
                        if (!stack.isEmpty() && stack.getName().getString().contains(trigger.itemNamePart)) found = true;
                    }
                    for (int i = 0; i < client.player.getInventory().size(); i++) {
                        ItemStack stack = client.player.getInventory().getStack(i);
                        if (!stack.isEmpty() && stack.getName().getString().contains(trigger.itemNamePart)) found = true;
                    }
                }
                if (!found) continue;
                String display = (entry.getValue() == 0) ? "Ready" : entry.getValue() + "s";
                String line = display + " " + entry.getKey();
                int width = textRenderer.getWidth(line) + 18; // icon + padding
                if (width > maxTextWidth) maxTextWidth = width;
                numLines++;
            }
            if (numLines == 0) { maxTextWidth = 80; numLines = 1; }
            overlayWidth = (int)(maxTextWidth * scale + 12);
            overlayHeight = (int)(numLines * lineHeight * scale + 12);
            lastOverlayWidth = overlayWidth;
            lastOverlayHeight = overlayHeight;
            // Apply scaling to the entire overlay
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1.0f);
            try {
                int drawX = 0;
                int drawY = 0;
                for (var entry : cooldowns.entrySet()) {
                    if (cooldownY + lineHeight > maxY) break; // Prevent drawing off-screen
                    String name = entry.getKey();
                    long remaining = entry.getValue();
                    CooldownTrigger trigger = munchyutils.client.CooldownManager.getTriggerByName(name);
                    if (trigger == null) continue;
                    boolean found = false;
                    if (remaining > 0) {
                        found = true; // Always show ongoing cooldowns
                    } else if (client.player != null) {
                        ItemStack mainHand = client.player.getMainHandStack();
                        if (!mainHand.isEmpty() && mainHand.getName().getString().contains(trigger.itemNamePart)) found = true;
                        ItemStack offHand = client.player.getOffHandStack();
                        if (!offHand.isEmpty() && offHand.getName().getString().contains(trigger.itemNamePart)) found = true;
                        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                            ItemStack stack = client.player.getEquippedStack(slot);
                            if (!stack.isEmpty() && stack.getName().getString().contains(trigger.itemNamePart)) found = true;
                        }
                        for (int i = 0; i < client.player.getInventory().size(); i++) {
                            ItemStack stack = client.player.getInventory().getStack(i);
                            if (!stack.isEmpty() && stack.getName().getString().contains(trigger.itemNamePart)) found = true;
                        }
                    }
                    if (!found) continue;
                    String displayLine;
                    int color;
                    long total = trigger.cooldownMs / 1000;
                    if (remaining == 0) {
                        displayLine = "Ready";
                        color = readyColor;
                    } else if (total > 0 && remaining < total / 2) {
                        displayLine = remaining + "s";
                        color = almostColor;
                    } else {
                        displayLine = remaining + "s";
                        color = notReadyColor;
                    }
                    String line = displayLine + " " + name;
                    // Draw subtle dot icon
                    int fontHeight = textRenderer.fontHeight;
                    int dotY = drawY + cooldownY + (fontHeight - iconRadius * 2) / 2;
                    int dotX = drawX + iconRadius * 2;
                    context.fill(dotX, dotY, dotX + iconRadius * 2, dotY + iconRadius * 2, color & 0xAAFFFFFF); // semi-transparent
                    // Draw text: timer part in color, rest in neutral
                    int timerWidth = textRenderer.getWidth(displayLine);
                    int nameX = drawX + iconRadius * 2 + iconPadding + timerWidth + 4;
                    context.drawText(textRenderer, displayLine, drawX + iconRadius * 2 + iconPadding, drawY + cooldownY, color, false);
                    context.drawText(textRenderer, " " + name, nameX, drawY + cooldownY, textColor, false);
                    cooldownY += lineHeight;
                }
                // Draw ghosted border in move mode
                boolean moveMode = munchyutils.munchyutils.MunchyUtilsClient.isMoveMode;
                boolean isActive = moveMode && munchyutils.munchyutils.MunchyUtilsClient.movingHudStatic == munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD;
                if (moveMode) {
                    int borderX = drawX - 6;
                    int borderY = drawY - 6;
                    int borderW = overlayWidth + 12;  // Added padding for resize handles
                    int borderH = overlayHeight + 12;
                    int bgTop = 0xAA44474A; // semi-transparent dark stone
                    int bgBottom = 0xAA232526; // semi-transparent even darker
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
                        context.drawText(textRenderer, "MOVING COOLDOWN HUD", drawX + overlayWidth / 2 - textRenderer.getWidth("MOVING COOLDOWN HUD") / 2, drawY - 18, 0x00BFFF, false);
                    }
                    context.drawText(textRenderer, "MOVE MODE", borderX + 6, borderY + 2, 0x80FFFFFF, false);
                }
            } finally {
                context.getMatrices().pop();
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
} 