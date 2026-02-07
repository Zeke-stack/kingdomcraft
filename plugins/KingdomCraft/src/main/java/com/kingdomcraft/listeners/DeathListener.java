package com.kingdomcraft.listeners;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Kingdom;
import com.kingdomcraft.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;

import java.time.Duration;

public class DeathListener implements Listener {
    private final KingdomCraft plugin;
    
    public DeathListener(KingdomCraft plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        PlayerData pd = plugin.getKingdomData().getPlayerData(player.getUniqueId());
        
        // Check if killed by another player
        Player killer = player.getKiller();
        
        // Check if player is a kingdom leader
        Kingdom kingdom = plugin.getKingdomData().getPlayerKingdom(player.getUniqueId());
        boolean isLeader = kingdom != null && kingdom.isLeader(player.getUniqueId());
        
        // If they're a protected kingdom leader killed by another player, prevent death
        if (isLeader && kingdom.isProtected() && killer != null) {
            event.setCancelled(true);
            player.setHealth(20.0);
            
            Component msg = Component.text("This kingdom is under protection! The leader cannot be killed by players for ")
                .color(NamedTextColor.RED)
                .append(Component.text(formatTime(kingdom.getProtectionTimeRemaining())))
                .append(Component.text("."));
            
            if (killer != null) {
                killer.sendMessage(msg);
            }
            player.sendMessage(Component.text("You are protected as a new kingdom leader!").color(NamedTextColor.GREEN));
            return;
        }

        // Keep inventory and levels on death
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Mark player as dead
        plugin.getKingdomData().setPlayerDead(player.getUniqueId(), true);
        
        // Broadcast if they were a king/leader
        if (isLeader) {
            Component broadcast = Component.text("The king has died!")
                .color(NamedTextColor.DARK_RED)
                .decorate(TextDecoration.ITALIC);
            plugin.getServer().broadcast(broadcast);
            
            // Notify event staff to select new leader
            Component staffMsg = Component.text("Event Staff: Select a new leader for kingdom ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(kingdom.getName()).color(NamedTextColor.GOLD))
                .append(Component.text(" using /transferkingdom"));
            
            plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("kingdomcraft.staff.transferkingdom"))
                .forEach(p -> p.sendMessage(staffMsg));
        }
        
        // Set to spectator mode after death
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                setupSpectatorMode(player);
            }
        }, 1L);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is dead
        if (plugin.getKingdomData().isPlayerDead(player.getUniqueId())) {
            setupSpectatorMode(player);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Prevent interaction if dead
        if (plugin.getKingdomData().isPlayerDead(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    private void setupSpectatorMode(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.setFlying(true);
        player.setAllowFlight(true);
        
        // Clear potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Send character creation prompt
        Component title = Component.text("You have died")
            .color(NamedTextColor.RED)
            .decorate(TextDecoration.BOLD);
        
        Component subtitle = Component.text("Type /createcharacter <name> <personality>")
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC);
        
        player.showTitle(Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(5),
                Duration.ofMillis(1000)
            )
        ));
        
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("You are in spectator mode.").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("You can view the world but cannot interact.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("To create a new character:").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/createcharacter <name> <personality>").color(NamedTextColor.GOLD).decorate(TextDecoration.ITALIC));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Example: ").color(NamedTextColor.GRAY)
            .append(Component.text("/createcharacter Arthur Brave").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY));
    }
    
    private String formatTime(long millis) {
        long days = millis / (24 * 60 * 60 * 1000);
        long hours = (millis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (millis % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " " + hours + " hour" + (hours != 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " " + minutes + " minute" + (minutes != 1 ? "s" : "");
        } else {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        }
    }
}
