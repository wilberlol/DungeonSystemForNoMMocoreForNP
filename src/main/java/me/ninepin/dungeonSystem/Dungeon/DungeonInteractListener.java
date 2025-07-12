package me.ninepin.dungeonSystem.Dungeon;

import me.ninepin.dungeonSystem.DungeonSystem;
import me.ninepin.dungeonSystem.key.KeyManager;
import me.ninepin.dungeonSystem.party.IPartySystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class DungeonInteractListener implements Listener {

    private final DungeonSystem plugin;
    private final DungeonManager dungeonManager;
    private final KeyManager keyManager;
    private final IPartySystem partySystem;

    public DungeonInteractListener(DungeonSystem plugin) {
        this.plugin = plugin;
        this.dungeonManager = plugin.getDungeonManager();
        this.keyManager = plugin.getKeyManager();
        this.partySystem = plugin.getPartySystem();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 檢查是否是右鍵點擊
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 檢查物品是否是副本入場卷
        if (item == null || !keyManager.isDungeonKey(item)) {
            return;
        }

        // 獲取副本基礎ID
        String baseId = keyManager.getDungeonIdFromKey(item);
        if (baseId == null) {
            return;
        }

        // 阻止事件 (防止物品被消耗或與方塊交互)
        event.setCancelled(true);

        // 检查玩家是否已在副本中
        if (dungeonManager.getPlayerDungeon(player.getUniqueId()) != null) {
            player.sendMessage("§c你已經在副本中，無法再次進入其他副本");
            return;
        }

        // 檢查組隊系統是否可用
        if (partySystem == null) {
            player.sendMessage("§c組隊系統不可用，無法進入副本");
            return;
        }

        // 檢查是否在隊伍中
        if (!partySystem.hasParty(player.getUniqueId())) {
            player.sendMessage("§c你必須加入一個隊伍才能使用副本入場卷");
            return;
        }

        // 檢查是否是隊長
        if (!partySystem.isPartyLeader(player.getUniqueId())) {
            player.sendMessage("§c只有隊長才能使用副本入場卷");
            return;
        }

        // 獲取隊伍成員
        Set<UUID> partyMembers = partySystem.getPartyMembers(player.getUniqueId());

        // 判断是否是波次副本入场券
        boolean isWaveKey = keyManager.isWaveDungeonKey(item);

        // 获取实例ID
        String instanceId = findBestAvailableInstance(baseId, isWaveKey);

        // 如果找不到可用的副本实例
        if (instanceId == null) {
            player.sendMessage("§c找不到可用的 " + baseId + " 副本，請稍後再試");
            return;
        }

        // 获取副本对象
        Dungeon dungeon = dungeonManager.getDungeon(instanceId);
        if (dungeon == null) {
            player.sendMessage("§c找不到可用的副本");
            return;
        }

        // 檢查副本類型是否與鑰匙類型匹配
        boolean isDungeonWave = (dungeon instanceof WaveDungeon);
        if (isWaveKey != isDungeonWave) {
            if (isWaveKey) {
                player.sendMessage("§c错误: 你正在使用波次副本入场券，但指定的副本不是波次模式");
            } else {
                player.sendMessage("§c错误: 你正在使用普通副本入场券，但指定的副本是波次模式");
            }
            return;
        }

        // 檢查隊伍人數是否超過副本上限
        if (partyMembers.size() > dungeon.getMaxPlayers()) {
            player.sendMessage("§c隊伍人數 (" + partyMembers.size() + ") 超過副本上限 (" + dungeon.getMaxPlayers() + ")");
            return;
        }

        // 檢查隊伍中每個成員的等級要求
        List<String> membersWithoutLevel = new ArrayList<>();

        for (UUID memberUUID : partyMembers) {
            Player member = Bukkit.getPlayer(memberUUID);

            // 如果成員不在線上，記錄下來
            if (member == null) {
                String memberName = getMemberName(memberUUID);
                membersWithoutLevel.add(memberName + " (離線)");
                continue;
            }

            // 檢查成員等級是否達到要求
            if (member.getLevel() < dungeon.getLevelRequired()) {
                membersWithoutLevel.add(member.getName() + " (等級 " + member.getLevel() + "/" + dungeon.getLevelRequired() + ")");
            }
        }

        // 如果有成員等級不足，阻止進入
        if (!membersWithoutLevel.isEmpty()) {
            player.sendMessage("§c以下隊員等級不足，無法進入副本 §e" + dungeon.getDisplayName() + " §c(需要等級 " + dungeon.getLevelRequired() + ")：");
            for (String memberInfo : membersWithoutLevel) {
                player.sendMessage("§c- " + memberInfo);
            }
            player.sendMessage("§c請確保所有隊員等級達到要求後再試");
            return;
        }

        // 尝试进入副本
        boolean success = dungeonManager.joinDungeon(player, instanceId);

        // 如果成功进入，消耗一个入场券
        if (success) {
            // 播放鑰匙使用音效
            keyManager.playKeyUseSound(player, baseId);

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }
            player.updateInventory();

            // 記錄日誌
            plugin.getLogger().info("玩家 " + player.getName() + " 使用 " + baseId +
                    (isWaveKey ? " 波次" : "") + " 副本入場券進入了實例 " + instanceId);

            // 根據副本類型顯示不同的訊息
            if (dungeon instanceof WaveDungeon) {
                WaveDungeon waveDungeon = (WaveDungeon) dungeon;
                player.sendMessage("§b你已使用波次副本入場券進入 §e" + dungeon.getDisplayName() + " §b(共 " + waveDungeon.getTotalWaves() + " 波)");
            } else {
                player.sendMessage("§b你已使用副本入場券進入 §e" + dungeon.getDisplayName());
            }
        }
    }

    /**
     * 尋找最佳可用的副本實例
     *
     * @param baseId       副本基礎ID
     * @param needWaveType 是否需要波次類型副本
     * @return 可用的實例ID，如果沒有則返回null
     */
    private String findBestAvailableInstance(String baseId, boolean needWaveType) {
        Map<String, Dungeon> allDungeons = dungeonManager.getAllDungeons();
        Map<String, UUID> activeDungeons = dungeonManager.getActiveDungeons();

        List<String> matchingInstances = new ArrayList<>();

        // 第一步：找出所有匹配基礎ID的實例
        for (Map.Entry<String, Dungeon> entry : allDungeons.entrySet()) {
            String instanceId = entry.getKey();
            Dungeon dungeon = entry.getValue();

            // 從實例ID提取基礎ID
            String extractedBaseId = extractBaseIdFromInstance(instanceId);

            // 檢查基礎ID是否匹配
            if (!baseId.equals(extractedBaseId)) {
                continue;
            }

            // 檢查副本類型是否匹配
            boolean isDungeonWave = (dungeon instanceof WaveDungeon);
            if (isDungeonWave == needWaveType) {
                matchingInstances.add(instanceId);
            }
        }

        if (matchingInstances.isEmpty()) {
            plugin.getLogger().warning("找不到基礎ID為 " + baseId + " 且類型為 " +
                    (needWaveType ? "波次" : "普通") + " 的副本實例");
            return null;
        }

        // 第二步：在匹配的實例中找空閒的
        for (String instanceId : matchingInstances) {
            if (!activeDungeons.containsKey(instanceId)) {
                plugin.getLogger().info("找到可用的空閒實例: " + instanceId +
                        " (基礎ID: " + baseId + ", 類型: " + (needWaveType ? "波次" : "普通") + ")");
                return instanceId;
            }
        }

        // 第三步：如果都被占用，記錄日誌並返回null
        plugin.getLogger().info("所有匹配的 " + baseId + " (" +
                (needWaveType ? "波次" : "普通") + ") 副本實例都被占用。" +
                "總匹配實例數: " + matchingInstances.size());

        return null;
    }

    /**
     * 從實例ID中提取基礎ID
     * 假設格式: baseId_數字 (例如: dungeon1_1, dungeon1_2)
     * 如果沒有這種格式，整個ID就是基礎ID
     */
    private String extractBaseIdFromInstance(String instanceId) {
        if (instanceId.contains("_")) {
            String[] parts = instanceId.split("_");
            if (parts.length > 1) {
                try {
                    // 嘗試解析最後一部分是否為數字
                    Integer.parseInt(parts[parts.length - 1]);

                    // 如果是數字，重建沒有最後數字部分的基礎ID
                    StringBuilder baseIdBuilder = new StringBuilder(parts[0]);
                    for (int i = 1; i < parts.length - 1; i++) {
                        baseIdBuilder.append("_").append(parts[i]);
                    }
                    return baseIdBuilder.toString();
                } catch (NumberFormatException e) {
                    // 最後一部分不是數字，整個ID就是基礎ID
                    return instanceId;
                }
            }
        }

        // 沒有下劃線或其他情況，整個ID就是基礎ID
        return instanceId;
    }

    /**
     * 獲取成員名稱的輔助方法
     * 對於自製組隊系統，可以從緩存獲取；對於MMOCore，嘗試從玩家獲取
     */
    private String getMemberName(UUID memberUUID) {
        // 如果使用自製組隊系統且可以獲取到PartyManager
        if (plugin.isCustomPartySystem() && plugin.getPartyManager() != null) {
            me.ninepin.dungeonSystem.party.Party party = plugin.getPartyManager().getPlayerParty(memberUUID);
            if (party != null) {
                String name = party.getMembers().get(memberUUID);
                if (name != null) {
                    return name;
                }
            }
        }

        // 嘗試從在線玩家獲取
        Player member = Bukkit.getPlayer(memberUUID);
        if (member != null) {
            return member.getName();
        }

        // 如果都無法獲取，使用UUID的短版本
        return "未知玩家";
    }
}