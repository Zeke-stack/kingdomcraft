package com.kingdomcraft.data;

import java.util.*;

public class Kingdom {
    private String name;
    private UUID leaderId;
    private Set<UUID> members;
    private Set<UUID> joinRequests;
    private boolean acceptingRequests;
    private long creationTime;
    private long protectionEndTime;
    
    public Kingdom(String name, UUID leaderId) {
        this.name = name;
        this.leaderId = leaderId;
        this.members = new HashSet<>();
        this.joinRequests = new HashSet<>();
        this.acceptingRequests = true;
        this.creationTime = System.currentTimeMillis();
        this.protectionEndTime = creationTime + (3 * 24 * 60 * 60 * 1000L); // 3 days
        this.members.add(leaderId);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public UUID getLeaderId() {
        return leaderId;
    }
    
    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }
    
    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }
    
    public void addMember(UUID playerId) {
        members.add(playerId);
        joinRequests.remove(playerId);
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }
    
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }
    
    public Set<UUID> getJoinRequests() {
        return new HashSet<>(joinRequests);
    }
    
    public void addJoinRequest(UUID playerId) {
        joinRequests.add(playerId);
    }
    
    public void removeJoinRequest(UUID playerId) {
        joinRequests.remove(playerId);
    }
    
    public void clearJoinRequests() {
        joinRequests.clear();
    }
    
    public boolean isAcceptingRequests() {
        return acceptingRequests;
    }
    
    public void setAcceptingRequests(boolean acceptingRequests) {
        this.acceptingRequests = acceptingRequests;
    }
    
    public boolean isProtected() {
        return System.currentTimeMillis() < protectionEndTime;
    }
    
    public long getProtectionTimeRemaining() {
        long remaining = protectionEndTime - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
}
