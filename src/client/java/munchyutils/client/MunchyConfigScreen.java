package munchyutils.client;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.ButtonOption;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class MunchyConfigScreen {
    public static Screen create(Screen parent) {
        MunchyConfig config = MunchyConfig.get();
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        // Info HUD options
        Option<Integer> infoHudX = Option.createBuilder(Integer.class)
            .name(Text.literal("X Position"))
            .description(OptionDescription.of(Text.literal("Horizontal position of the Info HUD")))
            .binding(config.getInfoHudX(), config::getInfoHudX, config::setInfoHudX)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, screenWidth).step(1))
            .build();
        Option<Integer> infoHudY = Option.createBuilder(Integer.class)
            .name(Text.literal("Y Position"))
            .description(OptionDescription.of(Text.literal("Vertical position of the Info HUD")))
            .binding(config.getInfoHudY(), config::getInfoHudY, config::setInfoHudY)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, screenHeight).step(1))
            .build();
        Option<Float> infoHudScale = Option.createBuilder(Float.class)
            .name(Text.literal("Scale (0.5-3.0)"))
            .description(OptionDescription.of(Text.literal("Scale of the Info HUD")))
            .binding(config.getInfoHudScale(), config::getInfoHudScale, config::setInfoHudScale)
            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 3.0f).step(0.01f))
            .build();

        // Cooldown HUD options
        Option<Integer> cooldownHudX = Option.createBuilder(Integer.class)
            .name(Text.literal("X Position"))
            .description(OptionDescription.of(Text.literal("Horizontal position of the Cooldown HUD")))
            .binding(config.getCooldownHudX(), config::getCooldownHudX, config::setCooldownHudX)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, screenWidth).step(1))
            .build();
        Option<Integer> cooldownHudY = Option.createBuilder(Integer.class)
            .name(Text.literal("Y Position"))
            .description(OptionDescription.of(Text.literal("Vertical position of the Cooldown HUD")))
            .binding(config.getCooldownHudY(), config::getCooldownHudY, config::setCooldownHudY)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, screenHeight).step(1))
            .build();
        Option<Float> cooldownHudScale = Option.createBuilder(Float.class)
            .name(Text.literal("Scale (0.5-3.0)"))
            .description(OptionDescription.of(Text.literal("Scale of the Cooldown HUD")))
            .binding(config.getCooldownHudScale(), config::getCooldownHudScale, config::setCooldownHudScale)
            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 3.0f).step(0.01f))
            .build();

        // Build categories
        ConfigCategory infoHudCategory = ConfigCategory.createBuilder()
            .name(Text.literal("Info HUD"))
            .option(infoHudX)
            .option(infoHudY)
            .option(infoHudScale)
            .build();

        ConfigCategory cooldownHudCategory = ConfigCategory.createBuilder()
            .name(Text.literal("Cooldown HUD"))
            .option(cooldownHudX)
            .option(cooldownHudY)
            .option(cooldownHudScale)
            .build();

        // Placeholder for triggers (list editing to be implemented)
        ConfigCategory triggersCategory = ConfigCategory.createBuilder()
            .name(Text.literal("Cooldown Triggers"))
            .option(Option.createBuilder(String.class)
                .name(Text.literal("Cooldown Triggers"))
                .description(OptionDescription.of(Text.literal("Trigger editing coming soon!")))
                .binding("", () -> "", v -> {})
                .controller(opt -> StringControllerBuilder.create(opt))
                .build())
            .build();

        // Placeholders for FishingHud and MiningHud
        ConfigCategory fishingHudCategory = ConfigCategory.createBuilder()
            .name(Text.literal("FishingHud"))
            .option(Option.createBuilder(String.class)
                .name(Text.literal("FishingHud"))
                .description(OptionDescription.of(Text.literal("Coming soon")))
                .binding("", () -> "", v -> {})
                .controller(opt -> StringControllerBuilder.create(opt))
                .build())
            .build();
        ConfigCategory miningHudCategory = ConfigCategory.createBuilder()
            .name(Text.literal("MiningHud"))
            .option(Option.createBuilder(String.class)
                .name(Text.literal("MiningHud"))
                .description(OptionDescription.of(Text.literal("Coming soon")))
                .binding("", () -> "", v -> {})
                .controller(opt -> StringControllerBuilder.create(opt))
                .build())
            .build();

        // Edit HUD Layout button
        ButtonOption editHudButton = ButtonOption.createBuilder()
            .name(Text.literal("Edit HUD Layout"))
            .description(OptionDescription.of(Text.literal("Drag and resize HUD overlays directly on your screen.")))
            .action((screen) -> {
                MinecraftClient.getInstance().setScreen(new HudEditScreen());
            })
            .build();

        // Edit HUD Layout button
        ButtonOption resetAllButton = ButtonOption.createBuilder()
            .name(Text.literal("Reset All"))
            .description(OptionDescription.of(Text.literal("Reset all HUD positions and scales to default.")))
            .action(screen -> {
                MinecraftClient.getInstance().setScreen(new ResetConfirmationScreen(screen, () -> {
                    config.setInfoHudX(0);
                    config.setInfoHudY(0);
                    config.setInfoHudScale(1.0f);
                    config.setCooldownHudX(0);
                    config.setCooldownHudY(0);
                    config.setCooldownHudScale(1.0f);
                    config.save();
                }));
            })
            .build();

        ConfigCategory hudLayoutCategory = ConfigCategory.createBuilder()
            .name(Text.literal("HUD Layout"))
            .option(editHudButton)
            .option(resetAllButton)
            .build();

        // Build the config screen
        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("MunchyUtils HUD Config"))
            .category(hudLayoutCategory)
            .category(infoHudCategory)
            .category(cooldownHudCategory)
            .category(triggersCategory)
            .category(fishingHudCategory)
            .category(miningHudCategory)
            .build()
            .generateScreen(parent);
    }

    // Confirmation dialog for reset
    public static class ResetConfirmationScreen extends Screen {
        private final Screen parent;
        private final Runnable onConfirm;
        public ResetConfirmationScreen(Screen parent, Runnable onConfirm) {
            super(Text.literal("Confirm Reset"));
            this.parent = parent;
            this.onConfirm = onConfirm;
        }
        @Override
        protected void init() {
            int w = this.width / 2;
            int h = this.height / 2;
            this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("Yes, reset all"), btn -> {
                onConfirm.run();
                this.client.setScreen(parent);
            }).dimensions(w - 80, h, 160, 20).build());
            this.addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("Cancel"), btn -> {
                this.client.setScreen(parent);
            }).dimensions(w - 80, h + 24, 160, 20).build());
        }
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Are you sure you want to reset all HUD settings to default?"), this.width / 2, this.height / 2 - 40, 0xFFFFFF);
            super.render(context, mouseX, mouseY, delta);
        }
    }
} 