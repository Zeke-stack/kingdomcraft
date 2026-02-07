package com.kingdomcraft.listeners;

import com.kingdomcraft.KingdomCraft;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {
    private final KingdomCraft plugin;

    public JoinLeaveListener(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getDiscordWebhook() != null && plugin.getDiscordWebhook().isEnabled()) {
            plugin.getDiscordWebhook().sendJoin(event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getDiscordWebhook() != null && plugin.getDiscordWebhook().isEnabled()) {
            plugin.getDiscordWebhook().sendLeave(event.getPlayer().getName());
        }
    }
}
