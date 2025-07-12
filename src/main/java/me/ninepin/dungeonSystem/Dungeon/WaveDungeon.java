package me.ninepin.dungeonSystem.Dungeon;

import org.bukkit.Location;

import java.util.List;
import java.util.Map;

public class WaveDungeon extends Dungeon {
    private final int totalWaves;
    private final Map<Integer, List<DungeonMob>> waveMobs;
    private int currentWave;
    private boolean inProgress;

    // 原有建構函數（向後兼容）
    public WaveDungeon(String id, int levelRequired, int maxPlayers, Location spawnPoint,
                       Location deathWaitingArea, List<DungeonMob> mobs, String targetMobId,
                       int totalWaves, Map<Integer, List<DungeonMob>> waveMobs) {
        super(id, levelRequired, maxPlayers, spawnPoint, deathWaitingArea, mobs, targetMobId);
        this.totalWaves = totalWaves;
        this.waveMobs = waveMobs;
        this.currentWave = 0;
        this.inProgress = false;
    }

    // 新的建構函數，包含顯示名稱
    public WaveDungeon(String id, String displayName, int levelRequired, int maxPlayers, Location spawnPoint,
                       Location deathWaitingArea, List<DungeonMob> mobs, String targetMobId,
                       int totalWaves, Map<Integer, List<DungeonMob>> waveMobs) {
        super(id, displayName, levelRequired, maxPlayers, spawnPoint, deathWaitingArea, mobs, targetMobId);
        this.totalWaves = totalWaves;
        this.waveMobs = waveMobs;
        this.currentWave = 0;
        this.inProgress = false;
    }

    @Override
    public String getType() {
        return "wave";
    }

    // ... 其餘方法保持不變
    public int getTotalWaves() {
        return totalWaves;
    }

    public List<DungeonMob> getWaveMobs(int wave) {
        return waveMobs.get(wave);
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    public int nextWave() {
        if (currentWave < totalWaves) {
            currentWave++;
            return currentWave;
        }
        return -1; // 表示已完成所有波次
    }

    public void addMobToWave(int wave, DungeonMob mob) {
        if (wave >= 1 && wave <= totalWaves) {
            List<DungeonMob> mobs = waveMobs.get(wave);
            if (mobs != null) {
                mobs.add(mob);
            }
        }
    }
}