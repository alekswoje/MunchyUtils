package munchyutils.client;

public abstract class HudSessionBase {
    public boolean isAfk = false;
    private long afkStartTime = 0;
    private long totalAfkTime = 0;

    public void setAfk(boolean afk) {
        if (afk && !isAfk) {
            afkStartTime = System.currentTimeMillis();
            isAfk = true;
        } else if (!afk && isAfk) {
            totalAfkTime += System.currentTimeMillis() - afkStartTime;
            afkStartTime = 0;
            isAfk = false;
        }
    }

    public void tickAfk() {
        // nothing needed, afk time accumulates
    }

    public long getActiveSessionSeconds(long sessionStartTime) {
        if (!isAfk) {
            return (System.currentTimeMillis() - sessionStartTime - totalAfkTime) / 1000;
        } else {
            return (System.currentTimeMillis() - sessionStartTime - totalAfkTime - (System.currentTimeMillis() - afkStartTime)) / 1000;
        }
    }

    public void resetAfk() {
        isAfk = false;
        afkStartTime = 0;
        totalAfkTime = 0;
    }
} 