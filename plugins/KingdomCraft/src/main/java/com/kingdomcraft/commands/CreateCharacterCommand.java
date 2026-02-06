package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCharacterCommand implements CommandExecutor {
    private final KingdomCraft plugin;
    
    public CreateCharacterCommand(KingdomCraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        if (!plugin.getKingdomData().isPlayerDead(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not dead! You already have a character.").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /createcharacter <name> <personality>").color(NamedTextColor.RED));
            player.sendMessage(Component.text("Example: /createcharacter Arthur Brave").color(NamedTextColor.GRAY));
            return true;
        }
        
        String characterName = args[0];
        String personality = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        
        // Validate character name
        if (characterName.length() > 16) {
            player.sendMessage(Component.text("Character name must be 16 characters or less.").color(NamedTextColor.RED));
            return true;
        }
        
        if (!characterName.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(Component.text("Character name can only contain letters, numbers, and underscores.").color(NamedTextColor.RED));
            return true;
        }
        
        // Create character
        plugin.getKingdomData().setCharacter(player.getUniqueId(), characterName, personality);
        
        // Teleport to spawn
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        player.teleport(spawn);
        
        // Set to survival
        player.setGameMode(GameMode.SURVIVAL);
        player.setFlying(false);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        
        // Welcome message
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Welcome to your new life!").color(NamedTextColor.GREEN).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Character Name: ").color(NamedTextColor.GRAY)
            .append(Component.text(characterName).color(NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Personality: ").color(NamedTextColor.GRAY)
            .append(Component.text(personality).color(NamedTextColor.YELLOW)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Your adventure begins now!").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        
        return true;
    }
}
