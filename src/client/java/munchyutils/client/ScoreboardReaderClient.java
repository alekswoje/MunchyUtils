package munchyutils.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import munchyutils.client.Utils;

public class ScoreboardReaderClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("munchyutils");
    private static int tickCounter = 0;
    private static final int LOG_INTERVAL = 20; // Log every second (20 ticks)
    private static boolean registered = false;
    private static final Pattern BAL_PATTERN = Pattern.compile("Bal: \\$([\\d.,]+[KMB]?)");

    private static double currentBalance = 0.0; // Add static field to store current balance

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        LOGGER.info("Registering ScoreboardReader client...");
        InfoHudOverlay.register();
        InfoHudCommand.register();
        FreethriftCommand.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                tickCounter++;
                if (tickCounter >= LOG_INTERVAL) {
                    tickCounter = 0;
                    try {
                        logClientScoreboardData(client);
                    } catch (Exception e) {
                        LOGGER.error("Error reading client scoreboard data: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                // Session timeout logic
                if (InfoHudOverlay.session.isActive && InfoHudOverlay.session.shouldTimeout()) {
                    InfoHudOverlay.session.reset();
                }
            }
        });
        LOGGER.info("ScoreboardReader client registered successfully!");
    }

    private static void logClientScoreboardData(MinecraftClient client) {
        if (client == null || client.world == null) {
            LOGGER.debug("Client or world is null!");
            return;
        }
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;
        // Get all scoreboard entries for the sidebar objective
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(sidebar)) {
            String owner = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(owner);
            String prefix = team != null && team.getPrefix() != null ? team.getPrefix().getString() : "";
            String suffix = team != null && team.getSuffix() != null ? team.getSuffix().getString() : "";
            String renderedLine = prefix + owner + suffix;
            // Strip Minecraft color codes (e.g., §6)
            String plainLine = renderedLine.replaceAll("§.", "");
            Matcher matcher = BAL_PATTERN.matcher(plainLine);
            if (matcher.find()) {
                double balance = Utils.parseBalance(matcher.group(1));
                currentBalance = balance; // Update the static currentBalance field
                InfoHudOverlay.session.update(balance);
            }
        }
    }

    // Add a public static getter for the current balance
    public static double getCurrentBalanceFromScoreboard() {
        return currentBalance;
    }
} 