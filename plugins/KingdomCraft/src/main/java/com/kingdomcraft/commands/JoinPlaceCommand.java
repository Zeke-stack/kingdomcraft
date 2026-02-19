package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Place;
import com.kingdomcraft.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class JoinPlaceCommand implements CommandExecutor {
    private final KingdomCraft plugin;

    private static final long HOUR_MS = 3_600_000L;
    private static final long DAY_MS = 86_400_000L;
    private static final long WEEK_MS = 604_800_000L;

    public JoinPlaceCommand(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!plugin.getCharacterData().isPlayerDead(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have a living character.").color(NamedTextColor.RED));
            return true;
        }

        PlayerData pd = plugin.getCharacterData().getPlayerData(player.getUniqueId());
        if (pd.getCharacterFirstName() == null) {
            player.sendMessage(Component.text("Create a character first with /createcharacter").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /joinplace <name>").color(NamedTextColor.RED));
            return true;
        }

        String placeName = String.join(" ", args);

        // Handle refugee
        if (placeName.equalsIgnoreCase("__refugee__")) {
            joinAsRefugee(player, pd);
            return true;
        }

        Place place = plugin.getPlaceData().getPlace(placeName);
        if (place == null) {
            player.sendMessage(Component.text("That place no longer exists.").color(NamedTextColor.RED));
            return true;
        }

        if (place.getTeleports().isEmpty()) {
            player.sendMessage(Component.text("That place has no spawn points set.").color(NamedTextColor.RED));
            return true;
        }

        // Check cooldowns
        long now = System.currentTimeMillis();
        long deathTime = pd.getDeathTimestamp();
        String lastPlace = pd.getLastPlaceName();
        String lastType = pd.getLastPlaceType();

        if (deathTime > 0 && lastType != null) {
            long elapsed = now - deathTime;

            // Government: 24hr before joining SAME government
            if (place.getType().equals("government") && "government".equals(lastType)
                    && place.getName().equalsIgnoreCase(lastPlace) && elapsed < DAY_MS) {
                player.sendMessage(Component.text("You must wait " + formatTime(DAY_MS - elapsed)
                    + " before rejoining this government.").color(NamedTextColor.RED));
                return true;
            }

            // Insurgent
            if (place.getType().equals("insurgent") && "insurgent".equals(lastType)) {
                // Same group: 1 week
                if (place.getName().equalsIgnoreCase(lastPlace) && elapsed < WEEK_MS) {
                    player.sendMessage(Component.text("You must wait " + formatTime(WEEK_MS - elapsed)
                        + " before rejoining this insurgent group.").color(NamedTextColor.RED));
                    return true;
                }
                // Different group: 24hr
                if (!place.getName().equalsIgnoreCase(lastPlace) && elapsed < DAY_MS) {
                    player.sendMessage(Component.text("You must wait " + formatTime(DAY_MS - elapsed)
                        + " before joining another insurgent group.").color(NamedTextColor.RED));
                    return true;
                }
            }

            // Community: 24hr before joining SAME community
            if (place.getType().equals("community") && "community".equals(lastType)
                    && place.getName().equalsIgnoreCase(lastPlace) && elapsed < DAY_MS) {
                player.sendMessage(Component.text("You must wait " + formatTime(DAY_MS - elapsed)
                    + " before rejoining this community.").color(NamedTextColor.RED));
                return true;
            }
        }

        // Pick random teleport
        Place.SpawnPoint sp = place.getTeleports().get(
                ThreadLocalRandom.current().nextInt(place.getTeleports().size()));
        World world = Bukkit.getWorld(sp.getWorld());
        if (world == null) world = Bukkit.getWorlds().get(0);
        Location loc = new Location(world, sp.getX(), sp.getY(), sp.getZ(), sp.getYaw(), 0);

        // Set place info and mark alive
        pd.setCurrentPlaceName(place.getName());
        pd.setCurrentPlaceType(place.getType());
        pd.setDead(false);
        pd.setSavedInventory(null);
        pd.setSavedArmor(null);
        pd.setSavedOffhand(null);
        plugin.getCharacterData().save();

        // Teleport and set survival
        player.teleport(loc);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.getInventory().clear();
        player.setExp(0);
        player.setLevel(0);

        String typeLabel = switch (place.getType()) {
            case "government" -> "government";
            case "insurgent" -> "insurgent group";
            case "community" -> "private community";
            default -> place.getType();
        };

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  Joined ").color(NamedTextColor.DARK_GRAY)
            .append(Component.text(place.getName()).color(NamedTextColor.WHITE))
            .append(Component.text(" (" + typeLabel + ")").color(NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.empty());

        return true;
    }

    private void joinAsRefugee(Player player, PlayerData pd) {
        pd.setCurrentPlaceName("Refugee");
        pd.setCurrentPlaceType("refugee");
        pd.setDead(false);
        pd.setSavedInventory(null);
        pd.setSavedArmor(null);
        pd.setSavedOffhand(null);
        plugin.getCharacterData().save();

        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.getInventory().clear();
        player.setExp(0);
        player.setLevel(0);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  You have arrived as a refugee.").color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        return minutes + "m";
    }
}
