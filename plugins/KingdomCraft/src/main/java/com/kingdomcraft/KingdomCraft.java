package com.kingdomcraft;

import com.kingdomcraft.commands.*;
import com.kingdomcraft.data.KingdomData;
import com.kingdomcraft.discord.DiscordWebhook;
import com.kingdomcraft.listeners.ChatSyncListener;
import com.kingdomcraft.listeners.DeathListener;
import com.kingdomcraft.listeners.DetailsListener;
import com.kingdomcraft.listeners.JoinLeaveListener;
import com.kingdomcraft.npc.NPCListener;
import com.kingdomcraft.npc.NPCManager;
import com.kingdomcraft.recipes.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class KingdomCraft extends JavaPlugin {
    private KingdomData kingdomData;
    private DiscordWebhook discordWebhook;
    private NPCManager npcManager;
    private RecipeManager recipeManager;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize data
        kingdomData = new KingdomData(this);
        
        // Initialize Discord webhook
        discordWebhook = new DiscordWebhook(this);
        
        // Initialize NPC manager
        npcManager = new NPCManager(this);
        
        // Register custom recipes
        recipeManager = new RecipeManager(this);
        recipeManager.registerRecipes();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatSyncListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this, npcManager), this);
        
        // Details — small immersive features
        DetailsListener detailsListener = new DetailsListener(this);
        getServer().getPluginManager().registerEvents(detailsListener, this);
        
        // Campfire healing tick (every 3 seconds = 60 ticks)
        getServer().getScheduler().runTaskTimer(this, detailsListener::tickCampfireHealing, 100L, 60L);
        
        // Register commands
        registerCommands();
        
        getLogger().info("KingdomCraft has been enabled!");
        getLogger().info("Death system and kingdom management active.");
        
        // Respawn NPCs that may have despawned
        getServer().getScheduler().runTaskLater(this, () -> {
            npcManager.respawnAll();
            getLogger().info("Travel NPCs checked/respawned.");
        }, 40L); // 2 seconds after enable
        
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
        
        // NPC commands
        NPCCommands npcCommands = new NPCCommands(this, npcManager);
        getCommand("createnpc").setExecutor(npcCommands);
        getCommand("deletenpc").setExecutor(npcCommands);
        getCommand("listnpcs").setExecutor(npcCommands);
        getCommand("movenpc").setExecutor(npcCommands);
        getCommand("setnpcdest").setExecutor(npcCommands);
    }
    
    public KingdomData getKingdomData() {
        return kingdomData;
    }
    
    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
    
    public NPCManager getNpcManager() {
        return npcManager;
    }
}
