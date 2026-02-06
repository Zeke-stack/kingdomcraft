package com.kingdomcraft.data;

import java.util.UUID;

public class PlayerData {
    private UUID playerId;
    private String kingdom;
    private boolean isDead;
    private String characterName;
    private String characterPersonality;
    
    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.isDead = false;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getKingdom() {
        return kingdom;
    }
    
    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }
    
    public boolean isDead() {
        return isDead;
    }
    
    public void setDead(boolean dead) {
        isDead = dead;
    }
    
    public String getCharacterName() {
        return characterName;
    }
    
    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }
    
    public String getCharacterPersonality() {
        return characterPersonality;
    }
    
    public void setCharacterPersonality(String characterPersonality) {
        this.characterPersonality = characterPersonality;
    }
}
