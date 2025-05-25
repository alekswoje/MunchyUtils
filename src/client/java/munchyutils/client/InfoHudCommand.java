package munchyutils.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import munchyutils.client.CooldownTrigger;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;

public class InfoHudCommand {
    private static final SuggestionProvider<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> TYPE_SUGGESTIONS = (context, builder) -> {
        builder.suggest("held");
        builder.suggest("worn");
        return builder.buildFuture();
    };
    private static final SuggestionProvider<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ACTION_SUGGESTIONS = (context, builder) -> {
        builder.suggest("crouch");
        builder.suggest("rclick");
        builder.suggest("lclick");
        builder.suggest("break");
        return builder.buildFuture();
    };
    private static final SuggestionProvider<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> FEATURE_SUGGESTIONS = (context, builder) -> {
        for (munchyutils.client.FeatureManager.ModFeature feature : munchyutils.client.FeatureManager.ModFeature.values()) {
            builder.suggest(feature.name().toLowerCase());
        }
        return builder.buildFuture();
    };
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("mu")
                .then(ClientCommandManager.literal("config")
                    .executes(ctx -> {
                        try {
                            MinecraftClient.getInstance().player.sendMessage(Text.literal("[MunchyUtils] Attempting to open test screen..."), false);
                            MinecraftClient.getInstance().setScreen(new TestScreen());
                        } catch (Exception e) {
                            MinecraftClient.getInstance().player.sendMessage(Text.literal("[MunchyUtils] Error opening test screen: " + e), false);
                            e.printStackTrace();
                        }
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("reset")
                    .executes(ctx -> {
                        munchyutils.client.InfoHudOverlay.session.reset();
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] Info HUD session has been reset."), false);
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("summary")
                    .then(ClientCommandManager.argument("copy", BoolArgumentType.bool())
                        .executes(ctx -> {
                            var session = munchyutils.client.InfoHudOverlay.session;
                            String income = session.getHourlyIncomeString();
                            String total = InfoHudSession.formatMoney(session.getTotalEarnings());
                            String sessionLength = session.getSessionLengthString().replace("Session: ", "");
                            String summary = String.format("[munchyutils] %s earned in %s (%s)", total, sessionLength, income);
                            net.minecraft.text.Text colorful = net.minecraft.text.Text.literal("")
                                .append(net.minecraft.text.Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
                                .append(net.minecraft.text.Text.literal(total).styled(s -> s.withColor(0x00BFFF)))
                                .append(net.minecraft.text.Text.literal(" earned in ").styled(s -> s.withColor(0xFFFFFF)))
                                .append(net.minecraft.text.Text.literal(sessionLength).styled(s -> s.withColor(0xFFD700)))
                                .append(net.minecraft.text.Text.literal(" (").styled(s -> s.withColor(0xFFFFFF)))
                                .append(net.minecraft.text.Text.literal(income).styled(s -> s.withColor(0xFFA500)))
                                .append(net.minecraft.text.Text.literal(")").styled(s -> s.withColor(0xFFFFFF)));
                            boolean copy = BoolArgumentType.getBool(ctx, "copy");
                            if (copy) {
                                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(summary);
                                MinecraftClient.getInstance().player.sendMessage(colorful.copy().append(net.minecraft.text.Text.literal(" (copied to clipboard)").styled(s -> s.withColor(0xAAAAAA))), false);
                            } else {
                                MinecraftClient.getInstance().player.sendMessage(colorful, false);
                            }
                            return 1;
                        })
                    )
                    .executes(ctx -> {
                        var session = munchyutils.client.InfoHudOverlay.session;
                        String income = session.getHourlyIncomeString();
                        String total = InfoHudSession.formatMoney(session.getTotalEarnings());
                        String sessionLength = session.getSessionLengthString().replace("Session: ", "");
                        String summary = String.format("[munchyutils] %s earned in %s (%s)", total, sessionLength, income);
                        net.minecraft.text.Text colorful = net.minecraft.text.Text.literal("")
                            .append(net.minecraft.text.Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
                            .append(net.minecraft.text.Text.literal(total).styled(s -> s.withColor(0x00BFFF)))
                            .append(net.minecraft.text.Text.literal(" earned in ").styled(s -> s.withColor(0xFFFFFF)))
                            .append(net.minecraft.text.Text.literal(sessionLength).styled(s -> s.withColor(0xFFD700)))
                            .append(net.minecraft.text.Text.literal(" (").styled(s -> s.withColor(0xFFFFFF)))
                            .append(net.minecraft.text.Text.literal(income).styled(s -> s.withColor(0xFFA500)))
                            .append(net.minecraft.text.Text.literal(")").styled(s -> s.withColor(0xFFFFFF)));
                        MinecraftClient.getInstance().player.sendMessage(colorful, false);
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("cooldown")
                    .then(ClientCommandManager.literal("trigger")
                        .then(ClientCommandManager.literal("add")
                            .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                .then(ClientCommandManager.argument("type", StringArgumentType.string())
                                    .suggests(TYPE_SUGGESTIONS)
                                    .then(ClientCommandManager.argument("action", StringArgumentType.string())
                                        .suggests(ACTION_SUGGESTIONS)
                                        .then(ClientCommandManager.argument("itemNamePart", StringArgumentType.string())
                                            .then(ClientCommandManager.argument("cooldownSeconds", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    String name = StringArgumentType.getString(context, "name");
                                                    String typeStr = StringArgumentType.getString(context, "type");
                                                    String actionStr = StringArgumentType.getString(context, "action");
                                                    String itemNamePart = StringArgumentType.getString(context, "itemNamePart");
                                                    int cooldownSeconds = IntegerArgumentType.getInteger(context, "cooldownSeconds");
                                                    CooldownTrigger.Type type;
                                                    CooldownTrigger.Action action;
                                                    try {
                                                        type = CooldownTrigger.Type.valueOf(typeStr.toUpperCase());
                                                        if (actionStr.equalsIgnoreCase("break")) {
                                                            action = CooldownTrigger.Action.BREAK;
                                                        } else {
                                                            action = CooldownTrigger.Action.valueOf(actionStr.toUpperCase());
                                                        }
                                                    } catch (Exception e) {
                                                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] Invalid type or action. Types: held, worn. Actions: crouch, rclick, lclick."), false);
                                                        return 0;
                                                    }
                                                    if (munchyutils.client.CooldownManager.getTriggerByName(name) != null) {
                                                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] A trigger with that name already exists."), false);
                                                        return 0;
                                                    }
                                                    CooldownTrigger trigger = new CooldownTrigger(name, type, action, itemNamePart, cooldownSeconds * 1000L);
                                                    munchyutils.client.CooldownManager.addTrigger(trigger);
                                                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Added trigger: " + name), false);
                                                    return 1;
                                                })
                                            )
                                        )
                                    )
                                )
                            )
                        )
                        .then(ClientCommandManager.literal("remove")
                            .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    boolean removed = munchyutils.client.CooldownManager.removeTrigger(name);
                                    if (removed) {
                                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] Removed trigger: " + name), false);
                                    } else {
                                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] No trigger found with that name."), false);
                                    }
                                    return 1;
                                })
                            )
                        )
                        .then(ClientCommandManager.literal("list")
                            .executes(context -> {
                                var triggers = munchyutils.client.CooldownManager.getTriggers();
                                if (triggers.isEmpty()) {
                                    MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] No triggers defined."), false);
                                } else {
                                    for (CooldownTrigger t : triggers) {
                                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] " + t.name + ": " + t.type + ", " + t.action + ", '" + t.itemNamePart + "', " + (t.cooldownMs/1000) + "s"), false);
                                    }
                                }
                                return 1;
                            })
                        )
                    )
                )
                .then(ClientCommandManager.literal("feature")
                    .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            var features = munchyutils.client.FeatureManager.getAll();
                            for (var entry : features.entrySet()) {
                                String name = entry.getKey().name().toLowerCase();
                                boolean enabled = entry.getValue();
                                int color = enabled ? 0x55FF55 : 0xFF5555; // green or red
                                MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("[munchyutils] " + (enabled ? "[ENABLED] " : "[DISABLED] ") + name).styled(s -> s.withColor(color)), false);
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("toggle")
                        .then(ClientCommandManager.argument("feature", StringArgumentType.string())
                            .suggests(FEATURE_SUGGESTIONS)
                            .executes(ctx -> {
                                String featureName = StringArgumentType.getString(ctx, "feature").toUpperCase();
                                try {
                                    var feature = munchyutils.client.FeatureManager.ModFeature.valueOf(featureName);
                                    munchyutils.client.FeatureManager.toggle(feature);
                                    boolean enabled = munchyutils.client.FeatureManager.isEnabled(feature);
                                    int color = enabled ? 0x55FF55 : 0xFF5555;
                                    MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("[munchyutils] " + (enabled ? "[ENABLED] " : "[DISABLED] ") + featureName.toLowerCase()).styled(s -> s.withColor(color)), false);
                                } catch (IllegalArgumentException e) {
                                    MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("[munchyutils] Unknown feature: " + featureName).styled(s -> s.withColor(0xFF5555)), false);
                                }
                                return 1;
                            })
                        )
                    )
                )
                .then(ClientCommandManager.argument("feature", StringArgumentType.string())
                    .then(ClientCommandManager.literal("reset")
                        .executes(ctx -> {
                            String feature = StringArgumentType.getString(ctx, "feature");
                            if (feature.equalsIgnoreCase("infohud")) {
                                munchyutils.client.InfoHudOverlay.session.reset();
                                MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] Info HUD session has been reset."), false);
                                return 1;
                            } else {
                                MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] Unknown feature: " + feature), false);
                                return 0;
                            }
                        })
                    )
                    .then(ClientCommandManager.literal("summary")
                        .then(ClientCommandManager.argument("copy", BoolArgumentType.bool())
                            .executes(ctx -> {
                                String feature = StringArgumentType.getString(ctx, "feature");
                                if (feature.equalsIgnoreCase("infohud")) {
                                    var session = munchyutils.client.InfoHudOverlay.session;
                                    String income = session.getHourlyIncomeString();
                                    String total = InfoHudSession.formatMoney(session.getTotalEarnings());
                                    String sessionLength = session.getSessionLengthString().replace("Session: ", "");
                                    String summary = String.format("[munchyutils] %s earned in %s (%s)", total, sessionLength, income);
                                    net.minecraft.text.Text colorful = net.minecraft.text.Text.literal("")
                                        .append(net.minecraft.text.Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
                                        .append(net.minecraft.text.Text.literal(total).styled(s -> s.withColor(0x00BFFF)))
                                        .append(net.minecraft.text.Text.literal(" earned in ").styled(s -> s.withColor(0xFFFFFF)))
                                        .append(net.minecraft.text.Text.literal(sessionLength).styled(s -> s.withColor(0xFFD700)))
                                        .append(net.minecraft.text.Text.literal(" (").styled(s -> s.withColor(0xFFFFFF)))
                                        .append(net.minecraft.text.Text.literal(income).styled(s -> s.withColor(0xFFA500)))
                                        .append(net.minecraft.text.Text.literal(")").styled(s -> s.withColor(0xFFFFFF)));
                                    boolean copy = BoolArgumentType.getBool(ctx, "copy");
                                    if (copy) {
                                        net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(summary);
                                        MinecraftClient.getInstance().player.sendMessage(colorful.copy().append(net.minecraft.text.Text.literal(" (copied to clipboard)").styled(s -> s.withColor(0xAAAAAA))), false);
                                    } else {
                                        MinecraftClient.getInstance().player.sendMessage(colorful, false);
                                    }
                                    return 1;
                                } else {
                                    MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] Unknown feature: " + feature), false);
                                    return 0;
                                }
                            })
                        )
                        .executes(ctx -> {
                            String feature = StringArgumentType.getString(ctx, "feature");
                            if (feature.equalsIgnoreCase("infohud")) {
                                var session = munchyutils.client.InfoHudOverlay.session;
                                String income = session.getHourlyIncomeString();
                                String total = InfoHudSession.formatMoney(session.getTotalEarnings());
                                String sessionLength = session.getSessionLengthString().replace("Session: ", "");
                                String summary = String.format("[munchyutils] %s earned in %s (%s)", total, sessionLength, income);
                                net.minecraft.text.Text colorful = net.minecraft.text.Text.literal("")
                                    .append(net.minecraft.text.Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
                                    .append(net.minecraft.text.Text.literal(total).styled(s -> s.withColor(0x00BFFF)))
                                    .append(net.minecraft.text.Text.literal(" earned in ").styled(s -> s.withColor(0xFFFFFF)))
                                    .append(net.minecraft.text.Text.literal(sessionLength).styled(s -> s.withColor(0xFFD700)))
                                    .append(net.minecraft.text.Text.literal(" (").styled(s -> s.withColor(0xFFFFFF)))
                                    .append(net.minecraft.text.Text.literal(income).styled(s -> s.withColor(0xFFA500)))
                                    .append(net.minecraft.text.Text.literal(")").styled(s -> s.withColor(0xFFFFFF)));
                                MinecraftClient.getInstance().player.sendMessage(colorful, false);
                                return 1;
                            } else {
                                MinecraftClient.getInstance().player.sendMessage(Text.literal("[munchyutils] Unknown feature: " + feature), false);
                                return 0;
                            }
                        })
                    )
                )
            );
        });
    }

    // Minimal test screen for debugging
    static class TestScreen extends Screen {
        public TestScreen() { super(Text.literal("Test Screen")); }
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            String msg = "Hello from TestScreen!";
            int x = (this.width - this.textRenderer.getWidth(msg)) / 2;
            int y = this.height / 2;
            context.drawText(this.textRenderer, msg, x, y, 0xFFFFFF, false);
        }
    }
} 