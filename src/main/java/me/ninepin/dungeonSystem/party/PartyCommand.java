package me.ninepin.dungeonSystem.party;

import me.ninepin.dungeonSystem.DungeonSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {
    private final DungeonSystem plugin;
    private final PartyManager partyManager;
    private final Map<UUID, UUID> pendingInvites = new HashMap<>(); // 玩家UUID -> 邀請者UUID

    public PartyCommand(DungeonSystem plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();

        // 設定邀請超時
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupInvites, 1200L, 1200L); // 每分鐘清理一次
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此指令只能由玩家執行");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreateCommand(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /party invite <玩家名稱>");
                    return true;
                }
                handleInviteCommand(player, args[1]);
                break;
            case "accept":
                handleAcceptCommand(player);
                break;
            case "decline":
                handleDeclineCommand(player);
                break;
            case "leave":
                handleLeaveCommand(player);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /party kick <玩家名稱>");
                    return true;
                }
                handleKickCommand(player, args[1]);
                break;
            case "info":
                handleInfoCommand(player);
                break;
            case "chat":
            case "c":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /party chat <訊息>");
                    return true;
                }
                handleChatCommand(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                break;
            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("§6========== §e隊伍系統指令 §6==========");
        player.sendMessage("§e/party create §7- 創建新隊伍");
        player.sendMessage("§e/party invite <玩家> §7- 邀請玩家加入隊伍");
        player.sendMessage("§e/party accept §7- 接受隊伍邀請");
        player.sendMessage("§e/party decline §7- 拒絕隊伍邀請");
        player.sendMessage("§e/party leave §7- 離開當前隊伍");
        player.sendMessage("§e/party kick <玩家> §7- 將玩家踢出隊伍");
        player.sendMessage("§e/party info §7- 顯示隊伍信息");
        player.sendMessage("§e/party chat <訊息> §7- 發送隊伍聊天訊息");
    }

    private void handleCreateCommand(Player player) {
        // 檢查玩家是否已經在隊伍中
        if (partyManager.getPlayerParty(player.getUniqueId()) != null) {
            player.sendMessage("§c你已經在一個隊伍中，請先離開當前隊伍");
            return;
        }

        // 創建新隊伍
        Party party = partyManager.createParty(player);
        player.sendMessage("§a你創建了一個新的隊伍！使用/party invite <玩家ID> 來邀請");
    }

    private void handleInviteCommand(Player player, String targetName) {
        // 查找目標玩家
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c找不到玩家 " + targetName + " 或該玩家不在線");
            return;
        }

        // 檢查目標玩家是否已經在隊伍中
        if (partyManager.getPlayerParty(target.getUniqueId()) != null) {
            player.sendMessage("§c該玩家已經在一個隊伍中");
            return;
        }

        // 檢查玩家是否在隊伍中
        Party party = partyManager.getPlayerParty(player.getUniqueId());

        // 如果玩家不在隊伍中，自動創建一個
        if (party == null) {
            party = partyManager.createParty(player);
            player.sendMessage("§a你創建了一個新的隊伍！");
        } else {
            // 檢查玩家是否是隊長
            if (!party.isOwner(player.getUniqueId())) {
                player.sendMessage("§c只有隊長才能邀請其他玩家");
                return;
            }
        }

        // 檢查是否已經有待處理的邀請
        if (pendingInvites.containsKey(target.getUniqueId())) {
            player.sendMessage("§c該玩家已經有一個待處理的邀請");
            return;
        }

        // 檢查隊伍是否已滿
        if (party.isFull()) {
            player.sendMessage("§c隊伍已滿，無法邀請更多玩家");
            return;
        }

        // 發送邀請
        pendingInvites.put(target.getUniqueId(), player.getUniqueId());
        player.sendMessage("§a已發送隊伍邀請給 " + target.getName());

        // 向目標玩家發送邀請訊息
        target.sendMessage("§a" + player.getName() + " 邀請你加入他的隊伍");

        // 創建可點擊按鈕（需要導入相關的類）
        TextComponent acceptMessage = new TextComponent("§a[接受邀請]");
        acceptMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
        acceptMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("點擊接受邀請").color(ChatColor.GREEN).create()));

        TextComponent spaceComponent = new TextComponent(" ");

        TextComponent declineMessage = new TextComponent("§c[拒絕邀請]");
        declineMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline"));
        declineMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("點擊拒絕邀請").color(ChatColor.RED).create()));

        target.spigot().sendMessage(acceptMessage, spaceComponent, declineMessage);

        // 設定邀請超時
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.containsKey(target.getUniqueId()) &&
                    pendingInvites.get(target.getUniqueId()).equals(player.getUniqueId())) {
                pendingInvites.remove(target.getUniqueId());
                if (target.isOnline()) {
                    target.sendMessage("§c來自 " + player.getName() + " 的隊伍邀請已過期");
                }
                if (player.isOnline()) {
                    player.sendMessage("§c發送給 " + target.getName() + " 的隊伍邀請已過期");
                }
            }
        }, 1200L); // 60秒後過期
    }

    private void handleAcceptCommand(Player player) {
        // 檢查玩家是否有待處理的邀請
        if (!pendingInvites.containsKey(player.getUniqueId())) {
            player.sendMessage("§c你沒有待處理的隊伍邀請");
            return;
        }

        // 獲取邀請者
        UUID inviterId = pendingInvites.get(player.getUniqueId());
        Player inviter = Bukkit.getPlayer(inviterId);

        // 清除邀請
        pendingInvites.remove(player.getUniqueId());

        // 檢查邀請者是否在線
        if (inviter == null || !inviter.isOnline()) {
            player.sendMessage("§c邀請者不在線，邀請已取消");
            return;
        }

        // 獲取邀請者的隊伍
        Party party = partyManager.getPlayerParty(inviterId);
        if (party == null) {
            player.sendMessage("§c邀請者不再是隊伍的一部分，邀請已取消");
            return;
        }

        // 檢查隊伍人數是否已滿
        if (party.isFull()) {
            player.sendMessage("§c隊伍已滿，無法加入");
            inviter.sendMessage("§c" + player.getName() + " 無法加入你的隊伍，因為隊伍已滿");
            return;
        }

        // 加入隊伍
        partyManager.addPlayerToParty(party, player);

        // 通知所有隊伍成員
        for (UUID memberId : party.getMemberUUIDs()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage("§a" + player.getName() + " 加入了隊伍！");
            }
        }
    }

    private void handleDeclineCommand(Player player) {
        // 檢查玩家是否有待處理的邀請
        if (!pendingInvites.containsKey(player.getUniqueId())) {
            player.sendMessage("§c你沒有待處理的隊伍邀請");
            return;
        }

        // 獲取邀請者
        UUID inviterId = pendingInvites.get(player.getUniqueId());
        Player inviter = Bukkit.getPlayer(inviterId);

        // 清除邀請
        pendingInvites.remove(player.getUniqueId());

        // 通知
        player.sendMessage("§a你拒絕了隊伍邀請");
        if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage("§c" + player.getName() + " 拒絕了你的隊伍邀請");
        }
    }

    private void handleLeaveCommand(Player player) {
        // 檢查玩家是否在隊伍中
        Party party = partyManager.getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§c你不在任何隊伍中");
            return;
        }

        // 檢查是否是隊長
        boolean isOwner = party.isOwner(player.getUniqueId());
        Set<UUID> members = new HashSet<>(party.getMemberUUIDs());

        // 離開隊伍
        partyManager.removePlayerFromParty(player.getUniqueId());

        if (isOwner && members.size() > 1) {
            // 如果是隊長且隊伍中還有其他成員，通知所有人隊伍已解散
            for (UUID memberId : members) {
                if (!memberId.equals(player.getUniqueId())) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.isOnline()) {
                        member.sendMessage("§c隊長已離開，隊伍已解散");
                    }
                }
            }
            player.sendMessage("§a你離開了隊伍，由於你是隊長，隊伍已解散");
        } else if (isOwner) {
            // 如果是隊長但是隊伍只有自己
            player.sendMessage("§a你的隊伍已解散");
        } else {
            // 如果不是隊長，通知所有隊員
            for (UUID memberId : members) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline() && !member.getUniqueId().equals(player.getUniqueId())) {
                    member.sendMessage("§c" + player.getName() + " 離開了隊伍");
                }
            }
            player.sendMessage("§a你離開了隊伍");
        }
    }

    private void handleKickCommand(Player player, String targetName) {
        // 檢查玩家是否在隊伍中
        Party party = partyManager.getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§c你不在任何隊伍中");
            return;
        }

        // 檢查玩家是否是隊長
        if (!party.isOwner(player.getUniqueId())) {
            player.sendMessage("§c只有隊長才能踢出隊員");
            return;
        }

        // 尋找目標玩家
        Player target = null;
        UUID targetId = null;

        // 先在隊伍成員中查找匹配的玩家名稱
        for (UUID memberId : party.getMemberUUIDs()) {
            String memberName = party.getMembers().get(memberId);
            if (memberName != null && memberName.equalsIgnoreCase(targetName)) {
                targetId = memberId;
                target = Bukkit.getPlayer(memberId);
                break;
            }
        }

        // 如果在隊伍成員中找不到，嘗試在線玩家
        if (targetId == null) {
            target = Bukkit.getPlayer(targetName);
            if (target != null) {
                targetId = target.getUniqueId();
                // 確認該玩家確實在這個隊伍中
                if (!party.getMemberUUIDs().contains(targetId)) {
                    target = null;
                    targetId = null;
                }
            }
        }

        if (targetId == null) {
            player.sendMessage("§c找不到玩家 " + targetName + " 或該玩家不在你的隊伍中");
            return;
        }

        // 不能踢自己
        if (targetId.equals(player.getUniqueId())) {
            player.sendMessage("§c你不能踢出自己，請使用 /party leave 離開隊伍");
            return;
        }

        // 使用 PartyManager 的 kickPlayer 方法
        boolean success = partyManager.kickPlayer(player.getUniqueId(), targetId);

        if (success) {
            // 通知被踢的玩家
            if (target != null && target.isOnline()) {
                target.sendMessage("§c你被踢出了隊伍");
            }

            // 通知所有隊員
            String kickedPlayerName = target != null ? target.getName() : party.getMembers().get(targetId);
            for (UUID memberId : party.getMemberUUIDs()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendMessage("§c" + kickedPlayerName + " 被踢出了隊伍");
                }
            }
        } else {
            player.sendMessage("§c踢出玩家失敗");
        }
    }

    private void handleInfoCommand(Player player) {
        // 檢查玩家是否在隊伍中
        Party party = partyManager.getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§c你不在任何隊伍中");
            return;
        }

        // 獲取隊長信息
        UUID ownerId = party.getOwnerId();
        String ownerName = party.getMembers().get(ownerId);
        if (ownerName == null) {
            Player owner = Bukkit.getPlayer(ownerId);
            ownerName = owner != null ? owner.getName() : "未知";
        }

        // 顯示隊伍基本信息
        player.sendMessage("§6========== §e隊伍信息 §6==========");
        player.sendMessage("§a隊長: §f" + ownerName);
        player.sendMessage("§a成員數量: §f" + party.getSize() + "/" + party.getMaxSize());
        player.sendMessage("§a成員列表:");

        // 首先顯示隊長
        player.sendMessage("  §e[隊長] §f" + ownerName);

        // 然後顯示其他成員
        for (UUID memberId : party.getMemberUUIDs()) {
            if (!memberId.equals(ownerId)) { // 排除隊長，因為已經顯示過了
                String memberName = party.getMembers().get(memberId);
                if (memberName == null) {
                    Player member = Bukkit.getPlayer(memberId);
                    memberName = member != null ? member.getName() : "未知玩家";
                }

                // 檢查玩家是否在線
                Player member = Bukkit.getPlayer(memberId);
                String status = (member != null && member.isOnline()) ? "§a[在線]" : "§7[離線]";
                player.sendMessage("  " + status + " §f" + memberName);
            }
        }
    }

    private void handleChatCommand(Player player, String message) {
        // 檢查玩家是否在隊伍中
        Party party = partyManager.getPlayerParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§c你不在任何隊伍中");
            return;
        }

        // 發送訊息給所有隊員
        String chatFormat = "§b[隊伍聊天] §e%s§f: %s";
        String formattedMessage = String.format(chatFormat, player.getName(), message);

        for (UUID memberId : party.getMemberUUIDs()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
    }

    private void cleanupInvites() {
        // 清理過期的邀請
        pendingInvites.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            UUID inviterId = entry.getValue();
            Player target = Bukkit.getPlayer(targetId);
            Player inviter = Bukkit.getPlayer(inviterId);
            return target == null || !target.isOnline() || inviter == null || !inviter.isOnline();
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            // 第一個參數：提供所有可用的子指令
            List<String> completions = Arrays.asList("create", "invite", "accept", "decline", "leave", "kick", "info", "chat");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "invite":
                    // 提供所有在線玩家的名稱，除了自己和已經在隊伍中的人
                    return Bukkit.getOnlinePlayers().stream()
                            .filter(p -> !p.equals(player)) // 不包括自己
                            .filter(p -> partyManager.getPlayerParty(p.getUniqueId()) == null) // 不包括已在隊伍中的人
                            .filter(p -> !pendingInvites.containsKey(p.getUniqueId())) // 不包括已有待處理邀請的人
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .sorted()
                            .collect(Collectors.toList());

                case "kick":
                    // 提供隊伍中的玩家名稱（除了自己）
                    Party party = partyManager.getPlayerParty(player.getUniqueId());
                    if (party != null && party.isOwner(player.getUniqueId())) {
                        return party.getMemberUUIDs().stream()
                                .filter(uuid -> !uuid.equals(player.getUniqueId())) // 不包括自己
                                .map(uuid -> {
                                    // 優先使用緩存的名稱，如果沒有則嘗試獲取在線玩家名稱
                                    String name = party.getMembers().get(uuid);
                                    if (name == null) {
                                        Player member = Bukkit.getPlayer(uuid);
                                        name = member != null ? member.getName() : null;
                                    }
                                    return name;
                                })
                                .filter(Objects::nonNull) // 過濾掉null值
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .sorted()
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }

        // 對於其他情況或不匹配的參數，返回空列表
        return Collections.emptyList();
    }
}