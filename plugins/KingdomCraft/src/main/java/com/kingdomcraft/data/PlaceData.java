package com.kingdomcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class PlaceData {
    private final JavaPlugin plugin;
    private final Gson gson;
    private final File dataFile;
    private List<Place> places;

    public PlaceData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "places.json");
        this.places = new ArrayList<>();
        load();
    }

    public void load() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            save();
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<List<Place>>(){}.getType();
            List<Place> loaded = gson.fromJson(reader, type);
            if (loaded != null) this.places = loaded;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load places: " + e.getMessage());
        }
    }

    public void save() {
        try {
            plugin.getDataFolder().mkdirs();
            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(places, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save places: " + e.getMessage());
        }
    }

    public Place getPlace(String name) {
        return places.stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    public List<Place> getPlacesByType(String type) {
        return places.stream()
            .filter(p -> p.getType().equalsIgnoreCase(type))
            .collect(Collectors.toList());
    }

    public List<Place> getAllPlaces() {
        return new ArrayList<>(places);
    }

    public boolean addPlace(String name, String type) {
        if (getPlace(name) != null) return false;
        places.add(new Place(name, type.toLowerCase()));
        save();
        return true;
    }

    public boolean removePlace(String name) {
        boolean removed = places.removeIf(p -> p.getName().equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }
}
