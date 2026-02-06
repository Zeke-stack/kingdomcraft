package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BringCommand implements CommandExecutor {
    
    private final StaffAuditManager auditManager;
    
    public BringCommand(StaffAuditManager auditManager) {
        this.auditManager = auditManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /bring <player>", NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }
        
        target.teleport(player.getLocation());
        player.sendMessage(Component.text("Brought ", NamedTextColor.GRAY)
            .append(Component.text(target.getName(), NamedTextColor.GOLD))
            .append(Component.text(" to you", NamedTextColor.GRAY)));
        target.sendMessage(Component.text("You were teleported to ", NamedTextColor.GRAY)
            .append(Component.text(player.getName(), NamedTextColor.GOLD)));
        
        auditManager.logAction(player, "BRING", target.getName(), 
            "Brought " + target.getName() + " to their location");
        
        return true;
    }
}
