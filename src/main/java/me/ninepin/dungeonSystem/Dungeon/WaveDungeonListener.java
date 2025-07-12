package me.ninepin.dungeonSystem.Dungeon;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.ninepin.dungeonSystem.DungeonSystem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.UUID;

public class WaveDungeonListener implements Listener {

    private final DungeonManager dungeonManager;
    private final DungeonSystem plugin;

    public WaveDungeonListener(DungeonSystem plugin, WaveDungeonManager waveDungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = plugin.getDungeonManager();
    }
    /**
     * 監聽實體傷害事件，記錄玩家造成的傷害
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 檢查攻擊者是否為玩家
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager();
        String dungeonId = dungeonManager.getPlayerDungeon(damager.getUniqueId());

        // 檢查玩家是否在副本中
        if (dungeonId == null) {
            return;
        }

        // 檢查受害者是否為怪物（非玩家實體）
        if (event.getEntity() instanceof Player) {
            return; // 忽略玩家對玩家的傷害
        }

        // 記錄傷害
        if (plugin.getDamageTracker().hasDungeonStats(dungeonId)) {
            plugin.getDamageTracker().recordDamage(
                    dungeonId,
                    damager.getUniqueId(),
                    event.getFinalDamage()
            );
        }
    }

    /**
     * 監聽實體死亡事件，記錄擊殺
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // 檢查是否有擊殺者且為玩家
        if (entity.getKiller() == null || !(entity.getKiller() instanceof Player)) {
            return;
        }

        Player killer = entity.getKiller();
        String dungeonId = dungeonManager.getPlayerDungeon(killer.getUniqueId());

        // 檢查玩家是否在副本中
        if (dungeonId == null) {
            return;
        }

        // 檢查死亡的實體是否為玩家
        if (entity instanceof Player) {
            return; // 忽略玩家擊殺玩家的情況
        }

        // 記錄擊殺
        if (plugin.getDamageTracker().hasDungeonStats(dungeonId)) {
            plugin.getDamageTracker().recordKill(dungeonId, killer.getUniqueId());
        }
    }

    /**
     * 監聽玩家死亡事件
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String dungeonId = dungeonManager.getPlayerDungeon(player.getUniqueId());

        // 檢查玩家是否在副本中
        if (dungeonId == null) {
            return;
        }

        // 記錄死亡
        if (plugin.getDamageTracker().hasDungeonStats(dungeonId)) {
            plugin.getDamageTracker().recordDeath(dungeonId, player.getUniqueId());
        }
    }

    /**
     * 監聽玩家受傷事件
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // 檢查受害者是否為玩家
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        String dungeonId = dungeonManager.getPlayerDungeon(victim.getUniqueId());

        // 檢查玩家是否在副本中
        if (dungeonId == null) {
            return;
        }

        // 記錄承受傷害
        if (plugin.getDamageTracker().hasDungeonStats(dungeonId)) {
            plugin.getDamageTracker().recordDamageReceived(
                    dungeonId,
                    victim.getUniqueId(),
                    event.getFinalDamage()
            );
        }
    }
    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        // 获取死亡怪物的ID
        String mobId = event.getMobType().getInternalName();

        // 检查是否需要记录这个怪物的死亡
        for (Map.Entry<String, UUID> entry : dungeonManager.getActiveDungeons().entrySet()) {
            String dungeonId = entry.getKey();
            Dungeon dungeon = dungeonManager.getDungeon(dungeonId);

            // 如果不是波次副本，跳过（这会由普通的DungeonMobListener处理）
            if (!(dungeon instanceof WaveDungeon)) {
                continue;
            }

            // 如果是最终目标怪物，它的死亡会由DungeonMobListener处理
            // 所以这里只处理波次中的普通怪物
            if (mobId.equals(dungeon.getTargetMobId())) {
                continue;
            }

            // 这里由WaveDungeonManager自动管理波次进度
            // 它会在checkWaveProgress方法中检查所有怪物是否被击杀

            // 获取杀死怪物的玩家，如果有
            if (event.getKiller() instanceof Player) {
                Player killer = (Player) event.getKiller();
                String playerDungeonId = dungeonManager.getPlayerDungeon(killer.getUniqueId());

                // 确认玩家在这个副本中
                if (dungeonId.equals(playerDungeonId)) {
                    // 可以在这里添加玩家击杀怪物的奖励或记录
                    // 例如经验值、金币等

                    // 如果需要，可以通知玩家
                    // killer.sendMessage("§a你击杀了 " + mobId);
                }
            }
        }
    }


}