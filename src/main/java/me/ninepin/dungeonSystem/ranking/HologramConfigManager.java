package me.ninepin.dungeonSystem.ranking;

import me.ninepin.dungeonSystem.DungeonSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HologramConfigManager {
    private final DungeonSystem plugin;
    private final File configFile;
    private FileConfiguration config;

    public HologramConfigManager(DungeonSystem plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "holograms.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            // 如果檔案不存在，創建一個空的配置檔案
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("無法創建全息圖配置檔案: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
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

    /**
     * 保存全息圖配置 - 使用標準化ID
     */
    public void saveHologram(String dungeonId, Location location) {
        // 標準化副本ID，去掉實例後綴
        String normalizedDungeonId = normalizeDungeonId(dungeonId);

        plugin.getLogger().info("保存全息圖配置: " + dungeonId + " -> " + normalizedDungeonId);

        String path = "permanent_holograms." + normalizedDungeonId;
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());

        saveConfig();

        plugin.getLogger().info("已保存標準化全息圖配置: " + normalizedDungeonId);
    }

    /**
     * 移除全息圖配置 - 使用標準化ID
     */
    public void removeHologram(String dungeonId) {
        // 標準化副本ID
        String normalizedDungeonId = normalizeDungeonId(dungeonId);

        plugin.getLogger().info("移除全息圖配置: " + dungeonId + " -> " + normalizedDungeonId);

        config.set("permanent_holograms." + normalizedDungeonId, null);
        saveConfig();

        plugin.getLogger().info("已移除標準化全息圖配置: " + normalizedDungeonId);
    }

    /**
     * 載入所有全息圖配置
     */
    public Map<String, Location> loadAllHolograms() {
        Map<String, Location> holograms = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("permanent_holograms");

        if (section != null) {
            plugin.getLogger().info("載入全息圖配置，找到 " + section.getKeys(false).size() + " 個配置項目");

            for (String dungeonId : section.getKeys(false)) {
                String worldName = section.getString(dungeonId + ".world");
                double x = section.getDouble(dungeonId + ".x");
                double y = section.getDouble(dungeonId + ".y");
                double z = section.getDouble(dungeonId + ".z");

                if (Bukkit.getWorld(worldName) != null) {
                    Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                    holograms.put(dungeonId, location);
                    plugin.getLogger().info("載入全息圖配置: " + dungeonId + " 在 " + worldName);
                } else {
                    plugin.getLogger().warning("無法載入全息圖配置 " + dungeonId + ": 世界 " + worldName + " 不存在");
                }
            }
        } else {
            plugin.getLogger().info("沒有找到永久全息圖配置");
        }

        return holograms;
    }

    /**
     * 檢查指定副本是否有保存的全息圖配置
     */
    public boolean hasHologramConfig(String dungeonId) {
        String normalizedDungeonId = normalizeDungeonId(dungeonId);
        return config.contains("permanent_holograms." + normalizedDungeonId);
    }

    /**
     * 獲取指定副本的全息圖位置
     */
    public Location getHologramLocation(String dungeonId) {
        String normalizedDungeonId = normalizeDungeonId(dungeonId);
        ConfigurationSection section = config.getConfigurationSection("permanent_holograms." + normalizedDungeonId);

        if (section != null) {
            String worldName = section.getString("world");
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");

            if (Bukkit.getWorld(worldName) != null) {
                return new Location(Bukkit.getWorld(worldName), x, y, z);
            }
        }

        return null;
    }

    /**
     * 更新現有全息圖的位置
     */
    public void updateHologramLocation(String dungeonId, Location newLocation) {
        if (hasHologramConfig(dungeonId)) {
            saveHologram(dungeonId, newLocation);
            plugin.getLogger().info("已更新全息圖位置: " + normalizeDungeonId(dungeonId));
        }
    }

    /**
     * 獲取所有標準化的副本ID列表
     */
    public java.util.Set<String> getAllNormalizedDungeonIds() {
        ConfigurationSection section = config.getConfigurationSection("permanent_holograms");
        if (section != null) {
            return section.getKeys(false);
        }
        return new java.util.HashSet<>();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
            plugin.getLogger().info("已保存全息圖配置檔案");
        } catch (IOException e) {
            plugin.getLogger().severe("無法保存全息圖配置: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 重新載入配置檔案
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("已重新載入全息圖配置");
    }

    /**
     * 調試方法：列出所有配置項目
     */
    public void debugPrintAllConfigs() {
        plugin.getLogger().info("=== 全息圖配置調試信息 ===");
        ConfigurationSection section = config.getConfigurationSection("permanent_holograms");

        if (section != null) {
            for (String dungeonId : section.getKeys(false)) {
                String worldName = section.getString(dungeonId + ".world");
                double x = section.getDouble(dungeonId + ".x");
                double y = section.getDouble(dungeonId + ".y");
                double z = section.getDouble(dungeonId + ".z");

                plugin.getLogger().info("配置項目: " + dungeonId +
                        " -> 世界: " + worldName +
                        ", 位置: (" + x + ", " + y + ", " + z + ")");
            }
        } else {
            plugin.getLogger().info("沒有找到任何配置項目");
        }
        plugin.getLogger().info("=== 配置調試信息結束 ===");
    }
}