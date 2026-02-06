# KingdomCraft Plugin

A comprehensive kingdom management system with permanent death mechanics, character creation, and kingdom warfare.

## Features

### 🏰 Kingdom System
- Event staff can create kingdoms for players who obtain kingdom spots
- Kingdom leaders manage member requests and kingdom operations
- 3-day protection period for new kingdoms (no attacks, leader cannot be killed by players)
- Automatic removal from kingdom upon death

### ☠️ Death System
- **Permanent Death**: When a player dies, they enter spectator mode
- **Spectator Mode**: Players can fly and view the world but cannot interact
- **King Death**: Server-wide announcement in cursive when a kingdom leader dies
- **Character Reset**: Dead players must create a new character to continue playing

### 👤 Character Creation
- Players who die must use `/createcharacter <name> <personality>` to respawn
- Characters have custom names and personality traits
- Players respawn at world spawn with full health

### 👑 Leadership System
- Lottcha has full permissions as the starter king
- Can grant permissions to everyone
- Event staff select new leaders when a king dies (through roleplay politics)

## Commands

### Staff Commands (Event Staff)
- `/revive <player>` - Revive a dead player (for mistakes)
- `/createkingdom <name> <leader>` - Create a new kingdom for a player
- `/deletekingdom <kingdom>` - Delete a kingdom
- `/transferkingdom <kingdom> <player>` - Transfer leadership to another member

### Kingdom Leader Commands
- `/renamekingdom <name>` - Rename your kingdom
- `/reqlistkingdom` - View all join requests
- `/reqacceptkingdom <player>` - Accept a player's join request
- `/reqdenykingdom <player>` - Deny a player's join request
- `/reqacceptallkingdom` - Accept all pending requests
- `/reqdenyallkingdom` - Deny all pending requests
- `/kickkingdom <player>` - Remove a member from the kingdom
- `/reqoffkingdom` - Disable join requests
- `/reqonkingdom` - Enable join requests
- `/kingdomlist` - List all kingdom members

### Player Commands
- `/createcharacter <name> <personality>` - Create a new character after death
  - Example: `/createcharacter Arthur Brave`
- `/joinkingdom <kingdom>` - Request to join a kingdom
- `/leavekingdom` - Leave your current kingdom

## Permissions

### Staff Permissions
- `kingdomcraft.staff.revive` - Revive dead players
- `kingdomcraft.staff.createkingdom` - Create kingdoms
- `kingdomcraft.staff.deletekingdom` - Delete kingdoms
- `kingdomcraft.staff.transferkingdom` - Transfer kingdom ownership

### Leader Permissions (automatically granted)
- `kingdomcraft.leader` - Access to leader commands
- `kingdomcraft.leader.rename` - Rename kingdom

## Kingdom Protection

New kingdoms receive 3 days of protection:
- Kingdom cannot be attacked or go to war
- Leader cannot be killed by other players
- Leader can still die to environment/mobs
- Protection timer displayed in messages

## Death Mechanics Details

### When a Player Dies:
1. Player is put into spectator mode
2. Player can fly and view the world (invisible, no collision)
3. All potion effects are cleared
4. Player is removed from their kingdom (if any)
5. Title message appears: "You have died"
6. Instructions for `/createcharacter` command shown

### When a King Dies:
1. Server broadcasts in cursive: "*The king has died!*"
2. Event staff are notified to select a new leader
3. Kingdom remains intact, awaiting new leadership
4. Staff use `/transferkingdom` to appoint successor

### Character Creation:
1. Player types `/createcharacter <name> <personality>`
2. Character name validated (16 chars max, alphanumeric + underscore)
3. Player teleported to spawn
4. Full health and hunger restored
5. Set to survival mode
6. Welcome message with character details

## Data Storage

All kingdom and player data is stored in:
```
plugins/KingdomCraft/kingdoms.json
```

Data includes:
- Kingdom information (name, leader, members, requests)
- Player character data (name, personality, dead status)
- Protection timestamps

## Installation

1. Plugin is automatically compiled during Docker build
2. Or manually build with: `./build-plugins.sh` or `build-plugins.bat`
3. Download and install LuckPerms
4. Run commands from `plugins/LuckPerms/setup-permissions.txt`
5. Restart server

## Integration with LuckPerms

Event staff permissions are configured in LuckPerms:
```
lp group eventstaff permission set kingdomcraft.staff.revive true
lp group eventstaff permission set kingdomcraft.staff.createkingdom true
lp group eventstaff permission set kingdomcraft.staff.deletekingdom true
lp group eventstaff permission set kingdomcraft.staff.transferkingdom true
```

Lottcha gets full access:
```
lp user Lottcha permission set * true
lp user Lottcha parent add founder
```

## Developer Notes

### Technologies
- Paper API 1.21.1
- Adventure API for chat/titles
- Gson for JSON data storage
- Maven for building

### Architecture
- `KingdomCraft.java` - Main plugin class
- `data/` - Kingdom and player data management
- `commands/` - All command handlers
- `listeners/` - Death and protection event handlers

## Troubleshooting

**Player stuck in spectator mode?**
- Staff can use `/revive <player>` to fix

**Kingdom not showing up?**
- Check `plugins/KingdomCraft/kingdoms.json`
- Verify LuckPerms permissions are set

**Leader died but no notification?**
- Check server logs for errors
- Verify player was actually kingdom leader

## Support

For issues or questions, contact event staff in-game.
