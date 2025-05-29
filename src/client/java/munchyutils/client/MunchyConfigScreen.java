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

        // General options
        Option<Boolean> updateCheckEnabled = Option.createBuilder(Boolean.class)
            .name(Text.literal("Enable Update Checker"))
            .description(OptionDescription.of(Text.literal("Check for new MunchyUtils releases on GitHub and warn if out of date.")))
            .binding(config.isUpdateCheckEnabled(), config::isUpdateCheckEnabled, config::setUpdateCheckEnabled)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Boolean> hideInventoryFullMessage = Option.createBuilder(Boolean.class)
            .name(Text.literal("Hide 'Inventory Full' Message"))
            .description(OptionDescription.of(Text.literal("Suppresses the 'Your inventory is full! Click here to sell your items, or type /sell!' chat message.")))
            .binding(config.isHideInventoryFullMessage(), config::isHideInventoryFullMessage, config::setHideInventoryFullMessage)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Boolean> hideSellSuccessMessage = Option.createBuilder(Boolean.class)
            .name(Text.literal("Hide Sell Success Message"))
            .description(OptionDescription.of(Text.literal("Suppresses the 'Successfully sold ... items for $...' chat message.")))
            .binding(config.isHideSellSuccessMessage(), config::isHideSellSuccessMessage, config::setHideSellSuccessMessage)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();

        // Build categories
        ConfigCategory generalCategory = ConfigCategory.createBuilder()
            .name(Text.literal("General"))
            .option(updateCheckEnabled)
            .option(hideInventoryFullMessage)
            .option(hideSellSuccessMessage)
            .build();

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
        Option<Boolean> preventPorgUseIfActive = Option.createBuilder(Boolean.class)
            .name(Text.literal("Prevent Porg Use if Buff Active"))
            .description(OptionDescription.of(Text.literal("Prevents right-clicking Roasted Porg if the Porg buff is already active (2 min cooldown).")))
            .binding(config.isPreventPorgUseIfActive(), config::isPreventPorgUseIfActive, config::setPreventPorgUseIfActive)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        ConfigCategory miningHudCategory = ConfigCategory.createBuilder()
            .name(Text.literal("MiningHud"))
            .option(preventPorgUseIfActive)
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

        // New Cellshop options
        Option<Boolean> autoAnnounceEnabled = Option.createBuilder(Boolean.class)
            .name(Text.literal("Auto Announce"))
            .description(OptionDescription.of(Text.literal("Automatically types /cellshop announce every 20 minutes and 5 seconds.")))
            .binding(config.isAutoAnnounceEnabled(), config::isAutoAnnounceEnabled, config::setAutoAnnounceEnabled)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Boolean> rotatingAnnouncementsEnabled = Option.createBuilder(Boolean.class)
            .name(Text.literal("Rotating Announcements"))
            .description(OptionDescription.of(Text.literal("Rotate between multiple announcements. If enabled, after each announce, the next message in the list will be set using /cellshop setannouncement.")))
            .binding(config.isRotatingAnnouncementsEnabled(), config::isRotatingAnnouncementsEnabled, config::setRotatingAnnouncementsEnabled)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<String> announcementList = Option.createBuilder(String.class)
            .name(Text.literal("Announcement List"))
            .description(OptionDescription.of(Text.literal("Enter announcements separated by | (pipe). Used for rotating announcements.")))
            .binding(String.join("|", config.getAnnouncementList()), () -> String.join("|", config.getAnnouncementList()), v -> config.setAnnouncementList(java.util.Arrays.asList(v.split("\\|"))))
            .controller(opt -> StringControllerBuilder.create(opt))
            .build();
        ConfigCategory cellshopCategory = ConfigCategory.createBuilder()
            .name(Text.literal("Cellshop"))
            .option(autoAnnounceEnabled)
            .option(rotatingAnnouncementsEnabled)
            .option(announcementList)
            .build();

        // Build the config screen
        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("MunchyUtils Config"))
            .category(generalCategory)
            .category(cooldownHudCategory)
            .category(infoHudCategory)
            .category(triggersCategory)
            .category(fishingHudCategory)
            .category(miningHudCategory)
            .category(cellshopCategory)
            .category(hudLayoutCategory)
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