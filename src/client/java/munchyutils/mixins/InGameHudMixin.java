package munchyutils.mixins;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import munchyutils.client.HudInputHandler;
import org.lwjgl.glfw.GLFW;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HudInputHandler.isMoveMode) {
            MinecraftClient client = MinecraftClient.getInstance();
            Window window = client.getWindow();
            
            // Handle mouse dragging
            if (GLFW.glfwGetMouseButton(window.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                double mouseX = client.mouse.getX() * window.getScaleFactor();
                double mouseY = client.mouse.getY() * window.getScaleFactor();
                
                // Update position with grid snapping
                HudInputHandler.moveXStatic = (int)Math.round(mouseX / HudInputHandler.GRID_SIZE) * HudInputHandler.GRID_SIZE;
                HudInputHandler.moveYStatic = (int)Math.round(mouseY / HudInputHandler.GRID_SIZE) * HudInputHandler.GRID_SIZE;
                
                // Clamp to screen bounds
                int[] size = HudInputHandler.movingHudStatic == munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD ? 
                    munchyutils.client.CooldownHudOverlay.getOverlaySize() : 
                    munchyutils.client.InfoHudOverlay.getOverlaySize();
                
                HudInputHandler.moveXStatic = Math.max(HudInputHandler.HUD_MARGIN, 
                    Math.min(HudInputHandler.moveXStatic, window.getScaledWidth() - size[0] - HudInputHandler.HUD_MARGIN));
                HudInputHandler.moveYStatic = Math.max(HudInputHandler.HUD_MARGIN, 
                    Math.min(HudInputHandler.moveYStatic, window.getScaledHeight() - size[1] - HudInputHandler.HUD_MARGIN));
            }
        }
    }
} 