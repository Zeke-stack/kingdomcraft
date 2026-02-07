package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.npc.NPCManager;
import com.kingdomcraft.npc.TravelNPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commands for managing Travel NPCs.
 * 
 * /createnpc <id> <displayName> - Creates an NPC where you stand, destination is your look target
 * /deletenpc <id>               - Removes an NPC
 * /listnpcs                     - Lists all travel NPCs
 * /movenpc <id>                 - Moves an NPC to your current location
 * /setnpcdest <id>              - Sets an NPC's destination to where you stand
 */
public class NPCCommands implements CommandExecutor {
    private final KingdomCraft plugin;
    private final NPCManager npcManager;

    public NPCCommands(KingdomCraft plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use NPC commands.");
            return true;
        }

        switch (label.toLowerCase()) {
            case "createnpc" -> handleCreate(player, args);
            case "deletenpc" -> handleDelete(player, args);
            case "listnpcs" -> handleList(player);
            case "movenpc" -> handleMove(player, args);
            case "setnpcdest" -> handleSetDest(player, args);
            default -> player.sendMessage(Component.text("Unknown NPC command.").color(NamedTextColor.RED));
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        // /createnpc <id> <display name...>
        // NPC spawns where you stand
        // Destination = where you're looking (100 blocks out, or current location if no target)
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /createnpc <id> <display name>").color(NamedTextColor.RED));
            player.sendMessage(Component.text("The NPC spawns at your location.").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Set the destination after with /setnpcdest <id>").color(NamedTextColor.GRAY));
            return;
        }

        String id = args[0].toLowerCase();
        // Join remaining args as display name
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) nameBuilder.append(" ");
            nameBuilder.append(args[i]);
        }
        String displayName = nameBuilder.toString();

        // Check duplicate
        if (npcManager.getNPC(id) != null) {
            player.sendMessage(Component.text("An NPC with ID '" + id + "' already exists.").color(NamedTextColor.RED));
            return;
        }

        Location spawnLoc = player.getLocation().clone();
        // Default destination = same location (must be set with /setnpcdest)
        Location destination = spawnLoc.clone();

        TravelNPC npc = npcManager.createNPC(id, displayName, spawnLoc, destination);
        if (npc == null) {
            player.sendMessage(Component.text("Failed to create NPC.").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Created travel NPC: ").color(NamedTextColor.GREEN)
                .append(Component.text(displayName).color(NamedTextColor.GOLD))
                .append(Component.text(" (id: " + id + ")").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Set destination with: /setnpcdest " + id).color(NamedTextColor.YELLOW));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /deletenpc <id>").color(NamedTextColor.RED));
            return;
        }

        String id = args[0].toLowerCase();
        if (npcManager.deleteNPC(id)) {
            player.sendMessage(Component.text("Deleted NPC: " + id).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No NPC found with ID: " + id).color(NamedTextColor.RED));
        }
    }

    private void handleList(Player player) {
        var npcs = npcManager.getAllNPCs();
        if (npcs.isEmpty()) {
            player.sendMessage(Component.text("No travel NPCs exist yet.").color(NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("── Travel NPCs ──").color(NamedTextColor.GOLD));
        for (TravelNPC npc : npcs) {
            Location loc = npc.getNpcLocation();
            Location dest = npc.getDestination();
            String locStr = loc != null
                    ? String.format("%d, %d, %d", (int) loc.getX(), (int) loc.getY(), (int) loc.getZ())
                    : "unknown";
            String destStr = dest != null
                    ? String.format("%d, %d, %d", (int) dest.getX(), (int) dest.getY(), (int) dest.getZ())
                    : "not set";

            player.sendMessage(Component.text(" " + npc.getId()).color(NamedTextColor.WHITE)
                    .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(npc.getDisplayName()).color(NamedTextColor.GOLD)));
            player.sendMessage(Component.text("   Location: " + locStr).color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("   Destination: " + destStr).color(NamedTextColor.GRAY));
        }
    }

    private void handleMove(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /movenpc <id>").color(NamedTextColor.RED));
            return;
        }

        String id = args[0].toLowerCase();
        TravelNPC npc = npcManager.getNPC(id);
        if (npc == null) {
            player.sendMessage(Component.text("No NPC found with ID: " + id).color(NamedTextColor.RED));
            return;
        }

        // Delete old entity and respawn at new location
        npcManager.deleteNPC(id);
        TravelNPC newNpc = npcManager.createNPC(id, npc.getDisplayName(), player.getLocation(), npc.getDestination());
        if (newNpc != null) {
            player.sendMessage(Component.text("Moved NPC '" + id + "' to your location.").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to move NPC.").color(NamedTextColor.RED));
        }
    }

    private void handleSetDest(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /setnpcdest <id>").color(NamedTextColor.RED));
            player.sendMessage(Component.text("Stand where you want players to be teleported.").color(NamedTextColor.GRAY));
            return;
        }

        String id = args[0].toLowerCase();
        TravelNPC npc = npcManager.getNPC(id);
        if (npc == null) {
            player.sendMessage(Component.text("No NPC found with ID: " + id).color(NamedTextColor.RED));
            return;
        }

        npc.setDestination(player.getLocation().clone());
        npcManager.save();

        Location dest = player.getLocation();
        player.sendMessage(Component.text("Set destination for '" + id + "' to: ").color(NamedTextColor.GREEN)
                .append(Component.text(String.format("%d, %d, %d",
                        (int) dest.getX(), (int) dest.getY(), (int) dest.getZ())).color(NamedTextColor.WHITE)));
    }
}
