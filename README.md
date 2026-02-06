# KingdomCraft Server - Railway Deployment

A realistic Minecraft 1.21.1 server with custom kingdom management, permanent death mechanics, and comprehensive staff systems. Designed for roleplay and kingdom warfare.

## 🎮 Server Features

### Kingdom System
- **Custom Kingdom Plugin**: Full kingdom management with leader controls
- **3-Day Protection**: New kingdoms get protection from attacks and leader death
- **Join Requests**: Players request to join, leaders approve/deny
- **Event Staff Control**: Staff create kingdoms and manage leadership succession

### Death & Character System  
- **Permanent Death**: Players enter spectator mode when they die
- **Character Creation**: Dead players create new characters with `/createcharacter <name> <personality>`
- **King Death Announcements**: Server-wide broadcast when kingdom leaders die
- **Staff Revive**: Event staff can revive players if death was a mistake

### Staff Management
- **4 Permission Groups**: Moderator, Administrator, Event Staff, Founder
- **Custom Staff Commands**: Spectate, freeze, teleport, bring, chat history, audit logging
- **Advanced Inventory Viewer**: Real-time inventory inspection (10 updates/second)
- **Audit System**: All staff actions logged to JSON

## 📦 Deployment

### Railway Setup

1. **Connect Repository**
   ```
   https://github.com/Zeke-stack/kingdomcraft.git
   ```

2. **Add Persistent Storage**
   - Mount point: `/data`
   - Recommended size: 5GB+

3. **Environment Variables**
   ```
   MEMORY=4G          # Recommended: 4G or higher
   MIN_MEMORY=2G      # Minimum allocation
   ```

4. **Deploy**
   - Railway will automatically build the Docker image
   - Plugins compile during build (Maven)
   - Server starts with Aikar's performance flags

### Ports
- **25565/TCP** - Minecraft server
- **24454/UDP** - Voice chat (optional)

## 🔌 Plugins

### Custom Plugins (Built-in)
1. **KingdomCraft** - Kingdom management and death system
2. **StaffCommands** - Staff moderation tools
3. **AdvancedInvViewer** - Real-time inventory viewing

### Required (Download Separately)
- **LuckPerms** - Permission management ([Download](https://download.luckperms.net/))

### Included
- **CoreProtect** - Block logging and rollback
- **ItemBlocker** - Prevent specific items
- **MobsDespawn** - Custom mob despawn rules
- **WorldEdit** - World editing
- **Simple Voice Chat** - Proximity voice

## ⚙️ Setup Instructions

### First Time Setup

1. **Deploy to Railway** (server starts automatically)

2. **Download LuckPerms**
   ```
   Download from: https://download.luckperms.net/
   Place in: plugins/LuckPerms-Bukkit-5.4.141.jar
   ```

3. **Restart Server** (LuckPerms loads)

4. **Configure Permissions**
   ```
   Run commands from: plugins/LuckPerms/setup-permissions.txt
   ```

5. **Server Ready!**
   - Lottcha has full permissions as starter king
   - Event staff can create kingdoms
   - Players can join and create characters

### Local Development

```bash
# Build all custom plugins
./build-plugins.sh         # Linux/Mac
build-plugins.bat          # Windows

# Plugins output to plugins/ directory
# - KingdomCraft.jar
# - AdvancedInvViewer.jar
# - StaffCommands.jar
```

## 📖 Documentation

- **[KingdomCraft Guide](KINGDOMCRAFT_GUIDE.md)** - Complete guide for players, leaders, and staff
- **[Plugin README](plugins/KingdomCraft/README.md)** - Technical documentation
- **[Setup Permissions](plugins/LuckPerms/setup-permissions.txt)** - LuckPerms configuration

## 🎯 Quick Command Reference

### For Players
```
/createcharacter <name> <personality>  - Create character after death
/joinkingdom <kingdom>                 - Request to join kingdom
/leavekingdom                          - Leave your kingdom
```

### For Kingdom Leaders
```
/reqlistkingdom              - View join requests
/reqacceptkingdom <player>   - Accept request
/kingdomlist                 - List members
/kickkingdom <player>        - Remove member
/renamekingdom <name>        - Rename kingdom
```

### For Event Staff
```
/createkingdom <name> <leader>          - Create kingdom
/deletekingdom <kingdom>                - Delete kingdom
/transferkingdom <kingdom> <player>     - Transfer leadership
/revive <player>                        - Revive dead player
```

## 🛡️ Permissions & Groups

### Lottcha (Starter King)
- Full server access (`*` permission)
- Founder group
- Can grant permissions to everyone

### Event Staff
- Kingdom creation/deletion
- Leadership transfers
- Player revival
- All administrator permissions

### Administrator
- Player bans/unbans
- Inventory modification
- All moderator permissions

### Moderator
- Player kicks
- Inventory viewing
- Basic moderation

## 🔧 Technical Details

### Server Software
- **Paper 1.21.1-133** (Optimized Minecraft server)
- **Java 21** (Eclipse Temurin JRE)
- **Aikar's Flags** (G1GC tuning for performance)

### Build System
- **Docker Multi-Stage Build**
  - Stage 1: Maven compilation
  - Stage 2: Java runtime
- **Automatic Plugin Compilation**

### Data Persistence
- **World Saves**: Synced to `/data` volume on startup/shutdown
- **Plugin Data**: Stored in `/data/plugins/`
- **Kingdom Data**: `plugins/KingdomCraft/kingdoms.json`

### Performance
- **Memory**: Configurable via `MEMORY` env var
- **GC**: G1GC with Aikar's tuning
- **Flags**: Optimized for 4GB+ RAM

## 📁 Project Structure

```
transfer/
├── Dockerfile              - Railway deployment
├── start.sh               - Startup script with save management
├── railway.json           - Railway configuration
├── KINGDOMCRAFT_GUIDE.md  - User guide
├── plugins/
│   ├── KingdomCraft/      - Custom kingdom plugin
│   ├── StaffCommands/     - Custom staff plugin
│   ├── AdvancedInvViewer/ - Custom inventory plugin
│   ├── LuckPerms/         - Permission setup (download separately)
│   ├── CoreProtect/       - Block logging
│   └── ...other plugins
└── world/                 - Main world data
```

## 🚀 Deployment Flow

1. **Push to GitHub** → Triggers Railway build
2. **Docker Build** → Compiles plugins with Maven
3. **Container Start** → Runs start.sh
4. **Save Sync** → Copies world from /data if exists
5. **Server Start** → Paper server with custom plugins
6. **On Stop** → Saves world back to /data

## ⚠️ Important Notes

### Kingdom Protection System
- New kingdoms: **3 days protection**
- Leader cannot be killed by players during protection
- Can still die to mobs/environment
- After protection: Full PvP/warfare enabled

### Death Mechanics
- **Permanent**: No respawning without character creation
- **Spectator Mode**: Can fly and view, but not interact
- **Kingdom Removal**: Auto-removed from kingdom on death
- **Staff Override**: Event staff can revive if needed

### Character Creation
- Names: 16 chars max, alphanumeric + underscore
- Personality: Multiple words allowed
- Spawn: Always at world spawn
- Fresh start: Full health, survival mode

## 🐛 Troubleshooting

**Server won't start?**
- Check Railway logs for errors
- Verify MEMORY env var is set
- Ensure /data volume is mounted

**Player stuck in spectator?**
- Event staff: `/revive <player>`

**Kingdom not saving?**
- Check `plugins/KingdomCraft/kingdoms.json` exists
- Verify /data volume persistence

**Permissions not working?**
- Ensure LuckPerms is installed
- Run setup commands from setup-permissions.txt
- Restart server after permission changes

## 📊 Server Info

- **Version**: 1.21.1 (Paper 133)
- **Java**: 21 (Eclipse Temurin)
- **Platform**: Railway (Docker)
- **Region**: Configurable in Railway
- **Uptime**: Auto-restart on crash (max 10 retries)

## 👥 Staff Structure

1. **Founder** (Lottcha)
   - Full access
   - Can grant all permissions
   - Starter king

2. **Event Staff**
   - Kingdom management
   - Leadership succession
   - Player revival
   - All admin powers

3. **Administrator**
   - Player moderation
   - Inventory management
   - All mod powers

4. **Moderator**
   - Player kicks
   - Inventory viewing
   - Basic moderation

## 📞 Support

- **In-Game**: Contact event staff
- **Technical**: Check server console logs
- **GitHub**: https://github.com/Zeke-stack/kingdomcraft

---

**Made for KingdomCraft - A Realistic Minecraft Experience** ⚔️
