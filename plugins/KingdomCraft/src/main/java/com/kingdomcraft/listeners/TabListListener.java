package com.kingdomcraft.listeners;

import com.kingdomcraft.KingdomCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabListListener implements Listener {
    private final KingdomCraft plugin;

    public TabListListener(KingdomCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Small delay so player count is accurate
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                updateTabList(event.getPlayer());
            }
        }, 5L);
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateTabList(p);
        }
    }

    private void updateTabList(Player player) {
        int online = Bukkit.getOnlinePlayers().size();

        Component header = Component.empty()
            .append(Component.newline())
            .append(Component.text("CONTINENTS").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
            .append(Component.newline())
            .append(Component.text("1835").color(NamedTextColor.DARK_GRAY))
            .append(Component.newline());

        Component footer = Component.empty()
            .append(Component.newline())
            .append(Component.text("continents.cc").color(NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text(online + " online").color(NamedTextColor.DARK_GRAY))
            .append(Component.newline());

        player.sendPlayerListHeaderAndFooter(header, footer);
    }
}
