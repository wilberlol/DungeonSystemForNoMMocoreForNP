package me.ninepin.dungeonSystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class MessageUtil {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    // 支援 hex 色碼的序列化器（gradient 會被展開為逐字元 §x§R§R§G§G§B§B 格式）
    private static final LegacyComponentSerializer hexLegacySerializer = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    /**
     * 將 legacy § 色碼轉換為對應的 MiniMessage tag，
     * 使字串可以安全地傳入 miniMessage.deserialize()
     */
    private static String convertLegacyToMiniMessage(String text) {
        if (text == null || !text.contains("§")) return text;
        return text
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
                .replace("§k", "<obfuscated>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>")
                .replace("§r", "<reset>");
    }

    public static Component parseMessage(String message) {
        return miniMessage.deserialize(convertLegacyToMiniMessage(message));
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(miniMessage.deserialize(convertLegacyToMiniMessage(message)));
    }

    public static String parseToLegacyString(String message) {
        return legacySerializer.serialize(miniMessage.deserialize(convertLegacyToMiniMessage(message)));
    }

    /**
     * 將 MiniMessage 格式字串轉換為支援 hex 色碼的 legacy 字串。
     * gradient / hex color 會被展開為逐字元 §x§R§R§G§G§B§B 格式，
     * 適用於 DecentHolograms 等支援 hex 的顯示元件。
     */
    public static String parseToHexLegacyString(String message) {
        return hexLegacySerializer.serialize(miniMessage.deserialize(convertLegacyToMiniMessage(message)));
    }
}
