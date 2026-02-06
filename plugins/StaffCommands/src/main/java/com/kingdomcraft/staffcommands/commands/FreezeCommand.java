package com.kingdomcraft.staffcommands.commands;

import com.kingdomcraft.staffcommands.managers.FreezeManager;
import com.kingdomcraft.staffcommands.managers.StaffAuditManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public class FreezeCommand implements CommandExecutor {
    
    private final FreezeManager freezeManager;
    private final StaffAuditManager auditManager;
    
    public FreezeCommand(FreezeManager freezeManager, StaffAuditManager auditManager) {
        this.freezeManager = freezeManager;
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
            staff.sendMessage(Component.text("Usage: /freeze <player>", NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            staff.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }
        
        freezeManager.toggleFreeze(target);
        boolean isFrozen = freezeManager.isFrozen(target);
        
        if (isFrozen) {
            // Freeze player
            target.playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            target.showTitle(Title.title(
                Component.text("❄ FROZEN ❄", NamedTextColor.AQUA, TextDecoration.BOLD),
                Component.text("You have been frozen by staff", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
            ));
            
            staff.sendMessage(Component.text("Froze ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.GOLD)));
            
            auditManager.logAction(staff, "FREEZE", target.getName(), "Froze player");
        } else {
            // Unfreeze player
            target.playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
            target.showTitle(Title.title(
                Component.text("UNFROZEN", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text("You can move again", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
            
            staff.sendMessage(Component.text("Unfroze ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.GOLD)));
            
            auditManager.logAction(staff, "UNFREEZE", target.getName(), "Unfroze player");
        }
        
        return true;
    }
}
