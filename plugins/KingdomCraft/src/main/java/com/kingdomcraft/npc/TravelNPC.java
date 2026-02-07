package com.kingdomcraft.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Represents a travel NPC that teleports players to a destination.
 */
public class TravelNPC {
    private String id;
    private String displayName;
    private UUID entityUUID;
    private String world;
    private double x, y, z;
    private float yaw, pitch;
    // Destination
    private String destWorld;
    private double destX, destY, destZ;
    private float destYaw, destPitch;

    public TravelNPC(String id, String displayName, UUID entityUUID, Location npcLocation, Location destination) {
        this.id = id;
        this.displayName = displayName;
        this.entityUUID = entityUUID;
        this.world = npcLocation.getWorld().getName();
        this.x = npcLocation.getX();
        this.y = npcLocation.getY();
        this.z = npcLocation.getZ();
        this.yaw = npcLocation.getYaw();
        this.pitch = npcLocation.getPitch();
        this.destWorld = destination.getWorld().getName();
        this.destX = destination.getX();
        this.destY = destination.getY();
        this.destZ = destination.getZ();
        this.destYaw = destination.getYaw();
        this.destPitch = destination.getPitch();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public UUID getEntityUUID() { return entityUUID; }
    public void setEntityUUID(UUID uuid) { this.entityUUID = uuid; }

    public Location getNpcLocation() {
        var w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public Location getDestination() {
        var w = Bukkit.getWorld(destWorld);
        if (w == null) return null;
        return new Location(w, destX, destY, destZ, destYaw, destPitch);
    }

    public void setDestination(Location loc) {
        this.destWorld = loc.getWorld().getName();
        this.destX = loc.getX();
        this.destY = loc.getY();
        this.destZ = loc.getZ();
        this.destYaw = loc.getYaw();
        this.destPitch = loc.getPitch();
    }
}
