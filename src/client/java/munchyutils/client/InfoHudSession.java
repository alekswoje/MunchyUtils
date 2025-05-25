package munchyutils.client;

public class InfoHudSession extends HudSessionBase {
    public long startTime = 0;
    public long lastChangeTime = 0;
    public double startBalance = 0;
    public double currentBalance = 0;
    public boolean isActive = false;

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
            // Only start session if balance goes up
            if (newBalance > startBalance) {
                startTime = now;
                startBalance = newBalance;
                isActive = true;
                lastChangeTime = now;
                currentBalance = newBalance;
            } else {
                // Don't start session yet
                startBalance = newBalance;
                currentBalance = newBalance;
            }
        } else if (newBalance != currentBalance) {
            lastChangeTime = now;
            currentBalance = newBalance;
            // Reset session if income goes below 0
            if (getHourlyIncome() < 0) {
                reset();
                startTime = now;
                startBalance = newBalance;
                isActive = true;
                lastChangeTime = now;
                currentBalance = newBalance;
            }
        }
    }

    public boolean shouldTimeout() {
        return isActive && (System.currentTimeMillis() - lastChangeTime > 2 * 60 * 1000);
    }

    public String getHourlyIncomeString() {
        if (!isActive) return "";
        long duration = getActiveSessionSeconds(startTime);
        duration = Math.max(1, duration);
        double earnings = currentBalance - startBalance;
        double hourly = (earnings / duration) * 3600;
        return formatMoney(hourly) + "/hr";
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

    public static String formatMoney(double amount) {
        if (amount >= 1_000_000_000) return String.format("$%.3fB", amount / 1_000_000_000);
        if (amount >= 1_000_000) return String.format("$%.3fM", amount / 1_000_000);
        if (amount >= 1_000) return String.format("$%.3fK", amount / 1_000);
        return String.format("$%.3f", amount);
    }
} 