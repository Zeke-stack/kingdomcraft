package com.kingdomcraft.npc;

import com.kingdomcraft.KingdomCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;

/**
 * Listens for right-click on NPC villagers, opens a confirmation GUI,
 * and teleports the player on confirm.
 */
public class NPCListener implements Listener {
    private final KingdomCraft plugin;
    private final NPCManager npcManager;

    // Track which NPC a player is confirming travel to
    private final Map<UUID, String> pendingTravel = new HashMap<>();

    private static final String GUI_TITLE_PREFIX = "Travel to ";

    public NPCListener(KingdomCraft plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    // ── Right-click NPC ──
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        UUID entityId = event.getRightClicked().getUniqueId();

        TravelNPC npc = npcManager.getNPCByEntity(entityId);
        if (npc == null) return;

        event.setCancelled(true);

        // Check if player is dead
        if (plugin.getKingdomData().isPlayerDead(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot travel while dead.")
                    .color(NamedTextColor.RED));
            return;
        }

        // Open confirmation GUI
        openConfirmGUI(player, npc);
    }

    // ── Confirmation GUI ──
    private void openConfirmGUI(Player player, TravelNPC npc) {
        String title = GUI_TITLE_PREFIX + npc.getDisplayName();
        Inventory gui = Bukkit.createInventory(null, 27, Component.text(title)
                .color(NamedTextColor.DARK_GRAY));

        // Fill background with dark glass
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.displayName(Component.text(" "));
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, bg);
        }

        // Info item in center top
        ItemStack info = new ItemStack(Material.COMPASS);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Travel to " + npc.getDisplayName())
                .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        Location dest = npc.getDestination();
        if (dest != null) {
            infoMeta.lore(List.of(
                    Component.text(String.format("X: %d  Y: %d  Z: %d",
                                    (int) dest.getX(), (int) dest.getY(), (int) dest.getZ()))
                            .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
        }
        info.setItemMeta(infoMeta);
        gui.setItem(4, info);

        // Confirm button (green)
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm Travel")
                .color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(List.of(
                Component.text("Click to travel").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        confirm.setItemMeta(confirmMeta);
        gui.setItem(11, confirm);
        gui.setItem(12, confirm);

        // Cancel button (red)
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel")
                .color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        cancelMeta.lore(List.of(
                Component.text("Click to cancel").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        cancel.setItemMeta(cancelMeta);
        gui.setItem(14, cancel);
        gui.setItem(15, cancel);

        // Track pending travel
        pendingTravel.put(player.getUniqueId(), npc.getId());

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ── Handle GUI Click ──
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = getPlainTitle(event.getView().title());
        if (title == null || !title.startsWith(GUI_TITLE_PREFIX)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String npcId = pendingTravel.remove(player.getUniqueId());
        if (npcId == null) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            // ── CONFIRM ──
            TravelNPC npc = npcManager.getNPC(npcId);
            if (npc == null) {
                player.sendMessage(Component.text("This NPC no longer exists.")
                        .color(NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            Location dest = npc.getDestination();
            if (dest == null || dest.getWorld() == null) {
                player.sendMessage(Component.text("Destination world is not loaded.")
                        .color(NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            player.closeInventory();

            // Teleport with a brief title
            player.showTitle(Title.title(
                    Component.text("Traveling...").color(NamedTextColor.GOLD),
                    Component.text(npc.getDisplayName()).color(NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(500))
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            // Small delay for the title to show, then teleport
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(dest);
                player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                player.sendMessage(Component.text("You arrived at ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(npc.getDisplayName())
                                .color(NamedTextColor.GOLD))
                        .append(Component.text(".").color(NamedTextColor.GRAY)));
            }, 10L); // 0.5 second delay

        } else if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            // ── CANCEL ──
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1f);
        }
    }

    /**
     * Extract plain text from a Component title.
     */
    private String getPlainTitle(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(component);
    }
}
