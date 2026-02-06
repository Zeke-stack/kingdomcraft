package com.kingdomcraft.staffcommands.listeners;

import com.kingdomcraft.staffcommands.managers.ChatHistoryManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatHistoryListener implements Listener {
    
    private final ChatHistoryManager chatHistoryManager;
    
    public ChatHistoryListener(ChatHistoryManager chatHistoryManager) {
        this.chatHistoryManager = chatHistoryManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        chatHistoryManager.logMessage(event.getPlayer(), message);
    }
}
