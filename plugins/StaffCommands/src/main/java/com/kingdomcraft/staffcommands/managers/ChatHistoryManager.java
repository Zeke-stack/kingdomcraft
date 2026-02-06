package com.kingdomcraft.staffcommands.managers;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ChatHistoryManager {
    
    private final JavaPlugin plugin;
    private final Map<UUID, LinkedList<ChatMessage>> chatHistory = new HashMap<>();
    private static final int MAX_HISTORY = 100;
    
    public ChatHistoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void logMessage(Player player, String message) {
        UUID uuid = player.getUniqueId();
        chatHistory.putIfAbsent(uuid, new LinkedList<>());
        
        LinkedList<ChatMessage> history = chatHistory.get(uuid);
        history.add(new ChatMessage(message, System.currentTimeMillis()));
        
        // Keep only last MAX_HISTORY messages
        if (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }
    
    public List<ChatMessage> getHistory(UUID uuid, int lines) {
        LinkedList<ChatMessage> history = chatHistory.get(uuid);
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        
        int start = Math.max(0, history.size() - lines);
        return new ArrayList<>(history.subList(start, history.size()));
    }
    
    public static class ChatMessage {
        private final String message;
        private final long timestamp;
        
        public ChatMessage(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
