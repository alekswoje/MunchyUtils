package munchyutils.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;

public abstract class BaseHudOverlay {
    protected static final int GRID_SIZE = 5;
    protected static final int HUD_MARGIN = 4;
    protected static final int ICON_RADIUS = 3;
    protected static final int ICON_PADDING = 4;
    protected static final int COLOR_TEXT = 0xFFE0E0E0;
    protected static final int COLOR_READY = 0xFF8FCB9B;
    protected static final int COLOR_ALMOST = 0xFFF2C97D;
    protected static final int COLOR_NOT_READY = 0xFFE57373;
    protected static final int COLOR_POS = 0xFF8FCB9B;
    protected static final int COLOR_NEUTRAL = 0xFFF2C97D;
    protected static final int COLOR_NEG = 0xFFE57373;

    protected int snapToGrid(int value) {
        return Math.round((float)value / GRID_SIZE) * GRID_SIZE;
    }

    protected int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    protected int[] getWindowSize() {
        Window window = MinecraftClient.getInstance().getWindow();
        return new int[]{window.getScaledWidth(), window.getScaledHeight()};
    }

    protected int getTextWidth(TextRenderer textRenderer, String text) {
        return textRenderer.getWidth(text);
    }
} 