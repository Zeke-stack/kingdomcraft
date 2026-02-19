package com.kingdomcraft;

import com.kingdomcraft.commands.*;
import com.kingdomcraft.data.CharacterData;
import com.kingdomcraft.data.PlaceData;
import com.kingdomcraft.discord.BridgeManager;
import com.kingdomcraft.discord.DiscordWebhook;
import com.kingdomcraft.listeners.ChatSyncListener;
import com.kingdomcraft.listeners.DeathListener;
import com.kingdomcraft.listeners.DetailsListener;
import com.kingdomcraft.listeners.JoinLeaveListener;
import com.kingdomcraft.listeners.TabListListener;
import com.kingdomcraft.npc.NPCListener;
import com.kingdomcraft.npc.NPCManager;
import com.kingdomcraft.recipes.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class KingdomCraft extends JavaPlugin {
    private CharacterData characterData;
    private PlaceData placeData;
    private DiscordWebhook discordWebhook;
    private BridgeManager bridgeManager;
    private NPCManager npcManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize character data
        characterData = new CharacterData(this);

        // Initialize place data
        placeData = new PlaceData(this);

        // Initialize Discord webhook
        discordWebhook = new DiscordWebhook(this);

        // Initialize HTTP bridge to Discord bot (replaces RCON)
        bridgeManager = new BridgeManager(this);

        // Initialize NPC manager
        npcManager = new NPCManager(this);

        // Register custom recipes
        recipeManager = new RecipeManager(this);
        recipeManager.registerRecipes();

        // Register listeners
        DeathListener deathListener = new DeathListener(this);
        getServer().getPluginManager().registerEvents(deathListener, this);
        getServer().getPluginManager().registerEvents(new ChatSyncListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this, npcManager), this);

        // Tab list
        TabListListener tabListListener = new TabListListener(this);
        getServer().getPluginManager().registerEvents(tabListListener, this);
        getServer().getScheduler().runTaskTimer(this, tabListListener::updateAll, 100L, 1200L);

        // Details — small immersive features
        DetailsListener detailsListener = new DetailsListener(this);
        getServer().getPluginManager().registerEvents(detailsListener, this);

        // Campfire healing tick (every 3 seconds = 60 ticks)
        getServer().getScheduler().runTaskTimer(this, detailsListener::tickCampfireHealing, 100L, 60L);

        // Lock dead players to sky position every 5 ticks
        getServer().getScheduler().runTaskTimer(this, deathListener::lockDeadPlayers, 20L, 5L);

        // Register commands
        registerCommands();

        getLogger().info("KingdomCraft has been enabled!");
        getLogger().info("Year 1835 — Character system and death mechanics active.");

        // Respawn NPCs that may have despawned
        getServer().getScheduler().runTaskLater(this, () -> {
            npcManager.respawnAll();
            getLogger().info("Travel NPCs checked/respawned.");
        }, 40L);

        // Notify Discord that server is online
        if (discordWebhook.isEnabled()) {
            discordWebhook.sendServerStatus("start");
        }

        // Start bridge polling (after a short delay to let server finish loading)
        if (bridgeManager.isEnabled()) {
            getServer().getScheduler().runTaskLater(this, () -> bridgeManager.start(), 100L);
        }
    }

    @Override
    public void onDisable() {
        // Notify Discord that server is stopping
        if (discordWebhook != null && discordWebhook.isEnabled()) {
            discordWebhook.sendServerStatus("stop");
        }

        if (characterData != null) {
            characterData.save();
        }

        if (placeData != null) {
            placeData.save();
        }

        if (discordWebhook != null) {
            discordWebhook.shutdown();
        }

        if (bridgeManager != null) {
            bridgeManager.stop();
        }

        getLogger().info("KingdomCraft has been disabled!");
    }

    private void registerCommands() {
        // Staff commands
        getCommand("revive").setExecutor(new ReviveCommand(this));

        // Character commands
        getCommand("createcharacter").setExecutor(new CreateCharacterCommand(this));
        getCommand("joinplace").setExecutor(new JoinPlaceCommand(this));

        // Place management commands
        PlaceCommands placeCommands = new PlaceCommands(this);
        getCommand("addplace").setExecutor(placeCommands);
        getCommand("listplace").setExecutor(placeCommands);
        getCommand("removeplace").setExecutor(placeCommands);
        getCommand("addteleport").setExecutor(placeCommands);
        getCommand("removeteleport").setExecutor(placeCommands);

        // NPC commands
        NPCCommands npcCommands = new NPCCommands(this, npcManager);
        getCommand("createnpc").setExecutor(npcCommands);
        getCommand("deletenpc").setExecutor(npcCommands);
        getCommand("listnpcs").setExecutor(npcCommands);
        getCommand("movenpc").setExecutor(npcCommands);
        getCommand("setnpcdest").setExecutor(npcCommands);
    }

    public CharacterData getCharacterData() {
        return characterData;
    }

    public PlaceData getPlaceData() {
        return placeData;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }
}
