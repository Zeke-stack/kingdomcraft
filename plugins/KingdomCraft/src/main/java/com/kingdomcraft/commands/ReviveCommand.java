package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReviveCommand implements CommandExecutor {
    private final KingdomCraft plugin;

    public ReviveCommand(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kingdomcraft.staff.revive")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /revive <player>").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getCharacterData().isPlayerDead(target.getUniqueId())) {
            sender.sendMessage(Component.text("That player is not dead.").color(NamedTextColor.RED));
            return true;
        }

        // Revive — restores inventory, teleports to death location, sets survival
        plugin.getCharacterData().revivePlayer(target);

        target.sendMessage(Component.text("You have been revived by staff! Your belongings have been restored.").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Revived ").color(NamedTextColor.GREEN)
            .append(Component.text(target.getName()).color(NamedTextColor.YELLOW)));

        return true;
    }
}
