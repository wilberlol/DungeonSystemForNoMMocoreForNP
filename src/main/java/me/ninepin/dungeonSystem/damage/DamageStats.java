package me.ninepin.dungeonSystem.damage;

/**
 * 玩家在副本中的戰鬥統計數據
 */
public class DamageStats {
    private double totalDamage = 0.0;      // 總傷害輸出
    private int kills = 0;                 // 擊殺數
    private int deaths = 0;                // 死亡次數
    private double damageReceived = 0.0;   // 承受傷害
    private long startTime;                // 開始時間（用於計算DPS）

    public DamageStats() {
        this.startTime = System.currentTimeMillis();
    }

    public void addDamage(double damage) {
        this.totalDamage += damage;
    }

    public void addKill() {
        this.kills++;
    }

    public void addDeath() {
        this.deaths++;
    }

    public void addDamageReceived(double damage) {
        this.damageReceived += damage;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public double getDamageReceived() {
        return damageReceived;
    }

    /**
     * 計算DPS（每秒傷害）
     */
    public double getDPS() {
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        if (duration <= 0) return 0.0;
        return totalDamage / duration;
    }

    /**
     * 獲取戰鬥時長（秒）
     */
    public long getDuration() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
}