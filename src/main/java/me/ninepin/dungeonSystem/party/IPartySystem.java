package me.ninepin.dungeonSystem.party;

import java.util.Set;
import java.util.UUID;

public interface IPartySystem {
    Set<UUID> getPartyMembers(UUID playerId);
    boolean isInSameParty(UUID player1, UUID player2);
    boolean isPartyLeader(UUID playerId);
    boolean hasParty(UUID playerId); // 新增：檢查玩家是否有隊伍
}
