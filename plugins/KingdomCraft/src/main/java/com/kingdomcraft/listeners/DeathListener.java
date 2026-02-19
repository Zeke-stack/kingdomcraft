package com.kingdomcraft.listeners;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
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

    /** Sky lock position for dead players. */
    public Location getSkyLocation() {
        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        return new Location(world, spawn.getX(), 500, spawn.getZ(), 0, 0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        plugin.getCharacterData().markDead(player);

        event.getDrops().clear();
        event.setDroppedExp(0);

        if (plugin.getDiscordWebhook() != null && plugin.getDiscordWebhook().isEnabled()) {
            String deathMsg = event.deathMessage() != null ?
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.deathMessage()) :
                player.getName() + " has perished";
            plugin.getDiscordWebhook().sendDeath(player.getName(), deathMsg, false);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                setupDeathMode(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData pd = plugin.getCharacterData().getPlayerData(player.getUniqueId());

        // First-time player with no character
        if (!pd.isDead() && pd.getCharacterFirstName() == null
                && plugin.getConfig().getBoolean("require-character-creation", true)) {
            pd.setDead(true);
            pd.setDeathTimestamp(0); // no cooldown for first join
            plugin.getCharacterData().save();
        }

        if (pd.isDead()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    setupDeathMode(player);
                }
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getCharacterData().isPlayerDead(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** Called from a repeating task to lock dead players at the sky position. */
    public void lockDeadPlayers() {
        Location sky = getSkyLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getCharacterData().isPlayerDead(p.getUniqueId())) {
                if (p.getGameMode() != GameMode.SPECTATOR) {
                    p.setGameMode(GameMode.SPECTATOR);
                }
                Location pLoc = p.getLocation();
                if (Math.abs(pLoc.getX() - sky.getX()) > 2
                        || Math.abs(pLoc.getY() - sky.getY()) > 2
                        || Math.abs(pLoc.getZ() - sky.getZ()) > 2) {
                    p.teleport(sky);
                }
            }
        }
    }

    private void setupDeathMode(Player player) {
        player.teleport(getSkyLocation());
        player.setGameMode(GameMode.SPECTATOR);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        PlayerData pd = plugin.getCharacterData().getPlayerData(player.getUniqueId());

        // Title
        Component title;
        Component subtitle;
        if (pd.getDeathTimestamp() == 0) {
            title = Component.text("CONTINENTS").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Create your character").color(NamedTextColor.GRAY);
        } else {
            title = Component.text("You Have Perished").color(NamedTextColor.RED).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Read chat for instructions").color(NamedTextColor.GRAY);
        }
        player.showTitle(Title.title(title, subtitle,
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(1000))));

        // Always show full guidelines (character is wiped on death)
        sendGuidelines(player);
    }

    private void sendGuidelines(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("CHARACTER GUIDELINES").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Realistic, Human Characters Only, meaning no special powers,").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("different species, or fantasy elements.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("All characters must be 18 years old or older. No exceptions.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("All characters spawn at the bottom rank. You may not start at a").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("specific rank. All new characters must join either:").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  - A government").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  - An insurgent group").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  - A private community").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  - As a refugee").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("You may not use another player's identity or name.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("When submitting a character you must follow the required format.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("You may only have one alive character at a time. No exceptions.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("You may not roleplay as any real-world political or historical figure.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Failure to follow this rule will result in extreme punishment.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("To create your character, do").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/createcharacter (first-name) (last-name) (age) (ethnicity) (gender)").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.empty());
    }
}
