package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.ChatHistoryManager;
import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryCommand implements CommandExecutor {
    
    private final ChatHistoryManager chatHistoryManager;
    private final StaffAuditManager auditManager;
    
    public HistoryCommand(ChatHistoryManager chatHistoryManager, StaffAuditManager auditManager) {
        this.chatHistoryManager = chatHistoryManager;
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
            staff.sendMessage(Component.text("Usage: /history <player> [lines]", NamedTextColor.RED));
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        int lines = 20;
        
        if (args.length > 1) {
            try {
                lines = Integer.parseInt(args[1]);
                lines = Math.min(lines, 100); // Max 100 lines
            } catch (NumberFormatException e) {
                staff.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                return true;
            }
        }
        
        List<ChatHistoryManager.ChatMessage> history = 
            chatHistoryManager.getHistory(target.getUniqueId(), lines);
        
        if (history.isEmpty()) {
            staff.sendMessage(Component.text("No chat history found for ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.GOLD)));
            return true;
        }
        
        staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        staff.sendMessage(Component.text("Chat History: ", NamedTextColor.GOLD)
            .append(Component.text(target.getName(), NamedTextColor.WHITE))
            .append(Component.text(" (" + history.size() + " messages)", NamedTextColor.GRAY)));
        staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        for (ChatHistoryManager.ChatMessage msg : history) {
            String time = sdf.format(new Date(msg.getTimestamp()));
            staff.sendMessage(Component.text("[" + time + "] ", NamedTextColor.GRAY)
                .append(Component.text(msg.getMessage(), NamedTextColor.WHITE)));
        }
        
        staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        
        auditManager.logAction(staff, "HISTORY", target.getName(), 
            "Viewed " + history.size() + " chat messages");
        
        return true;
    }
}
