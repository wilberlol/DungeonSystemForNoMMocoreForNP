package me.ninepin.dungeonSystem.party;

import me.ninepin.dungeonSystem.DungeonSystem;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager implements IPartySystem {
    private final DungeonSystem plugin;
    private final Map<UUID, Party> parties; // Party ID -> Party
    private final Map<UUID, UUID> playerParties; // Player UUID -> Party ID
    private final Map<UUID, UUID> pendingInvites; // Target UUID -> Inviter UUID
    @Override
    public boolean isInSameParty(UUID player1, UUID player2) {
        Party party1 = getPlayerParty(player1);
        Party party2 = getPlayerParty(player2);
        return party1 != null && party2 != null && party1.getId().equals(party2.getId());
    }
    @Override
    public boolean hasParty(UUID playerId) {
        return getPlayerParty(playerId) != null;
    }
    @Override
    public boolean isPartyLeader(UUID playerId) {
        Party party = getPlayerParty(playerId);
        return party != null && party.isOwner(playerId);
    }
    public PartyManager(DungeonSystem plugin) {
        this.plugin = plugin;
        this.parties = new HashMap<>();
        this.playerParties = new HashMap<>();
        this.pendingInvites = new HashMap<>();
    }

    public Party createParty(Player owner) {
        // Check if player is already in party
        if (getPlayerParty(owner.getUniqueId()) != null) {
            return null;
        }

        Party party = new Party(owner);
        parties.put(party.getId(), party);
        playerParties.put(owner.getUniqueId(), party.getId());
        return party;
    }

    public boolean disbandParty(UUID partyId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Remove all members from playerParties map
        for (UUID memberId : party.getMemberUUIDs()) {
            playerParties.remove(memberId);
        }

        // Remove party
        parties.remove(partyId);
        return true;
    }

    public Party getParty(UUID partyId) {
        return parties.get(partyId);
    }

    public Party getPlayerParty(UUID playerId) {
        UUID partyId = playerParties.get(playerId);
        if (partyId == null) {
            return null;
        }
        return parties.get(partyId);
    }

    /**
     * 發送邀請給玩家
     *
     * @param inviterId 邀請者UUID
     * @param targetId  目標玩家UUID
     * @return 是否成功發送邀請
     */
    public boolean sendInvite(UUID inviterId, UUID targetId) {
        // 檢查邀請者是否在隊伍中
        Party party = getPlayerParty(inviterId);
        if (party == null) {
            return false;
        }

        // 檢查邀請者是否是隊長
        if (!party.isOwner(inviterId)) {
            return false;
        }

        // 檢查目標玩家是否已經在隊伍中
        if (getPlayerParty(targetId) != null) {
            return false;
        }

        // 檢查是否已經有待處理的邀請
        if (pendingInvites.containsKey(targetId)) {
            return false;
        }

        // 添加邀請
        pendingInvites.put(targetId, inviterId);
        return true;
    }

    /**
     * 接受邀請
     *
     * @param playerId 接受邀請的玩家UUID
     * @return 是否成功接受邀請
     */
    public boolean acceptInvite(UUID playerId) {
        // 檢查是否有待處理的邀請
        UUID inviterId = pendingInvites.get(playerId);
        if (inviterId == null) {
            return false;
        }

        // 檢查邀請者的隊伍
        Party party = getPlayerParty(inviterId);
        if (party == null) {
            pendingInvites.remove(playerId);
            return false;
        }

        // 檢查玩家是否已經在其他隊伍中
        if (getPlayerParty(playerId) != null) {
            pendingInvites.remove(playerId);
            return false;
        }

        // 移除邀請
        pendingInvites.remove(playerId);

        // 添加玩家到隊伍
        if (addPlayerToParty(party, playerId)) {
            return true;
        }

        return false;
    }

    /**
     * 拒絕邀請
     *
     * @param playerId 拒絕邀請的玩家UUID
     * @return 是否成功拒絕邀請
     */
    public boolean declineInvite(UUID playerId) {
        if (!pendingInvites.containsKey(playerId)) {
            return false;
        }

        pendingInvites.remove(playerId);
        return true;
    }

    /**
     * 從隊伍中移除玩家
     *
     * @param playerId 要移除的玩家UUID
     * @return 是否成功移除
     */
    public boolean removePlayerFromParty(UUID playerId) {
        // 獲取玩家所在的隊伍
        Party party = getPlayerParty(playerId);
        if (party == null) {
            return false; // 玩家不在任何隊伍中
        }

        // 檢查是否是隊長
        if (party.isOwner(playerId)) {
            // 如果是隊長，解散整個隊伍
            return disbandParty(party.getId());
        } else {
            // 如果不是隊長，只移除該玩家
            playerParties.remove(playerId);
            return party.removeMember(playerId);
        }
    }

    /**
     * 獲取待處理的邀請
     *
     * @param playerId 玩家UUID
     * @return 邀請者UUID，如果沒有邀請則返回null
     */
    public UUID getPendingInvite(UUID playerId) {
        return pendingInvites.get(playerId);
    }

    /**
     * 添加玩家到隊伍
     *
     * @param party    隊伍
     * @param playerId 玩家UUID
     * @return 是否成功添加
     */
    public boolean addPlayerToParty(Party party, UUID playerId) {
        // 檢查玩家是否已經在隊伍中
        if (getPlayerParty(playerId) != null) {
            return false;
        }

        // 檢查隊伍人數
        if (party.getSize() >= 5) { // 假設最大隊伍人數為5
            return false;
        }

        // 添加到隊伍
        if (party.addMember(playerId)) {
            playerParties.put(playerId, party.getId());
            return true;
        }
        return false;
    }

    /**
     * 將玩家添加到隊伍（包裝方法，適配Player對象）
     *
     * @param party  隊伍
     * @param player 玩家
     * @return 是否成功添加
     */
    public boolean addPlayerToParty(Party party, Player player) {
        return addPlayerToParty(party, player.getUniqueId());
    }

    /**
     * 踢出隊員
     *
     * @param kickerId 踢人者UUID（應為隊長）
     * @param targetId 目標玩家UUID
     * @return 是否成功踢出
     */
    public boolean kickPlayer(UUID kickerId, UUID targetId) {
        // 檢查踢人者的隊伍
        Party party = getPlayerParty(kickerId);
        if (party == null) {
            return false;
        }

        // 檢查踢人者是否是隊長
        if (!party.isOwner(kickerId)) {
            return false;
        }

        // 檢查目標玩家是否在隊伍中
        if (getPlayerParty(targetId) != party) {
            return false;
        }

        // 不能踢自己
        if (targetId.equals(kickerId)) {
            return false;
        }

        // 踢出玩家
        playerParties.remove(targetId);
        return party.removeMember(targetId);
    }

    public boolean leaveParty(UUID playerId) {
        Party party = getPlayerParty(playerId);
        if (party == null) {
            return false;
        }

        if (party.isOwner(playerId)) {
            // If owner leaves, disband the party or transfer ownership
            // This example disbands the party
            return disbandParty(party.getId());
        } else {
            playerParties.remove(playerId);
            return party.removeMember(playerId);
        }
    }

    @Override
    public Set<UUID> getPartyMembers(UUID playerId) {
        Party party = getPlayerParty(playerId);
        if (party == null) {
            Set<UUID> singlePlayer = new HashSet<>();
            singlePlayer.add(playerId);
            return singlePlayer;
        }
        return party.getMemberUUIDs();
    }

    /**
     * 清理所有過期的邀請
     */
    public void cleanupInvites() {
        pendingInvites.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            UUID inviterId = entry.getValue();

            // 如果任何一方離線或不在隊伍中，取消邀請
            if (getPlayerParty(inviterId) == null) {
                return true;
            }

            return false;
        });
    }
}