package me.ninepin.dungeonSystem.revive;

import me.ninepin.dungeonSystem.DungeonSystem;
import me.ninepin.dungeonSystem.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Set;
import java.util.UUID;

public class ReviveListener implements Listener {
    private final DungeonSystem plugin;
    private final ReviveItemManager reviveItemManager;
    private final ReviveGUI reviveGUI;
    private final ReviveManager reviveManager;

    public ReviveListener(DungeonSystem plugin) {
        this.plugin = plugin;
        this.reviveItemManager = plugin.getReviveItemManager();
        this.reviveGUI = new ReviveGUI(plugin);
        this.reviveManager = new ReviveManager(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!plugin.isRevivalSystemEnabled()) {
            return;  // 如果復活系統被禁用，直接返回，不處理任何復活相關事件
        }
        // 檢查是否是右鍵點擊
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 檢查是否是復活道具
        if (item == null || !reviveItemManager.isReviveItem(item)) {
            return;
        }

        // 阻止事件
        event.setCancelled(true);

        // 檢查玩家是否在副本中
        String dungeonId = plugin.getDungeonManager().getPlayerDungeon(player.getUniqueId());
        if (dungeonId == null) {
            MessageUtil.sendMessage(player, "§c你必須在副本中才能使用此道具");
            return;
        }

        // 檢查玩家是否死亡
        Set<UUID> deadPlayers = plugin.getDungeonManager().getDeadPlayers(dungeonId);
        if (deadPlayers == null || deadPlayers.isEmpty()) {
            MessageUtil.sendMessage(player, "§c目前沒有需要復活的玩家");
            return;
        }

        // 檢查玩家自己是否死亡
        if (deadPlayers.contains(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "§c你已經死亡，無法使用復活裝置");
            return;
        }

        // 獲取復活道具類型
        String reviveType = reviveItemManager.getReviveItemType(item);

        // 打開復活GUI
        reviveGUI.openReviveGUI(player, dungeonId, reviveType);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!plugin.isRevivalSystemEnabled()) {
            return;  // 如果復活系統被禁用，直接返回，不處理任何復活相關事件
        }
        // 檢查是否是我們的GUI
        if (!event.getView().getTitle().equals("§6選擇要復活的玩家")) {
            return;
        }

        event.setCancelled(true);

        // 檢查是否點擊空氣
        if (event.getCurrentItem() == null) {
            return;
        }

        // 檢查是否是玩家頭顱
        if (!(event.getWhoClicked() instanceof Player) ||
                !(event.getCurrentItem().getItemMeta() instanceof SkullMeta)) {
            return;
        }

        Player reviver = (Player) event.getWhoClicked();

        // 獲取目標玩家UUID和復活類型
        UUID targetUuid = reviveGUI.getPlayerUuidFromItem(event.getCurrentItem());
        String reviveType = reviveGUI.getReviveTypeFromItem(event.getCurrentItem());

        if (targetUuid == null || reviveType == null) {
            return;
        }

        // 獲取目標玩家
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendMessage(reviver, "§c目標玩家不在線");
            return;
        }

        // 關閉GUI
        reviver.closeInventory();

        // 獲取玩家手持的復活道具
        ItemStack item = reviver.getInventory().getItemInMainHand();
        if (!reviveItemManager.isReviveItem(item) ||
                !reviveItemManager.getReviveItemType(item).equals(reviveType)) {
            MessageUtil.sendMessage(reviver, "§c你手上沒有對應的復活裝置");
            return;
        }

        // 開始復活流程
        reviveManager.startRevive(reviver, target, reviveType, item);
    }
}
