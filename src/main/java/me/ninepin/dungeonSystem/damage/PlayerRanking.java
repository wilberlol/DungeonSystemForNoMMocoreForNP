package me.ninepin.dungeonSystem.damage;

import java.util.UUID;

/**
 * 玩家排名數據類
 */
public class PlayerRanking {
    private final UUID playerId;
    private final String playerName;
    private final double totalDamage;
    private final int kills;
    private final int deaths;
    private final double damageReceived;
    private final double dps;
    private final long duration;

    public PlayerRanking(UUID playerId, String playerName, DamageStats stats) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.totalDamage = stats.getTotalDamage();
        this.kills = stats.getKills();
        this.deaths = stats.getDeaths();
        this.damageReceived = stats.getDamageReceived();
        this.dps = stats.getDPS();
        this.duration = stats.getDuration();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
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

    public double getDPS() {
        return dps;
    }

    public long getDuration() {
        return duration;
    }
}