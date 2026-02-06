package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Kingdom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class KingdomLeaderCommands implements CommandExecutor {
    private final KingdomCraft plugin;
    
    public KingdomLeaderCommands(KingdomCraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        Kingdom kingdom = plugin.getKingdomData().getPlayerKingdom(player.getUniqueId());
        
        if (kingdom == null || !kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a kingdom leader to use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        switch (label.toLowerCase()) {
            case "renamekingdom" -> {
                return handleRename(player, kingdom, args);
            }
            case "reqlistkingdom" -> {
                return handleRequestList(player, kingdom);
            }
            case "reqacceptkingdom" -> {
                return handleAcceptRequest(player, kingdom, args);
            }
            case "reqdenykingdom" -> {
                return handleDenyRequest(player, kingdom, args);
            }
            case "reqacceptallkingdom" -> {
                return handleAcceptAll(player, kingdom);
            }
            case "reqdenyallkingdom" -> {
                return handleDenyAll(player, kingdom);
            }
            case "kickkingdom" -> {
                return handleKick(player, kingdom, args);
            }
            case "reqoffkingdom" -> {
                return handleRequestsOff(player, kingdom);
            }
            case "reqonkingdom" -> {
                return handleRequestsOn(player, kingdom);
            }
            case "kingdomlist" -> {
                return handleMemberList(player, kingdom);
            }
        }
        
        return true;
    }
    
    private boolean handleRename(Player player, Kingdom kingdom, String[] args) {
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /renamekingdom <new_name>").color(NamedTextColor.RED));
            return true;
        }
        
        String newName = args[0];
        
        if (plugin.getKingdomData().getKingdom(newName) != null) {
            player.sendMessage(Component.text("A kingdom with that name already exists.").color(NamedTextColor.RED));
            return true;
        }
        
        String oldName = kingdom.getName();
        
        // Update kingdom name in data
        plugin.getKingdomData().getAllKingdoms().stream()
            .filter(k -> k.getName().equalsIgnoreCase(oldName))
            .findFirst()
            .ifPresent(k -> {
                // Remove old entry and add new one
                plugin.getKingdomData().deleteKingdom(oldName);
                Kingdom newKingdom = plugin.getKingdomData().createKingdom(newName, k.getLeaderId());
                // Copy all data
                k.getMembers().forEach(m -> {
                    newKingdom.addMember(m);
                    plugin.getKingdomData().getPlayerData(m).setKingdom(newName.toLowerCase());
                });
                k.getJoinRequests().forEach(newKingdom::addJoinRequest);
                newKingdom.setAcceptingRequests(k.isAcceptingRequests());
            });
        
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Renamed kingdom to: ").color(NamedTextColor.GREEN)
            .append(Component.text(newName).color(NamedTextColor.YELLOW)));
        
        return true;
    }
    
    private boolean handleRequestList(Player player, Kingdom kingdom) {
        Set<UUID> requests = kingdom.getJoinRequests();
        
        if (requests.isEmpty()) {
            player.sendMessage(Component.text("No pending join requests.").color(NamedTextColor.YELLOW));
            return true;
        }
        
        player.sendMessage(Component.text("━━━━━ Join Requests ━━━━━").color(NamedTextColor.GOLD));
        
        for (UUID requestId : requests) {
            Player requester = Bukkit.getPlayer(requestId);
            String name = requester != null ? requester.getName() : "Unknown";
            
            player.sendMessage(Component.text("• ").color(NamedTextColor.GRAY)
                .append(Component.text(name).color(NamedTextColor.WHITE)));
        }
        
        player.sendMessage(Component.text("Use /reqacceptkingdom <player> or /reqdenykingdom <player>").color(NamedTextColor.GRAY));
        
        return true;
    }
    
    private boolean handleAcceptRequest(Player player, Kingdom kingdom, String[] args) {
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /reqacceptkingdom <player>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return true;
        }
        
        if (!kingdom.getJoinRequests().contains(target.getUniqueId())) {
            player.sendMessage(Component.text("That player has not requested to join.").color(NamedTextColor.RED));
            return true;
        }
        
        kingdom.addMember(target.getUniqueId());
        plugin.getKingdomData().getPlayerData(target.getUniqueId()).setKingdom(kingdom.getName().toLowerCase());
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Accepted ").color(NamedTextColor.GREEN)
            .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text(" into the kingdom!").color(NamedTextColor.GREEN)));
        
        target.sendMessage(Component.text("You have been accepted into ").color(NamedTextColor.GREEN)
            .append(Component.text(kingdom.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text("!").color(NamedTextColor.GREEN)));
        
        return true;
    }
    
    private boolean handleDenyRequest(Player player, Kingdom kingdom, String[] args) {
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /reqdenykingdom <player>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return true;
        }
        
        if (!kingdom.getJoinRequests().contains(target.getUniqueId())) {
            player.sendMessage(Component.text("That player has not requested to join.").color(NamedTextColor.RED));
            return true;
        }
        
        kingdom.removeJoinRequest(target.getUniqueId());
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Denied ").color(NamedTextColor.RED)
            .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text("'s request.").color(NamedTextColor.RED)));
        
        target.sendMessage(Component.text("Your request to join ").color(NamedTextColor.RED)
            .append(Component.text(kingdom.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text(" was denied.").color(NamedTextColor.RED)));
        
        return true;
    }
    
    private boolean handleAcceptAll(Player player, Kingdom kingdom) {
        Set<UUID> requests = kingdom.getJoinRequests();
        
        if (requests.isEmpty()) {
            player.sendMessage(Component.text("No pending join requests.").color(NamedTextColor.YELLOW));
            return true;
        }
        
        for (UUID requestId : requests) {
            kingdom.addMember(requestId);
            plugin.getKingdomData().getPlayerData(requestId).setKingdom(kingdom.getName().toLowerCase());
            
            Player target = Bukkit.getPlayer(requestId);
            if (target != null) {
                target.sendMessage(Component.text("You have been accepted into ").color(NamedTextColor.GREEN)
                    .append(Component.text(kingdom.getName()).color(NamedTextColor.YELLOW))
                    .append(Component.text("!").color(NamedTextColor.GREEN)));
            }
        }
        
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Accepted all pending requests!").color(NamedTextColor.GREEN));
        
        return true;
    }
    
    private boolean handleDenyAll(Player player, Kingdom kingdom) {
        Set<UUID> requests = kingdom.getJoinRequests();
        
        if (requests.isEmpty()) {
            player.sendMessage(Component.text("No pending join requests.").color(NamedTextColor.YELLOW));
            return true;
        }
        
        for (UUID requestId : requests) {
            Player target = Bukkit.getPlayer(requestId);
            if (target != null) {
                target.sendMessage(Component.text("Your request to join ").color(NamedTextColor.RED)
                    .append(Component.text(kingdom.getName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" was denied.").color(NamedTextColor.RED)));
            }
        }
        
        kingdom.clearJoinRequests();
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Denied all pending requests.").color(NamedTextColor.RED));
        
        return true;
    }
    
    private boolean handleKick(Player player, Kingdom kingdom, String[] args) {
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /kickkingdom <player>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return true;
        }
        
        if (!kingdom.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text("That player is not in your kingdom.").color(NamedTextColor.RED));
            return true;
        }
        
        if (kingdom.isLeader(target.getUniqueId())) {
            player.sendMessage(Component.text("You cannot kick yourself!").color(NamedTextColor.RED));
            return true;
        }
        
        kingdom.removeMember(target.getUniqueId());
        plugin.getKingdomData().getPlayerData(target.getUniqueId()).setKingdom(null);
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Kicked ").color(NamedTextColor.RED)
            .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text(" from the kingdom.").color(NamedTextColor.RED)));
        
        target.sendMessage(Component.text("You have been kicked from ").color(NamedTextColor.RED)
            .append(Component.text(kingdom.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text("!").color(NamedTextColor.RED)));
        
        return true;
    }
    
    private boolean handleRequestsOff(Player player, Kingdom kingdom) {
        kingdom.setAcceptingRequests(false);
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Disabled join requests.").color(NamedTextColor.YELLOW));
        
        return true;
    }
    
    private boolean handleRequestsOn(Player player, Kingdom kingdom) {
        kingdom.setAcceptingRequests(true);
        plugin.getKingdomData().save();
        
        player.sendMessage(Component.text("Enabled join requests.").color(NamedTextColor.GREEN));
        
        return true;
    }
    
    private boolean handleMemberList(Player player, Kingdom kingdom) {
        Set<UUID> members = kingdom.getMembers();
        
        player.sendMessage(Component.text("━━━━━ Kingdom Members ━━━━━").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Kingdom: ").color(NamedTextColor.GRAY)
            .append(Component.text(kingdom.getName()).color(NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Total Members: ").color(NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(members.size())).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());
        
        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            String name = member != null ? member.getName() : "Unknown";
            boolean isLeader = kingdom.isLeader(memberId);
            
            Component line = Component.text("• ").color(NamedTextColor.GRAY)
                .append(Component.text(name).color(NamedTextColor.WHITE));
            
            if (isLeader) {
                line = line.append(Component.text(" [Leader]").color(NamedTextColor.GOLD));
            }
            
            player.sendMessage(line);
        }
        
        return true;
    }
}
