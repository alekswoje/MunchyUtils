package munchyutils.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import munchyutils.client.FeatureManager;
import net.minecraft.client.gui.DrawContext;

public class MunchyUtilsConfigScreen extends Screen {
    private ButtonWidget toggleOverlayButton;

    public MunchyUtilsConfigScreen() {
        super(Text.literal("Mining Utils Settings"));
        System.out.println("[MunchyUtils] MunchyUtilsConfigScreen constructor");
    }

    @Override
    protected void init() {
        System.out.println("[MunchyUtils] MunchyUtilsConfigScreen.init()");
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        toggleOverlayButton = ButtonWidget.builder(
                Text.literal("Toggle Info HUD (Currently: " + (FeatureManager.isEnabled(FeatureManager.ModFeature.INFO_HUD) ? "ON" : "OFF") + ")"),
                button -> {
                    FeatureManager.toggle(FeatureManager.ModFeature.INFO_HUD);
                    boolean enabled = FeatureManager.isEnabled(FeatureManager.ModFeature.INFO_HUD);
                    button.setMessage(Text.literal("Toggle Info HUD (Currently: " + (enabled ? "ON" : "OFF") + ")"));
                }
        ).dimensions(centerX - 100, centerY - 10, 200, 20).build();
        this.addDrawableChild(toggleOverlayButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        System.out.println("[MunchyUtils] MunchyUtilsConfigScreen.render()");
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        System.out.println("[MunchyUtils] MunchyUtilsConfigScreen.close()");
        assert this.client != null;
        this.client.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
} 