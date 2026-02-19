package com.kingdomcraft.discord;

import com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordWebhook {
    private final JavaPlugin plugin;
    private final Gson gson;
    private final ExecutorService executor;
    private String botUrl;

    public DiscordWebhook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.botUrl = System.getenv("DISCORD_BOT_URL");
        if (this.botUrl == null || this.botUrl.isEmpty()) {
            this.botUrl = plugin.getConfig().getString("discord-bot-url", "");
        }
        if (this.botUrl.isEmpty()) {
            plugin.getLogger().warning("Discord bot URL not set. Discord sync disabled.");
            plugin.getLogger().warning("Set DISCORD_BOT_URL env var or discord-bot-url in config.yml");
        } else {
            plugin.getLogger().info("Discord webhook enabled: " + this.botUrl);
        }
    }

    public boolean isEnabled() {
        return botUrl != null && !botUrl.isEmpty();
    }

    public void sendChat(String player, String message) {
        Map<String, String> data = new HashMap<>();
        data.put("player", player);
        data.put("message", message);
        post("/mc/chat", data);
    }

    public void sendJoin(String player) {
        Map<String, String> data = new HashMap<>();
        data.put("player", player);
        data.put("action", "join");
        post("/mc/join", data);
    }

    public void sendLeave(String player) {
        Map<String, String> data = new HashMap<>();
        data.put("player", player);
        data.put("action", "leave");
        post("/mc/join", data);
    }

    public void sendDeath(String player, String deathMessage, boolean isKing) {
        Map<String, Object> data = new HashMap<>();
        data.put("player", player);
        data.put("message", deathMessage);
        data.put("isKing", isKing);
        post("/mc/death", data);
    }

    public void sendServerStatus(String action) {
        Map<String, String> data = new HashMap<>();
        data.put("action", action);
        post("/mc/server", data);
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void post(String endpoint, Map<String, ?> data) {
        if (!isEnabled()) return;

        executor.submit(() -> {
            try {
                URL url = new URL(botUrl + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String json = gson.toJson(data);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    plugin.getLogger().warning("Discord webhook returned " + code + " for " + endpoint);
                }
                conn.disconnect();
            } catch (Exception e) {
                // Silent fail - don't spam console if bot is offline
            }
        });
    }
}
