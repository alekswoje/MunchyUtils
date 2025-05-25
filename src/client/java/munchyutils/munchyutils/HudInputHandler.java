package munchyutils.munchyutils;

import net.minecraft.client.option.KeyBinding;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import munchyutils.client.FeatureManager;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.client.util.InputUtil;

import java.util.EnumMap;
import java.util.Map;

public class HudInputHandler {
    public static boolean isMoveMode = false;
    public static FeatureManager.ModFeature movingHudStatic = FeatureManager.ModFeature.COOLDOWN_HUD;
    public static int moveXStatic = 0, moveYStatic = 0;
    public static final int GRID_SIZE = 5;
    public static final int HUD_MARGIN = 4;
    private static Map<FeatureManager.ModFeature, int[]> tempHudPositions = new EnumMap<>(FeatureManager.ModFeature.class);
    private static final int INFO_HUD_DEFAULT_Y = 250;
    
    // Mouse dragging state
    private static boolean isDragging = false;
    private static boolean isResizing = false;
    private static int dragStartX = 0;
    private static int dragStartY = 0;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static int resizeStartWidth = 0;
    private static int resizeStartHeight = 0;
    private static String resizeCorner = null; // "tl", "tr", "bl", "br"

    static {
        // Set defaults
        tempHudPositions.put(FeatureManager.ModFeature.COOLDOWN_HUD, new int[]{10, 10});
        tempHudPositions.put(FeatureManager.ModFeature.INFO_HUD, new int[]{10, INFO_HUD_DEFAULT_Y});
    }

    public static void register(KeyBinding moveModeKey) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (moveModeKey.wasPressed()) {
                if (!isMoveMode) {
                    // Enter move mode
                    isMoveMode = true;
                    movingHudStatic = FeatureManager.ModFeature.COOLDOWN_HUD;
                    moveXStatic = FeatureManager.getHudPosition(movingHudStatic)[0];
                    moveYStatic = FeatureManager.getHudPosition(movingHudStatic)[1];
                    client.player.sendMessage(Text.literal("Entered HUD move mode. Use TAB to switch HUDs, click and drag to move, and ENTER to save."), false);
                    // Lock mouse cursor
                    client.mouse.lockCursor();
                    // Prevent menu from opening
                    client.options.pauseOnLostFocus = false;
                } else {
                    // Exit move mode
                    isMoveMode = false;
                    movingHudStatic = null;
                    moveXStatic = 0;
                    moveYStatic = 0;
                    client.player.sendMessage(Text.literal("Exited HUD move mode."), false);
                    // Unlock mouse cursor
                    client.mouse.unlockCursor();
                    // Restore menu behavior
                    client.options.pauseOnLostFocus = true;
                }
            }

            if (isMoveMode) {
                // Handle TAB key for switching HUDs
                if (client.options.sprintKey.wasPressed()) {
                    if (movingHudStatic == FeatureManager.ModFeature.COOLDOWN_HUD) {
                        movingHudStatic = FeatureManager.ModFeature.INFO_HUD;
                        moveXStatic = FeatureManager.getHudPosition(movingHudStatic)[0];
                        moveYStatic = FeatureManager.getHudPosition(movingHudStatic)[1];
                    } else {
                        movingHudStatic = FeatureManager.ModFeature.COOLDOWN_HUD;
                        moveXStatic = FeatureManager.getHudPosition(movingHudStatic)[0];
                        moveYStatic = FeatureManager.getHudPosition(movingHudStatic)[1];
                    }
                    client.player.sendMessage(Text.literal("Switched to " + movingHudStatic.name() + " HUD"), false);
                }

                // Handle ENTER key for saving position
                if (client.options.attackKey.wasPressed()) {
                    if (movingHudStatic != null) {
                        FeatureManager.setHudPosition(movingHudStatic, moveXStatic, moveYStatic);
                        client.player.sendMessage(Text.literal("Saved " + movingHudStatic.name() + " HUD position"), false);
                    }
                }

                // Handle ESC key for canceling
                if (client.options.inventoryKey.wasPressed()) {
                    isMoveMode = false;
                    movingHudStatic = null;
                    moveXStatic = 0;
                    moveYStatic = 0;
                    client.player.sendMessage(Text.literal("Cancelled HUD move mode."), false);
                    // Unlock mouse cursor
                    client.mouse.unlockCursor();
                    // Restore menu behavior
                    client.options.pauseOnLostFocus = true;
                }

                // Handle mouse dragging
                if (client.mouse.wasLeftButtonClicked()) {
                    double mouseX = client.mouse.getX() * client.getWindow().getScaleFactor();
                    double mouseY = client.mouse.getY() * client.getWindow().getScaleFactor();
                    
                    // Check if mouse is over the active HUD
                    if (movingHudStatic != null) {
                        int[] size = movingHudStatic == FeatureManager.ModFeature.COOLDOWN_HUD ? 
                            munchyutils.client.CooldownHudOverlay.getOverlaySize() : 
                            munchyutils.client.InfoHudOverlay.getOverlaySize();
                        
                        if (mouseX >= moveXStatic && mouseX <= moveXStatic + size[0] &&
                            mouseY >= moveYStatic && mouseY <= moveYStatic + size[1]) {
                            isDragging = true;
                            dragStartX = (int)mouseX;
                            dragStartY = (int)mouseY;
                            dragOffsetX = moveXStatic - dragStartX;
                            dragOffsetY = moveYStatic - dragStartY;
                        }
                    }
                }

                if (!client.mouse.isCursorLocked()) {
                    isDragging = false;
                }

                if (isDragging) {
                    double mouseX = client.mouse.getX() * client.getWindow().getScaleFactor();
                    double mouseY = client.mouse.getY() * client.getWindow().getScaleFactor();
                    
                    // Update position with grid snapping
                    moveXStatic = (int)Math.round((dragOffsetX + mouseX) / GRID_SIZE) * GRID_SIZE;
                    moveYStatic = (int)Math.round((dragOffsetY + mouseY) / GRID_SIZE) * GRID_SIZE;
                    
                    // Clamp to screen bounds
                    int[] size = movingHudStatic == FeatureManager.ModFeature.COOLDOWN_HUD ? 
                        munchyutils.client.CooldownHudOverlay.getOverlaySize() : 
                        munchyutils.client.InfoHudOverlay.getOverlaySize();
                    
                    moveXStatic = Math.max(HUD_MARGIN, Math.min(moveXStatic, client.getWindow().getScaledWidth() - size[0] - HUD_MARGIN));
                    moveYStatic = Math.max(HUD_MARGIN, Math.min(moveYStatic, client.getWindow().getScaledHeight() - size[1] - HUD_MARGIN));
                }

                // Release drag when mouse button is released
                if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_RELEASE) {
                    isDragging = false;
                }
            }
        });
    }
} 