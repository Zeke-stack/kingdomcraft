package com.kingdomcraft.invviewer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdvancedInvViewer extends JavaPlugin {
    
    private final Map<UUID, InventorySession> activeSessions = new HashMap<>();
    private InventoryUpdateTask updateTask;
    
    @Override
    public void onEnable() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        
        // Start update task for real-time syncing
        updateTask = new InventoryUpdateTask(this);
        updateTask.runTaskTimer(this, 0L, 2L); // Update every 2 ticks (10 times per second)
        
        getLogger().info("AdvancedInvViewer has been enabled!");
        getLogger().info("Real-time inventory viewing is now active!");
    }
    
    @Override
    public void onDisable() {
        // Close all active sessions
        activeSessions.values().forEach(InventorySession::close);
        activeSessions.clear();
        
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        getLogger().info("AdvancedInvViewer has been disabled!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player viewer = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("invsee")) {
            if (args.length == 0) {
                viewer.sendMessage("§cUsage: /invsee <player>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                viewer.sendMessage("§cPlayer not found!");
                return true;
            }
            
            if (target.equals(viewer)) {
                viewer.sendMessage("§cYou cannot view your own inventory!");
                return true;
            }
            
            // Check permissions
            if (!viewer.hasPermission("invviewer.use")) {
                viewer.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            
            // Close existing session if any
            closeSession(viewer);
            
            // Create new session
            InventorySession session = new InventorySession(viewer, target, this);
            activeSessions.put(viewer.getUniqueId(), session);
            session.open();
            
            viewer.sendMessage("§aNow viewing §e" + target.getName() + "§a's inventory in real-time!");
            
            return true;
        }
        
        return false;
    }
    
    public void closeSession(Player viewer) {
        InventorySession session = activeSessions.remove(viewer.getUniqueId());
        if (session != null) {
            session.close();
        }
    }
    
    public Map<UUID, InventorySession> getActiveSessions() {
        return activeSessions;
    }
}
