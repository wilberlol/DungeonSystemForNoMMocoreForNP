package me.ninepin.dungeonSystem.Dungeon;

import org.bukkit.Location;

public class DungeonMob {

    private final String id;
    private final Location location;
    private final int amount;      // 生成數量
    private final double radius;   // 生成半徑
    private final int level;       // 怪物等級
    private final String type;     // 怪物類型：NORMAL 或 BOSS

    // 兼容構造函數（向後兼容）- 預設為 NORMAL 類型
    public DungeonMob(String id, Location location, int amount, double radius) {
        this(id, location, amount, radius, 1, "NORMAL");
    }

    // 兼容構造函數（向後兼容）- 預設為 NORMAL 類型
    public DungeonMob(String id, Location location, int amount, double radius, int level) {
        this(id, location, amount, radius, level, "NORMAL");
    }

    // 新完整構造函數
    public DungeonMob(String id, Location location, int amount, double radius, int level, String type) {
        this.id = id;
        this.location = location;
        this.amount = amount;
        this.radius = radius;
        this.level = Math.max(1, level); // 確保等級至少為1
        this.type = (type != null && type.toUpperCase().equals("BOSS")) ? "BOSS" : "NORMAL"; // 預設為 NORMAL
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public int getAmount() {
        return amount;
    }

    public double getRadius() {
        return radius;
    }

    public int getLevel() {
        return level;
    }

    public String getType() {
        return type;
    }

    public boolean isBoss() {
        return "BOSS".equals(type);
    }

    public boolean isNormal() {
        return "NORMAL".equals(type);
    }

    @Override
    public String toString() {
        return String.format("DungeonMob{id='%s', type='%s', amount=%d, radius=%.1f, level=%d, location=%s}",
                id, type, amount, radius, level, locationToString(location));
    }

    private String locationToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown";
        return String.format("%s,%.1f,%.1f,%.1f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}