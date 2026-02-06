package com.kingdomcraft.invviewer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {
    
    private final AdvancedInvViewer plugin;
    
    public InventoryListener(AdvancedInvViewer plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player viewer = (Player) event.getWhoClicked();
        InventorySession session = plugin.getActiveSessions().get(viewer.getUniqueId());
        
        if (session == null) return;
        if (!event.getInventory().equals(session.getViewInventory())) return;
        
        event.setCancelled(true); // Cancel by default
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        // Handle close button (slot 53)
        if (slot == 53) {
            viewer.closeInventory();
            viewer.sendMessage(Component.text("Closed inventory viewer.", NamedTextColor.GRAY));
            return;
        }
        
        // Handle ender chest button (slot 45)
        if (slot == 45 && viewer.hasPermission("invviewer.enderchest")) {
            viewer.openInventory(session.getTarget().getEnderChest());
            viewer.sendMessage(Component.text("Viewing ", NamedTextColor.GRAY)
                .append(Component.text(session.getTarget().getName(), NamedTextColor.GOLD))
                .append(Component.text("'s ender chest", NamedTextColor.GRAY)));
            plugin.closeSession(viewer);
            return;
        }
        
        // Allow item modification if viewer has permission and clicking main inventory slots
        if (viewer.hasPermission("invviewer.modify") && slot < 36) {
            event.setCancelled(false); // Allow the click
            
            // Map to actual inventory slot
            int targetSlot;
            if (slot < 27) {
                // Main inventory
                targetSlot = slot + 9;
            } else {
                // Hotbar
                targetSlot = slot - 27;
            }
            
            // Sync the change to target's inventory
            ItemStack newItem = event.getCursor();
            session.getTarget().getInventory().setItem(targetSlot, newItem);
            
            viewer.sendMessage(Component.text("Modified item in slot " + slot, NamedTextColor.YELLOW));
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player viewer = (Player) event.getPlayer();
        InventorySession session = plugin.getActiveSessions().get(viewer.getUniqueId());
        
        if (session != null && event.getInventory().equals(session.getViewInventory())) {
            plugin.closeSession(viewer);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Close session if player is a viewer
        plugin.closeSession(player);
        
        // Close sessions viewing this player
        plugin.getActiveSessions().values().stream()
            .filter(session -> session.getTarget().equals(player))
            .forEach(session -> {
                session.getViewer().closeInventory();
                session.getViewer().sendMessage(Component.text(player.getName() + " has logged out.", NamedTextColor.RED));
            });
    }
}
