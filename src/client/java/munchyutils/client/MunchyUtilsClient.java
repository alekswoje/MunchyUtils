package munchyutils.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import munchyutils.client.HudInputHandler;
import munchyutils.client.Utils;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;

public class MunchyUtilsClient implements ClientModInitializer {
	private KeyBinding moveHudKey;
	// Only use the static fields below for edit mode state
	private int moveX = 0, moveY = 0;
	private int origX = 0, origY = 0;
	private boolean tabPressed = false;
	public static boolean hudEditMode = false;
	public static munchyutils.client.FeatureManager.ModFeature editingHudTypeStatic = munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD;
	public static int moveXStatic = 0, moveYStatic = 0;
	private static Map<munchyutils.client.FeatureManager.ModFeature, int[]> tempHudPositions = new EnumMap<>(munchyutils.client.FeatureManager.ModFeature.class);
	private static final int GRID_SIZE = 10;
	private static final int HUD_MARGIN = 4;
	// Auto Announce timer state
	private long lastAnnounceTime = 0;
	private int currentAnnouncementIndex = 0;

	@Override
	public void onInitializeClient() {
		registerKeyBindings();
		HudInputHandler.register(moveHudKey);
		registerEventHandlers();
		registerCooldownTriggers();
		registerBlockBreakTriggers();
		registerSneakTriggers();
		initializeModFeatures();
		munchyutils.client.InfoHudCommand.register();
		// Load fishing stats on startup
		munchyutils.client.InfoHudOverlay.fishingSession.loadStats();
		// Update checker (runs once on startup)
		if (MunchyConfig.get().isUpdateCheckEnabled()) {
			new Thread(() -> {
				try {
					URL url = new URL("https://api.github.com/repos/alekswoje/MunchyUtils/releases/latest");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Accept", "application/vnd.github+json");
					InputStream is = conn.getInputStream();
					String json = new Scanner(is).useDelimiter("\\A").next();
					JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
					String latest = obj.get("tag_name").getAsString();
					String current = getModVersion();
					if (!latest.equals(current)) {
						MinecraftClient.getInstance().execute(() -> {
							MinecraftClient.getInstance().player.sendMessage(
								net.minecraft.text.Text.literal("§6[MunchyUtils] §cA new version is available: " + latest + " (You have: " + current + ")"),
								false
							);
						});
					}
				} catch (Exception e) {
					// Optionally log or ignore
				}
			}, "MunchyUtils Update Checker").start();
		}
		// Register tick event for delayed config screen opening
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (munchyutils.client.InfoHudCommand.scheduledConfigScreenTicks >= 0) {
				munchyutils.client.InfoHudCommand.scheduledConfigScreenTicks--;
				if (munchyutils.client.InfoHudCommand.scheduledConfigScreenTicks == 0) {
					munchyutils.client.InfoHudCommand.openConfigScreen();
				}
			}
			// Tick fishing HUD session timeout
			munchyutils.client.InfoHudOverlay.fishingSession.tickTimeout();
		});
		// Register tick event for auto announce
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			MunchyConfig config = MunchyConfig.get();
			if (!config.isAutoAnnounceEnabled()) return;
			long now = System.currentTimeMillis();
			// 20 minutes and 5 seconds = 1,205,000 ms
			if (lastAnnounceTime == 0) lastAnnounceTime = now;
			if (now - lastAnnounceTime >= 1_205_000) {
				if (config.isRotatingAnnouncementsEnabled() && !config.getAnnouncementList().isEmpty()) {
					// Set the next announcement
					String next = config.getAnnouncementList().get(currentAnnouncementIndex);
					client.player.networkHandler.sendChatCommand("cellshop setannouncement " + next);
					currentAnnouncementIndex = (currentAnnouncementIndex + 1) % config.getAnnouncementList().size();
				}
				// Announce
				client.player.networkHandler.sendChatCommand("cellshop announce");
				lastAnnounceTime = now;
			}
		});
	}

	private void registerKeyBindings() {
		moveHudKey = new KeyBinding("key.munchyutils.move_hud", GLFW.GLFW_KEY_L, "MunchyUtils");
		KeyBindingHelper.registerKeyBinding(moveHudKey);
	}

	private void registerEventHandlers() {
		// Place any other event handler registrations here if needed
	}

	private void registerCooldownTriggers() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD)) return;
			if (client.player == null) return;
			if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
				ItemStack mainHand = client.player.getMainHandStack();
				String name = Utils.stripColorCodes(mainHand.getName().getString()).toLowerCase();
				for (CooldownTrigger trigger : CooldownManager.getTriggers()) {
					if (trigger.type == CooldownTrigger.Type.HELD && trigger.action == CooldownTrigger.Action.LCLICK) {
						if (name.contains(trigger.itemNamePart.toLowerCase())) {
							long remaining = CooldownManager.getRemaining(trigger.name);
							if (remaining == 0) {
								CooldownManager.startCooldown(trigger.name, trigger.cooldownMs);
							}
						}
					}
				}
			}
		});
	}

	private void registerBlockBreakTriggers() {
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD)) return ActionResult.PASS;
			if (player == null) return ActionResult.PASS;
			ItemStack mainHand = player.getMainHandStack();
			String name = Utils.stripColorCodes(mainHand.getName().getString()).toLowerCase();
			for (CooldownTrigger trigger : CooldownManager.getTriggers()) {
				if (trigger.type == CooldownTrigger.Type.HELD && trigger.action == CooldownTrigger.Action.BREAK) {
					if (name.contains(trigger.itemNamePart.toLowerCase())) {
						long remaining = CooldownManager.getRemaining(trigger.name);
						if (remaining == 0) {
							CooldownManager.startCooldown(trigger.name, trigger.cooldownMs);
						}
					}
				}
			}
			return ActionResult.PASS;
		});
	}

	private void registerSneakTriggers() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			if (munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD)) {
				for (CooldownTrigger trigger : CooldownManager.getTriggers()) {
					if (trigger.type == CooldownTrigger.Type.WORN && trigger.action == CooldownTrigger.Action.CROUCH) {
						ItemStack helmet = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
						if (helmet != null && Utils.stripColorCodes(helmet.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase())) {
							if (client.player.isSneaking()) {
								long remaining = CooldownManager.getRemaining(trigger.name);
								if (remaining == 0) {
									CooldownManager.startCooldown(trigger.name, trigger.cooldownMs);
								}
							}
						}
					}
				}
				if (client.player.isSneaking()) {
					for (CooldownTrigger trigger : CooldownManager.getTriggers()) {
						if (trigger.type == CooldownTrigger.Type.HELD && trigger.action == CooldownTrigger.Action.CROUCH) {
							// Check main hand
							ItemStack mainHand = client.player.getMainHandStack();
							boolean match = !mainHand.isEmpty() && Utils.stripColorCodes(mainHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase());
							// Check offhand
							ItemStack offHand = client.player.getOffHandStack();
							match = match || (!offHand.isEmpty() && Utils.stripColorCodes(offHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase()));
							if (match) {
								long remaining = CooldownManager.getRemaining(trigger.name);
								if (remaining == 0) {
									CooldownManager.startCooldown(trigger.name, trigger.cooldownMs);
								}
							}
						}
					}
				}
			}
		});
	}

	private void initializeModFeatures() {
		CooldownManager.load();
		ScoreboardReaderClient.register();
		munchyutils.client.CooldownHudOverlay.register();
		munchyutils.client.InfoHudOverlay.register();
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD)) return ActionResult.PASS;
			if (world.isClient()) {
				ItemStack stack = player.getStackInHand(hand);
				String name = Utils.stripColorCodes(stack.getName().getString()).toLowerCase();
				// --- PORG BUFF DETECTION & PREVENTION ---
				if (name.contains("roasted porg")) {
					MunchyConfig config = MunchyConfig.get();
					if (config.isPreventPorgUseIfActive() && munchyutils.client.InfoHudOverlay.session.isPorgBuffActive()) {
						if (MinecraftClient.getInstance().player != null) {
							MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("[munchyutils] Roasted Porg buff is already active! Wait for it to expire before using again.").styled(s -> s.withColor(0xA0522D)), true);
						}
						return ActionResult.FAIL;
					} else {
						// Activate the buff and show debug message
						munchyutils.client.InfoHudOverlay.session.activatePorgBuff();
						if (MinecraftClient.getInstance().player != null) {
							MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("[munchyutils] Roasted Porg buff activated for 2 minutes!").styled(s -> s.withColor(0xA0522D)), false);
						}
					}
				}
				for (CooldownTrigger trigger : CooldownManager.getTriggers()) {
					if (trigger.type == CooldownTrigger.Type.HELD && trigger.action == CooldownTrigger.Action.RCLICK) {
						if (name.contains(trigger.itemNamePart.toLowerCase())) {
							long remaining = CooldownManager.getRemaining(trigger.name);
							if (remaining == 0) {
								CooldownManager.startCooldown(trigger.name, trigger.cooldownMs);
							}
						}
					}
				}
			}
			return ActionResult.PASS;
		});
		// Generalized sneaking for worn triggers (helmet, etc.)
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			if (munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD)) {
				for (CooldownTrigger trigger : CooldownManager.getTriggers()) {
					if (trigger.type == CooldownTrigger.Type.WORN && trigger.action == CooldownTrigger.Action.CROUCH) {
						ItemStack helmet = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
						if (helmet != null && Utils.stripColorCodes(helmet.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase())) {
							if (client.player.isSneaking()) {
								long remaining = CooldownManager.getRemaining(trigger.name);
								if (remaining == 0) {
									CooldownManager.startCooldown(trigger.name, trigger.cooldownMs);
								}
							}
						}
					}
				}
				if (client.player.isSneaking()) {
					for (CooldownTrigger trigger : CooldownManager.getTriggers()) {
						if (trigger.type == CooldownTrigger.Type.HELD && trigger.action == CooldownTrigger.Action.CROUCH) {
							// Check main hand
							ItemStack mainHand = client.player.getMainHandStack();
							boolean match = !mainHand.isEmpty() && Utils.stripColorCodes(mainHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase());
							// Check offhand
							ItemStack offHand = client.player.getOffHandStack();
							match = match || (!offHand.isEmpty() && Utils.stripColorCodes(offHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase()));
							if (match) {
								long remaining = CooldownManager.getRemaining(trigger.name);
								if (remaining == 0) {
									CooldownManager.startCooldown(trigger.name, trigger.cooldownMs);
								}
							}
						}
					}
				}
			}
			if (munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.INFO_HUD)) {
				// Hourly income overlay and AFK detection logic
				// (Assume HourlyIncomeOverlay and AFK logic is here)
			}
		});
		// AFK detection
		final long[] lastInput = {System.currentTimeMillis()};
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.INFO_HUD)) return;
			if (client.player == null) return;
			long now = System.currentTimeMillis();
			long window = MinecraftClient.getInstance().getWindow().getHandle();
			boolean inputDetected = false;
			for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
				if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
					inputDetected = true;
					break;
				}
			}
			for (int button = GLFW.GLFW_MOUSE_BUTTON_1; button <= GLFW.GLFW_MOUSE_BUTTON_LAST; button++) {
				if (GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS) {
					inputDetected = true;
					break;
				}
			}
			if (inputDetected) {
				lastInput[0] = now;
				munchyutils.client.InfoHudOverlay.session.setAfk(false);
				munchyutils.client.InfoHudOverlay.fishingSession.setAfk(false);
			}
			if (!munchyutils.client.InfoHudOverlay.session.isAfk && now - lastInput[0] > 60_000) {
				munchyutils.client.InfoHudOverlay.session.setAfk(true);
			}
			if (!munchyutils.client.InfoHudOverlay.fishingSession.isAfk && now - lastInput[0] > 60_000) {
				munchyutils.client.InfoHudOverlay.fishingSession.setAfk(true);
			}
			if (munchyutils.client.InfoHudOverlay.session.isAfk) {
				munchyutils.client.InfoHudOverlay.session.tickAfk();
			}
			if (munchyutils.client.InfoHudOverlay.fishingSession.isAfk) {
				munchyutils.client.InfoHudOverlay.fishingSession.tickAfk();
			}
		});
		// Tick handler for Porg buff
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			munchyutils.client.InfoHudOverlay.session.tickPorgBuff();
		});
	}

	public static boolean isHudEditMode() { return hudEditMode; }
	public static int[] getMoveHudPosition(munchyutils.client.FeatureManager.ModFeature feature) {
		if (hudEditMode) {
			int[] temp = tempHudPositions.get(feature);
			if (temp != null && temp.length == 2) return new int[]{temp[0], temp[1]};
			int[] saved = munchyutils.client.FeatureManager.getHudPosition(feature);
			if (saved != null && saved.length == 2) return new int[]{saved[0], saved[1]};
			return new int[]{10, 10};
		}
		return null;
	}

	// Move the fishing HUD chat parsing logic into a new static method:
	public static void handleFishingChatMessage(net.minecraft.text.Text message) {
		String rawMsg = message.getString();
		String msg = Utils.stripColorCodes(rawMsg);
		if (!munchyutils.client.FeatureManager.isEnabled(munchyutils.client.FeatureManager.ModFeature.INFO_HUD)) return;
		// --- /fish stats parsing ---
		// Example:
		// [10:05:01] [Render thread/INFO]: [System] [CHAT] Stats of HogMower:
		// [10:05:01] [Render thread/INFO]: [System] [CHAT]   Level: 47
		// [10:05:01] [Render thread/INFO]: [System] [CHAT]   Experience: [8411/61497]
		// We need to parse these three lines in sequence
		// Use a static buffer to store the last seen stats lines
		updateFishStatsBuffer(msg);
		// --- Fishing HUD chat parsing (existing) ---
		String rewardRegex = "(?:Multiple|Double|Quadruple|Fish) Reward[s]*!\\s*(.+)";
		java.util.regex.Matcher rewardMatcher = java.util.regex.Pattern.compile(rewardRegex).matcher(msg);
		if (rewardMatcher.find()) {
			String rewardsStr = rewardMatcher.group(1);
			String[] parts = rewardsStr.split("\\+");
			for (String part : parts) {
				String trimmed = part.trim();
				if (trimmed.isEmpty()) continue;
				String xpInRewardRegex = "(.+?)\\s*\\+(\\d+)XP";
				java.util.regex.Matcher xpInReward = java.util.regex.Pattern.compile(xpInRewardRegex).matcher(trimmed);
				if (xpInReward.matches()) {
					String rewardName = xpInReward.group(1).trim();
					int xp = Integer.parseInt(xpInReward.group(2));
					String category = munchyutils.client.InfoHudOverlay.getRewardCategoryMap().getOrDefault(rewardName, "uncategorized");
					munchyutils.client.InfoHudOverlay.fishingSession.addReward(rewardName, category);
					munchyutils.client.InfoHudOverlay.fishingSession.addXP(xp);
				} else if (trimmed.matches("\\d+XP")) {
					int xp = Integer.parseInt(trimmed.replace("XP", "").replace("+", "").trim());
					munchyutils.client.InfoHudOverlay.fishingSession.addXP(xp);
				} else {
					String category = munchyutils.client.InfoHudOverlay.getRewardCategoryMap().getOrDefault(trimmed, "uncategorized");
					munchyutils.client.InfoHudOverlay.fishingSession.addReward(trimmed, category);
				}
			}
			if (munchyutils.client.InfoHudOverlay.fishingSession.isActive) {
				// Session started
			}
		}
		String xpRegex = "Fishing EXP:((?:\\s*\\+\\d+XP)+)";
		java.util.regex.Matcher xpMatcher = java.util.regex.Pattern.compile(xpRegex).matcher(msg);
		if (xpMatcher.find()) {
			String xpStr = xpMatcher.group(1);
			java.util.regex.Matcher singleXp = java.util.regex.Pattern.compile("\\+(\\d+)XP").matcher(xpStr);
			while (singleXp.find()) {
				int xp = Integer.parseInt(singleXp.group(1));
				munchyutils.client.InfoHudOverlay.fishingSession.addXP(xp);
			}
			if (munchyutils.client.InfoHudOverlay.fishingSession.isActive) {
				// Session started
			}
		}
		munchyutils.client.InfoHudOverlay.fishingSession.endCast();
	}

	// Buffer for /fish stats parsing
	private static String lastStatsName = null;
	private static Integer lastStatsLevel = null;
	private static Integer lastStatsXP = null;
	private static long lastStatsTimestamp = 0;
	private static void updateFishStatsBuffer(String msg) {
		// Match 'Stats of <name>:'
		java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile("Stats of ([^:]+):").matcher(msg);
		if (nameMatcher.find()) {
			lastStatsName = nameMatcher.group(1).trim();
			lastStatsLevel = null;
			lastStatsXP = null;
			lastStatsTimestamp = System.currentTimeMillis();
			return;
		}
		// Match 'Level: <num>'
		java.util.regex.Matcher levelMatcher = java.util.regex.Pattern.compile("Level: (\\d+)").matcher(msg);
		if (levelMatcher.find() && lastStatsName != null && lastStatsLevel == null) {
			lastStatsLevel = Integer.parseInt(levelMatcher.group(1));
			lastStatsTimestamp = System.currentTimeMillis();
			return;
		}
		// Match 'Experience: [<xp>/<max>]' (we want <xp>)
		java.util.regex.Matcher xpMatcher = java.util.regex.Pattern.compile("Experience: \\[(\\d+)/(\\d+)\\]").matcher(msg);
		if (xpMatcher.find() && lastStatsName != null && lastStatsLevel != null && lastStatsXP == null) {
			lastStatsXP = Integer.parseInt(xpMatcher.group(1));
			lastStatsTimestamp = System.currentTimeMillis();
			// Now, if the name matches the local player, update session
			String localName = getLocalPlayerName();
			if (localName != null && localName.equalsIgnoreCase(lastStatsName)) {
				munchyutils.client.InfoHudOverlay.fishingSession.setLevelAndXP(lastStatsLevel, lastStatsXP);
				// Send colored chat message
				net.minecraft.text.Text chatMsg = net.minecraft.text.Text.literal("")
					.append(net.minecraft.text.Text.literal("[munchyutils] ").styled(s -> s.withColor(0x55FF55)))
					.append(net.minecraft.text.Text.literal("(🎣) ").styled(s -> s.withColor(0x00BFFF)))
					.append(net.minecraft.text.Text.literal("FishingHud - Saved Fishing Level ").styled(s -> s.withColor(0xFFD700)))
					.append(net.minecraft.text.Text.literal(String.valueOf(lastStatsLevel)).styled(s -> s.withColor(0xFFFF55)))
					.append(net.minecraft.text.Text.literal(", and Current Xp ").styled(s -> s.withColor(0xFFD700)))
					.append(net.minecraft.text.Text.literal(String.valueOf(lastStatsXP)).styled(s -> s.withColor(0xFFFF55)))
					.append(net.minecraft.text.Text.literal(" to file successfully!").styled(s -> s.withColor(0x00FFFF)));
				MinecraftClient.getInstance().player.sendMessage(chatMsg, false);
			}
			// Reset buffer after use
			lastStatsName = null;
			lastStatsLevel = null;
			lastStatsXP = null;
		}
	}
	private static String getLocalPlayerName() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.player != null) {
			return client.player.getName().getString();
		}
		return null;
	}
	public static boolean isFishingStatsLoaded() {
		return munchyutils.client.InfoHudOverlay.fishingSession.playerLevel >= 0 && munchyutils.client.InfoHudOverlay.fishingSession.playerXP >= 0;
	}

	// Add a new public static method for chat message handling
	public static void handleChatHudMessage(net.minecraft.text.Text message) {
		String msg = message.getString();
		MunchyConfig config = MunchyConfig.get();
		if (config.isHideInventoryFullMessage() && msg.contains("Your inventory is full! Click here to sell your items, or type /sell!")) {
			// Suppress message: handled in mixin by not calling super/addMessage
			return;
		}
		if (config.isHideSellSuccessMessage() && msg.matches("Successfully sold \\d+ items for \\$[\\d,]+.*")) {
			// Suppress message: handled in mixin by not calling super/addMessage
			return;
		}
		handleFishingChatMessage(message);
	}

	// Helper to get the current mod version from fabric.mod.json
	private static String getModVersion() {
		try {
			InputStream is = MunchyUtilsClient.class.getClassLoader().getResourceAsStream("fabric.mod.json");
			if (is != null) {
				Scanner scanner = new Scanner(is).useDelimiter("\\A");
				String json = scanner.hasNext() ? scanner.next() : "";
				scanner.close();
				JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
				return obj.get("version").getAsString();
			}
		} catch (Exception e) {
			// ignore
		}
		return "unknown";
	}
}