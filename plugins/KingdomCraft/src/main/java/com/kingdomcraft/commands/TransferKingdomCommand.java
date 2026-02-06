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

public class TransferKingdomCommand implements CommandExecutor {
    private final KingdomCraft plugin;
    
    public TransferKingdomCommand(KingdomCraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kingdomcraft.staff.transferkingdom")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /transferkingdom <kingdom> <new_leader>").color(NamedTextColor.RED));
            return true;
        }
        
        String kingdomName = args[0];
        Kingdom kingdom = plugin.getKingdomData().getKingdom(kingdomName);
        
        if (kingdom == null) {
            sender.sendMessage(Component.text("Kingdom not found.").color(NamedTextColor.RED));
            return true;
        }
        
        Player newLeader = Bukkit.getPlayer(args[1]);
        if (newLeader == null) {
            sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return true;
        }
        
        if (!kingdom.isMember(newLeader.getUniqueId())) {
            sender.sendMessage(Component.text("That player is not a member of this kingdom.").color(NamedTextColor.RED));
            return true;
        }
        
        // Remove old leader permissions
        Player oldLeader = Bukkit.getPlayer(kingdom.getLeaderId());
        if (oldLeader != null && plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + oldLeader.getName() + " permission unset kingdomcraft.leader");
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + oldLeader.getName() + " permission unset kingdomcraft.leader.rename");
        }
        
        // Transfer leadership
        kingdom.setLeaderId(newLeader.getUniqueId());
        plugin.getKingdomData().save();
        
        // Give new leader permissions
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + newLeader.getName() + " permission set kingdomcraft.leader true");
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + newLeader.getName() + " permission set kingdomcraft.leader.rename true");
        }
        
        sender.sendMessage(Component.text("Transferred leadership of ").color(NamedTextColor.GREEN)
            .append(Component.text(kingdomName).color(NamedTextColor.YELLOW))
            .append(Component.text(" to ").color(NamedTextColor.GREEN))
            .append(Component.text(newLeader.getName()).color(NamedTextColor.YELLOW)));
        
        // Notify kingdom members
        kingdom.getMembers().forEach(memberId -> {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(Component.text("Leadership of ").color(NamedTextColor.YELLOW)
                    .append(Component.text(kingdomName).color(NamedTextColor.GOLD))
                    .append(Component.text(" has been transferred to ").color(NamedTextColor.YELLOW))
                    .append(Component.text(newLeader.getName()).color(NamedTextColor.GOLD)));
            }
        });
        
        return true;
    }
}
