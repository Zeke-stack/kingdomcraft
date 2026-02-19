package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Place;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PlaceCommands implements CommandExecutor {
    private final KingdomCraft plugin;
    private static final Set<String> VALID_TYPES = Set.of("government", "insurgent", "community");

    public PlaceCommands(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kingdomcraft.staff.places")) {
            sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }

        switch (label.toLowerCase()) {
            case "addplace" -> handleAddPlace(sender, args);
            case "listplace" -> handleListPlace(sender, args);
            case "removeplace" -> handleRemovePlace(sender, args);
            case "addteleport" -> handleAddTeleport(sender, args);
            case "removeteleport" -> handleRemoveTeleport(sender, args);
        }
        return true;
    }

    private void handleAddPlace(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /addplace <type> <name>").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Types: government, insurgent, community").color(NamedTextColor.GRAY));
            return;
        }
        String type = args[0].toLowerCase();
        if (!VALID_TYPES.contains(type)) {
            sender.sendMessage(Component.text("Invalid type. Use: government, insurgent, community").color(NamedTextColor.RED));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (plugin.getPlaceData().addPlace(name, type)) {
            sender.sendMessage(Component.text("Created " + type + ": " + name).color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("A place with that name already exists.").color(NamedTextColor.RED));
        }
    }

    private void handleListPlace(CommandSender sender, String[] args) {
        String filter = args.length > 0 ? args[0].toLowerCase() : "all";
        List<Place> list;
        if (filter.equals("all")) {
            list = plugin.getPlaceData().getAllPlaces();
        } else if (VALID_TYPES.contains(filter)) {
            list = plugin.getPlaceData().getPlacesByType(filter);
        } else {
            sender.sendMessage(Component.text("Usage: /listplace <type|all>").color(NamedTextColor.RED));
            return;
        }

        if (list.isEmpty()) {
            sender.sendMessage(Component.text("No places found.").color(NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  Places" + (filter.equals("all") ? "" : " (" + filter + ")")).color(NamedTextColor.WHITE));
        for (Place p : list) {
            String typeTag = p.getType().substring(0, 1).toUpperCase() + p.getType().substring(1);
            sender.sendMessage(Component.text("  " + typeTag).color(NamedTextColor.DARK_GRAY)
                .append(Component.text(" " + p.getName()).color(NamedTextColor.WHITE))
                .append(Component.text(" [" + p.getTeleports().size() + " spawns]").color(NamedTextColor.DARK_GRAY)));
        }
        sender.sendMessage(Component.empty());
    }

    private void handleRemovePlace(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /removeplace <type> <name>").color(NamedTextColor.RED));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (plugin.getPlaceData().removePlace(name)) {
            sender.sendMessage(Component.text("Removed: " + name).color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Place not found.").color(NamedTextColor.RED));
        }
    }

    private void handleAddTeleport(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /addteleport <place> <x> <y> <z> <rotation>").color(NamedTextColor.RED));
            return;
        }

        try {
            float rotation = Float.parseFloat(args[args.length - 1]);
            double z = Double.parseDouble(args[args.length - 2]);
            double y = Double.parseDouble(args[args.length - 3]);
            double x = Double.parseDouble(args[args.length - 4]);
            String placeName = String.join(" ", Arrays.copyOfRange(args, 0, args.length - 4));

            Place place = plugin.getPlaceData().getPlace(placeName);
            if (place == null) {
                sender.sendMessage(Component.text("Place not found: " + placeName).color(NamedTextColor.RED));
                return;
            }

            String world = sender instanceof Player p ? p.getWorld().getName() : "world";
            place.addTeleport(new Place.SpawnPoint(world, x, y, z, rotation));
            plugin.getPlaceData().save();
            sender.sendMessage(Component.text("Teleport added to " + placeName).color(NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid coordinates.").color(NamedTextColor.RED));
        }
    }

    private void handleRemoveTeleport(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /removeteleport <place> <x> <y> <z> <rotation>").color(NamedTextColor.RED));
            return;
        }

        try {
            double z = Double.parseDouble(args[args.length - 2]);
            double y = Double.parseDouble(args[args.length - 3]);
            double x = Double.parseDouble(args[args.length - 4]);
            String placeName = String.join(" ", Arrays.copyOfRange(args, 0, args.length - 4));

            Place place = plugin.getPlaceData().getPlace(placeName);
            if (place == null) {
                sender.sendMessage(Component.text("Place not found: " + placeName).color(NamedTextColor.RED));
                return;
            }

            if (place.removeTeleport(x, y, z)) {
                plugin.getPlaceData().save();
                sender.sendMessage(Component.text("Teleport removed from " + placeName).color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("No teleport found near those coordinates.").color(NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid coordinates.").color(NamedTextColor.RED));
        }
    }
}
