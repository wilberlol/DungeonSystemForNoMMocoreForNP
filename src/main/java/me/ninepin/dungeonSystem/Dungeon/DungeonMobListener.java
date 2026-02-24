package me.ninepin.dungeonSystem.Dungeon;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.ninepin.dungeonSystem.DungeonSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonMobListener implements Listener {

    private final DungeonSystem plugin;
    private final DungeonManager dungeonManager;

    public DungeonMobListener(DungeonSystem plugin) {
        this.plugin = plugin;
        this.dungeonManager = plugin.getDungeonManager();
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        String mobId = event.getMobType().getInternalName();

        for (Map.Entry<String, UUID> entry : dungeonManager.getActiveDungeons().entrySet()) {
            String dungeonId = entry.getKey();
            Dungeon dungeon = dungeonManager.getDungeon(dungeonId);

            if (dungeon != null && mobId.equals(dungeon.getTargetMobId())) {
                // 通知玩家副本完成
                for (Map.Entry<UUID, String> playerEntry : dungeonManager.getPlayerDungeons().entrySet()) {
                    if (dungeonId.equals(playerEntry.getValue())) {
                        Player player = Bukkit.getPlayer(playerEntry.getKey());
                        if (player != null && player.isOnline()) {
                            player.sendMessage("§a目標怪物已被擊殺！副本挑戰成功，5秒後將傳送出副本...");
                        }
                    }
                }

                // 延遲5秒後處理副本完成
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 從配置文件獲取退出點坐標
                        String exitPointStr = plugin.getConfig().getString("settings.exit-point");
                        Location exitPoint = dungeonManager.parseLocation(exitPointStr);

                        if (exitPoint == null) {
                            exitPoint = Bukkit.getWorlds().get(0).getSpawnLocation();
                        }

                        // 傳送所有玩家並移除記錄
                        for (Map.Entry<UUID, String> playerEntry : new HashMap<>(dungeonManager.getPlayerDungeons()).entrySet()) {
                            if (dungeonId.equals(playerEntry.getValue())) {
                                UUID playerId = playerEntry.getKey();
                                Player player = Bukkit.getPlayer(playerId);
                                if (player != null && player.isOnline()) {
                                    player.teleport(exitPoint);
                                    player.sendMessage("§a恭喜你！副本挑戰成功，獲得豐厚獎勵！");

                                    // 記錄此玩家攻略此副本
                                    String baseDungeonId = dungeonManager.getBaseDungeonId(dungeonId);
                                    plugin.getRankingManager().recordCompletion(player, baseDungeonId);
                                }
                                // 從記錄中移除玩家
                                dungeonManager.getPlayerDungeons().remove(playerId);
                            }
                        }

                        // 清理副本
                        dungeonManager.cleanupDungeon(dungeonId);
                    }
                }.runTaskLater(plugin, 100L); // 5秒 = 100 ticks

                break; // 找到匹配的副本後跳出循環
            }
        }
    }

    /**
     * 處理副本挑戰成功
     */
    private void handleDungeonSuccess(String dungeonId) {
        plugin.getLogger().info("Dungeon " + dungeonId + " challenge succeeded!");

        // 找出在此副本的所有玩家
        for (Map.Entry<UUID, String> entry : dungeonManager.getPlayerDungeons().entrySet()) {
            if (dungeonId.equals(entry.getValue())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.sendMessage("§a目標怪物已被擊殺！副本挑戰成功，5秒後將傳送出副本...");
                }
            }
        }

        // 延遲5秒後傳送玩家出副本
        new BukkitRunnable() {
            @Override
            public void run() {
                // 從配置文件獲取退出點坐標
                String exitPointStr = plugin.getConfig().getString("settings.exit-point");
                Location exitPoint = dungeonManager.parseLocation(exitPointStr);

                // 如果配置中沒有退出點或格式不正確，使用世界出生點作為備用
                if (exitPoint == null) {
                    exitPoint = Bukkit.getWorlds().get(0).getSpawnLocation();
                }

                // 傳送所有在該副本中的玩家出副本
                for (Map.Entry<UUID, String> entry : dungeonManager.getPlayerDungeons().entrySet()) {
                    if (dungeonId.equals(entry.getValue())) {
                        UUID playerId = entry.getKey();
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            player.teleport(exitPoint);
                            player.sendMessage("§a恭喜你！副本挑戰成功，獲得豐厚獎勵！");
                            // 這裡可以添加獎勵發放邏輯

                            // 記錄此玩家攻略此副本
                            String baseDungeonId = dungeonManager.getBaseDungeonId(dungeonId);
                            plugin.getRankingManager().recordCompletion(player, baseDungeonId);
                        }
                        // 從記錄中移除此玩家
                        dungeonManager.getPlayerDungeons().remove(playerId);
                    }
                }

                // 清理副本
                dungeonManager.cleanupDungeon(dungeonId);
                dungeonManager.getDeadPlayers(dungeonId).remove(dungeonId);
                dungeonManager.getActiveDungeons().remove(dungeonId);

                plugin.getLogger().info("Cleaned up successful dungeon: " + dungeonId);
            }
        }.runTaskLater(plugin, 5 * 20L); // 5秒 = 5 * 20 ticks
    }
}