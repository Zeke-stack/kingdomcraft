package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Kingdom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KingdomPlayerCommands implements CommandExecutor {
    private final KingdomCraft plugin;
    
    public KingdomPlayerCommands(KingdomCraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        if (label.equalsIgnoreCase("leavekingdom")) {
            return handleLeave(player);
        } else if (label.equalsIgnoreCase("joinkingdom")) {
            return handleJoin(player, args);
        }
        
        return true;
    }
    
    private boolean handleLeave(Player player) {
        Kingdom kingdom = plugin.getKingdomData().getPlayerKingdom(player.getUniqueId());
        
        if (kingdom == null) {
            player.sendMessage(Component.text("You are not in a kingdom.").color(NamedTextColor.RED));
            return true;
        }
        
        if (kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot leave your own kingdom! Use /transferkingdom to transfer leadership first.").color(NamedTextColor.RED));
            return true;
        }
        
        kingdom.removeMember(player.getUniqueId());
        plugin.getKingdomData().getPlayerData(player.getUniqueId()).setKingdom(null);
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("You have left ").color(NamedTextColor.YELLOW)
            .append(Component.text(kingdom.getName()).color(NamedTextColor.GOLD)));
        
        return true;
    }
    
    private boolean handleJoin(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /joinkingdom <kingdom>").color(NamedTextColor.RED));
            return true;
        }
        
        Kingdom currentKingdom = plugin.getKingdomData().getPlayerKingdom(player.getUniqueId());
        if (currentKingdom != null) {
            player.sendMessage(Component.text("You are already in a kingdom! Use /leavekingdom first.").color(NamedTextColor.RED));
            return true;
        }
        
        String kingdomName = args[0];
        Kingdom kingdom = plugin.getKingdomData().getKingdom(kingdomName);
        
        if (kingdom == null) {
            player.sendMessage(Component.text("Kingdom not found.").color(NamedTextColor.RED));
            return true;
        }
        
        if (!kingdom.isAcceptingRequests()) {
            player.sendMessage(Component.text("That kingdom is not accepting join requests.").color(NamedTextColor.RED));
            return true;
        }
        
        if (kingdom.getJoinRequests().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("You have already requested to join this kingdom.").color(NamedTextColor.YELLOW));
            return true;
        }
        
        kingdom.addJoinRequest(player.getUniqueId());
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Sent join request to ").color(NamedTextColor.GREEN)
            .append(Component.text(kingdom.getName()).color(NamedTextColor.YELLOW)));
        
        // Notify leader
        var leader = plugin.getServer().getPlayer(kingdom.getLeaderId());
        if (leader != null) {
            leader.sendMessage(Component.text(player.getName()).color(NamedTextColor.YELLOW)
                .append(Component.text(" wants to join your kingdom!").color(NamedTextColor.GREEN)));
            leader.sendMessage(Component.text("Use /reqlistkingdom to see all requests.").color(NamedTextColor.GRAY));
        }
        
        return true;
    }
}
