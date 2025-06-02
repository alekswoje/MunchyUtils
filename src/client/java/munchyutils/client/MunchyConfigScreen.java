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
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import java.util.List;
import java.util.ArrayList;
import munchyutils.client.CooldownTrigger.Type;
import munchyutils.client.CooldownTrigger.Action;

public class MunchyConfigScreen {
    public static Screen create(Screen parent) {
        MunchyConfig config = MunchyConfig.get();
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

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
        Option<String> trackedPlayerNameOrUuid = Option.createBuilder(String.class)
            .name(Text.literal("Tracked Player Name/UUID"))
            .description(OptionDescription.of(Text.literal("Warn and play a sound when any of these players log in (name or UUID, comma-separated list). Leave blank to disable.")))
            .binding(config.getTrackedPlayerNameOrUuid(), config::getTrackedPlayerNameOrUuid, config::setTrackedPlayerNameOrUuid)
            .controller(opt -> StringControllerBuilder.create(opt))
            .build();
        Option<Boolean> trackPlayerLogout = Option.createBuilder(Boolean.class)
            .name(Text.literal("Track Tracked Player Logouts"))
            .description(OptionDescription.of(Text.literal("Show a warning and play a sound when a tracked player logs out.")))
            .binding(config.isTrackPlayerLogout(), config::isTrackPlayerLogout, config::setTrackPlayerLogout)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();

        // Edit HUD Layout button
        ButtonOption editHudButton = ButtonOption.createBuilder()
            .name(Text.literal("Edit HUD Layout"))
            .description(OptionDescription.of(Text.literal("Drag and resize HUD overlays directly on your screen.")))
            .action((screen) -> {
                MinecraftClient.getInstance().setScreen(new HudEditScreen());
            })
            .build();

        // Reset HUD Locations button
        ButtonOption resetHudButton = ButtonOption.createBuilder()
            .name(Text.literal("Reset HUD Locations"))
            .description(OptionDescription.of(Text.literal("Reset all HUD positions and scales to default.")))
            .action(screen -> {
                MinecraftClient.getInstance().setScreen(new ResetConfirmationScreen(screen, () -> {
                    config.save();
                }));
            })
            .build();

        // Add HUD layout options to General tab
        ConfigCategory generalCategory = ConfigCategory.createBuilder()
            .name(Text.literal("General"))
            .option(updateCheckEnabled)
            .option(hideInventoryFullMessage)
            .option(hideSellSuccessMessage)
            .option(trackedPlayerNameOrUuid)
            .option(trackPlayerLogout)
            .option(editHudButton)
            .option(resetHudButton)
            .build();

        // --- Cooldown Triggers Tab ---
        ButtonOption editTriggersButton = ButtonOption.createBuilder()
            .name(Text.literal("Edit Cooldown Triggers"))
            .description(OptionDescription.of(Text.literal("Open the trigger management screen.")))
            .action(screen -> {
                MinecraftClient.getInstance().setScreen(new CooldownTriggersScreen(screen));
            })
            .build();

        ConfigCategory triggersCategory = ConfigCategory.createBuilder()
            .name(Text.literal("Cooldown Triggers"))
            .option(editTriggersButton)
            .option(ButtonOption.createBuilder()
                .name(Text.literal("Reset to Default"))
                .description(OptionDescription.of(Text.literal("Restore default triggers from JSON.")))
                .action(screen -> {
                    CooldownManager.load();
                })
                .build())
            .build();

        // --- Fishing HUD options ---
        Option<Boolean> fishingHudAfkTimeoutEnabled = Option.createBuilder(Boolean.class)
            .name(Text.literal("Enable Fishing HUD AFK Timeout"))
            .description(OptionDescription.of(Text.literal("Enable or disable the AFK timeout for Fishing HUD.")))
            .binding(config.isFishingHudAfkTimeoutEnabled(), config::isFishingHudAfkTimeoutEnabled, config::setFishingHudAfkTimeoutEnabled)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Integer> fishingHudAfkTimeoutMs = Option.createBuilder(Integer.class)
            .name(Text.literal("Fishing HUD AFK Timeout (ms)"))
            .description(OptionDescription.of(Text.literal("Time (ms) of inactivity before Fishing HUD session is marked AFK.")))
            .binding(config.getFishingHudAfkTimeoutMs(), config::getFishingHudAfkTimeoutMs, config::setFishingHudAfkTimeoutMs)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1000, 600_000).step(1000))
            .build();
        Option<Boolean> fishingHudSessionTimeoutEnabled = Option.createBuilder(Boolean.class)
            .name(Text.literal("Enable Fishing HUD Session Reset Timeout"))
            .description(OptionDescription.of(Text.literal("Enable or disable the session reset timeout for Fishing HUD.")))
            .binding(config.isFishingHudSessionTimeoutEnabled(), config::isFishingHudSessionTimeoutEnabled, config::setFishingHudSessionTimeoutEnabled)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Integer> fishingHudSessionTimeoutMs = Option.createBuilder(Integer.class)
            .name(Text.literal("Fishing HUD Session Reset Timeout (ms)"))
            .description(OptionDescription.of(Text.literal("Time (ms) of inactivity before Fishing HUD session resets.")))
            .binding(config.getFishingHudSessionTimeoutMs(), config::getFishingHudSessionTimeoutMs, config::setFishingHudSessionTimeoutMs)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1000, 600_000).step(1000))
            .build();
        ConfigCategory fishingHudCategory = ConfigCategory.createBuilder()
            .name(Text.literal("FishingHud"))
            .option(fishingHudAfkTimeoutEnabled)
            .option(fishingHudAfkTimeoutMs)
            .option(fishingHudSessionTimeoutEnabled)
            .option(fishingHudSessionTimeoutMs)
            .build();

        // --- Mining HUD options ---
        Option<Boolean> preventPorgUseIfActive = Option.createBuilder(Boolean.class)
            .name(Text.literal("Prevent Porg Use if Buff Active"))
            .description(OptionDescription.of(Text.literal("Prevents right-clicking Roasted Porg if the Porg buff is already active (2 min cooldown).")))
            .binding(config.isPreventPorgUseIfActive(), config::isPreventPorgUseIfActive, config::setPreventPorgUseIfActive)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Boolean> miningHudAfkTimeoutEnabled = Option.createBuilder(Boolean.class)
            .name(Text.literal("Enable Mining HUD AFK Timeout"))
            .description(OptionDescription.of(Text.literal("Enable or disable the AFK timeout for Mining HUD.")))
            .binding(config.isMiningHudAfkTimeoutEnabled(), config::isMiningHudAfkTimeoutEnabled, config::setMiningHudAfkTimeoutEnabled)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Integer> miningHudAfkTimeoutMs = Option.createBuilder(Integer.class)
            .name(Text.literal("Mining HUD AFK Timeout (ms)"))
            .description(OptionDescription.of(Text.literal("Time (ms) of inactivity before Mining HUD session is marked AFK.")))
            .binding(config.getMiningHudAfkTimeoutMs(), config::getMiningHudAfkTimeoutMs, config::setMiningHudAfkTimeoutMs)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1000, 600_000).step(1000))
            .build();
        Option<Boolean> miningHudSessionTimeoutEnabled = Option.createBuilder(Boolean.class)
            .name(Text.literal("Enable Mining HUD Session Reset Timeout"))
            .description(OptionDescription.of(Text.literal("Enable or disable the session reset timeout for Mining HUD.")))
            .binding(config.isMiningHudSessionTimeoutEnabled(), config::isMiningHudSessionTimeoutEnabled, config::setMiningHudSessionTimeoutEnabled)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Integer> miningHudSessionTimeoutMs = Option.createBuilder(Integer.class)
            .name(Text.literal("Mining HUD Session Reset Timeout (ms)"))
            .description(OptionDescription.of(Text.literal("Time (ms) of inactivity before Mining HUD session resets.")))
            .binding(config.getMiningHudSessionTimeoutMs(), config::getMiningHudSessionTimeoutMs, config::setMiningHudSessionTimeoutMs)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1000, 600_000).step(1000))
            .build();
        Option<Boolean> hideNearbyGroundItemsWhenMining = Option.createBuilder(Boolean.class)
            .name(Text.literal("Hide Ground Items Near Player When Mining"))
            .description(OptionDescription.of(Text.literal("Hide ground items (dropped items) within 1.5 blocks of you while mining.")))
            .binding(config.isHideNearbyGroundItemsWhenMining(), config::isHideNearbyGroundItemsWhenMining, config::setHideNearbyGroundItemsWhenMining)
            .controller(opt -> dev.isxander.yacl3.api.controller.BooleanControllerBuilder.create(opt))
            .build();
        Option<Float> nearbyGroundItemScale = Option.createBuilder(Float.class)
            .name(Text.literal("Scale of Nearby Ground Items When Mining"))
            .description(OptionDescription.of(Text.literal("Scale ground items within 1.5 blocks of you while mining. 1.0 = normal, 0.2 = 5x smaller. Only applies if hiding is disabled.")))
            .binding(config.getNearbyGroundItemScale(), config::getNearbyGroundItemScale, config::setNearbyGroundItemScale)
            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 1.0f).step(0.05f))
            .build();
        ConfigCategory miningHudCategory = ConfigCategory.createBuilder()
            .name(Text.literal("MiningHud"))
            .option(preventPorgUseIfActive)
            .option(miningHudAfkTimeoutEnabled)
            .option(miningHudAfkTimeoutMs)
            .option(miningHudSessionTimeoutEnabled)
            .option(miningHudSessionTimeoutMs)
            .option(hideNearbyGroundItemsWhenMining)
            .option(nearbyGroundItemScale)
            .build();

        // Build the config screen
        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("MunchyUtils Config"))
            .category(generalCategory)
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