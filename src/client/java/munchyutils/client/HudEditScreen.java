package munchyutils.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudEditScreen extends Screen {
    public enum HudType { NONE, INFO, COOLDOWN }
    private static HudType selectedHud = HudType.NONE;

    public HudEditScreen() {
        super(Text.literal("HUD Edit Mode"));
    }

    @Override
    public boolean shouldPause() {
        return true; // Lock camera and show mouse
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw shaded overlay first
        int width = this.width;
        int height = this.height;
        context.fill(0, 0, width, height, 0x88000000); // semi-transparent black
        // Draw message
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("HUD Edit Mode"), width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Drag HUDs to move, drag corner to resize. Press ESC to exit."), width / 2, 40, 0xAAAAAA);
        // Render overlays for edit mode, only selected HUD is editable
        InfoHudOverlay.renderForEdit(context, mouseX, mouseY, selectedHud == HudType.INFO);
        CooldownHudOverlay.renderForEdit(context, mouseX, mouseY, selectedHud == HudType.COOLDOWN);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // On click, select the HUD under the mouse (if any)
        if (InfoHudOverlay.isMouseOver(mouseX, mouseY)) {
            selectedHud = HudType.INFO;
            return true;
        } else if (CooldownHudOverlay.isMouseOver(mouseX, mouseY)) {
            selectedHud = HudType.COOLDOWN;
            return true;
        } else {
            selectedHud = HudType.NONE;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            MinecraftClient.getInstance().setScreen(null); // Exit edit mode
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
} 