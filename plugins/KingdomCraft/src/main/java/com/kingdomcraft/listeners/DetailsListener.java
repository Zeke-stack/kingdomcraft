package com.kingdomcraft.listeners;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Kingdom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Small immersive details that make the world feel alive.
 */
public class DetailsListener implements Listener {
    private final KingdomCraft plugin;
    private final Map<UUID, Long> lastActionBarUpdate = new HashMap<>();

    public DetailsListener(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    // ── 1. Player heads drop on PvP kill ──
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPvPDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        // Drop victim's head
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(victim);
        meta.displayName(Component.text(victim.getName() + "'s Head")
                .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Slain by " + killer.getName()).color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        skull.setItemMeta(meta);
        victim.getWorld().dropItemNaturally(victim.getLocation(), skull);
    }

    // ── 2. Blood particles on damage ──
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (event.getDamage() < 1.0) return;

        Location loc = entity.getLocation().add(0, 1, 0);
        entity.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 8, 0.3, 0.5, 0.3, 0.01);
    }

    // ── 3. Lightning strike effect on player kill (small visual, no damage) ──
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        // Visual-only lightning at death location
        victim.getWorld().strikeLightningEffect(victim.getLocation());
    }

    // ── 4. Campfire healing — stand near campfires to slowly heal ──
    // Runs via a repeating task scheduled in the main class

    /**
     * Called every 3 seconds by the main plugin scheduler.
     * Players near campfires get slow regen.
     */
    public void tickCampfireHealing() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL) continue;
            if (player.isDead()) continue;

            // Check 3-block radius for campfires
            Location loc = player.getLocation();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        Block block = loc.getWorld().getBlockAt(
                                loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz);
                        if (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
                            // Give 5 seconds of regen I
                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.REGENERATION, 100, 0, true, false, true));
                            // Show particles
                            player.getWorld().spawnParticle(Particle.HEART, 
                                    player.getLocation().add(0, 2, 0), 1, 0.3, 0.2, 0.3, 0);
                            return; // Only one campfire heal per tick
                        }
                    }
                }
            }
        }
    }

    // ── 5. Sneak + right-click with empty hand on stair = sit ──
    private final Set<UUID> sitting = new HashSet<>();

    @EventHandler
    public void onInteractSit(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        String typeName = block.getType().name();
        if (!typeName.contains("STAIRS")) return;

        // Already sitting?
        if (sitting.contains(player.getUniqueId())) return;

        event.setCancelled(true);

        // Spawn an invisible armor stand to sit on
        Location sitLoc = block.getLocation().add(0.5, -0.2, 0.5);
        ArmorStand seat = block.getWorld().spawn(sitLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stand.setPersistent(false);
        });

        seat.addPassenger(player);
        sitting.add(player.getUniqueId());

        // Remove seat when player dismounts
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!seat.isValid() || seat.getPassengers().isEmpty() || !player.isOnline()) {
                seat.remove();
                sitting.remove(player.getUniqueId());
                task.cancel();
            }
        }, 20L, 10L);
    }

    @EventHandler
    public void onDismount(org.bukkit.event.entity.EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            sitting.remove(player.getUniqueId());
            if (event.getDismounted() instanceof ArmorStand stand && stand.isMarker()) {
                stand.remove();
            }
        }
    }

    // ── 6. Action bar showing kingdom name while playing ──
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Throttle to once every 5 seconds
        long now = System.currentTimeMillis();
        Long last = lastActionBarUpdate.get(uuid);
        if (last != null && now - last < 5000) return;
        lastActionBarUpdate.put(uuid, now);

        Kingdom kingdom = plugin.getKingdomData().getPlayerKingdom(uuid);
        if (kingdom != null) {
            boolean isLeader = kingdom.isLeader(uuid);
            Component bar = Component.text(kingdom.getName())
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true);
            if (isLeader) {
                bar = Component.text("Crown of ").color(NamedTextColor.GRAY)
                        .append(bar);
            }
            player.sendActionBar(bar);
        }
    }

    // ── 7. Crop trampling prevention — no farmland destruction ──
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFarmlandTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    // ── 8. Tool durability warning ──
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        int maxDur = item.getType().getMaxDurability();
        if (maxDur <= 0) return;

        int remaining = maxDur - (item.getDurability() + event.getDamage());
        // Warn at 10% durability
        if (remaining > 0 && remaining <= maxDur * 0.1 && remaining > maxDur * 0.1 - 2) {
            player.sendActionBar(Component.text("Your " + formatItemName(item.getType()) + " is about to break!")
                    .color(NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.5f);
        }
    }

    // ── 9. Sleeping speeds up time (1 player enough) ──
    @EventHandler
    public void onSleep(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        // Paper already has single-player-sleep, but broadcast a message
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            event.getPlayer().getWorld().getPlayers().forEach(p -> {
                if (p.getUniqueId() != event.getPlayer().getUniqueId()) {
                    p.sendMessage(Component.text(event.getPlayer().getName() + " is sleeping...")
                            .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true));
                }
            });
        }, 40L);
    }

    // ── 10. Respawn with short resistance ──
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 3, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, true, false, true));
        }, 1L);
    }

    // ── Helper ──
    private String formatItemName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        // Capitalize first letter of each word
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
