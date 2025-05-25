package munchyutils.munchyutils;

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

import net.minecraft.client.option.KeyBinding;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import com.mojang.authlib.GameProfile;

import munchyutils.client.CooldownHudOverlay;
import munchyutils.client.CooldownManager;
import munchyutils.client.CooldownTrigger;
import munchyutils.client.InfoHudOverlay;
import munchyutils.client.ScoreboardReaderClient;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import munchyutils.munchyutils.HudInputHandler;
import munchyutils.client.FishingHudSession;

public class MunchyUtilsClient implements ClientModInitializer {
	private KeyBinding moveHudKey;
	private boolean moveMode = false;
	private munchyutils.client.FeatureManager.ModFeature movingHud = munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD;
	private int moveX = 0, moveY = 0;
	private int origX = 0, origY = 0;
	private boolean tabPressed = false;
	public static boolean isMoveMode = false;
	public static munchyutils.client.FeatureManager.ModFeature movingHudStatic = munchyutils.client.FeatureManager.ModFeature.COOLDOWN_HUD;
	public static int moveXStatic = 0, moveYStatic = 0;
	private static Map<munchyutils.client.FeatureManager.ModFeature, int[]> tempHudPositions = new EnumMap<>(munchyutils.client.FeatureManager.ModFeature.class);
	private static final int GRID_SIZE = 10;
	private static final int HUD_MARGIN = 4;

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
	}

	private void registerKeyBindings() {
		moveHudKey = new KeyBinding("key.munchyutils.move_hud", GLFW.GLFW_KEY_L, "MunchyUtils");
		net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(moveHudKey);
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
				String name = stripColorCodes(mainHand.getName().getString()).toLowerCase();
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
			String name = stripColorCodes(mainHand.getName().getString()).toLowerCase();
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
						if (helmet != null && stripColorCodes(helmet.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase())) {
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
							boolean match = !mainHand.isEmpty() && stripColorCodes(mainHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase());
							// Check offhand
							ItemStack offHand = client.player.getOffHandStack();
							match = match || (!offHand.isEmpty() && stripColorCodes(offHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase()));
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
				String name = stripColorCodes(stack.getName().getString()).toLowerCase();
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
						if (helmet != null && stripColorCodes(helmet.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase())) {
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
							boolean match = !mainHand.isEmpty() && stripColorCodes(mainHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase());
							// Check offhand
							ItemStack offHand = client.player.getOffHandStack();
							match = match || (!offHand.isEmpty() && stripColorCodes(offHand.getName().getString()).toLowerCase().contains(trigger.itemNamePart.toLowerCase()));
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
		// Register both CHAT and GAME message events for fishing HUD debug
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			//                System.out.println("[FishingHUD] CHAT event fired");
			handleFishingChatMessage(message);
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
			if (!munchyutils.client.InfoHudOverlay.session.isAfk && now - lastInput[0] > 10_000) {
				munchyutils.client.InfoHudOverlay.session.setAfk(true);
			}
			if (!munchyutils.client.InfoHudOverlay.fishingSession.isAfk && now - lastInput[0] > 10_000) {
				munchyutils.client.InfoHudOverlay.fishingSession.setAfk(true);
			}
			if (munchyutils.client.InfoHudOverlay.session.isAfk) {
				munchyutils.client.InfoHudOverlay.session.tickAfk();
			}
			if (munchyutils.client.InfoHudOverlay.fishingSession.isAfk) {
				munchyutils.client.InfoHudOverlay.fishingSession.tickAfk();
			}
		});
	}

	private boolean isPickaxe(Item item) {
		return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE;
	}

	public static boolean isMoveMode() { return isMoveMode; }
	public static int[] getMoveHudPosition(munchyutils.client.FeatureManager.ModFeature feature) {
		if (isMoveMode) {
			int[] temp = tempHudPositions.get(feature);
			if (temp != null && temp.length == 2) return new int[]{temp[0], temp[1]};
			int[] saved = munchyutils.client.FeatureManager.getHudPosition(feature);
			if (saved != null && saved.length == 2) return new int[]{saved[0], saved[1]};
			return new int[]{10, 10};
		}
		return null;
	}

	// Utility to strip Minecraft color codes from a string
	private static String stripColorCodes(String input) {
		return input.replaceAll("§.", "");
	}

	// Move the fishing HUD chat parsing logic into a new static method:
	public static void handleFishingChatMessage(net.minecraft.text.Text message) {
		String rawMsg = message.getString();
		String msg = stripColorCodes(rawMsg);
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
				munchyutils.client.InfoHudOverlay.fishingSession.playerLevel = lastStatsLevel;
				munchyutils.client.InfoHudOverlay.fishingSession.playerXP = lastStatsXP;
				munchyutils.client.InfoHudOverlay.fishingSession.saveStatsToFile(localName, lastStatsLevel, lastStatsXP);
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
}