package munchyutils.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.List;
import java.util.ArrayList;

public class CooldownTriggersScreen extends Screen {
    private final Screen parent;
    private List<CooldownTrigger> triggers;
    private ButtonWidget doneButton;

    public CooldownTriggersScreen(Screen parent) {
        super(Text.literal("Edit Cooldown Triggers"));
        this.parent = parent;
        this.triggers = new ArrayList<>(CooldownManager.getTriggers());
    }

    @Override
    protected void init() {
        int w = this.width / 2;
        int h = this.height / 2;
        int y = h - 80;
        // For now, just a Done button
        doneButton = ButtonWidget.builder(Text.literal("Done"), btn -> {
            CooldownManager.getTriggers().clear();
            CooldownManager.getTriggers().addAll(triggers);
            CooldownManager.saveTriggers();
            this.client.setScreen(parent);
        }).dimensions(w - 60, h + 80, 120, 20).build();
        this.addDrawableChild(doneButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Cooldown Triggers (WIP)"), this.width / 2, 20, 0xFFFFFF);
        int y = 50;
        for (CooldownTrigger trigger : triggers) {
            context.drawText(this.textRenderer, Text.literal(trigger.name + " [" + trigger.type + ", " + trigger.action + "] " + trigger.itemNamePart + " " + (trigger.cooldownMs / 1000) + "s " + trigger.color), 40, y, 0xFFFFFF, false);
            y += 16;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
} 