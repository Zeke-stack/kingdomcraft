package com.kingdomcraft.invviewer;

import org.bukkit.scheduler.BukkitRunnable;

public class InventoryUpdateTask extends BukkitRunnable {
    
    private final AdvancedInvViewer plugin;
    
    public InventoryUpdateTask(AdvancedInvViewer plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        // Update all active sessions
        plugin.getActiveSessions().values().forEach(InventorySession::updateInventory);
    }
}
