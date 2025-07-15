package me.ninepin.dungeonSystem.party;

import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.provided.Party;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MMOCorePartyAdapter implements IPartySystem {

    @Override
    public Set<UUID> getPartyMembers(UUID playerId) {
        Set<UUID> members = new HashSet<>();
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            members.add(playerId);
            return members;
        }

        PlayerData playerData = PlayerData.get(player);
        if (playerData.getParty() != null) {
            Party party = (Party) playerData.getParty();
            for (PlayerData member : party.getMembers()) {
                members.add(member.getUniqueId());
            }
        } else {
            members.add(playerId);
        }

        return members;
    }

    @Override
    public boolean isInSameParty(UUID player1, UUID player2) {
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);

        if (p1 == null || p2 == null) return false;

        PlayerData data1 = PlayerData.get(p1);
        PlayerData data2 = PlayerData.get(p2);

        Party party1 = (Party) data1.getParty();
        Party party2 = (Party) data2.getParty();

        return party1 != null && party2 != null && party1.equals(party2);
    }
    @Override
    public boolean hasParty(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;

        PlayerData playerData = PlayerData.get(player);
        return playerData.getParty() != null;
    }
    @Override
    public boolean isPartyLeader(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;

        PlayerData playerData = PlayerData.get(player);
        Party party = (Party) playerData.getParty();

        return party != null && party.getOwner().equals(playerData);
    }
}
