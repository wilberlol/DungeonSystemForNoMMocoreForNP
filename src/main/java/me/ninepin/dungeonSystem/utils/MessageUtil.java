package me.ninepin.dungeonSystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class MessageUtil {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    public static Component parseMessage(String message) {
        return miniMessage.deserialize(message);
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(miniMessage.deserialize(message));
    }

    public static String parseToLegacyString(String message) {
        return legacySerializer.serialize(miniMessage.deserialize(message));
    }
}
