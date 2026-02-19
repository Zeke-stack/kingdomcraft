package com.kingdomcraft.data;

import java.util.List;
import java.util.UUID;

public class PlayerData {
    private UUID playerId;
    private boolean isDead;
    private String characterFirstName;
    private String characterLastName;
    private int characterAge;
    private String characterEthnicity;
    private String characterGender;

    // Cooldown + place tracking
    private long deathTimestamp;
    private String lastPlaceName;
    private String lastPlaceType;
    private String currentPlaceName;
    private String currentPlaceType;

    // Death location (saved so revive returns them here)
    private String deathWorld;
    private double deathX, deathY, deathZ;
    private float deathYaw, deathPitch;

    // Serialized inventory (base64) — saved on death, restored on revive
    private String savedInventory;
    private String savedArmor;
    private String savedOffhand;
    private int savedXpLevel;
    private float savedXpProgress;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.isDead = false;
    }

    public UUID getPlayerId() { return playerId; }

    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }

    public String getCharacterFirstName() { return characterFirstName; }
    public void setCharacterFirstName(String name) { this.characterFirstName = name; }

    public String getCharacterLastName() { return characterLastName; }
    public void setCharacterLastName(String name) { this.characterLastName = name; }

    public int getCharacterAge() { return characterAge; }
    public void setCharacterAge(int age) { this.characterAge = age; }

    public String getCharacterEthnicity() { return characterEthnicity; }
    public void setCharacterEthnicity(String e) { this.characterEthnicity = e; }

    public String getCharacterGender() { return characterGender; }
    public void setCharacterGender(String g) { this.characterGender = g; }

    public long getDeathTimestamp() { return deathTimestamp; }
    public void setDeathTimestamp(long ts) { this.deathTimestamp = ts; }

    public String getLastPlaceName() { return lastPlaceName; }
    public void setLastPlaceName(String n) { this.lastPlaceName = n; }

    public String getLastPlaceType() { return lastPlaceType; }
    public void setLastPlaceType(String t) { this.lastPlaceType = t; }

    public String getCurrentPlaceName() { return currentPlaceName; }
    public void setCurrentPlaceName(String n) { this.currentPlaceName = n; }

    public String getCurrentPlaceType() { return currentPlaceType; }
    public void setCurrentPlaceType(String t) { this.currentPlaceType = t; }

    // Death location
    public String getDeathWorld() { return deathWorld; }
    public void setDeathWorld(String deathWorld) { this.deathWorld = deathWorld; }

    public double getDeathX() { return deathX; }
    public void setDeathX(double deathX) { this.deathX = deathX; }

    public double getDeathY() { return deathY; }
    public void setDeathY(double deathY) { this.deathY = deathY; }

    public double getDeathZ() { return deathZ; }
    public void setDeathZ(double deathZ) { this.deathZ = deathZ; }

    public float getDeathYaw() { return deathYaw; }
    public void setDeathYaw(float deathYaw) { this.deathYaw = deathYaw; }

    public float getDeathPitch() { return deathPitch; }
    public void setDeathPitch(float deathPitch) { this.deathPitch = deathPitch; }

    // Saved inventory
    public String getSavedInventory() { return savedInventory; }
    public void setSavedInventory(String savedInventory) { this.savedInventory = savedInventory; }

    public String getSavedArmor() { return savedArmor; }
    public void setSavedArmor(String savedArmor) { this.savedArmor = savedArmor; }

    public String getSavedOffhand() { return savedOffhand; }
    public void setSavedOffhand(String savedOffhand) { this.savedOffhand = savedOffhand; }

    public int getSavedXpLevel() { return savedXpLevel; }
    public void setSavedXpLevel(int savedXpLevel) { this.savedXpLevel = savedXpLevel; }

    public float getSavedXpProgress() { return savedXpProgress; }
    public void setSavedXpProgress(float savedXpProgress) { this.savedXpProgress = savedXpProgress; }
}
