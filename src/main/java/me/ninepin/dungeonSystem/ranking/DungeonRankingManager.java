package me.ninepin.dungeonSystem.ranking;

import me.ninepin.dungeonSystem.DungeonSystem;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class DungeonRankingManager {
    private final DungeonSystem plugin;
    private final me.ninepin.dungeonSystem.ranking.JsonDataManager dataManager;

    public DungeonRankingManager(DungeonSystem plugin) {
        this.plugin = plugin;
        this.dataManager = new me.ninepin.dungeonSystem.ranking.JsonDataManager(plugin);
    }

    /**
     * 記錄玩家完成副本
     */
    public void recordCompletion(UUID playerId, String dungeonId, String playerName) {
        dataManager.recordCompletion(playerId, dungeonId, playerName);
    }

    /**
     * 記錄玩家完成副本（重載方法，自動獲取玩家名稱）
     */
    public void recordCompletion(Player player, String dungeonId) {
        recordCompletion(player.getUniqueId(), dungeonId, player.getName());
    }

    /**
     * 獲取副本排行榜
     */
    public List<me.ninepin.dungeonSystem.ranking.JsonDataManager.PlayerRankingData> getDungeonRanking(String dungeonId, int limit) {
        return dataManager.getDungeonRanking(dungeonId, limit);
    }

    /**
     * 獲取玩家數據
     */
    public me.ninepin.dungeonSystem.ranking.JsonDataManager.PlayerRankingData getPlayerData(UUID playerId, String dungeonId) {
        return dataManager.getPlayerData(playerId, dungeonId);
    }


}