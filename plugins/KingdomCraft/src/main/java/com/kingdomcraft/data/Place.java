package com.kingdomcraft.data;

import java.util.ArrayList;
import java.util.List;

public class Place {
    private String name;
    private String type; // government, insurgent, community
    private List<SpawnPoint> teleports;

    public Place() {
        this.teleports = new ArrayList<>();
    }

    public Place(String name, String type) {
        this.name = name;
        this.type = type.toLowerCase();
        this.teleports = new ArrayList<>();
    }

    public static class SpawnPoint {
        private String world;
        private double x, y, z;
        private float yaw;

        public SpawnPoint() {}

        public SpawnPoint(String world, double x, double y, double z, float yaw) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }

        public String getWorld() { return world; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<SpawnPoint> getTeleports() {
        if (teleports == null) teleports = new ArrayList<>();
        return teleports;
    }

    public void addTeleport(SpawnPoint sp) {
        getTeleports().add(sp);
    }

    public boolean removeTeleport(double x, double y, double z) {
        return getTeleports().removeIf(tp ->
            Math.abs(tp.x - x) < 1 && Math.abs(tp.y - y) < 1 && Math.abs(tp.z - z) < 1);
    }
}
