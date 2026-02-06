package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AuditCommand implements CommandExecutor {
    
    private final StaffAuditManager auditManager;
    
    public AuditCommand(StaffAuditManager auditManager) {
        this.auditManager = auditManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player staff = (Player) sender;
        
        List<StaffAuditManager.AuditEntry> entries;
        String targetName = args.length > 0 ? args[0] : null;
        
        if (targetName != null) {
            entries = auditManager.getAuditLogForPlayer(targetName);
        } else {
            entries = auditManager.getAuditLog();
            // Limit to last 50 for performance
            if (entries.size() > 50) {
                entries = entries.subList(entries.size() - 50, entries.size());
            }
        }
        
        if (entries.isEmpty()) {
            staff.sendMessage(Component.text("No audit log entries found", NamedTextColor.GRAY));
            return true;
        }
        
        staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        if (targetName != null) {
            staff.sendMessage(Component.text("Audit Log for: ", NamedTextColor.GOLD)
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .append(Component.text(" (" + entries.size() + " entries)", NamedTextColor.GRAY)));
        } else {
            staff.sendMessage(Component.text("Recent Audit Log ", NamedTextColor.GOLD)
                .append(Component.text("(Last " + entries.size() + " entries)", NamedTextColor.GRAY)));
        }
        staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        
        for (StaffAuditManager.AuditEntry entry : entries) {
            staff.sendMessage(
                Component.text("[" + entry.getFormattedTimestamp() + "] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(entry.getStaffName(), NamedTextColor.YELLOW))
                    .append(Component.text(" → ", NamedTextColor.GRAY))
                    .append(Component.text(entry.getAction(), NamedTextColor.GOLD))
                    .append(Component.text(" → ", NamedTextColor.GRAY))
                    .append(Component.text(entry.getTarget(), NamedTextColor.AQUA))
            );
            if (!entry.getDetails().isEmpty()) {
                staff.sendMessage(Component.text("  ↳ " + entry.getDetails(), NamedTextColor.GRAY));
            }
        }
        
        staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        
        return true;
    }
}
