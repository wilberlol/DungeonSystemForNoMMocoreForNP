package me.ninepin.dungeonSystem.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import me.ninepin.dungeonSystem.DungeonSystem;
import me.ninepin.dungeonSystem.Dungeon.Dungeon;
import me.ninepin.dungeonSystem.Dungeon.DungeonManager;
import me.ninepin.dungeonSystem.damage.DamageTracker;
import me.ninepin.dungeonSystem.damage.PlayerRanking;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Instant;
import java.util.*;

/**
 * Discord 通知管理器
 * 透過 DiscordSRV 的 JDA 在副本通關時發送 Embed 訊息到指定頻道
 */
public class DiscordNotifier {

    private final DungeonSystem plugin;
    private boolean enabled;
    private List<String> channelIds;

    // Embed 設定
    private Color embedColor;
    private String embedTitle;
    private String embedDescription;
    private List<FieldTemplate> fieldTemplates;
    private String embedFooter;
    private String embedThumbnail;

    public DiscordNotifier(DungeonSystem plugin) {
        this.plugin = plugin;
        this.channelIds = new ArrayList<>();
        this.fieldTemplates = new ArrayList<>();
        reload();
    }

    /**
     * 從 config.yml 載入設定
     */
    public void reload() {
        enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        channelIds = plugin.getConfig().getStringList("discord.channel-ids");

        // Embed 設定
        String colorStr = plugin.getConfig().getString("discord.embed.color", "#FFD700");
        embedColor = parseColor(colorStr);
        embedTitle = plugin.getConfig().getString("discord.embed.title", "\uD83C\uDFC6 副本通關！");
        embedDescription = plugin.getConfig().getString("discord.embed.description", "**{dungeon_name}** 已被成功攻略！");
        embedFooter = plugin.getConfig().getString("discord.embed.footer", "副本類型：{dungeon_type} | 人數：{player_count}");
        embedThumbnail = plugin.getConfig().getString("discord.embed.thumbnail", "");

        // 載入自訂 fields
        fieldTemplates.clear();
        List<Map<?, ?>> fieldsConfig = plugin.getConfig().getMapList("discord.embed.fields");
        for (Map<?, ?> fieldMap : fieldsConfig) {
            Object nameObj = fieldMap.get("name");
            Object valueObj = fieldMap.get("value");
            Object inlineObj = fieldMap.get("inline");
            String name = nameObj != null ? String.valueOf(nameObj) : "";
            String value = valueObj != null ? String.valueOf(valueObj) : "";
            boolean inline = inlineObj != null && Boolean.parseBoolean(String.valueOf(inlineObj));
            if (!name.isEmpty() && !value.isEmpty()) {
                fieldTemplates.add(new FieldTemplate(name, value, inline));
            }
        }

        if (enabled) {
            plugin.getLogger().info("Discord 通知已啟用，頻道數量: " + channelIds.size());
        } else {
            plugin.getLogger().info("Discord 通知已停用");
        }
    }

    /**
     * 檢查 DiscordSRV 是否可用
     */
    private boolean isDiscordSRVAvailable() {
        return Bukkit.getPluginManager().getPlugin("DiscordSRV") != null
                && Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
    }

    /**
     * 發送副本通關通知到 Discord
     * <p>
     * 此方法必須在主執行緒呼叫（從 Bukkit 事件處理中呼叫），
     * 會先在主執行緒收集所有 Bukkit API 資料，再非同步發送到 Discord。
     *
     * @param dungeonId       副本實例 ID（例如 "normal_sample_1"）
     * @param playerUUIDs     通關玩家的 UUID 集合
     * @param durationSeconds 通關耗時（秒），-1 表示無法取得
     */
    public void sendClearMessage(String dungeonId, Collection<UUID> playerUUIDs, long durationSeconds) {
        if (!enabled || channelIds.isEmpty()) return;
        if (!isDiscordSRVAvailable()) {
            plugin.getLogger().warning("Discord 通知已啟用但 DiscordSRV 未安裝或未啟用，跳過發送");
            return;
        }

        // === 主執行緒：收集所有需要 Bukkit API 的資料 ===
        Map<String, String> placeholders = collectPlaceholders(dungeonId, playerUUIDs, durationSeconds);

        // 建構 Embed（純資料操作，不依賴 Bukkit API，但在主執行緒做也無妨）
        MessageEmbed embed = buildEmbed(placeholders);
        if (embed == null) return;

        // 複製 channelIds 避免 reload 時的並發問題
        List<String> targetChannels = new ArrayList<>(channelIds);

        // === 非同步：只做 JDA 發送，不碰 Bukkit API ===
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                JDA jda = DiscordSRV.getPlugin().getJda();
                if (jda == null) {
                    plugin.getLogger().warning("DiscordSRV JDA 尚未就緒（機器人可能尚在連線），跳過本次通知");
                    return;
                }

                for (String channelId : targetChannels) {
                    try {
                        TextChannel channel = jda.getTextChannelById(channelId);
                        if (channel != null) {
                            channel.sendMessageEmbeds(embed).queue(
                                    success -> plugin.getLogger().info("Discord 通關通知已發送到頻道: " + channelId),
                                    error -> plugin.getLogger().warning("發送 Discord 通知失敗（頻道 " + channelId + "）: " + error.getMessage())
                            );
                        } else {
                            plugin.getLogger().warning("找不到 Discord 頻道: " + channelId + "，請檢查頻道 ID 是否正確");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("發送 Discord 通知到頻道 " + channelId + " 時發生錯誤: " + e.getMessage());
                    }
                }
            } catch (NoClassDefFoundError e) {
                plugin.getLogger().severe("DiscordSRV 類別載入失敗，請確認 DiscordSRV 已正確安裝: " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().warning("發送 Discord 通知時發生錯誤: " + e.getMessage());
            }
        });
    }

    /**
     * 在主執行緒收集所有佔位符資料（涉及 Bukkit API 的部分都在這裡處理）
     */
    private Map<String, String> collectPlaceholders(String dungeonId, Collection<UUID> playerUUIDs, long durationSeconds) {
        DungeonManager dungeonManager = plugin.getDungeonManager();
        Dungeon dungeon = dungeonManager.getDungeon(dungeonId);

        // 取得副本顯示名稱（去除 MiniMessage 格式碼）
        String dungeonName;
        String dungeonType;
        if (dungeon != null) {
            dungeonName = stripMiniMessage(dungeon.getDisplayName());
            dungeonType = dungeon.getType();
        } else {
            String baseDungeonId = dungeonManager.getBaseDungeonId(dungeonId);
            dungeonName = baseDungeonId != null ? baseDungeonId : dungeonId;
            dungeonType = "unknown";
        }

        // 收集玩家名稱（Bukkit.getPlayer 必須在主執行緒呼叫）
        List<String> playerNames = new ArrayList<>();
        for (UUID uuid : playerUUIDs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                playerNames.add(player.getName());
            }
        }
        String playersStr = playerNames.isEmpty() ? "無" : String.join(", ", playerNames);
        String playerCount = String.valueOf(playerNames.size());

        // 計算耗時
        String timeStr;
        if (durationSeconds >= 0) {
            long minutes = durationSeconds / 60;
            long seconds = durationSeconds % 60;
            timeStr = String.format("%02d:%02d", minutes, seconds);
        } else {
            timeStr = "N/A";
        }

        // 取得 MVP（傷害最高的玩家）
        String mvpName = "N/A";
        String mvpDamage = "N/A";
        DamageTracker damageTracker = plugin.getDamageTracker();
        if (damageTracker.hasDungeonStats(dungeonId)) {
            List<PlayerRanking> rankings = damageTracker.generateRankings(dungeonId);
            if (!rankings.isEmpty()) {
                PlayerRanking mvp = rankings.get(0);
                mvpName = mvp.getPlayerName();
                mvpDamage = String.format("%,.1f", mvp.getTotalDamage());
            }
        }

        // 建立佔位符映射
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{dungeon_name}", dungeonName);
        placeholders.put("{players}", playersStr);
        placeholders.put("{player_count}", playerCount);
        placeholders.put("{time}", timeStr);
        placeholders.put("{dungeon_type}", dungeonType);
        placeholders.put("{mvp_name}", mvpName);
        placeholders.put("{mvp_damage}", mvpDamage);
        return placeholders;
    }

    /**
     * 建構 Embed 訊息（純資料操作，不依賴 Bukkit API）
     */
    private MessageEmbed buildEmbed(Map<String, String> placeholders) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(embedColor);
        builder.setTitle(replacePlaceholders(embedTitle, placeholders));
        builder.setDescription(replacePlaceholders(embedDescription, placeholders));
        builder.setTimestamp(Instant.now());

        // 自訂 fields
        for (FieldTemplate field : fieldTemplates) {
            builder.addField(
                    replacePlaceholders(field.name, placeholders),
                    replacePlaceholders(field.value, placeholders),
                    field.inline
            );
        }

        // Footer
        if (embedFooter != null && !embedFooter.isEmpty()) {
            builder.setFooter(replacePlaceholders(embedFooter, placeholders));
        }

        // Thumbnail
        if (embedThumbnail != null && !embedThumbnail.isEmpty()) {
            builder.setThumbnail(replacePlaceholders(embedThumbnail, placeholders));
        }

        return builder.build();
    }

    /**
     * 替換佔位符
     */
    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 去除 MiniMessage 格式碼（如 <yellow>, <#ff0000> 等）
     */
    private String stripMiniMessage(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "").trim();
    }

    /**
     * 解析顏色字串（支援 #RRGGBB 格式）
     */
    private Color parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return Color.decode(colorStr);
            }
            return Color.decode("#" + colorStr);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("無法解析顏色: " + colorStr + "，使用預設金色");
            return new Color(255, 215, 0);
        }
    }

    /**
     * Embed Field 模板
     */
    private static class FieldTemplate {
        final String name;
        final String value;
        final boolean inline;

        FieldTemplate(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
}
