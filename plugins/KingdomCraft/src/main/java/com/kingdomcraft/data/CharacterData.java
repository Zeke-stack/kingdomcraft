package com.kingdomcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class CharacterData {
    private final JavaPlugin plugin;
    private final Gson gson;
    private final File dataFile;

    private Map<UUID, PlayerData> playerData;

    public CharacterData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "characters.json");
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
            Type type = new TypeToken<Map<UUID, PlayerData>>(){}.getType();
            Map<UUID, PlayerData> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                this.playerData = loaded;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load character data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(dataFile)) {
            gson.toJson(playerData, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save character data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, k -> new PlayerData(playerId));
    }

    // ── Death handling ──

    /**
     * Mark a player as dead and save their death location + inventory.
     */
    public void markDead(Player player) {
        PlayerData pd = getPlayerData(player.getUniqueId());
        pd.setDead(true);
        pd.setDeathTimestamp(System.currentTimeMillis());

        // Track last place for cooldown purposes
        pd.setLastPlaceName(pd.getCurrentPlaceName());
        pd.setLastPlaceType(pd.getCurrentPlaceType());
        pd.setCurrentPlaceName(null);
        pd.setCurrentPlaceType(null);

        // Wipe character identity so they must re-create
        pd.setCharacterFirstName(null);
        pd.setCharacterLastName(null);
        pd.setCharacterAge(0);
        pd.setCharacterEthnicity(null);
        pd.setCharacterGender(null);

        // Save death location
        Location loc = player.getLocation();
        pd.setDeathWorld(loc.getWorld().getName());
        pd.setDeathX(loc.getX());
        pd.setDeathY(loc.getY());
        pd.setDeathZ(loc.getZ());
        pd.setDeathYaw(loc.getYaw());
        pd.setDeathPitch(loc.getPitch());

        // Save inventory as base64
        pd.setSavedInventory(itemArrayToBase64(player.getInventory().getContents()));
        pd.setSavedArmor(itemArrayToBase64(player.getInventory().getArmorContents()));
        pd.setSavedOffhand(itemToBase64(player.getInventory().getItemInOffHand()));

        // Save XP
        pd.setSavedXpLevel(player.getLevel());
        pd.setSavedXpProgress(player.getExp());

        save();
    }

    /**
     * Check if a player is dead.
     */
    public boolean isPlayerDead(UUID playerId) {
        PlayerData pd = playerData.get(playerId);
        return pd != null && pd.isDead();
    }

    /**
     * Get the death location of a dead player.
     */
    public Location getDeathLocation(UUID playerId) {
        PlayerData pd = playerData.get(playerId);
        if (pd == null || pd.getDeathWorld() == null) return null;
        World world = Bukkit.getWorld(pd.getDeathWorld());
        if (world == null) return null;
        return new Location(world, pd.getDeathX(), pd.getDeathY(), pd.getDeathZ(),
                pd.getDeathYaw(), pd.getDeathPitch());
    }

    /**
     * Revive a player — restore their items and teleport to death location.
     * Returns true if successful.
     */
    public boolean revivePlayer(Player player) {
        PlayerData pd = getPlayerData(player.getUniqueId());
        if (!pd.isDead()) return false;

        // Get death location
        Location deathLoc = getDeathLocation(player.getUniqueId());

        // Restore inventory
        player.getInventory().clear();
        if (pd.getSavedInventory() != null) {
            ItemStack[] contents = base64ToItemArray(pd.getSavedInventory());
            if (contents != null) {
                player.getInventory().setContents(contents);
            }
        }
        if (pd.getSavedArmor() != null) {
            ItemStack[] armor = base64ToItemArray(pd.getSavedArmor());
            if (armor != null) {
                player.getInventory().setArmorContents(armor);
            }
        }
        if (pd.getSavedOffhand() != null) {
            ItemStack offhand = base64ToItem(pd.getSavedOffhand());
            if (offhand != null) {
                player.getInventory().setItemInOffHand(offhand);
            }
        }

        // Restore XP
        player.setLevel(pd.getSavedXpLevel());
        player.setExp(pd.getSavedXpProgress());

        // Teleport to death location
        if (deathLoc != null) {
            player.teleport(deathLoc);
        }

        // Set game mode
        player.setGameMode(GameMode.SURVIVAL);
        player.setFlying(false);
        player.setAllowFlight(false);

        // Mark as alive
        pd.setDead(false);
        pd.setSavedInventory(null);
        pd.setSavedArmor(null);
        pd.setSavedOffhand(null);
        save();

        return true;
    }

    /**
     * Create a new character — clears death state, does NOT restore items.
     */
    public void createCharacter(UUID playerId, String firstName, String lastName, int age, String ethnicity, String gender) {
        PlayerData pd = getPlayerData(playerId);
        pd.setCharacterFirstName(firstName);
        pd.setCharacterLastName(lastName);
        pd.setCharacterAge(age);
        pd.setCharacterEthnicity(ethnicity);
        pd.setCharacterGender(gender);
        // Player stays dead until they /joinplace
        pd.setSavedInventory(null);
        pd.setSavedArmor(null);
        pd.setSavedOffhand(null);
        save();
    }

    // ── Base64 serialization helpers ──

    private String itemArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to serialize inventory: " + e.getMessage());
            return null;
        }
    }

    private ItemStack[] base64ToItemArray(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize inventory: " + e.getMessage());
            return null;
        }
    }

    private String itemToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack base64ToItem(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            return null;
        }
    }
}
