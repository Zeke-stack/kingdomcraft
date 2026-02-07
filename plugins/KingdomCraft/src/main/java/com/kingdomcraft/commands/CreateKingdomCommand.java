package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Kingdom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateKingdomCommand implements CommandExecutor {
    private final KingdomCraft plugin;
    
    public CreateKingdomCommand(KingdomCraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kingdomcraft.staff.createkingdom")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /createkingdom <name> <leader>").color(NamedTextColor.RED));
            return true;
        }
        
        String kingdomName = args[0];
        Player leader = Bukkit.getPlayer(args[1]);
        
        if (leader == null) {
            sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return true;
        }
        
        if (plugin.getKingdomData().getKingdom(kingdomName) != null) {
            sender.sendMessage(Component.text("A kingdom with that name already exists.").color(NamedTextColor.RED));
            return true;
        }
        
        if (plugin.getKingdomData().getPlayerKingdom(leader.getUniqueId()) != null) {
            sender.sendMessage(Component.text("That player is already in a kingdom.").color(NamedTextColor.RED));
            return true;
        }
        
        // Create kingdom
        Kingdom kingdom = plugin.getKingdomData().createKingdom(kingdomName, leader.getUniqueId());
        plugin.getKingdomData().getPlayerData(leader.getUniqueId()).setKingdom(kingdomName.toLowerCase());
        plugin.getKingdomData().save();
        
        // Give leader permissions
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + leader.getName() + " permission set kingdomcraft.leader true");
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + leader.getName() + " permission set kingdomcraft.leader.rename true");
        }
        
        // Broadcast
        Component broadcast = Component.text("The kingdom of ")
            .color(NamedTextColor.GOLD)
            .append(Component.text(kingdomName).color(NamedTextColor.YELLOW).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true))
            .append(Component.text(" has been established!").color(NamedTextColor.GOLD));
        
        plugin.getServer().broadcast(broadcast);
        
        // Discord notification
        if (plugin.getDiscordWebhook() != null && plugin.getDiscordWebhook().isEnabled()) {
            plugin.getDiscordWebhook().sendKingdomEvent("created", kingdomName, leader.getName());
        }
        
        leader.sendMessage(Component.text("You are now the leader of ").color(NamedTextColor.GREEN)
            .append(Component.text(kingdomName).color(NamedTextColor.YELLOW))
            .append(Component.text("!").color(NamedTextColor.GREEN)));
        
        leader.sendMessage(Component.text("Your kingdom is protected from attacks for 3 days.").color(NamedTextColor.GRAY));
        
        return true;
    }
}
