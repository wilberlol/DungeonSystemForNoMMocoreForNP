package me.ninepin.dungeonSystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class MessageUtil {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    /**
     * 智能解析訊息：同時支援 MiniMessage 和 § 符號
     *
     * @param message 訊息文字（可包含 § 符號或 MiniMessage 格式）
     * @return 解析後的 Component
     */
    public static Component parseMessage(String message) {
        try {
            // 如果包含 MiniMessage 標記，使用 MiniMessage 解析
            if (message.matches(".*<[a-zA-Z_#][a-zA-Z0-9_:#]*>.*") ||
                    message.matches(".*</[a-zA-Z_][a-zA-Z0-9_]*>.*")) {
                // 包含有效的 MiniMessage 標記
                String convertedMessage = convertLegacyToMiniMessage(message);
                return miniMessage.deserialize(convertedMessage);
            } else {
                // 只包含 § 符號，使用傳統解析
                return legacySerializer.deserialize(message);
            }
        } catch (Exception e) {
            // 解析失敗時，回退到傳統 § 符號解析
            return legacySerializer.deserialize(message);
        }
    }

    /**
     * 將 § 符號轉換為 MiniMessage 格式（用於混合使用時）
     */
    private static String convertLegacyToMiniMessage(String message) {
        // 先檢查是否已經包含 MiniMessage 標記，如果有則不進行 § 符號轉換
        if (message.matches(".*<[a-zA-Z_#][a-zA-Z0-9_:#]*>.*")) {
            return message; // 已經是 MiniMessage 格式，直接返回
        }

        // 只轉換 § 符號
        return message
                .replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>")
                .replace("§b", "<aqua>")
                .replace("§c", "<red>")
                .replace("§d", "<light_purple>")
                .replace("§e", "<yellow>")
                .replace("§f", "<white>")
                .replace("§l", "<bold>")
                .replace("§o", "<italic>")
                .replace("§n", "<underlined>")
                .replace("§m", "<strikethrough>")
                .replace("§k", "<obfuscated>")
                .replace("§r", "<reset>");
    }

    /**
     * 發送訊息給玩家（智能解析）
     */
    public static void sendMessage(Player player, String message) {
        Component component = parseMessage(message);
        player.sendMessage(component);
    }

    /**
     * 將訊息轉換為傳統格式字串（保持顏色），用於 Hologram 顯示
     */
    public static String parseToLegacyString(String message) {
        try {
            Component component = parseMessage(message);
            return legacySerializer.serialize(component);
        } catch (Exception e) {
            return message; // 解析失敗時返回原文
        }
    }
}