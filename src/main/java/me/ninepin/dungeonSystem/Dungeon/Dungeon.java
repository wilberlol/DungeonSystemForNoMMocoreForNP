package me.ninepin.dungeonSystem.Dungeon;

import me.ninepin.dungeonSystem.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import java.util.List;

public class Dungeon {

    private final String id;
    private final String displayName; // 新增顯示名稱
    private final int levelRequired;
    private final int maxPlayers;
    private final Location spawnPoint;
    private final List<DungeonMob> mobs;
    private final Location deathWaitingArea;
    private final String targetMobId;

    public Dungeon(String id, int levelRequired, int maxPlayers, Location spawnPoint, Location deathWaitingArea, List<DungeonMob> mobs, String targetMobId) {
        this.id = id;
        this.displayName = null; // 預設為 null，使用 ID
        this.levelRequired = levelRequired;
        this.maxPlayers = maxPlayers;
        this.spawnPoint = spawnPoint;
        this.deathWaitingArea = deathWaitingArea;
        this.mobs = mobs;
        this.targetMobId = targetMobId;
    }

    // 新的建構函數，包含顯示名稱
    public Dungeon(String id, String displayName, int levelRequired, int maxPlayers, Location spawnPoint, Location deathWaitingArea, List<DungeonMob> mobs, String targetMobId) {
        this.id = id;
        this.displayName = displayName;
        this.levelRequired = levelRequired;
        this.maxPlayers = maxPlayers;
        this.spawnPoint = spawnPoint;
        this.deathWaitingArea = deathWaitingArea;
        this.mobs = mobs;
        this.targetMobId = targetMobId;
    }

    /**
     * 获取副本类型
     * @return 副本类型 "normal"
     */
    public String getType() {
        return "normal";
    }

    /**
     * 獲取顯示名稱，如果沒有設置則返回ID
     * @return 顯示名稱或ID
     */
    public String getDisplayName() {
        return displayName != null ? displayName : id;
    }
    /**
     * 獲取解析後的顯示名稱 Component（支援 MiniMessage）
     */
    public Component getDisplayNameComponent() {
        String name = getDisplayName();
        return MessageUtil.parseMessage(name);
    }
    public String getParsedDisplayName() {
        String name = getDisplayName();
        return MessageUtil.parseToLegacyString(name);
    }
    public Location getDeathWaitingArea() {
        return deathWaitingArea;
    }

    public String getId() {
        return id;
    }

    public int getLevelRequired() {
        return levelRequired;
    }

    public String getTargetMobId() {
        return targetMobId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public List<DungeonMob> getMobs() {
        return mobs;
    }
}