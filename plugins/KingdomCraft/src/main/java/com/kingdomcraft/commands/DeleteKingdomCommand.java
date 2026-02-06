package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Kingdom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DeleteKingdomCommand implements CommandExecutor {
    private final KingdomCraft plugin;
    
    public DeleteKingdomCommand(KingdomCraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kingdomcraft.staff.deletekingdom")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /deletekingdom <kingdom>").color(NamedTextColor.RED));
            return true;
        }
        
        String kingdomName = args[0];
        Kingdom kingdom = plugin.getKingdomData().getKingdom(kingdomName);
        
        if (kingdom == null) {
            sender.sendMessage(Component.text("Kingdom not found.").color(NamedTextColor.RED));
            return true;
        }
        
        plugin.getKingdomData().deleteKingdom(kingdomName);
        
        sender.sendMessage(Component.text("Deleted kingdom: ").color(NamedTextColor.GREEN)
            .append(Component.text(kingdomName).color(NamedTextColor.YELLOW)));
        
        // Broadcast
        Component broadcast = Component.text("The kingdom of ")
            .color(NamedTextColor.RED)
            .append(Component.text(kingdomName).color(NamedTextColor.YELLOW))
            .append(Component.text(" has fallen!").color(NamedTextColor.RED));
        
        plugin.getServer().broadcast(broadcast);
        
        return true;
    }
}
