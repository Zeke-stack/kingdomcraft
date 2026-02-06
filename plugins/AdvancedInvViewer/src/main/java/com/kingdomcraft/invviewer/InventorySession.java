package com.kingdomcraft.invviewer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventorySession {
    
    private final Player viewer;
    private final Player target;
    private final AdvancedInvViewer plugin;
    private final Inventory viewInventory;
    private boolean isOpen = false;
    
    public InventorySession(Player viewer, Player target, AdvancedInvViewer plugin) {
        this.viewer = viewer;
        this.target = target;
        this.plugin = plugin;
        
        // Create custom inventory (6 rows)
        this.viewInventory = Bukkit.createInventory(null, 54, 
            Component.text("Viewing: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(target.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" (LIVE)", NamedTextColor.GREEN, TextDecoration.BOLD))
        );
    }
    
    public void open() {
        updateInventory();
        viewer.openInventory(viewInventory);
        isOpen = true;
    }
    
    public void updateInventory() {
        if (!isOpen || !target.isOnline()) {
            close();
            return;
        }
        
        // Clear current view
        viewInventory.clear();
        
        // Row 1-3: Main inventory (27 slots)
        for (int i = 9; i < 36; i++) {
            ItemStack item = target.getInventory().getItem(i);
            viewInventory.setItem(i - 9, item);
        }
        
        // Row 4: Hotbar (9 slots)
        for (int i = 0; i < 9; i++) {
            ItemStack item = target.getInventory().getItem(i);
            viewInventory.setItem(27 + i, item);
        }
        
        // Row 5: Armor and offhand (8 slots with separators)
        viewInventory.setItem(36, createSeparator());
        
        // Armor slots
        ItemStack helmet = target.getInventory().getHelmet();
        ItemStack chestplate = target.getInventory().getChestplate();
        ItemStack leggings = target.getInventory().getLeggings();
        ItemStack boots = target.getInventory().getBoots();
        ItemStack offhand = target.getInventory().getItemInOffHand();
        
        viewInventory.setItem(37, helmet != null ? helmet : createPlaceholder("Helmet"));
        viewInventory.setItem(38, chestplate != null ? chestplate : createPlaceholder("Chestplate"));
        viewInventory.setItem(39, leggings != null ? leggings : createPlaceholder("Leggings"));
        viewInventory.setItem(40, boots != null ? boots : createPlaceholder("Boots"));
        viewInventory.setItem(41, createSeparator());
        viewInventory.setItem(42, offhand != null && offhand.getType() != Material.AIR ? offhand : createPlaceholder("Offhand"));
        
        // Stats button
        viewInventory.setItem(43, createStatsItem());
        viewInventory.setItem(44, createSeparator());
        
        // Row 6: Controls
        viewInventory.setItem(45, createEnderChestButton());
        viewInventory.setItem(46, createRefreshButton());
        viewInventory.setItem(47, createSeparator());
        viewInventory.setItem(48, createHealthIndicator());
        viewInventory.setItem(49, createHungerIndicator());
        viewInventory.setItem(50, createEffectsButton());
        viewInventory.setItem(51, createSeparator());
        viewInventory.setItem(52, createSeparator());
        viewInventory.setItem(53, createCloseButton());
    }
    
    private ItemStack createSeparator() {
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = separator.getItemMeta();
        meta.displayName(Component.text(" "));
        separator.setItemMeta(meta);
        return separator;
    }
    
    private ItemStack createPlaceholder(String slot) {
        ItemStack placeholder = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.displayName(Component.text("Empty " + slot, NamedTextColor.GRAY, TextDecoration.ITALIC));
        placeholder.setItemMeta(meta);
        return placeholder;
    }
    
    private ItemStack createStatsItem() {
        ItemStack stats = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stats.getItemMeta();
        meta.displayName(Component.text(target.getName() + "'s Stats", NamedTextColor.AQUA, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Health: ", NamedTextColor.RED)
            .append(Component.text(String.format("%.1f/%.1f", target.getHealth(), target.getMaxHealth()), NamedTextColor.WHITE)));
        lore.add(Component.text("Food: ", NamedTextColor.GOLD)
            .append(Component.text(target.getFoodLevel() + "/20", NamedTextColor.WHITE)));
        lore.add(Component.text("Level: ", NamedTextColor.GREEN)
            .append(Component.text(target.getLevel(), NamedTextColor.WHITE)));
        lore.add(Component.text("Gamemode: ", NamedTextColor.YELLOW)
            .append(Component.text(target.getGameMode().name(), NamedTextColor.WHITE)));
        lore.add(Component.text("Flying: ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(target.isFlying() ? "Yes" : "No", target.isFlying() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        
        meta.lore(lore);
        stats.setItemMeta(meta);
        return stats;
    }
    
    private ItemStack createHealthIndicator() {
        double healthPercent = (target.getHealth() / target.getMaxHealth()) * 100;
        Material material;
        NamedTextColor color;
        
        if (healthPercent > 75) {
            material = Material.LIME_DYE;
            color = NamedTextColor.GREEN;
        } else if (healthPercent > 50) {
            material = Material.YELLOW_DYE;
            color = NamedTextColor.YELLOW;
        } else if (healthPercent > 25) {
            material = Material.ORANGE_DYE;
            color = NamedTextColor.GOLD;
        } else {
            material = Material.RED_DYE;
            color = NamedTextColor.RED;
        }
        
        ItemStack health = new ItemStack(material);
        ItemMeta meta = health.getItemMeta();
        meta.displayName(Component.text("❤ Health", color, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(String.format("%.1f / %.1f", target.getHealth(), target.getMaxHealth()), NamedTextColor.WHITE));
        meta.lore(lore);
        health.setItemMeta(meta);
        return health;
    }
    
    private ItemStack createHungerIndicator() {
        ItemStack hunger = new ItemStack(Material.COOKED_BEEF);
        ItemMeta meta = hunger.getItemMeta();
        meta.displayName(Component.text("🍖 Hunger", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(target.getFoodLevel() + " / 20", NamedTextColor.WHITE));
        lore.add(Component.text("Saturation: " + String.format("%.1f", target.getSaturation()), NamedTextColor.GRAY));
        meta.lore(lore);
        hunger.setItemMeta(meta);
        return hunger;
    }
    
    private ItemStack createEffectsButton() {
        ItemStack effects = new ItemStack(Material.BREWING_STAND);
        ItemMeta meta = effects.getItemMeta();
        meta.displayName(Component.text("Active Effects", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        
        if (target.getActivePotionEffects().isEmpty()) {
            lore.add(Component.text("No active effects", NamedTextColor.GRAY, TextDecoration.ITALIC));
        } else {
            target.getActivePotionEffects().forEach(effect -> {
                lore.add(Component.text("• " + effect.getType().getName(), NamedTextColor.WHITE)
                    .append(Component.text(" " + (effect.getAmplifier() + 1), NamedTextColor.AQUA))
                    .append(Component.text(" (" + effect.getDuration() / 20 + "s)", NamedTextColor.GRAY)));
            });
        }
        
        meta.lore(lore);
        effects.setItemMeta(meta);
        return effects;
    }
    
    private ItemStack createEnderChestButton() {
        ItemStack enderChest = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = enderChest.getItemMeta();
        meta.displayName(Component.text("View Ender Chest", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to view", NamedTextColor.GRAY));
        lore.add(Component.text(target.getName() + "'s ender chest", NamedTextColor.GRAY));
        meta.lore(lore);
        enderChest.setItemMeta(meta);
        return enderChest;
    }
    
    private ItemStack createRefreshButton() {
        ItemStack refresh = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = refresh.getItemMeta();
        meta.displayName(Component.text("Auto-Refresh: ON", NamedTextColor.GREEN, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Updates 10x per second", NamedTextColor.GRAY));
        lore.add(Component.text("Live view enabled", NamedTextColor.GREEN));
        meta.lore(lore);
        refresh.setItemMeta(meta);
        return refresh;
    }
    
    private ItemStack createCloseButton() {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        meta.displayName(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to close", NamedTextColor.GRAY));
        meta.lore(lore);
        close.setItemMeta(meta);
        return close;
    }
    
    public void close() {
        isOpen = false;
        if (viewer.getOpenInventory().getTopInventory().equals(viewInventory)) {
            viewer.closeInventory();
        }
    }
    
    public Player getViewer() {
        return viewer;
    }
    
    public Player getTarget() {
        return target;
    }
    
    public Inventory getViewInventory() {
        return viewInventory;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
}
