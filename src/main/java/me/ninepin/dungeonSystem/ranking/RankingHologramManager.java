package me.ninepin.dungeonSystem.ranking;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.ninepin.dungeonSystem.Dungeon.Dungeon;
import me.ninepin.dungeonSystem.DungeonSystem;
import me.ninepin.dungeonSystem.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingHologramManager {
    private final DungeonSystem plugin;
    // 緩存排行榜數據以減少數據加載頻率 - 使用標準化ID作為key
    private final Map<String, List<JsonDataManager.PlayerRankingData>> rankingCache = new HashMap<>();
    private final Map<String, Long> cacheTimestamps = new HashMap<>();

    private static final int CACHE_EXPIRY_SECONDS = 300; // 緩存過期時間(秒)
    private static final int MAX_DISPLAY_ENTRIES = 5;    // 顯示前幾名(縮短)

    // 全息圖名稱前綴
    private static final String TEMP_HOLOGRAM_PREFIX = "dungeon_ranking_";
    private static final String PERM_HOLOGRAM_PREFIX = "permanent_ranking_";
    private BukkitRunnable updateTask;
    private final HologramConfigManager configManager;

    public RankingHologramManager(DungeonSystem plugin) {
        this.plugin = plugin;
        this.configManager = new HologramConfigManager(plugin);

        // 延遲載入以確保世界已載入
        new BukkitRunnable() {
            @Override
            public void run() {
                loadSavedHolograms();
                startPeriodicUpdateTask();
            }
        }.runTaskLater(plugin, 20L); // 延遲1秒
    }

    /**
     * 標準化副本ID - 移除實例後綴（如 _1, _2）
     */
    private String normalizeDungeonId(String dungeonId) {
        if (dungeonId == null) return dungeonId;

        // 檢查是否以 _數字 結尾
        int lastUnderscoreIndex = dungeonId.lastIndexOf('_');
        if (lastUnderscoreIndex > 0) { // 確保不是以 _ 開頭
            String suffix = dungeonId.substring(lastUnderscoreIndex + 1);
            try {
                // 如果後綴是數字，則移除它
                Integer.parseInt(suffix);
                return dungeonId.substring(0, lastUnderscoreIndex);
            } catch (NumberFormatException e) {
                // 如果不是數字後綴，保持原樣
                return dungeonId;
            }
        }
        return dungeonId;
    }

    // 啟動定期更新任務
    private void startPeriodicUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllVisibleHolograms();
            }
        };

        // 每5秒執行一次（5秒 = 100 ticks）
        updateTask.runTaskTimer(plugin, 100L, 100L);
    }

    private void loadSavedHolograms() {
        Map<String, Location> savedHolograms = configManager.loadAllHolograms();

        for (Map.Entry<String, Location> entry : savedHolograms.entrySet()) {
            String normalizedDungeonId = entry.getKey(); // 現在配置中保存的就是標準化ID
            Location location = entry.getValue();

            try {
                // 直接使用標準化ID創建
                createPermanentRankingWithNormalizedId(normalizedDungeonId, location);
            } catch (Exception e) {
                plugin.getLogger().warning("載入永久排行榜失敗 " + normalizedDungeonId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

    }

    private void createPermanentRankingWithNormalizedId(String normalizedDungeonId, Location location) {
        String hologramName = getPermanentHologramName(normalizedDungeonId);


        Dungeon dungeon = findDungeon(normalizedDungeonId);
        String displayName = dungeon != null ? dungeon.getDisplayName() : normalizedDungeonId;

        // 獲取統一格式化的排行榜數據
        List<JsonDataManager.PlayerRankingData> topPlayers = getUnifiedRankingData(normalizedDungeonId, 10);

        // 構建hologram內容
        List<String> lines = buildCompactHologram(displayName, topPlayers, normalizedDungeonId);

        // 檢查是否已存在
        Hologram existingHologram = DHAPI.getHologram(hologramName);
        if (existingHologram != null) {
            // 更新現有hologram
            DHAPI.setHologramLines(existingHologram, lines);
        } else {
            // 創建新hologram
            Hologram createdHologram = DHAPI.createHologram(hologramName, location, lines);
            if (createdHologram != null) {
                plugin.getLogger().info("成功創建永久排行榜: " + normalizedDungeonId);
            } else {
                plugin.getLogger().severe("創建永久排行榜失敗: " + normalizedDungeonId);
            }
        }
    }

    // 更新所有可見的全息圖
    private void updateAllVisibleHolograms() {
        try {

            // 更新所有永久排行榜 - 基於配置文件中的記錄（現在都是標準化ID）
            Map<String, Location> savedHolograms = configManager.loadAllHolograms();

            for (String normalizedDungeonId : savedHolograms.keySet()) {
                String hologramName = getPermanentHologramName(normalizedDungeonId);
                Hologram existingHologram = DHAPI.getHologram(hologramName);

                if (existingHologram != null) {

                    // 強制清除緩存以獲取最新數據
                    clearCacheForDungeon(normalizedDungeonId);

                    // 更新全息圖內容
                    updatePermanentHologramContentWithNormalizedId(normalizedDungeonId, existingHologram);
                } else {
                    plugin.getLogger().warning("找不到永久全息圖: " + hologramName);
                }
            }

            // 更新所有臨時排行榜
            updateTemporaryHolograms();


        } catch (Exception e) {
            plugin.getLogger().warning("定期更新全息圖時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 使用標準化ID更新永久全息圖內容
     */
    private void updatePermanentHologramContentWithNormalizedId(String normalizedDungeonId, Hologram hologram) {
        try {

            Dungeon dungeon = findDungeon(normalizedDungeonId);
            String displayName = dungeon != null ? dungeon.getDisplayName() : normalizedDungeonId;

            // 獲取統一格式化後的排行榜數據
            List<JsonDataManager.PlayerRankingData> topPlayers = getUnifiedRankingData(normalizedDungeonId, 10);

            // 構建全息圖內容
            List<String> lines = buildCompactHologram(displayName, topPlayers, normalizedDungeonId);

            // 更新hologram
            DHAPI.setHologramLines(hologram, lines);


        } catch (Exception e) {
            plugin.getLogger().warning("更新永久全息圖 " + normalizedDungeonId + " 時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清除指定副本的緩存
     */
    private void clearCacheForDungeon(String normalizedDungeonId) {
        cacheTimestamps.remove(normalizedDungeonId);
        rankingCache.remove(normalizedDungeonId);
    }

    // 更新臨時全息圖
    private void updateTemporaryHolograms() {
        try {
            // 獲取所有現有的臨時全息圖
            Map<String, Hologram> existingTempHolograms = new HashMap<>();

            // 檢查所有可能的臨時全息圖名稱
            for (String dungeonId : plugin.getDungeonManager().getAllDungeons().keySet()) {
                String normalizedId = normalizeDungeonId(dungeonId);
                String hologramName = TEMP_HOLOGRAM_PREFIX + normalizedId.replace(" ", "_");
                Hologram tempHologram = DHAPI.getHologram(hologramName);

                if (tempHologram != null) {
                    existingTempHolograms.put(normalizedId, tempHologram);
                }
            }


            // 更新找到的臨時全息圖
            for (Map.Entry<String, Hologram> entry : existingTempHolograms.entrySet()) {
                String normalizedId = entry.getKey();
                Hologram hologram = entry.getValue();

                // 清除緩存以獲取最新數據
                clearCacheForDungeon(normalizedId);

                // 更新臨時全息圖
                updateTemporaryHologramContent(normalizedId, hologram);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("更新臨時全息圖時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 更新臨時全息圖內容
    private void updateTemporaryHologramContent(String normalizedDungeonId, Hologram hologram) {
        try {

            Dungeon dungeon = findDungeon(normalizedDungeonId);
            String displayName = dungeon != null ? dungeon.getDisplayName() : normalizedDungeonId;

            // 獲取統一格式化後的排行榜數據
            List<JsonDataManager.PlayerRankingData> topPlayers = getUnifiedRankingData(normalizedDungeonId, 10);

            // 構建全息圖內容
            List<String> lines = buildCompactHologram(displayName, topPlayers, normalizedDungeonId);
            lines.add("§7§o(30秒後消失)");

            // 更新hologram
            DHAPI.setHologramLines(hologram, lines);

            plugin.getLogger().info("成功更新臨時全息圖: " + normalizedDungeonId + " (玩家數: " + topPlayers.size() + ")");

        } catch (Exception e) {
            plugin.getLogger().warning("更新臨時全息圖 " + normalizedDungeonId + " 時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 獲取統一格式化後的排行榜數據 - 核心方法
     * 這個方法確保我們總是獲取合併後的數據，而不是單個實例的數據
     */
    private List<JsonDataManager.PlayerRankingData> getUnifiedRankingData(String dungeonId, int limit) {
        // 確保使用標準化ID
        String normalizedDungeonId = normalizeDungeonId(dungeonId);

        long currentTime = System.currentTimeMillis();
        Long cacheTime = cacheTimestamps.get(normalizedDungeonId);

        // 檢查緩存
        if (cacheTime != null && (currentTime - cacheTime) / 1000 < CACHE_EXPIRY_SECONDS
                && rankingCache.containsKey(normalizedDungeonId)) {
            return rankingCache.get(normalizedDungeonId);
        }

        // 從數據管理器獲取統一的排行榜數據
        List<JsonDataManager.PlayerRankingData> data =
                plugin.getRankingManager().getDungeonRanking(normalizedDungeonId, limit);

        // 更新緩存
        rankingCache.put(normalizedDungeonId, data);
        cacheTimestamps.put(normalizedDungeonId, currentTime);

        return data;
    }

    /**
     * 構建簡化版的預設hologram內容
     */
    private List<String> buildCompactHologram(String displayName,
                                              List<JsonDataManager.PlayerRankingData> topPlayers,
                                              String dungeonId) {
        List<String> lines = new ArrayList<>();
        String titleLine = MessageUtil.parseToLegacyString("<gold><bold>" + displayName + " 排行</bold></gold>");
        lines.add(titleLine);

        // 如果沒有記錄
        if (topPlayers.isEmpty()) {
            lines.add(MessageUtil.parseToLegacyString("§7暫無記錄"));
            // 使用個人化佔位符
            lines.add("%dungeonrank_" + dungeonId + "_info%");
            return lines;
        }

        // 只顯示前幾名
        for (int i = 0; i < Math.min(MAX_DISPLAY_ENTRIES, topPlayers.size()); i++) {
            JsonDataManager.PlayerRankingData data = topPlayers.get(i);

            String rankText;
            String rankColor;

            switch (i) {
                case 0:
                    rankText = "第一名";
                    rankColor = "§6"; // 金色
                    break;
                case 1:
                    rankText = "第二名";
                    rankColor = "§d"; // 淺紫色
                    break;
                case 2:
                    rankText = "第三名";
                    rankColor = "§9"; // 藍色
                    break;
                default:
                    rankText = "第" + (i + 1) + "名";
                    rankColor = "§7"; // 灰色
                    break;
            }

            String rankLine = rankColor + rankText + " §f༻ " + rankColor + data.playerName + " §a" + data.completionCount + "次";
            lines.add(MessageUtil.parseToLegacyString(rankLine));
        }

        // 添加分隔線
        lines.add(MessageUtil.parseToLegacyString("§7§m───────"));

        // 使用個人化佔位符
        lines.add("%dungeonrank_" + dungeonId + "_info%");

        return lines;
    }

    /**
     * 創建或更新臨時排行榜hologram
     */
    public void createOrUpdateRanking(String dungeonId, Location location, Player requester) {
        // 使用標準化ID進行所有操作
        String normalizedDungeonId = normalizeDungeonId(dungeonId);
        String hologramName = TEMP_HOLOGRAM_PREFIX + normalizedDungeonId.replace(" ", "_");

        plugin.getLogger().info("創建臨時排行榜: " + dungeonId + " -> " + normalizedDungeonId);

        Dungeon dungeon = findDungeon(dungeonId);
        String displayName = dungeon != null ? dungeon.getDisplayName() : normalizedDungeonId;

        // 獲取統一格式化的排行榜數據
        List<JsonDataManager.PlayerRankingData> topPlayers = getUnifiedRankingData(normalizedDungeonId, 10);

        // 構建hologram內容
        List<String> lines = buildCompactHologram(displayName, topPlayers, normalizedDungeonId);
        lines.add("§7§o(30秒後消失)");

        // 移除現有的hologram
        removeRanking(normalizedDungeonId);

        // 創建新的hologram
        Hologram hologram = DHAPI.createHologram(hologramName, location, lines);
        if (hologram != null) {
            plugin.getLogger().info("成功創建臨時排行榜: " + normalizedDungeonId);
            // 設置自動消失
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeRanking(normalizedDungeonId);
                }
            }.runTaskLater(plugin, 30 * 20L);
        } else {
            plugin.getLogger().severe("創建hologram失敗: " + hologramName);
            requester.sendMessage("§c創建排行榜hologram失敗");
        }
    }

    /**
     * 智能尋找副本
     */
    private Dungeon findDungeon(String dungeonId) {
        // 首先嘗試直接查找原始ID
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon != null) {
            return dungeon;
        }

        // 如果原始ID找不到，嘗試標準化ID
        String normalizedId = normalizeDungeonId(dungeonId);
        dungeon = plugin.getDungeonManager().getDungeon(normalizedId);
        if (dungeon != null) {
            return dungeon;
        }

        // 搜索相關實例
        for (Map.Entry<String, Dungeon> entry : plugin.getDungeonManager().getAllDungeons().entrySet()) {
            String instanceId = entry.getKey();

            // 檢查是否匹配標準化後的ID
            if (normalizeDungeonId(instanceId).equals(normalizedId)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 清除所有快取
     */
    public void clearAllCache() {
        cacheTimestamps.clear();
        rankingCache.clear();
        plugin.getLogger().info("已清除所有排行榜快取");
    }

    /**
     * 創建永久排行榜hologram
     */
    public void createPermanentRanking(String dungeonId, Location location) {
        // 標準化ID用於數據查詢和配置保存
        String normalizedDungeonId = normalizeDungeonId(dungeonId);
        // 使用標準化ID命名全息圖
        String hologramName = getPermanentHologramName(normalizedDungeonId);

        plugin.getLogger().info("創建永久排行榜: " + dungeonId + " -> 標準化為: " + normalizedDungeonId);

        Dungeon dungeon = findDungeon(dungeonId);
        String displayName = dungeon != null ? dungeon.getDisplayName() : normalizedDungeonId;

        // 獲取統一格式化的排行榜數據
        List<JsonDataManager.PlayerRankingData> topPlayers = getUnifiedRankingData(normalizedDungeonId, 10);

        // 構建hologram內容
        List<String> lines = buildCompactHologram(displayName, topPlayers, normalizedDungeonId);

        // 檢查是否已存在
        Hologram existingHologram = DHAPI.getHologram(hologramName);
        if (existingHologram != null) {
            // 更新現有hologram
            DHAPI.setHologramLines(existingHologram, lines);
            plugin.getLogger().info("更新現有永久排行榜: " + normalizedDungeonId);
        } else {
            // 創建新hologram
            Hologram createdHologram = DHAPI.createHologram(hologramName, location, lines);
            if (createdHologram != null) {
                plugin.getLogger().info("成功創建新永久排行榜: " + normalizedDungeonId);
            } else {
                plugin.getLogger().severe("創建永久排行榜失敗: " + normalizedDungeonId);
                return;
            }

            // 保存到配置檔案 - 使用標準化ID（這裡會自動標準化）
            configManager.saveHologram(dungeonId, location);
            plugin.getLogger().info("已保存永久排行榜配置: " + normalizedDungeonId);
        }
    }

    /**
     * 更新永久排行榜
     */
    public void updatePermanentRanking(String dungeonId) {
        String normalizedDungeonId = normalizeDungeonId(dungeonId);
        String hologramName = getPermanentHologramName(normalizedDungeonId);
        Hologram existingHologram = DHAPI.getHologram(hologramName);

        if (existingHologram != null) {
            plugin.getLogger().info("更新永久排行榜: " + dungeonId + " -> " + normalizedDungeonId);

            // 強制清除緩存以獲取最新數據
            clearCacheForDungeon(normalizedDungeonId);

            // 更新hologram
            Location location = existingHologram.getLocation();
            createPermanentRanking(dungeonId, location);
        } else {
            plugin.getLogger().warning("找不到要更新的永久排行榜: " + normalizedDungeonId);
        }
    }

    /**
     * 計算玩家在排行榜中的排名
     */
    public int calculatePlayerRank(List<JsonDataManager.PlayerRankingData> topPlayers, String playerName) {
        for (int i = 0; i < topPlayers.size(); i++) {
            if (topPlayers.get(i).playerName.equals(playerName)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * 刪除臨時排行榜hologram
     */
    public void removeRanking(String dungeonId) {
        String normalizedId = normalizeDungeonId(dungeonId);
        String hologramName = TEMP_HOLOGRAM_PREFIX + normalizedId.replace(" ", "_");
        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram != null) {
            DHAPI.removeHologram(hologramName);
        }
    }

    /**
     * 刪除永久排行榜hologram
     */
    public void removePermanentRanking(String dungeonId) {
        String normalizedId = normalizeDungeonId(dungeonId);
        String hologramName = getPermanentHologramName(normalizedId);

        plugin.getLogger().info("刪除永久排行榜: " + dungeonId + " -> " + normalizedId);

        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram != null) {
            DHAPI.removeHologram(hologramName);
            plugin.getLogger().info("已刪除全息圖: " + hologramName);
        }

        // 從配置檔案中移除（會自動使用標準化ID）
        configManager.removeHologram(dungeonId);

        // 清除緩存
        clearCacheForDungeon(normalizedId);

        plugin.getLogger().info("已完全刪除永久排行榜: " + normalizedId);
    }

    /**
     * 更新所有永久排行榜
     */
    public void updateAllPermanentRankings() {
        plugin.getLogger().info("手動更新所有永久排行榜...");

        // 清除所有緩存以獲取最新數據
        cacheTimestamps.clear();
        rankingCache.clear();

        // 基於配置文件更新所有hologram
        Map<String, Location> savedHolograms = configManager.loadAllHolograms();
        for (String normalizedDungeonId : savedHolograms.keySet()) {
            String hologramName = getPermanentHologramName(normalizedDungeonId);
            Hologram existingHologram = DHAPI.getHologram(hologramName);

            if (existingHologram != null) {
                Location location = existingHologram.getLocation();
                // 直接使用標準化ID
                createPermanentRankingWithNormalizedId(normalizedDungeonId, location);
            }
        }

        plugin.getLogger().info("完成手動更新所有永久排行榜");
    }

    /**
     * 手動強制更新所有全息圖 - 用於調試
     */
    public void forceUpdateAll() {
        plugin.getLogger().info("手動強制更新所有全息圖");

        // 清除所有緩存
        cacheTimestamps.clear();
        rankingCache.clear();

        // 強制更新
        updateAllVisibleHolograms();
    }

    /**
     * 獲取永久hologram名稱
     */
    public String getPermanentHologramName(String dungeonId) {
        String normalizedId = normalizeDungeonId(dungeonId);
        return PERM_HOLOGRAM_PREFIX + normalizedId.replace(" ", "_");
    }

    /**
     * 檢查是否有永久排行榜
     */
    public boolean hasPermanentRanking(String dungeonId) {
        String normalizedId = normalizeDungeonId(dungeonId);
        String hologramName = getPermanentHologramName(normalizedId);
        return DHAPI.getHologram(hologramName) != null;
    }

    /**
     * 獲取所有永久排行榜副本ID
     */
    public List<String> getAllPermanentRankingDungeons() {
        List<String> dungeonIds = new ArrayList<>();
        // 直接從配置管理器獲取所有標準化的副本ID
        java.util.Set<String> normalizedIds = configManager.getAllNormalizedDungeonIds();
        dungeonIds.addAll(normalizedIds);
        return dungeonIds;
    }

    /**
     * 獲取全息圖位置的字符串表示
     */
    public String getHologramLocation(String hologramName) {
        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram != null) {
            return locationToString(hologram.getLocation());
        }
        return "未知位置";
    }

    /**
     * 位置轉換為字符串
     */
    private String locationToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return String.format("%s, %.2f, %.2f, %.2f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * 設置自動更新任務
     */
    public void startAutoUpdateTask(int intervalMinutes) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPermanentRankings();
            }
        }.runTaskTimer(plugin, 0L, intervalMinutes * 60 * 20L);
    }

    /**
     * 停止所有任務
     */
    public void shutdown() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            plugin.getLogger().info("已停止全息圖定期更新任務");
        }
    }

    /**
     * 修改更新間隔
     */
    public void setUpdateInterval(int seconds) {
        // 停止現有任務
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        // 啟動新的任務
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllVisibleHolograms();
            }
        };

        long ticks = seconds * 20L;
        updateTask.runTaskTimer(plugin, ticks, ticks);
        plugin.getLogger().info("已更新全息圖更新間隔為: " + seconds + "秒");
    }
}