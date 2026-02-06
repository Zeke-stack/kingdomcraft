package com.kingdomcraft.staffcommands;

import com.kingdomcraft.staffcommands.commands.*;
import com.kingdomcraft.staffcommands.listeners.ChatHistoryListener;
import com.kingdomcraft.staffcommands.listeners.FreezeListener;
import com.kingdomcraft.staffcommands.managers.ChatHistoryManager;
import com.kingdomcraft.staffcommands.managers.FreezeManager;
import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class StaffCommands extends JavaPlugin {
    
    private FreezeManager freezeManager;
    private ChatHistoryManager chatHistoryManager;
    private StaffAuditManager auditManager;
    
    @Override
    public void onEnable() {
        // Create data folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Initialize managers
        freezeManager = new FreezeManager();
        chatHistoryManager = new ChatHistoryManager(this);
        auditManager = new StaffAuditManager(this);
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new FreezeListener(freezeManager), this);
        getServer().getPluginManager().registerEvents(new ChatHistoryListener(chatHistoryManager), this);
        
        getLogger().info("StaffCommands has been enabled!");
        getLogger().info("All staff management commands are now active!");
    }
    
    @Override
    public void onDisable() {
        // Save audit logs
        if (auditManager != null) {
            auditManager.saveToFile();
        }
        
        getLogger().info("StaffCommands has been disabled!");
    }
    
    private void registerCommands() {
        // Teleportation commands
        getCommand("spectate").setExecutor(new SpectateCommand(auditManager));
        getCommand("survival").setExecutor(new SurvivalCommand(auditManager));
        getCommand("to").setExecutor(new ToCommand(auditManager));
        getCommand("bring").setExecutor(new BringCommand(auditManager));
        
        // Moderation commands
        getCommand("freeze").setExecutor(new FreezeCommand(freezeManager, auditManager));
        getCommand("history").setExecutor(new HistoryCommand(chatHistoryManager, auditManager));
        
        // Announcement commands
        getCommand("hint").setExecutor(new HintCommand(auditManager));
        getCommand("announce").setExecutor(new AnnounceCommand(auditManager));
        getCommand("chatannounce").setExecutor(new ChatAnnounceCommand(auditManager));
        
        // Audit command
        getCommand("audit").setExecutor(new AuditCommand(auditManager));
    }
    
    public FreezeManager getFreezeManager() {
        return freezeManager;
    }
    
    public ChatHistoryManager getChatHistoryManager() {
        return chatHistoryManager;
    }
    
    public StaffAuditManager getAuditManager() {
        return auditManager;
    }
}
