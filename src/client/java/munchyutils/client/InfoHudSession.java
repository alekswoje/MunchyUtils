package munchyutils.client;

import munchyutils.client.Utils;

public class InfoHudSession extends HudSessionBase {
    public long startTime = 0;
    public long lastChangeTime = 0;
    public double startBalance = 0;
    public double currentBalance = 0;
    public boolean isActive = false;

    // --- PORG BUFF TRACKING ---
    private boolean porgBuffActive = false;
    private long porgBuffExpireTime = 0;

    public void reset() {
        startTime = 0;
        lastChangeTime = 0;
        startBalance = 0;
        currentBalance = 0;
        isActive = false;
        resetAfk();
    }

    public void update(double newBalance) {
        long now = System.currentTimeMillis();
        if (!isActive) {
            // On first update after login, just set balances
            if (currentBalance == 0 && startBalance == 0) {
                startBalance = newBalance;
                currentBalance = newBalance;
                return;
            }
            // Only start session if balance goes up after initial login
            if (currentBalance != 0 && newBalance > currentBalance) {
                startTime = now;
                startBalance = currentBalance;
                isActive = true;
                lastChangeTime = now;
                currentBalance = newBalance;
            } else {
                // Don't start session yet, just update currentBalance
                currentBalance = newBalance;
            }
        } else if (newBalance != currentBalance) {
            lastChangeTime = now;
            currentBalance = newBalance;
            // Reset session if income goes below 0
            if (getHourlyIncome() < 0) {
                reset();
                startTime = now;
                startBalance = currentBalance;
                isActive = true;
                lastChangeTime = now;
                currentBalance = newBalance;
            }
        }
    }

    public boolean shouldTimeout() {
        MunchyConfig config = munchyutils.client.MunchyConfig.get();
        if (!config.isMiningHudSessionTimeoutEnabled()) return false;
        int timeout = config.getMiningHudSessionTimeoutMs();
        return isActive && (System.currentTimeMillis() - lastChangeTime > timeout);
    }

    public String getHourlyIncomeString() {
        if (!isActive) return "";
        long duration = getActiveSessionSeconds(startTime);
        duration = Math.max(1, duration);
        double earnings = currentBalance - startBalance;
        double hourly = (earnings / duration) * 3600;
        return Utils.formatMoney(hourly) + "/hr";
    }

    public String getSessionLengthString() {
        if (!isActive) return "";
        long sessionLength = getActiveSessionSeconds(startTime);
        sessionLength = Math.max(0, sessionLength);
        return String.format("Session: %dm %ds", sessionLength / 60, sessionLength % 60);
    }

    public double getHourlyIncome() {
        if (!isActive) return 0;
        long duration = getActiveSessionSeconds(startTime);
        duration = Math.max(1, duration);
        double earnings = currentBalance - startBalance;
        return (earnings / duration) * 3600;
    }

    public double getTotalEarnings() {
        if (!isActive) return 0;
        return currentBalance - startBalance;
    }

    public void activatePorgBuff() {
        porgBuffActive = true;
        porgBuffExpireTime = System.currentTimeMillis() + 2 * 60 * 1000; // 2 minutes
    }
    public boolean isPorgBuffActive() {
        return porgBuffActive && System.currentTimeMillis() < porgBuffExpireTime;
    }
    public long getPorgBuffRemainingMs() {
        return isPorgBuffActive() ? (porgBuffExpireTime - System.currentTimeMillis()) : 0;
    }
    public void clearPorgBuff() {
        porgBuffActive = false;
        porgBuffExpireTime = 0;
    }
    // Call this in a tick handler to auto-clear
    public void tickPorgBuff() {
        if (porgBuffActive && System.currentTimeMillis() >= porgBuffExpireTime) {
            clearPorgBuff();
        }
    }
} 