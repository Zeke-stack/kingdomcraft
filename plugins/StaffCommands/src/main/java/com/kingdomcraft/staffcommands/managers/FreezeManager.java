package com.kingdomcraft.staffcommands.managers;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {
    
    private final Set<UUID> frozenPlayers = new HashSet<>();
    
    public void freezePlayer(Player player) {
        frozenPlayers.add(player.getUniqueId());
    }
    
    public void unfreezePlayer(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }
    
    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
    
    public void toggleFreeze(Player player) {
        if (isFrozen(player)) {
            unfreezePlayer(player);
        } else {
            freezePlayer(player);
        }
    }
}
