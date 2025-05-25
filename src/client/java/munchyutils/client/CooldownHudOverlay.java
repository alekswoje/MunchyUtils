package munchyutils.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CooldownHudOverlay extends BaseHudOverlay {
    private static int lastOverlayWidth = 120;
    private static int lastOverlayHeight = 48;

    // Drag/resize state for edit mode
    private static boolean dragging = false;
    private static boolean resizing = false;
    private static double dragOffsetX = 0, dragOffsetY = 0;
    private static double origScale = 1.0;
    private static double origMouseX = 0, origMouseY = 0;

    private static final int HANDLE_SIZE = 12;

    public static void register() {
        HudRenderCallback.EVENT.register((DrawContext context, RenderTickCounter tickCounter) -> {
            if (munchyutils.client.InfoHudOverlay.isEditScreenActive()) return; // Hide real HUD in edit mode
            MunchyConfig config = MunchyConfig.get();
            float scale = Math.min(config.getCooldownHudScale(), 2.0f);
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
            // After calculating overlayWidth/overlayHeight:
            int[] posSize = getClampedPositionAndSize(x, y, overlayWidth, overlayHeight, winW, winH);
            x = posSize[0];
            y = posSize[1];
            overlayWidth = posSize[2];
            overlayHeight = posSize[3];
            // Wrap all drawing code in context.getMatrices().push()/scale()/pop()
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
                // --- EDIT MODE LOGIC ---
                boolean editMode = munchyutils.munchyutils.MunchyUtilsClient.hudEditMode &&
                    munchyutils.munchyutils.MunchyUtilsClient.editingHudTypeStatic == munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD;
                // Mouse state
                double mouseX = client.mouse.getX() / window.getScaleFactor();
                double mouseY = client.mouse.getY() / window.getScaleFactor();
                boolean mouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
                // Calculate handle position/size using scale
                int handleSize = (int)(HANDLE_SIZE * scale);
                int handleX0 = x + overlayWidth - handleSize;
                int handleY0 = y + overlayHeight - handleSize;
                int handleX1 = x + overlayWidth;
                int handleY1 = y + overlayHeight;
                boolean overHandle = mouseX >= handleX0 && mouseX <= handleX1 && mouseY >= handleY0 && mouseY <= handleY1;
                boolean overHud = mouseX >= x && mouseX <= x + overlayWidth && mouseY >= y && mouseY <= y + overlayHeight;
                // Handle mouse events
                if (editMode) {
                    if (mouseDown) {
                        if (!dragging && !resizing) {
                            if (overHandle) {
                                resizing = true;
                                origScale = scale;
                                origMouseX = mouseX;
                                origMouseY = mouseY;
                            } else if (overHud) {
                                dragging = true;
                                dragOffsetX = mouseX - x;
                                dragOffsetY = mouseY - y;
                            }
                        }
                    } else {
                        if (dragging || resizing) {
                            // Save config on mouse release
                            MunchyConfig.get().save();
                        }
                        dragging = false;
                        resizing = false;
                    }
                    if (resizing) {
                        double dx = mouseX - origMouseX;
                        double dy = mouseY - origMouseY;
                        double d = Math.max(dx, dy);
                        float newScale = (float)Math.max(0.5, Math.min(origScale + d / 100.0, 2.0));
                        config.setCooldownHudScale(newScale);
                    } else if (dragging) {
                        int newX = (int)(mouseX - dragOffsetX);
                        int newY = (int)(mouseY - dragOffsetY);
                        config.setCooldownHudX(Math.max(0, Math.min(newX, winW - overlayWidth)));
                        config.setCooldownHudY(Math.max(0, Math.min(newY, winH - overlayHeight)));
                    }
                }
                // Draw border and handle in edit mode
                if (editMode) {
                    int borderColor = 0xFF00BFFF;
                    context.drawBorder(x - 2, y - 2, overlayWidth + 4, overlayHeight + 4, borderColor);
                    // Draw resize handle (bottom-right corner)
                    int handleColor = 0xFF00BFFF;
                    context.fill(handleX0, handleY0, handleX1, handleY1, handleColor);
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

    // Render the Cooldown HUD overlay for edit mode (from a Screen)
    public static void renderForEdit(DrawContext context, int mouseX, int mouseY, boolean isSelected) {
        MunchyConfig config = MunchyConfig.get();
        float scale = Math.min(config.getCooldownHudScale(), 2.0f);
        int x = config.getCooldownHudX();
        int y = config.getCooldownHudY();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        Window window = client.getWindow();
        TextRenderer textRenderer = client.textRenderer;
        int winW = window.getScaledWidth();
        int winH = window.getScaledHeight();
        int lineHeight = 12;
        // Always show 5 example cooldowns in edit mode
        String[] names = {"Example 1", "Example 2", "Example 3", "Example 4", "Example 5"};
        String[] timers = {"Ready", "2s", "5s", "10s", "30s"};
        int[] colors = {0xFF8FCB9B, 0xFFF2C97D, 0xFFE57373, 0xFFF2C97D, 0xFFE57373};
        int maxTextWidth = 0;
        for (int i = 0; i < names.length; i++) {
            String line = timers[i] + " " + names[i];
            int width = textRenderer.getWidth(line) + 18;
            if (width > maxTextWidth) maxTextWidth = width;
        }
        // Clamp and default position logic (match real HUD)
        if (x == -1) x = 10;
        if (y == -1) y = 10;
        int overlayWidth = (int)(maxTextWidth * scale + 12);
        int overlayHeight = (int)(names.length * lineHeight * scale + 12);
        x = Math.max(0, Math.min(x, winW - overlayWidth));
        y = Math.max(0, Math.min(y, winH - overlayHeight));
        x = Math.round((float)x / 5) * 5;
        y = Math.round((float)y / 5) * 5;
        x = Math.max(4, Math.min(x, winW - overlayWidth - 4));
        y = Math.max(4, Math.min(y, winH - overlayHeight - 4));
        lastOverlayWidth = overlayWidth;
        lastOverlayHeight = overlayHeight;
        // --- EDIT MODE LOGIC ---
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
        if (isSelected) {
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
                config.setCooldownHudScale(newScale);
            } else if (dragging) {
                int newX = (int)(mouseXd - dragOffsetX);
                int newY = (int)(mouseYd - dragOffsetY);
                config.setCooldownHudX(Math.max(0, Math.min(newX, winW - overlayWidth)));
                config.setCooldownHudY(Math.max(0, Math.min(newY, winH - overlayHeight)));
            }
        }
        // Draw the example cooldown HUD
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        try {
            int drawX = 0;
            int drawY = 0;
            int cooldownY = 0;
            for (int i = 0; i < names.length; i++) {
                String displayLine = timers[i];
                String name = names[i];
                int color = colors[i];
                int fontHeight = textRenderer.fontHeight;
                int dotY = drawY + cooldownY + (fontHeight - 6) / 2;
                int dotX = drawX + 6;
                context.fill(dotX, dotY, dotX + 6, dotY + 6, color & 0xAAFFFFFF);
                int timerWidth = textRenderer.getWidth(displayLine);
                int nameX = drawX + 6 + 4 + timerWidth + 4;
                context.drawText(textRenderer, displayLine, drawX + 6 + 4, drawY + cooldownY, color, false);
                context.drawText(textRenderer, " " + name, nameX, drawY + cooldownY, 0xFFE0E0E0, false);
                cooldownY += lineHeight;
            }
        } finally {
            context.getMatrices().pop();
        }
        int borderColor = isSelected ? 0xFF00BFFF : 0xFF888888;
        context.drawBorder(x - 2, y - 2, overlayWidth + 4, overlayHeight + 4, borderColor);
        int handleColor = isSelected ? 0xFF00BFFF : 0xFF888888;
        int[] triX = {handleX1, handleX0, handleX1};
        int[] triY = {handleY0, handleY1, handleY1};
        context.fill(triX[0], triY[0], triX[1], triY[1], handleColor);
        context.fill(triX[1], triY[1], triX[2], triY[2], handleColor);
    }

    public static boolean isMouseOver(double mouseX, double mouseY) {
        MunchyConfig config = MunchyConfig.get();
        float scale = Math.min(config.getCooldownHudScale(), 2.0f);
        int x = config.getCooldownHudX();
        int y = config.getCooldownHudY();
        int overlayWidth = (int)(lastOverlayWidth * scale);
        int overlayHeight = (int)(lastOverlayHeight * scale);
        if (x == -1) x = 10;
        if (y == -1) y = 10;
        return mouseX >= x && mouseX <= x + overlayWidth && mouseY >= y && mouseY <= y + overlayHeight;
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