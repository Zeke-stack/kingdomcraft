package com.kingdomcraft.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP bridge between this MC plugin and the Discord bot on Railway.
 * Replaces RCON — the bot queues commands, this plugin polls and executes them.
 */
public class BridgeManager {
    private final JavaPlugin plugin;
    private final Gson gson;
    private final ExecutorService executor;
    private final String botUrl;
    private final String apiKey;
    private int pollTaskId = -1;
    private int heartbeatTaskId = -1;

    public BridgeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.executor = Executors.newFixedThreadPool(2);

        // Get bot URL from env or config
        String url = System.getenv("DISCORD_BOT_URL");
        if (url == null || url.isEmpty()) {
            url = plugin.getConfig().getString("discord-bot-url", "");
        }
        this.botUrl = url;

        // Get API key from env or config
        String key = System.getenv("MC_API_KEY");
        if (key == null || key.isEmpty()) {
            key = plugin.getConfig().getString("mc-api-key", "kc-bridge-2025");
        }
        this.apiKey = key;

        if (botUrl.isEmpty()) {
            plugin.getLogger().warning("[Bridge] Bot URL not set. Bridge disabled.");
            plugin.getLogger().warning("[Bridge] Set DISCORD_BOT_URL env var or discord-bot-url in config.yml");
        } else {
            plugin.getLogger().info("[Bridge] Connecting to bot at: " + botUrl);
        }
    }

    public boolean isEnabled() {
        return botUrl != null && !botUrl.isEmpty();
    }

    /**
     * Start polling for commands and sending heartbeats.
     * Must be called from onEnable after a short delay.
     */
    public void start() {
        if (!isEnabled()) return;

        // Poll for commands every 3 seconds (60 ticks)
        pollTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::pollCommands, 60L, 60L).getTaskId();

        // Send heartbeat every 20 seconds (400 ticks)
        heartbeatTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendHeartbeat, 100L, 400L).getTaskId();

        plugin.getLogger().info("[Bridge] Command polling and heartbeat started.");
    }

    /**
     * Stop polling and clean up.
     */
    public void stop() {
        if (pollTaskId != -1) {
            Bukkit.getScheduler().cancelTask(pollTaskId);
            pollTaskId = -1;
        }
        if (heartbeatTaskId != -1) {
            Bukkit.getScheduler().cancelTask(heartbeatTaskId);
            heartbeatTaskId = -1;
        }
        executor.shutdown();
    }

    /**
     * Poll the bot for pending commands and execute them on the main thread.
     */
    private void pollCommands() {
        try {
            URL url = new URL(botUrl + "/bridge/poll");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Api-Key", apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Send empty body
            try (OutputStream os = conn.getOutputStream()) {
                os.write("{}".getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) return;

            // Read response
            String body = readResponse(conn);
            conn.disconnect();

            JsonObject response = gson.fromJson(body, JsonObject.class);
            JsonArray commands = response.getAsJsonArray("commands");
            if (commands == null || commands.size() == 0) return;

            // Execute each command on the main server thread
            for (JsonElement el : commands) {
                JsonObject cmd = el.getAsJsonObject();
                int id = cmd.get("id").getAsInt();
                String command = cmd.get("command").getAsString();

                // Run on main thread since Bukkit commands must be on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // Capture command output using a custom CommandSender wrapper
                        StringBuilder output = new StringBuilder();
                        
                        // Use console command sender - output goes to console
                        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        
                        String result = success ? "Command executed" : "Command failed";
                        
                        // Send result back asynchronously
                        executor.submit(() -> sendResult(id, result));
                    } catch (Exception e) {
                        executor.submit(() -> sendResult(id, "Error: " + e.getMessage()));
                    }
                });
            }
        } catch (Exception e) {
            // Silent - bot might be offline, don't spam console
        }
    }

    /**
     * Send heartbeat with player list to the bot.
     */
    private void sendHeartbeat() {
        try {
            // Collect player data (must access Bukkit API, but names are thread-safe in practice)
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }

            JsonObject data = new JsonObject();
            JsonArray playerArray = new JsonArray();
            for (String name : players) {
                playerArray.add(name);
            }
            data.add("players", playerArray);
            data.addProperty("playerCount", players.size());

            URL url = new URL(botUrl + "/bridge/heartbeat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Api-Key", apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String json = gson.toJson(data);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            // Silent - bot might be offline
        }
    }

    /**
     * Send a command execution result back to the bot.
     */
    private void sendResult(int id, String result) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("id", id);
            data.addProperty("result", result);

            URL url = new URL(botUrl + "/bridge/result");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Api-Key", apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String json = gson.toJson(data);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            // Silent
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
