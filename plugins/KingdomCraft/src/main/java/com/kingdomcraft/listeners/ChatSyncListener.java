package com.kingdomcraft.listeners;

import com.kingdomcraft.KingdomCraft;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatSyncListener implements Listener {
    private final KingdomCraft plugin;

    public ChatSyncListener(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (plugin.getDiscordWebhook() == null || !plugin.getDiscordWebhook().isEnabled()) return;

        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getDiscordWebhook().sendChat(playerName, message);
    }
}
