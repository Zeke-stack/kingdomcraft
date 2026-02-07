package com.kingdomcraft;

import com.kingdomcraft.commands.*;
import com.kingdomcraft.data.KingdomData;
import com.kingdomcraft.discord.DiscordWebhook;
import com.kingdomcraft.listeners.ChatSyncListener;
import com.kingdomcraft.listeners.DeathListener;
import com.kingdomcraft.listeners.JoinLeaveListener;
import org.bukkit.plugin.java.JavaPlugin;

public class KingdomCraft extends JavaPlugin {
    private KingdomData kingdomData;
    private DiscordWebhook discordWebhook;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize data
        kingdomData = new KingdomData(this);
        
        // Initialize Discord webhook
        discordWebhook = new DiscordWebhook(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatSyncListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        
        // Register commands
        registerCommands();
        
        getLogger().info("KingdomCraft has been enabled!");
        getLogger().info("Death system and kingdom management active.");
        
        // Notify Discord that server is online
        if (discordWebhook.isEnabled()) {
            discordWebhook.sendServerStatus("start");
        }
    }
    
    @Override
    public void onDisable() {
        // Notify Discord that server is stopping
        if (discordWebhook != null && discordWebhook.isEnabled()) {
            discordWebhook.sendServerStatus("stop");
        }
        
        if (kingdomData != null) {
            kingdomData.save();
        }
        
        if (discordWebhook != null) {
            discordWebhook.shutdown();
        }
        
        getLogger().info("KingdomCraft has been disabled!");
    }
    
    private void registerCommands() {
        // Staff commands
        getCommand("revive").setExecutor(new ReviveCommand(this));
        getCommand("createkingdom").setExecutor(new CreateKingdomCommand(this));
        getCommand("deletekingdom").setExecutor(new DeleteKingdomCommand(this));
        getCommand("transferkingdom").setExecutor(new TransferKingdomCommand(this));
        
        // Character command
        getCommand("createcharacter").setExecutor(new CreateCharacterCommand(this));
        
        // Leader commands
        KingdomLeaderCommands leaderCommands = new KingdomLeaderCommands(this);
        getCommand("renamekingdom").setExecutor(leaderCommands);
        getCommand("reqlistkingdom").setExecutor(leaderCommands);
        getCommand("reqacceptkingdom").setExecutor(leaderCommands);
        getCommand("reqdenykingdom").setExecutor(leaderCommands);
        getCommand("reqacceptallkingdom").setExecutor(leaderCommands);
        getCommand("reqdenyallkingdom").setExecutor(leaderCommands);
        getCommand("kickkingdom").setExecutor(leaderCommands);
        getCommand("reqoffkingdom").setExecutor(leaderCommands);
        getCommand("reqonkingdom").setExecutor(leaderCommands);
        getCommand("kingdomlist").setExecutor(leaderCommands);
        
        // Player commands
        KingdomPlayerCommands playerCommands = new KingdomPlayerCommands(this);
        getCommand("leavekingdom").setExecutor(playerCommands);
        getCommand("joinkingdom").setExecutor(playerCommands);
    }
    
    public KingdomData getKingdomData() {
        return kingdomData;
    }
    
    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
}
