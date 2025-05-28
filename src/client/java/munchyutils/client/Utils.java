package munchyutils.client;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.Item;

public class Utils {
    // Color constants for overlays
    public static final int COLOR_TEXT = 0xFFE0E0E0;
    public static final int COLOR_READY = 0xFF8FCB9B;
    public static final int COLOR_ALMOST = 0xFFF2C97D;
    public static final int COLOR_NOT_READY = 0xFFE57373;
    public static final int COLOR_POS = 0xFF8FCB9B;
    public static final int COLOR_NEUTRAL = 0xFFF2C97D;
    public static final int COLOR_NEG = 0xFFE57373;

    public static String stripColorCodes(String input) {
        return input.replaceAll("§.", "");
    }

    public static boolean isPickaxe(ItemStack stack) {
        return stack.getItem() == Items.WOODEN_PICKAXE ||
               stack.getItem() == Items.STONE_PICKAXE ||
               stack.getItem() == Items.IRON_PICKAXE ||
               stack.getItem() == Items.GOLDEN_PICKAXE ||
               stack.getItem() == Items.DIAMOND_PICKAXE ||
               stack.getItem() == Items.NETHERITE_PICKAXE;
    }

    public static boolean isPickaxe(Item item) {
        return item == Items.WOODEN_PICKAXE ||
               item == Items.STONE_PICKAXE ||
               item == Items.IRON_PICKAXE ||
               item == Items.GOLDEN_PICKAXE ||
               item == Items.DIAMOND_PICKAXE ||
               item == Items.NETHERITE_PICKAXE;
    }

    public static int[] getClampedPositionAndSize(int x, int y, int width, int height, int winW, int winH) {
        x = Math.max(0, Math.min(x, winW - width));
        y = Math.max(0, Math.min(y, winH - height));
        x = Math.round((float)x / 5) * 5;
        y = Math.round((float)y / 5) * 5;
        x = Math.max(4, Math.min(x, winW - width - 4));
        y = Math.max(4, Math.min(y, winH - height - 4));
        return new int[]{x, y, width, height};
    }

    public static String formatMoney(double amount) {
        if (amount >= 1_000_000_000) return String.format("$%.3fB", amount / 1_000_000_000);
        if (amount >= 1_000_000) return String.format("$%.3fM", amount / 1_000_000);
        if (amount >= 1_000) return String.format("$%.3fK", amount / 1_000);
        return String.format("$%.3f", amount);
    }

    public static int snapToGrid(int value, int gridSize) {
        return Math.round((float)value / gridSize) * gridSize;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static int[] getWindowSize() {
        var window = MinecraftClient.getInstance().getWindow();
        return new int[]{window.getScaledWidth(), window.getScaledHeight()};
    }

    public static int getTextWidth(TextRenderer textRenderer, String text) {
        return textRenderer.getWidth(text);
    }

    public static String toSnakeCase(String input) {
        return input.replace("'", "")
                    .trim()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]+", "_")
                    .replaceAll("^_+|_+$", "");
    }

    public static double parseBalance(String balanceStr) {
        if (balanceStr == null) return 0.0;
        balanceStr = balanceStr.replace(",", "").trim();
        double multiplier = 1.0;
        if (balanceStr.endsWith("B")) {
            multiplier = 1_000_000_000;
            balanceStr = balanceStr.substring(0, balanceStr.length() - 1);
        } else if (balanceStr.endsWith("M")) {
            multiplier = 1_000_000;
            balanceStr = balanceStr.substring(0, balanceStr.length() - 1);
        } else if (balanceStr.endsWith("K")) {
            multiplier = 1_000;
            balanceStr = balanceStr.substring(0, balanceStr.length() - 1);
        }
        try {
            return Double.parseDouble(balanceStr) * multiplier;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
} 