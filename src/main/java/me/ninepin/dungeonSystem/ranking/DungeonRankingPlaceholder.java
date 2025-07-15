package me.ninepin.dungeonSystem.ranking;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ninepin.dungeonSystem.DungeonSystem;
import me.ninepin.dungeonSystem.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DungeonRankingPlaceholder extends PlaceholderExpansion {
    private final DungeonSystem plugin;

    public DungeonRankingPlaceholder(DungeonSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "dungeonrank";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "YourName";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        // 改為從後面分割，最後一個底線後的部分是 type
        int lastUnderscoreIndex = params.lastIndexOf("_");
        if (lastUnderscoreIndex == -1) return "";

        String dungeonId = params.substring(0, lastUnderscoreIndex);
        String type = params.substring(lastUnderscoreIndex + 1);
        switch (type) {
            case "rank":
                return getPlayerRank(player, dungeonId);
            case "count":
                return getPlayerCount(player, dungeonId);
            case "info":
                return getPlayerInfo(player, dungeonId);
            default:
                return "";
        }
    }

    private String getPlayerRank(Player player, String dungeonId) {
        try {
            List<JsonDataManager.PlayerRankingData> allPlayers =
                    plugin.getRankingManager().getDungeonRanking(dungeonId, Integer.MAX_VALUE);
            // 使用 HologramManager 的 calculatePlayerRank 方法
            int rank = plugin.getHologramManager().calculatePlayerRank(allPlayers, player.getName());
            return rank > 0 ? String.valueOf(rank) : "未上榜";
        } catch (Exception e) {
            plugin.getLogger().warning("獲取玩家排名時出錯: " + e.getMessage());
            return "錯誤";
        }
    }

    private String getPlayerCount(Player player, String dungeonId) {
        try {
            JsonDataManager.PlayerRankingData playerData =
                    plugin.getRankingManager().getPlayerData(player.getUniqueId(), dungeonId);
            return playerData != null ? String.valueOf(playerData.completionCount) : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    private String getPlayerInfo(Player player, String dungeonId) {
        try {
            JsonDataManager.PlayerRankingData playerData =
                    plugin.getRankingManager().getPlayerData(player.getUniqueId(), dungeonId);

            if (playerData != null) {
                String rank = getPlayerRank(player, dungeonId);
                String infoText = "§b" + player.getName() + " §7排名:§e#" + rank +
                        " §7完成:§a" + playerData.completionCount + "次";
                return MessageUtil.parseToLegacyString(infoText);
            } else {
                String noRecordText = "§b" + player.getName() + " §7尚未攻略此副本";
                return MessageUtil.parseToLegacyString(noRecordText);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("獲取玩家資訊時出錯: " + e.getMessage());
            return "§b" + player.getName() + " §7資料載入中...";
        }
    }
}