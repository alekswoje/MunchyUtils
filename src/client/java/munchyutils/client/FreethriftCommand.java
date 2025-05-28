package munchyutils.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import munchyutils.client.Utils;

public class FreethriftCommand {
    private static final String JSON_RESOURCE = "/mu_freethrift_items.json";
    private static final Gson GSON = new Gson();
    private static List<ItemInfo> items = null;
    private static Map<String, ItemInfo> tagToItem = null;
    private static Map<String, ItemInfo> displayNameToItem = null;

    private static void loadItems() {
        if (items != null) return;
        try {
            InputStream is = FreethriftCommand.class.getResourceAsStream(JSON_RESOURCE);
            if (is == null) {
                System.err.println("[FreethriftCommand] Could not find resource: " + JSON_RESOURCE);
                items = new ArrayList<>();
                tagToItem = new HashMap<>();
                displayNameToItem = new HashMap<>();
                return;
            }
            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<ItemInfo>>(){}.getType();
            items = GSON.fromJson(reader, listType);
            tagToItem = new HashMap<>();
            displayNameToItem = new HashMap<>();
            for (ItemInfo item : items) {
                tagToItem.put(item.tag.toLowerCase(), item);
                displayNameToItem.put(item.display_name.toLowerCase(), item);
            }
        } catch (Exception e) {
            System.err.println("[FreethriftCommand] Failed to load items: " + e.getMessage());
            items = new ArrayList<>();
            tagToItem = new HashMap<>();
            displayNameToItem = new HashMap<>();
        }
    }

    private static final SuggestionProvider<FabricClientCommandSource> ITEM_SUGGESTIONS = (context, builder) -> {
        loadItems();
        for (ItemInfo item : items) {
            builder.suggest(item.tag);
        }
        return builder.buildFuture();
    };

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("mu")
                    .then(ClientCommandManager.literal("freethrift")
                        .executes(ctx -> {
                            loadItems();
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player == null) {
                                ctx.getSource().sendFeedback(Text.literal("[munchyutils] Player not found."));
                                return 0;
                            }
                            ItemStack held = client.player.getMainHandStack();
                            if (held.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal("[munchyutils] You must hold an item or specify an item tag."));
                                return 0;
                            }
                            String displayName = Utils.stripColorCodes(held.getName().getString());
                            String tag = Utils.toSnakeCase(displayName);
                            ItemInfo item = tagToItem.get(tag);
                            if (item == null) {
                                MutableText msg = buildNoInfoMessage(displayName, tag);
                                maybeAppendEasterEgg(msg);
                                ctx.getSource().sendFeedback(msg);
                                return 0;
                            }
                            MutableText colorful = buildItemInfoMessage(item);
                            maybeAppendEasterEgg(colorful);
                            ctx.getSource().sendFeedback(colorful);
                            return 1;
                        })
                        .then(ClientCommandManager.argument("tag", StringArgumentType.string())
                            .suggests(ITEM_SUGGESTIONS)
                            .executes(ctx -> {
                                loadItems();
                                String tag = StringArgumentType.getString(ctx, "tag").toLowerCase();
                                ItemInfo item = tagToItem.get(tag);
                                if (item == null) {
                                    MutableText msg = buildNoInfoTagMessage(tag);
                                    maybeAppendEasterEgg(msg);
                                    ctx.getSource().sendFeedback(msg);
                                    return 0;
                                }
                                MutableText colorful = buildItemInfoMessage(item);
                                maybeAppendEasterEgg(colorful);
                                ctx.getSource().sendFeedback(colorful);
                                return 1;
                            })
                            .then(ClientCommandManager.argument("copy", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    loadItems();
                                    String tag = StringArgumentType.getString(ctx, "tag").toLowerCase();
                                    boolean copy = BoolArgumentType.getBool(ctx, "copy");
                                    ItemInfo item = tagToItem.get(tag);
                                    if (item == null) {
                                        MutableText msg = Text.literal("")
                                            .append(Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
                                            .append(Text.literal("No thrift info for tag: ").styled(s -> s.withColor(0x00BFFF)))
                                            .append(Text.literal(tag).styled(s -> s.withColor(0xFFA500)))
                                            .append(Text.literal("\nIf you'd like to add it, please DM me on Discord (alwoj) or make a PR to the GitHub: https://github.com/alekswoje/munchyutils.").styled(s -> s.withColor(0xAAAAAA)));
                                        maybeAppendEasterEgg(msg);
                                        ctx.getSource().sendFeedback(msg);
                                        return 0;
                                    }
                                    String prefix = "[munchyutils] ";
                                    String label = "freethrift ";
                                    String fullMsg = prefix + label + item.display_name + ": " + item.description;
                                    if (item.note != null && !item.note.isEmpty()) {
                                        fullMsg += "\nNote: " + item.note;
                                    }
                                    MutableText colorful = buildItemInfoMessage(item);
                                    maybeAppendEasterEgg(colorful);
                                    if (copy) {
                                        MinecraftClient.getInstance().keyboard.setClipboard(fullMsg);
                                        ctx.getSource().sendFeedback(colorful.copy().append(Text.literal(" (copied to clipboard)").styled(s -> s.withColor(0xAAAAAA))));
                                    } else {
                                        ctx.getSource().sendFeedback(colorful);
                                    }
                                    return 1;
                                })
                            )
                        )
                    )
            );
        });
    }

    private static MutableText buildItemInfoMessage(ItemInfo item) {
        String prefix = "[munchyutils] ";
        String label = "freethrift ";
        String description = item.description;
        MutableText colorful = Text.literal("")
            .append(Text.literal(prefix).styled(s -> s.withColor(0x55FF55)))
            .append(Text.literal(label).styled(s -> s.withColor(0x00BFFF)))
            .append(Text.literal(item.display_name).styled(s -> s.withColor(0xFFA500)))
            .append(Text.literal(": ").styled(s -> s.withColor(0x00BFFF)))
            .append(Text.literal(description).styled(s -> s.withColor(0xFFD700)));
        if (item.note != null && !item.note.isEmpty()) {
            colorful.append(Text.literal("\nNote: ").styled(s -> s.withColor(0x00BFFF)))
                .append(Text.literal(item.note).styled(s -> s.withColor(0x55FF55)));
        }
        return colorful;
    }

    private static MutableText buildNoInfoMessage(String displayName, String tag) {
        return Text.literal("")
            .append(Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
            .append(Text.literal("No thrift info for: ").styled(s -> s.withColor(0x00BFFF)))
            .append(Text.literal(displayName + " (tag: " + tag + ")").styled(s -> s.withColor(0xFFA500)))
            .append(Text.literal("\nIf you'd like to add it, please DM me on Discord (alwoj) or make a PR to the GitHub: https://github.com/alekswoje/munchyutils.").styled(s -> s.withColor(0xAAAAAA)));
    }

    private static MutableText buildNoInfoTagMessage(String tag) {
        return Text.literal("")
            .append(Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
            .append(Text.literal("No thrift info for tag: ").styled(s -> s.withColor(0x00BFFF)))
            .append(Text.literal(tag).styled(s -> s.withColor(0xFFA500)))
            .append(Text.literal("\nIf you'd like to add it, please DM me on Discord (alwoj) or make a PR to the GitHub: https://github.com/alekswoje/munchyutils.").styled(s -> s.withColor(0xAAAAAA)));
    }

    private static void maybeAppendEasterEgg(MutableText text) {
        if (new java.util.Random().nextInt(1000) == 0) {
            text.append(Text.literal("\nYou found a secret! 🥚"));
        }
    }

    private static class ItemInfo {
        String display_name;
        String tag;
        String description;
        String note;
    }
} 