package munchyutils.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MunchyConfigScreen {
    public static Screen create(Screen parent) {
        MunchyConfig config = MunchyConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("MunchyUtils HUD Config"));
        ConfigCategory infoHud = builder.getOrCreateCategory(Text.literal("Info HUD"));
        ConfigCategory cooldownHud = builder.getOrCreateCategory(Text.literal("Cooldown HUD"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Info HUD sliders
        infoHud.addEntry(entryBuilder.startIntSlider(Text.literal("X Position"), config.getInfoHudX(), -1, 5000)
            .setDefaultValue(-1)
            .setSaveConsumer(val -> { config.setInfoHudX(val); })
            .build());
        infoHud.addEntry(entryBuilder.startIntSlider(Text.literal("Y Position"), config.getInfoHudY(), -1, 5000)
            .setDefaultValue(-1)
            .setSaveConsumer(val -> { config.setInfoHudY(val); })
            .build());
        infoHud.addEntry(entryBuilder.startFloatField(Text.literal("Scale"), config.getInfoHudScale())
            .setDefaultValue(1.0f)
            .setMin(0.5f)
            .setMax(3.0f)
            .setSaveConsumer(val -> { config.setInfoHudScale(val); })
            .build());

        // Cooldown HUD sliders
        cooldownHud.addEntry(entryBuilder.startIntSlider(Text.literal("X Position"), config.getCooldownHudX(), -1, 5000)
            .setDefaultValue(-1)
            .setSaveConsumer(val -> { config.setCooldownHudX(val); })
            .build());
        cooldownHud.addEntry(entryBuilder.startIntSlider(Text.literal("Y Position"), config.getCooldownHudY(), -1, 5000)
            .setDefaultValue(-1)
            .setSaveConsumer(val -> { config.setCooldownHudY(val); })
            .build());
        cooldownHud.addEntry(entryBuilder.startFloatField(Text.literal("Scale"), config.getCooldownHudScale())
            .setDefaultValue(1.0f)
            .setMin(0.5f)
            .setMax(3.0f)
            .setSaveConsumer(val -> { config.setCooldownHudScale(val); })
            .build());

        builder.setSavingRunnable(() -> config.save());
        return builder.build();
    }
} 