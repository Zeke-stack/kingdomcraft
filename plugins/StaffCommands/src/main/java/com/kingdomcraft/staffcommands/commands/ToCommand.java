package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToCommand implements CommandExecutor {
    
    private final StaffAuditManager auditManager;
    
    public ToCommand(StaffAuditManager auditManager) {
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
            player.sendMessage(Component.text("Usage: /to <player>", NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }
        
        player.teleport(target.getLocation());
        player.sendMessage(Component.text("Teleported to ", NamedTextColor.GRAY)
            .append(Component.text(target.getName(), NamedTextColor.GOLD)));
        
        auditManager.logAction(player, "TELEPORT_TO", target.getName(), 
            "Teleported to " + target.getName());
        
        return true;
    }
}
