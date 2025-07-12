package me.ninepin.dungeonSystem;

import me.ninepin.dungeonSystem.Dungeon.*;
import me.ninepin.dungeonSystem.damage.DamageTracker;
import me.ninepin.dungeonSystem.key.KeyManager;
import me.ninepin.dungeonSystem.party.IPartySystem;
import me.ninepin.dungeonSystem.party.MMOCorePartyAdapter;
import me.ninepin.dungeonSystem.party.PartyCommand;
import me.ninepin.dungeonSystem.party.PartyManager;
import me.ninepin.dungeonSystem.ranking.DungeonRankingManager;
import me.ninepin.dungeonSystem.ranking.DungeonRankingPlaceholder;
import me.ninepin.dungeonSystem.ranking.RankingHologramManager;
import me.ninepin.dungeonSystem.revive.ReviveItemManager;
import me.ninepin.dungeonSystem.revive.ReviveListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import me.ninepin.dungeonSystem.party.PartyListener;

public class DungeonSystem extends JavaPlugin {

    private DungeonManager dungeonManager;
    private KeyManager keyManager;
    private ReviveItemManager reviveItemManager;
    private PartyManager partyManager;
    private WaveDungeonManager waveDungeonManager;
    private SoundConfig soundConfig;
    private DungeonRankingManager rankingManager;
    private RankingHologramManager hologramManager;
    private DamageTracker damageTracker;
    private IPartySystem partySystem;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        loadSoundConfig();
        // 初始化各个管理器
        damageTracker = new DamageTracker(this);
        if (isCustomPartySystem()) {
            partyManager = new PartyManager(this);
            partySystem = partyManager;
            // 註冊自製組隊系統的命令和事件
            getCommand("party").setExecutor(new PartyCommand(this));
            getServer().getPluginManager().registerEvents(new PartyListener(this), this);
            getLogger().info("Using custom party system.");
        } else if (isMMOCorePartySystem()) {
            if (Bukkit.getPluginManager().getPlugin("MMOCore") != null) {
                partyManager = null;
                partySystem = new MMOCorePartyAdapter();
                getLogger().info("Using MMOCore party system.");
            } else {
                getLogger().warning("MMOCore plugin not found! Falling back to custom party system.");
                partyManager = new PartyManager(this);
                partySystem = partyManager;
                getCommand("party").setExecutor(new PartyCommand(this));
                getServer().getPluginManager().registerEvents(new PartyListener(this), this);
            }
        }
        dungeonManager = new DungeonManager(this);
        keyManager = new KeyManager(this);
        rankingManager = new DungeonRankingManager(this);
        hologramManager = new RankingHologramManager(this);
        // 設置全息圖更新間隔
        int hologramUpdateInterval = getConfig().getInt("ranking.hologram-update-interval", 5);
        hologramManager.setUpdateInterval(hologramUpdateInterval);
        // 檢查復活系統是否啟用
        if (isRevivalSystemEnabled()) {
            reviveItemManager = new ReviveItemManager(this);
            // 注册復活系統相關事件监听器
            getServer().getPluginManager().registerEvents(new ReviveListener(this), this);
            getLogger().info("Revival system is enabled.");
        } else {
            reviveItemManager = null;
            getLogger().info("Revival system is disabled.");
        }
        if (getConfig().getBoolean("ranking.auto-update-enabled", true)) {
            int updateInterval = getConfig().getInt("ranking.auto-update-interval", 30);
            hologramManager.startAutoUpdateTask(updateInterval);
            getLogger().info("已啟動永久排行榜自動更新，間隔: " + updateInterval + " 分鐘");
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DungeonRankingPlaceholder(this).register();
            getLogger().info("DungeonRanking PlaceholderAPI 擴展已註冊");
        } else {
            getLogger().warning("PlaceholderAPI 未安裝，個人化功能將無法使用");
        }
        // 获取WaveDungeonManager实例
        waveDungeonManager = dungeonManager.getWaveDungeonManager();

        // 注册命令
        getCommand("dungeon").setExecutor(new DungeonCommand(this));

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonMobListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonInteractListener(this), this);

        // 注册波次副本事件监听器
        getServer().getPluginManager().registerEvents(new WaveDungeonListener(this, waveDungeonManager), this);

        // 初始化副本状态检查任务
        dungeonManager.initDungeonCheckTask();

        getLogger().info("DungeonSystem enabled successfully!");
    }
    public IPartySystem getPartySystem() {
        return partySystem;
    }
    // 修改檢查方法
    public boolean isCustomPartySystem() {
        return getConfig().getString("party.system-type", "custom").equalsIgnoreCase("custom");
    }

    public boolean isMMOCorePartySystem() {
        return getConfig().getString("party.system-type", "custom").equalsIgnoreCase("mmocore");
    }

    public DungeonRankingManager getRankingManager() {
        return rankingManager;
    }

    public RankingHologramManager getHologramManager() {
        return hologramManager;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }

    /**
     * 載入音效配置到快取
     */
    private void loadSoundConfig() {
        String countdownSound = getConfig().getString("wave-dungeon.sounds.countdown", "BLOCK_NOTE_BLOCK_PLING");
        String waveStartSound = getConfig().getString("wave-dungeon.sounds.wave-start", "ENTITY_ENDER_DRAGON_GROWL");
        String waveClearSound = getConfig().getString("wave-dungeon.sounds.wave-clear", "ENTITY_PLAYER_LEVELUP");
        String dungeonCompleteSound = getConfig().getString("wave-dungeon.sounds.dungeon-complete", "UI_TOAST_CHALLENGE_COMPLETE");
        String forceNextWaveSound = getConfig().getString("wave-dungeon.sounds.force-next-wave", "BLOCK_ANVIL_LAND");  // 新增
        double volume = getConfig().getDouble("wave-dungeon.sound-options.volume", 1.0);
        double pitch = getConfig().getDouble("wave-dungeon.sound-options.pitch", 1.0);
        double countdownPitch = getConfig().getDouble("wave-dungeon.sound-options.countdown-pitch", 1.2);
        double forceNextWavePitch = getConfig().getDouble("wave-dungeon.sound-options.force-next-wave-pitch", 0.8);  // 新增

        soundConfig = new SoundConfig(countdownSound, waveStartSound, waveClearSound, dungeonCompleteSound,
                forceNextWaveSound, volume, pitch, countdownPitch, forceNextWavePitch);  // 修改
        getLogger().info("音效配置已載入 - 倒數音效: " + countdownSound + ", 波次開始音效: " + waveStartSound +
                ", 波次完成音效: " + waveClearSound + ", 副本完成音效: " + dungeonCompleteSound +
                ", 強制下一波音效: " + forceNextWaveSound);  // 修改
    }

    /**
     * 獲取音效配置
     *
     * @return 音效配置實例
     */
    public SoundConfig getSoundConfig() {
        return soundConfig;
    }

    /**
     * 音效配置快取類別
     */
    public static class SoundConfig {
        private final String countdownSound;
        private final String waveStartSound;
        private final String waveClearSound;
        private final String dungeonCompleteSound;
        private final String forceNextWaveSound;  // 新增
        private final double volume;
        private final double pitch;
        private final double countdownPitch;
        private final double forceNextWavePitch;  // 新增

        public SoundConfig(String countdownSound, String waveStartSound, String waveClearSound,
                           String dungeonCompleteSound, String forceNextWaveSound, double volume,
                           double pitch, double countdownPitch, double forceNextWavePitch) {  // 修改
            this.countdownSound = countdownSound;
            this.waveStartSound = waveStartSound;
            this.waveClearSound = waveClearSound;
            this.dungeonCompleteSound = dungeonCompleteSound;
            this.forceNextWaveSound = forceNextWaveSound;  // 新增
            this.volume = volume;
            this.pitch = pitch;
            this.countdownPitch = countdownPitch;
            this.forceNextWavePitch = forceNextWavePitch;  // 新增
        }

        // Getters
        public String getCountdownSound() {
            return countdownSound;
        }

        public String getWaveStartSound() {
            return waveStartSound;
        }

        public String getWaveClearSound() {
            return waveClearSound;
        }

        public String getDungeonCompleteSound() {
            return dungeonCompleteSound;
        }

        public String getForceNextWaveSound() {
            return forceNextWaveSound;
        }  // 新增

        public double getVolume() {
            return volume;
        }

        public double getPitch() {
            return pitch;
        }

        public double getCountdownPitch() {
            return countdownPitch;
        }

        public double getForceNextWavePitch() {
            return forceNextWavePitch;
        }  // 新增
    }

    @Override
    public void onDisable() {
        getLogger().info("DungeonSystem has been disabled!");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        // 如果有其他配置文件需要重新加載，可以在這裡處理
    }

    /**
     * 重新加載所有配置
     */
    public void reloadAllConfigs() {
        reloadConfig();

        // 重新載入音效配置快取
        loadSoundConfig();

        if (keyManager != null) {
            keyManager.reload();
        }

        // 檢查復活系統狀態
        boolean revivalEnabled = isRevivalSystemEnabled();

        // 重新加載或初始化復活系統
        if (revivalEnabled) {
            if (reviveItemManager == null) {
                reviveItemManager = new ReviveItemManager(this);
                getServer().getPluginManager().registerEvents(new ReviveListener(this), this);
                getLogger().info("Revival system has been enabled.");
            } else {
                reviveItemManager.reload();
            }
        } else if (reviveItemManager != null) {
            reviveItemManager = null;
            getLogger().info("Revival system has been disabled.");
        }

        if (dungeonManager != null) {
            dungeonManager.reloadDungeons();
        }

        // 通知 WaveDungeonManager 配置已重載
        if (waveDungeonManager != null) {
            getLogger().info("波次副本音效配置已重載");
        }
    }

    public boolean isRevivalSystemEnabled() {
        return getConfig().getBoolean("settings.revival-system-enabled", true);
    }

    // 添加 PartyManager 的 getter 方法
    public PartyManager getPartyManager() {
        return partyManager;
    }

    /**
     * 獲取副本管理器實例
     *
     * @return DungeonManager實例
     */
    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    /**
     * 獲取副本鑰匙管理器實例
     *
     * @return KeyManager實例
     */
    public KeyManager getKeyManager() {
        return keyManager;
    }

    /**
     * 獲取復活物品管理器
     */
    public ReviveItemManager getReviveItemManager() {
        if (!isRevivalSystemEnabled()) {
            getLogger().warning("Attempted to access revival system when it is disabled!");
            return null;
        }
        return reviveItemManager;
    }

    public WaveDungeonManager getWaveDungeonManager() {
        return waveDungeonManager;
    }
}
