package com.kingdomcraft.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Custom recipes - Iron armor uses iron blocks instead of iron ingots.
 */
public class RecipeManager {
    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        // Remove vanilla iron armor recipes
        removeVanillaRecipe("iron_helmet");
        removeVanillaRecipe("iron_chestplate");
        removeVanillaRecipe("iron_leggings");
        removeVanillaRecipe("iron_boots");

        // Register custom iron armor recipes using iron blocks
        registerIronHelmet();
        registerIronChestplate();
        registerIronLeggings();
        registerIronBoots();

        plugin.getLogger().info("Custom iron armor recipes registered (iron blocks).");
    }

    private void registerIronHelmet() {
        NamespacedKey key = new NamespacedKey(plugin, "iron_helmet");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.IRON_HELMET));
        recipe.shape("BBB", "B B");
        recipe.setIngredient('B', Material.IRON_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private void registerIronChestplate() {
        NamespacedKey key = new NamespacedKey(plugin, "iron_chestplate");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.IRON_CHESTPLATE));
        recipe.shape("B B", "BBB", "BBB");
        recipe.setIngredient('B', Material.IRON_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private void registerIronLeggings() {
        NamespacedKey key = new NamespacedKey(plugin, "iron_leggings");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.IRON_LEGGINGS));
        recipe.shape("BBB", "B B", "B B");
        recipe.setIngredient('B', Material.IRON_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private void registerIronBoots() {
        NamespacedKey key = new NamespacedKey(plugin, "iron_boots");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.IRON_BOOTS));
        recipe.shape("B B", "B B");
        recipe.setIngredient('B', Material.IRON_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    private void removeVanillaRecipe(String recipeName) {
        NamespacedKey vanillaKey = NamespacedKey.minecraft(recipeName);
        Bukkit.removeRecipe(vanillaKey);
    }
}
