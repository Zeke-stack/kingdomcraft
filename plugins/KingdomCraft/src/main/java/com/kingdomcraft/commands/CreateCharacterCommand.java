package com.kingdomcraft.commands;

import com.kingdomcraft.KingdomCraft;
import com.kingdomcraft.data.Place;
import com.kingdomcraft.data.PlayerData;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateCharacterCommand implements CommandExecutor {
    private final KingdomCraft plugin;
    private static final long HOUR_MS = 3_600_000L;

    public CreateCharacterCommand(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getCharacterData().isPlayerDead(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have a living character.").color(NamedTextColor.RED));
            return true;
        }

        PlayerData pd = plugin.getCharacterData().getPlayerData(player.getUniqueId());

        // If character already created, just re-open the book
        if (pd.getCharacterFirstName() != null) {
            openPlaceBook(player);
            return true;
        }

        // Check 1hr death cooldown (skip for first-time players with deathTimestamp=0)
        if (pd.getDeathTimestamp() > 0) {
            long elapsed = System.currentTimeMillis() - pd.getDeathTimestamp();
            if (elapsed < HOUR_MS) {
                long remaining = HOUR_MS - elapsed;
                player.sendMessage(Component.text("You must wait " + formatTime(remaining) + " before creating a new character.").color(NamedTextColor.RED));
                return true;
            }
        }

        if (args.length < 5) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("  Usage: ").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("/createcharacter <first> <last> <age> <ethnicity> <gender>").color(NamedTextColor.WHITE)));
            player.sendMessage(Component.text("  Example: ").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("/createcharacter James Whitfield 32 Caucasian Male").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.empty());
            return true;
        }

        String firstName = args[0];
        String lastName = args[1];
        String ageStr = args[2];
        String ethnicity = args[3];
        String gender = args[4];

        // Validate names
        if (!firstName.matches("[a-zA-Z]+") || !lastName.matches("[a-zA-Z]+")) {
            player.sendMessage(Component.text("Names may only contain letters.").color(NamedTextColor.RED));
            return true;
        }

        if (firstName.length() > 16 || lastName.length() > 16) {
            player.sendMessage(Component.text("Names must be 16 characters or fewer.").color(NamedTextColor.RED));
            return true;
        }

        // Validate age
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Age must be a number.").color(NamedTextColor.RED));
            return true;
        }
        if (age < 18 || age > 80) {
            player.sendMessage(Component.text("Age must be between 18 and 80.").color(NamedTextColor.RED));
            return true;
        }

        // Capitalize
        firstName = cap(firstName);
        lastName = cap(lastName);
        ethnicity = cap(ethnicity);
        gender = cap(gender);

        // Create character (stays dead until /joinplace)
        plugin.getCharacterData().createCharacter(player.getUniqueId(), firstName, lastName, age, ethnicity, gender);

        // Confirm character
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500").color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  " + firstName + " " + lastName + ", " + age).color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  " + ethnicity + " \u00b7 " + gender).color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500").color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());

        // Open place selection book
        openPlaceBook(player);

        return true;
    }

    public void openPlaceBook(Player player) {
        List<Place> allPlaces = new ArrayList<>(plugin.getPlaceData().getAllPlaces());
        Collections.shuffle(allPlaces);

        Book.Builder book = Book.builder()
            .title(Component.text("Registry"))
            .author(Component.text("Continents"));

        for (Place place : allPlaces) {
            if (place.getTeleports().isEmpty()) continue;

            String typeLabel = switch (place.getType()) {
                case "government" -> "GOVERNMENT";
                case "insurgent" -> "INSURGENT GROUP";
                case "community" -> "PRIVATE COMMUNITY";
                default -> place.getType().toUpperCase();
            };

            Component page = Component.empty()
                .append(Component.text(typeLabel).color(NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(place.getName()).color(NamedTextColor.BLACK).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("[ Join ]")
                    .color(NamedTextColor.DARK_GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/joinplace " + place.getName())));

            book.addPage(page);
        }

        // Refugee page (always available)
        Component refugeePage = Component.empty()
            .append(Component.text("REFUGEE").color(NamedTextColor.DARK_GRAY))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("Arrive owing").color(NamedTextColor.BLACK))
            .append(Component.newline())
            .append(Component.text("allegiance to none.").color(NamedTextColor.BLACK))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("[ Arrive ]")
                .color(NamedTextColor.DARK_GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/joinplace __refugee__")));
        book.addPage(refugeePage);

        player.openBook(book.build());
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        return minutes + "m";
    }
}
