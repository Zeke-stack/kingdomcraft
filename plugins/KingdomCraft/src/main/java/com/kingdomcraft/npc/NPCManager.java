package com.kingdomcraft.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Manages Travel NPCs — villagers that teleport players on right-click.
 */
public class NPCManager {
    private final JavaPlugin plugin;
    private final Gson gson;
    private final File dataFile;
    private Map<String, TravelNPC> npcs; // id -> NPC

    public NPCManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "npcs.json");
        this.npcs = new HashMap<>();
        load();
    }

    // ── Persistence ──

    public void load() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            save();
            return;
        }
        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, TravelNPC>>(){}.getType();
            Map<String, TravelNPC> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                this.npcs = loaded;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load NPC data: " + e.getMessage());
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(dataFile)) {
            gson.toJson(npcs, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save NPC data: " + e.getMessage());
        }
    }

    // ── NPC CRUD ──

    /**
     * Creates a new Travel NPC villager at the given location.
     * @param id unique string ID (e.g. "north_gate")
     * @param displayName shown above the villager's head
     * @param spawnLoc where the NPC stands
     * @param destination where players get teleported
     * @return the created TravelNPC, or null if ID already exists
     */
    public TravelNPC createNPC(String id, String displayName, Location spawnLoc, Location destination) {
        if (npcs.containsKey(id.toLowerCase())) return null;

        // Spawn villager
        Villager villager = (Villager) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);
        villager.customName(net.kyori.adventure.text.Component.text(displayName)
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setProfession(Villager.Profession.NITWIT);

        TravelNPC npc = new TravelNPC(id.toLowerCase(), displayName, villager.getUniqueId(), spawnLoc, destination);
        npcs.put(id.toLowerCase(), npc);
        save();
        return npc;
    }

    /**
     * Deletes a Travel NPC and removes the villager entity.
     */
    public boolean deleteNPC(String id) {
        TravelNPC npc = npcs.remove(id.toLowerCase());
        if (npc == null) return false;

        // Remove the entity from the world
        removeEntity(npc.getEntityUUID());
        save();
        return true;
    }

    public TravelNPC getNPC(String id) {
        return npcs.get(id.toLowerCase());
    }

    public Collection<TravelNPC> getAllNPCs() {
        return npcs.values();
    }

    /**
     * Find which TravelNPC a given entity UUID belongs to.
     */
    public TravelNPC getNPCByEntity(UUID entityUUID) {
        for (TravelNPC npc : npcs.values()) {
            if (npc.getEntityUUID().equals(entityUUID)) {
                return npc;
            }
        }
        return null;
    }

    /**
     * Respawn all NPC villagers (call on server startup to restore despawned ones).
     */
    public void respawnAll() {
        for (TravelNPC npc : npcs.values()) {
            Location loc = npc.getNpcLocation();
            if (loc == null || loc.getWorld() == null) continue;

            // Check if entity still exists
            Entity existing = Bukkit.getEntity(npc.getEntityUUID());
            if (existing != null && !existing.isDead()) continue;

            // Respawn
            Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
            villager.customName(net.kyori.adventure.text.Component.text(npc.getDisplayName())
                    .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setProfession(Villager.Profession.NITWIT);

            npc.setEntityUUID(villager.getUniqueId());
            plugin.getLogger().info("Respawned NPC: " + npc.getId());
        }
        save();
    }

    private void removeEntity(UUID uuid) {
        Entity entity = Bukkit.getEntity(uuid);
        if (entity != null) {
            entity.remove();
        }
    }
}
