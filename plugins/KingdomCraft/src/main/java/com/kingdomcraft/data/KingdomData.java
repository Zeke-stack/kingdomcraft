package com.kingdomcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class KingdomData {
    private final JavaPlugin plugin;
    private final Gson gson;
    private final File dataFile;
    
    private Map<String, Kingdom> kingdoms; // Kingdom name -> Kingdom
    private Map<UUID, PlayerData> playerData; // UUID -> PlayerData
    
    public KingdomData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "kingdoms.json");
        this.kingdoms = new HashMap<>();
        this.playerData = new HashMap<>();
        load();
    }
    
    public void load() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            save();
            return;
        }
        
        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<DataContainer>(){}.getType();
            DataContainer data = gson.fromJson(reader, type);
            if (data != null) {
                this.kingdoms = data.kingdoms != null ? data.kingdoms : new HashMap<>();
                this.playerData = data.playerData != null ? data.playerData : new HashMap<>();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load kingdom data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void save() {
        try (Writer writer = new FileWriter(dataFile)) {
            DataContainer data = new DataContainer();
            data.kingdoms = this.kingdoms;
            data.playerData = this.playerData;
            gson.toJson(data, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save kingdom data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Kingdom methods
    public Kingdom createKingdom(String name, UUID leaderId) {
        Kingdom kingdom = new Kingdom(name, leaderId);
        kingdoms.put(name.toLowerCase(), kingdom);
        save();
        return kingdom;
    }
    
    public void deleteKingdom(String name) {
        Kingdom kingdom = kingdoms.remove(name.toLowerCase());
        if (kingdom != null) {
            // Remove all members from kingdom
            for (UUID memberId : kingdom.getMembers()) {
                PlayerData pd = getPlayerData(memberId);
                if (pd != null) {
                    pd.setKingdom(null);
                }
            }
            save();
        }
    }
    
    public Kingdom getKingdom(String name) {
        return kingdoms.get(name.toLowerCase());
    }
    
    public Kingdom getPlayerKingdom(UUID playerId) {
        PlayerData pd = getPlayerData(playerId);
        if (pd == null || pd.getKingdom() == null) return null;
        return getKingdom(pd.getKingdom());
    }
    
    public Collection<Kingdom> getAllKingdoms() {
        return kingdoms.values();
    }
    
    // Player data methods
    public PlayerData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, k -> new PlayerData(playerId));
    }
    
    public void setPlayerDead(UUID playerId, boolean dead) {
        PlayerData pd = getPlayerData(playerId);
        pd.setDead(dead);
        
        // Remove from kingdom if dead
        if (dead) {
            Kingdom kingdom = getPlayerKingdom(playerId);
            if (kingdom != null) {
                kingdom.removeMember(playerId);
                pd.setKingdom(null);
            }
        }
        save();
    }
    
    public boolean isPlayerDead(UUID playerId) {
        PlayerData pd = playerData.get(playerId);
        return pd != null && pd.isDead();
    }
    
    // Character data
    public void setCharacter(UUID playerId, String name, String personality) {
        PlayerData pd = getPlayerData(playerId);
        pd.setCharacterName(name);
        pd.setCharacterPersonality(personality);
        pd.setDead(false);
        save();
    }
    
    private static class DataContainer {
        Map<String, Kingdom> kingdoms;
        Map<UUID, PlayerData> playerData;
    }
}
