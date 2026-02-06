package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatAnnounceCommand implements CommandExecutor {
    
    private final StaffAuditManager auditManager;
    
    public ChatAnnounceCommand(StaffAuditManager auditManager) {
        this.auditManager = auditManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player staff = (Player) sender;
        
        if (args.length < 3) {
            staff.sendMessage(Component.text("Usage: /chatannounce <message> <bold/italic/none> <color>", NamedTextColor.RED));
            staff.sendMessage(Component.text("Colors: red, gold, yellow, green, aqua, blue, light_purple, white", NamedTextColor.GRAY));
            return true;
        }
        
        // Parse arguments
        int styleIndex = args.length - 2;
        int colorIndex = args.length - 1;
        
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 0, styleIndex));
        String style = args[styleIndex].toLowerCase();
        String colorName = args[colorIndex].toLowerCase();
        
        // Get color
        NamedTextColor color;
        try {
            color = NamedTextColor.NAMES.value(colorName);
            if (color == null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            staff.sendMessage(Component.text("Invalid color! Use: red, gold, yellow, green, aqua, blue, light_purple, white", NamedTextColor.RED));
            return true;
        }
        
        // Build message with style
        Component announcement = Component.text("📢 ", NamedTextColor.WHITE);
        Component textComponent = Component.text(message, color);
        
        if (style.equals("bold")) {
            textComponent = textComponent.decorate(TextDecoration.BOLD);
        } else if (style.equals("italic")) {
            textComponent = textComponent.decorate(TextDecoration.ITALIC);
        } else if (!style.equals("none")) {
            staff.sendMessage(Component.text("Invalid style! Use: bold, italic, or none", NamedTextColor.RED));
            return true;
        }
        
        announcement = announcement.append(textComponent);
        
        // Broadcast
        Bukkit.broadcast(announcement);
        
        staff.sendMessage(Component.text("Custom announcement sent!", NamedTextColor.GREEN));
        
        auditManager.logAction(staff, "CHAT_ANNOUNCE", "ALL", 
            message + " [" + style + ", " + colorName + "]");
        
        return true;
    }
}
