package com.kingdomcraft.staffcommands.listeners;

import com.kingdomcraft.staffcommands.managers.FreezeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class FreezeListener implements Listener {
    
    private final FreezeManager freezeManager;
    
    public FreezeListener(FreezeManager freezeManager) {
        this.freezeManager = freezeManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (freezeManager.isFrozen(player)) {
            // Only cancel if player actually moved position
            if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getY() != event.getTo().getY() ||
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("❄ You are frozen! ❄", NamedTextColor.AQUA, TextDecoration.BOLD));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (freezeManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("❄ You are frozen! ❄", NamedTextColor.AQUA, TextDecoration.BOLD));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (freezeManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("❄ You are frozen! ❄", NamedTextColor.AQUA, TextDecoration.BOLD));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (freezeManager.isFrozen(player)) {
            String command = event.getMessage().toLowerCase();
            // Allow only helpop and similar commands
            if (!command.startsWith("/helpop") && !command.startsWith("/help")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❄ You cannot use commands while frozen!", NamedTextColor.RED));
            }
        }
    }
}
