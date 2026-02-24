package me.ninepin.dungeonSystem.revive;

import me.ninepin.dungeonSystem.DungeonSystem;
import me.ninepin.dungeonSystem.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ReviveManager {
    private final DungeonSystem plugin;
    private final Set<UUID> revivingPlayers = new HashSet<>(); // 正在執行復活的玩家
    private final Map<UUID, BukkitRunnable> reviveTasks = new HashMap<>(); // 復活倒計時任務

    public ReviveManager(DungeonSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * 開始復活流程
     */
    public boolean startRevive(Player reviver, Player target, String reviveType, ItemStack reviveItem) {
        if (!plugin.isRevivalSystemEnabled()) {
            MessageUtil.sendMessage(reviver, "§c復活系統目前已被禁用");
            return false;
        }
        String reviverDungeonId = plugin.getDungeonManager().getPlayerDungeon(reviver.getUniqueId());
        String targetDungeonId = plugin.getDungeonManager().getPlayerDungeon(target.getUniqueId());

        if (reviverDungeonId == null || !reviverDungeonId.equals(targetDungeonId)) {
            MessageUtil.sendMessage(reviver, "§c你和目標玩家不在同一個副本中");
            return false;
        }

        // 檢查玩家是否死亡
        Set<UUID> deadPlayers = plugin.getDungeonManager().getDeadPlayers(reviverDungeonId);
        if (deadPlayers == null || !deadPlayers.contains(target.getUniqueId())) {
            MessageUtil.sendMessage(reviver, "§c目標玩家並不需要復活");
            return false;
        }

        // 檢查是否已經在復活中
        if (revivingPlayers.contains(target.getUniqueId())) {
            MessageUtil.sendMessage(reviver, "§c該玩家正在被其他人復活中");
            return false;
        }

        // 標記玩家為正在復活中
        revivingPlayers.add(target.getUniqueId());

        if (reviveType.equals(ReviveItemManager.ADVANCED_REVIVE)) {
            // 高級復活，立即復活
            completeRevive(reviver, target, reviverDungeonId, reviveItem);
            return true;
        } else {
            // 普通復活，等待10秒
            startReviveTimer(reviver, target, reviverDungeonId, reviveItem);
            return true;
        }
    }

    /**
     * 開始復活倒計時（普通復活裝置）
     */
    private void startReviveTimer(Player reviver, Player target, String dungeonId, ItemStack reviveItem) {
        // 給予不能移動的效果
        reviver.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 220, 255));

        // 儲存原始位置
        Location reviverLocation = reviver.getLocation().clone();

        // 顯示開始復活的訊息
        MessageUtil.sendMessage(reviver, "§a開始復活 §e" + target.getName() + " §a，請保持不動，倒計時10秒");
        MessageUtil.sendMessage(target, "§a玩家 §e" + reviver.getName() + " §a正在復活你，倒計時10秒");

        // 創建並啟動復活倒計時任務
        BukkitRunnable reviveTask = new BukkitRunnable() {
            private int secondsLeft = 10;

            @Override
            public void run() {
                // 檢查是否有人掉線或死亡
                if (!reviver.isOnline() || !target.isOnline() ||
                        plugin.getDungeonManager().getPlayerDungeon(reviver.getUniqueId()) == null ||
                        plugin.getDungeonManager().getPlayerDungeon(target.getUniqueId()) == null) {
                    cancelRevive(reviver, target);
                    return;
                }

                // 檢查施術者是否移動
                if (reviverLocation.getWorld() != reviver.getLocation().getWorld() ||
                        reviverLocation.distance(reviver.getLocation()) > 0.5) {
                    MessageUtil.sendMessage(reviver, "§c你移動了，復活取消");
                    MessageUtil.sendMessage(target, "§c復活你的玩家移動了，復活取消");
                    cancelRevive(reviver, target);
                    return;
                }

                if (secondsLeft > 0) {
                    if (secondsLeft <= 5 || secondsLeft == 10) {
                        MessageUtil.sendMessage(reviver, "§a復活倒計時: §e" + secondsLeft + " §a秒");
                        MessageUtil.sendMessage(target, "§a復活倒計時: §e" + secondsLeft + " §a秒");
                    }
                    secondsLeft--;
                } else {
                    completeRevive(reviver, target, dungeonId, reviveItem);
                    cancel();
                }
            }
        };

        // 存儲任務以便可以取消
        reviveTasks.put(target.getUniqueId(), reviveTask);

        // 每秒執行一次，從現在開始
        reviveTask.runTaskTimer(plugin, 0, 20);
    }

    /**
     * 取消復活流程
     */
    public void cancelRevive(Player reviver, Player target) {
        UUID targetId = target.getUniqueId();

        // 取消任務
        if (reviveTasks.containsKey(targetId)) {
            reviveTasks.get(targetId).cancel();
            reviveTasks.remove(targetId);
        }

        // 移除復活中標記
        revivingPlayers.remove(targetId);

        // 移除施術者的效果
        reviver.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    /**
     * 完成復活流程
     */
    private void completeRevive(Player reviver, Player target, String dungeonId, ItemStack reviveItem) {
        UUID targetId = target.getUniqueId();

        // 移除復活中標記
        revivingPlayers.remove(targetId);
        reviveTasks.remove(targetId);

        // 先從死亡名單中移除
        Set<UUID> deadPlayers = plugin.getDungeonManager().getDeadPlayers(dungeonId);
        if (deadPlayers != null) {
            deadPlayers.remove(targetId);
        }

        // 傳送玩家到復活者的位置 (而不是副本出生點)
        target.teleport(reviver.getLocation());

        // 移除施術者的效果
        reviver.removePotionEffect(PotionEffectType.SLOWNESS);

        // 消耗物品
        if (reviveItem.getAmount() > 1) {
            reviveItem.setAmount(reviveItem.getAmount() - 1);
        } else {
            reviver.getInventory().removeItem(reviveItem);
        }
        reviver.updateInventory();

        // 發送成功訊息
        MessageUtil.sendMessage(reviver, "§a成功復活了 §e" + target.getName());
        MessageUtil.sendMessage(target, "§a你被 §e" + reviver.getName() + " §a復活了");

        // 向所有隊友通知
        String reviverPartyId = getPartyId(reviver);
        if (reviverPartyId != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != reviver && p != target && reviverPartyId.equals(getPartyId(p))) {
                    MessageUtil.sendMessage(p, "§a隊友 §e" + target.getName() + " §a被 §e" + reviver.getName() + " §a復活了");
                }
            }
        }
    }

    /**
     * 獲取玩家的隊伍ID
     */
    private String getPartyId(Player player) {
        me.ninepin.dungeonSystem.party.Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        return party != null ? party.getId().toString() : null;
    }
}
