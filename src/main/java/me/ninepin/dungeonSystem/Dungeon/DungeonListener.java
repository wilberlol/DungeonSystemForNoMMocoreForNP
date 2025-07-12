package me.ninepin.dungeonSystem.Dungeon;

import me.ninepin.dungeonSystem.DungeonSystem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class DungeonListener implements Listener {

    private final DungeonSystem plugin;
    private final DungeonManager dungeonManager;

    public DungeonListener(DungeonSystem plugin) {
        this.plugin = plugin;
        this.dungeonManager = plugin.getDungeonManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 延迟检查，确保玩家数据已加载
        new BukkitRunnable() {
            @Override
            public void run() {
                // 檢查玩家是否有未清理的副本記錄
                if (dungeonManager.getPlayerDungeon(playerId) != null) {
                    // 清理記錄
                    dungeonManager.getPlayerDungeons().remove(playerId);

                    // 可選：如果仍需要傳送玩家到出生點
                    String exitPointStr = plugin.getConfig().getString("settings.exit-point");
                    Location exitPoint = dungeonManager.parseLocation(exitPointStr);

                    if (exitPoint == null) {
                        exitPoint = player.getWorld().getSpawnLocation();
                    }

                    player.teleport(exitPoint);
                    player.sendMessage("§a你之前在副本中，已被傳送到出生點");
                }
            }
        }.runTaskLater(plugin, 20L); // 延迟1秒执行
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String dungeonId = dungeonManager.getPlayerDungeon(player.getUniqueId());

        if (dungeonId != null) {
            // 取消死亡訊息和掉落
            event.setDeathMessage(null);
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);

            // 使用延遲任務來確保玩家能被正確復活
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isDead()) {
                        player.spigot().respawn(); // 強制玩家復活
                    }

                    // 處理玩家死亡
                    dungeonManager.handlePlayerDeath(player, dungeonId);
                }
            }.runTaskLater(plugin, 1L); // 等待1tick以確保死亡處理完成
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String dungeonId = dungeonManager.getPlayerDungeon(player.getUniqueId());

        if (dungeonId != null) {
            // 將斷線玩家視為退出副本
            dungeonManager.handlePlayerDisconnect(player, dungeonId);
        }
    }
}