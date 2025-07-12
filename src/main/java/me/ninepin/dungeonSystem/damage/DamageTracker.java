package me.ninepin.dungeonSystem.damage;

import me.ninepin.dungeonSystem.DungeonSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 傷害統計追蹤管理器
 */
public class DamageTracker {

    private final DungeonSystem plugin;
    // 儲存結構：副本ID -> 玩家UUID -> 傷害統計
    private final Map<String, Map<UUID, DamageStats>> dungeonDamageData;

    public DamageTracker(DungeonSystem plugin) {
        this.plugin = plugin;
        this.dungeonDamageData = new HashMap<>();
    }

    /**
     * 初始化副本的傷害統計
     */
    public void initDungeonStats(String dungeonId, Set<UUID> playerIds) {
        Map<UUID, DamageStats> playerStats = new HashMap<>();
        for (UUID playerId : playerIds) {
            playerStats.put(playerId, new DamageStats());
        }
        dungeonDamageData.put(dungeonId, playerStats);
        plugin.getLogger().info("已初始化副本 " + dungeonId + " 的傷害統計，玩家數: " + playerIds.size());
    }

    /**
     * 記錄玩家造成的傷害
     */
    public void recordDamage(String dungeonId, UUID playerId, double damage) {
        Map<UUID, DamageStats> playerStats = dungeonDamageData.get(dungeonId);
        if (playerStats != null) {
            DamageStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.addDamage(damage);
            }
        }
    }

    /**
     * 記錄玩家擊殺
     */
    public void recordKill(String dungeonId, UUID playerId) {
        Map<UUID, DamageStats> playerStats = dungeonDamageData.get(dungeonId);
        if (playerStats != null) {
            DamageStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.addKill();
            }
        }
    }

    /**
     * 記錄玩家死亡
     */
    public void recordDeath(String dungeonId, UUID playerId) {
        Map<UUID, DamageStats> playerStats = dungeonDamageData.get(dungeonId);
        if (playerStats != null) {
            DamageStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.addDeath();
            }
        }
    }

    /**
     * 記錄玩家承受傷害
     */
    public void recordDamageReceived(String dungeonId, UUID playerId, double damage) {
        Map<UUID, DamageStats> playerStats = dungeonDamageData.get(dungeonId);
        if (playerStats != null) {
            DamageStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.addDamageReceived(damage);
            }
        }
    }

    /**
     * 生成排名列表
     */
    public List<PlayerRanking> generateRankings(String dungeonId) {
        Map<UUID, DamageStats> playerStats = dungeonDamageData.get(dungeonId);
        if (playerStats == null || playerStats.isEmpty()) {
            return new ArrayList<>();
        }

        List<PlayerRanking> rankings = new ArrayList<>();

        for (Map.Entry<UUID, DamageStats> entry : playerStats.entrySet()) {
            UUID playerId = entry.getKey();
            DamageStats stats = entry.getValue();

            Player player = Bukkit.getPlayer(playerId);
            String playerName = player != null ? player.getName() : "Unknown";

            rankings.add(new PlayerRanking(playerId, playerName, stats));
        }

        // 排序：首先按總傷害，然後按擊殺數，最後按死亡數（越少越好）
        rankings.sort((a, b) -> {
            // 首要：總傷害（降序）
            int damageCompare = Double.compare(b.getTotalDamage(), a.getTotalDamage());
            if (damageCompare != 0) return damageCompare;

            // 次要：擊殺數（降序）
            int killsCompare = Integer.compare(b.getKills(), a.getKills());
            if (killsCompare != 0) return killsCompare;

            // 最後：死亡數（升序，越少越好）
            return Integer.compare(a.getDeaths(), b.getDeaths());
        });

        return rankings;
    }

    /**
     * 清理副本的傷害統計數據
     */
    public void clearDungeonData(String dungeonId) {
        dungeonDamageData.remove(dungeonId);
        plugin.getLogger().info("已清理副本 " + dungeonId + " 的傷害統計數據");
    }

    /**
     * 獲取指定副本和玩家的統計數據
     */
    public DamageStats getPlayerStats(String dungeonId, UUID playerId) {
        Map<UUID, DamageStats> playerStats = dungeonDamageData.get(dungeonId);
        if (playerStats != null) {
            return playerStats.get(playerId);
        }
        return null;
    }

    /**
     * 檢查副本是否有統計數據
     */
    public boolean hasDungeonStats(String dungeonId) {
        return dungeonDamageData.containsKey(dungeonId);
    }
}