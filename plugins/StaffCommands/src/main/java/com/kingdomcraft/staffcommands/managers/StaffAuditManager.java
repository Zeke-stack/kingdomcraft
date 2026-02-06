package com.kingdomcraft.staffcommands.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class StaffAuditManager {
    
    private final JavaPlugin plugin;
    private final List<AuditEntry> auditLog = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File auditFile;
    
    public StaffAuditManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.auditFile = new File(plugin.getDataFolder(), "audit-log.json");
        loadFromFile();
    }
    
    public void logAction(Player staff, String action, String target, String details) {
        AuditEntry entry = new AuditEntry(
            staff.getName(),
            staff.getUniqueId().toString(),
            action,
            target,
            details,
            System.currentTimeMillis()
        );
        auditLog.add(entry);
        
        // Auto-save every 10 entries
        if (auditLog.size() % 10 == 0) {
            saveToFile();
        }
    }
    
    public List<AuditEntry> getAuditLog() {
        return new ArrayList<>(auditLog);
    }
    
    public List<AuditEntry> getAuditLogForPlayer(String playerName) {
        List<AuditEntry> filtered = new ArrayList<>();
        for (AuditEntry entry : auditLog) {
            if (entry.getStaffName().equalsIgnoreCase(playerName) || 
                entry.getTarget().equalsIgnoreCase(playerName)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
    
    public void saveToFile() {
        try (Writer writer = new FileWriter(auditFile)) {
            gson.toJson(auditLog, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save audit log: " + e.getMessage());
        }
    }
    
    private void loadFromFile() {
        if (!auditFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(auditFile)) {
            Type listType = new TypeToken<ArrayList<AuditEntry>>(){}.getType();
            List<AuditEntry> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                auditLog.addAll(loaded);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load audit log: " + e.getMessage());
        }
    }
    
    public static class AuditEntry {
        private final String staffName;
        private final String staffUuid;
        private final String action;
        private final String target;
        private final String details;
        private final long timestamp;
        
        public AuditEntry(String staffName, String staffUuid, String action, 
                         String target, String details, long timestamp) {
            this.staffName = staffName;
            this.staffUuid = staffUuid;
            this.action = action;
            this.target = target;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        public String getStaffName() {
            return staffName;
        }
        
        public String getAction() {
            return action;
        }
        
        public String getTarget() {
            return target;
        }
        
        public String getDetails() {
            return details;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getFormattedTimestamp() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date(timestamp));
        }
    }
}
