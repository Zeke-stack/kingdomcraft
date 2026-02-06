package com.kingdomcraft;

import com.kingdomcraft.commands.*;
import com.kingdomcraft.data.KingdomData;
import com.kingdomcraft.listeners.DeathListener;
import org.bukkit.plugin.java.JavaPlugin;

public class KingdomCraft extends JavaPlugin {
    private KingdomData kingdomData;
    
    @Override
    public void onEnable() {
        // Initialize data
        kingdomData = new KingdomData(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        
        // Register commands
        registerCommands();
        
        getLogger().info("KingdomCraft has been enabled!");
        getLogger().info("Death system and kingdom management active.");
    }
    
    @Override
    public void onDisable() {
        if (kingdomData != null) {
            kingdomData.save();
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
}
