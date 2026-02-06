package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public class HintCommand implements CommandExecutor {
    
    private final StaffAuditManager auditManager;
    
    public HintCommand(StaffAuditManager auditManager) {
        this.auditManager = auditManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player staff = (Player) sender;
        
        if (args.length == 0) {
            staff.sendMessage(Component.text("Usage: /hint <message>", NamedTextColor.RED));
            return true;
        }
        
        String message = String.join(" ", args);
        
        // Send title subtitle to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(
                Component.empty(),
                Component.text(message, NamedTextColor.GOLD, TextDecoration.ITALIC),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
            ));
        }
        
        // Send chat message
        Component chatMessage = Component.text("💡 ", NamedTextColor.YELLOW)
            .append(Component.text(message, NamedTextColor.GOLD, TextDecoration.ITALIC));
        Bukkit.broadcast(chatMessage);
        
        staff.sendMessage(Component.text("Hint sent!", NamedTextColor.GREEN));
        
        auditManager.logAction(staff, "HINT", "ALL", message);
        
        return true;
    }
}
