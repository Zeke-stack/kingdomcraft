# KingdomCraft Quick Start Guide

## 🎮 For Players

### When You Die
1. You'll enter spectator mode (can fly and view, but not interact)
2. A message will appear: "You have died"
3. Type: `/createcharacter <name> <personality>`
   - Example: `/createcharacter Aria Cunning`
4. You'll respawn at spawn with your new character!

### Joining a Kingdom
1. Find out kingdom names from other players
2. Type: `/joinkingdom <kingdom_name>`
3. Wait for the kingdom leader to accept your request
4. You'll get a notification when accepted!

### Leaving a Kingdom
- Type: `/leavekingdom`
- Note: Leaders cannot leave their own kingdom

---

## 👑 For Kingdom Leaders

### Managing Join Requests
```
/reqlistkingdom              - See all pending requests
/reqacceptkingdom <player>   - Accept one player
/reqdenykingdom <player>     - Deny one player
/reqacceptallkingdom         - Accept everyone
/reqdenyallkingdom          - Deny everyone
```

### Managing Your Kingdom
```
/kingdomlist                 - See all members
/kickkingdom <player>        - Remove a member
/renamekingdom <new_name>    - Change kingdom name
/reqoffkingdom              - Turn off join requests
/reqonkingdom               - Turn on join requests
```

### Your Kingdom Protection
- **First 3 days**: You cannot be killed by other players
- After 3 days: Full warfare enabled
- You can still die to mobs/environment

---

## 🛠️ For Event Staff

### Creating Kingdoms
1. Award kingdom spots through your events/RP
2. When ready: `/createkingdom <name> <player_name>`
3. The player becomes the kingdom leader automatically
4. They get 3 days of protection

### Managing Kingdoms
```
/createkingdom <name> <leader>    - Create new kingdom
/deletekingdom <kingdom>          - Remove a kingdom
/transferkingdom <kingdom> <new_leader> - Change leadership
/revive <player>                  - Revive if death was a mistake
```

### When a King Dies
1. You'll get a notification
2. Decide the next leader through RP/politics
3. Use: `/transferkingdom <kingdom_name> <new_leader_name>`
4. New leader must already be a kingdom member

---

## 📋 Key Rules

### Death System
- ☠️ Death is permanent (unless staff revives)
- 👻 Dead players are in spectator mode
- 🎭 Must create new character to respawn
- 🏰 Death removes you from your kingdom

### Kingdom Rules
- 🛡️ New kingdoms: 3 days protection
- ⚔️ Cannot attack during protection period
- 👑 Leader death = event staff picks successor
- 🚪 Auto-removed from kingdom when you die

### Character Creation
- ✏️ Name: Max 16 characters, letters/numbers/underscores only
- 🎨 Personality: Can be multiple words
- 🏠 Always spawn at world spawn
- ❤️ Start with full health

---

## 🎯 Example Scenarios

### Scenario 1: New Player Death
```
Player dies → Enters spectator mode
Types: /createcharacter Bjorn Warrior
Spawns at world spawn, ready to play!
```

### Scenario 2: Joining a Kingdom
```
Player: /joinkingdom Stormwind
Leader sees notification
Leader: /reqacceptkingdom PlayerName
Player gets accepted and joins!
```

### Scenario 3: King Dies
```
King dies in battle
Server broadcasts: "The king has died!"
Event staff get notification
Staff decides new leader through RP
Staff: /transferkingdom Stormwind NewLeaderName
Kingdom continues with new leadership
```

### Scenario 4: Creating a Kingdom
```
Player earns kingdom spot in event
Event Staff: /createkingdom Ironforge PlayerName
Server announces new kingdom
Player is now leader with 3-day protection
```

---

## ⚠️ Common Mistakes to Avoid

❌ Don't create character name with spaces: `/createcharacter John Smith Brave`
✅ Do this instead: `/createcharacter JohnSmith Brave`

❌ Don't try to join kingdom you're already in
✅ Check with `/kingdomlist` if you're a member

❌ Don't kick yourself as leader
✅ Use `/transferkingdom` to pass leadership first

❌ Don't forget personality in character creation
✅ Always include both name AND personality

---

## 🔧 Technical Info

**Data Location**: `plugins/KingdomCraft/kingdoms.json`

**Dependencies**: 
- Paper 1.21.1
- LuckPerms (for permissions)

**Built-in Protection**:
- Dead players cannot interact with world
- Protected leaders cannot be killed by players
- All data auto-saves

---

## 💡 Pro Tips

🔹 **For Players**: Choose meaningful character names that fit the RP!
🔹 **For Leaders**: Keep requests open to grow your kingdom
🔹 **For Event Staff**: Use protection period to help kingdoms establish
🔹 **For Everyone**: Death is permanent - be strategic in combat!

---

## 📞 Getting Help

**In-Game Help**: Ask event staff or type the command without arguments
**Technical Issues**: Check server console for errors
**RP Questions**: Consult with event staff about leadership succession

---

Made with ⚔️ for KingdomCraft Server
