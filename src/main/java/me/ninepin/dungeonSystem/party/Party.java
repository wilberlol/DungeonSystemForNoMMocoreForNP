package me.ninepin.dungeonSystem.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Party {
    private UUID id;
    private UUID ownerId;
    private Map<UUID, String> members; // UUID -> Player Name
    private int maxSize;

    public Party(Player owner) {
        this.id = UUID.randomUUID();
        this.ownerId = owner.getUniqueId();
        this.members = new HashMap<>();
        this.members.put(owner.getUniqueId(), owner.getName());
        this.maxSize = 4; // Default party size
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public boolean isOwner(UUID playerId) {
        return ownerId.equals(playerId);
    }

    public Set<UUID> getMemberUUIDs() {
        return members.keySet();
    }

    public Map<UUID, String> getMembers() {
        return members;
    }

    public int getSize() {
        return members.size();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    // Party management methods
    public boolean addMember(UUID playerId) {
        if (members.size() >= maxSize) {
            return false;
        }

        // Check if player is already in the party
        if (members.containsKey(playerId)) {
            return false;
        }

        // Try to get player name
        String playerName = "Unknown";
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            playerName = player.getName();
        }

        members.put(playerId, playerName);
        return true;
    }

    public boolean removeMember(UUID playerId) {
        // Cannot remove the owner
        if (isOwner(playerId)) {
            return false;
        }

        return members.remove(playerId) != null;
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
    @Override
    public String toString() {
        return "Party{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", members=" + members.size() +
                ", maxSize=" + maxSize +
                '}';
    }
}