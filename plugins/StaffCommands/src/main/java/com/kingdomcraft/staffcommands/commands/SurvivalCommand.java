package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SurvivalCommand implements CommandExecutor {
    
    private final StaffAuditManager auditManager;
    
    public SurvivalCommand(StaffAuditManager auditManager) {
        this.auditManager = auditManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(Component.text("Returned to survival mode", NamedTextColor.GRAY));
        
        auditManager.logAction(player, "SURVIVAL", player.getName(), "Returned to survival mode");
        
        return true;
    }
}
