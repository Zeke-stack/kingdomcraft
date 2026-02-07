#!/bin/bash

# Minecraft Server Startup Script for Railway
echo "Starting Minecraft Server..."

# Set memory based on Railway's available memory (usually 512MB-8GB)
# Adjust these values based on your Railway plan
MEMORY=${MEMORY:-"2G"}
MIN_MEMORY=${MIN_MEMORY:-"1G"}

echo "Allocating ${MIN_MEMORY} to ${MEMORY} of RAM"

# Accept EULA automatically
echo "eula=true" > eula.txt

# Ensure world data is properly loaded
if [ -d "/data/world" ]; then
    echo "Found existing world data in persistent storage, syncing..."
    # Merge persistent data INTO server dir (persistent data wins for world chunks)
    cp -rn /data/world/* ./world/ 2>/dev/null || true
    cp -rn /data/world_nether/* ./world_nether/ 2>/dev/null || true
    cp -rn /data/world_the_end/* ./world_the_end/ 2>/dev/null || true
    
    # For playerdata, persistent storage always wins (latest inventories)
    if [ -d "/data/world/playerdata" ]; then
        echo "Restoring player data from persistent storage..."
        cp -rf /data/world/playerdata/* ./world/playerdata/ 2>/dev/null || true
    fi
    
    # Restore plugin data from persistent storage
    if [ -d "/data/plugins" ]; then
        echo "Restoring plugin data from persistent storage..."
        for dir in /data/plugins/*/; do
            dirname=$(basename "$dir")
            # Only copy plugin config/data, not plugin jars (those come from Docker build)
            if [ -d "$dir" ] && [ "$dirname" != "AdvancedInvViewer" ] && [ "$dirname" != "StaffCommands" ] && [ "$dirname" != "KingdomCraft" ]; then
                cp -rf "$dir" ./plugins/ 2>/dev/null || true
            else
                # For custom plugins, only copy data files not the source
                mkdir -p "./plugins/$dirname"
                find "$dir" -maxdepth 1 -type f \( -name "*.yml" -o -name "*.json" -o -name "*.txt" -o -name "*.db" \) -exec cp -f {} "./plugins/$dirname/" \; 2>/dev/null || true
            fi
        done
    fi
else
    echo "No existing world data found, using fresh server files."
fi

# Disable broken datapack (no_underground_lava) to prevent registry errors
if [ -d "./world/datapacks/no_underground_lava" ]; then
    echo "Disabling no_underground_lava datapack (registry error fix)..."
    rm -rf ./world/datapacks/no_underground_lava
fi

# Always use the latest server.properties from Docker image (ensures RCON etc. stay enabled)
echo "Applying latest server.properties..."
cp -f /tmp/server.properties.bak ./server.properties 2>/dev/null || echo "No server.properties backup found"

# Start the server
java -Xms${MIN_MEMORY} -Xmx${MEMORY} \
    -XX:+UseG1GC \
    -XX:+ParallelRefProcEnabled \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+DisableExplicitGC \
    -XX:+AlwaysPreTouch \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1HeapRegionSize=8M \
    -XX:G1ReservePercent=20 \
    -XX:G1HeapWastePercent=5 \
    -XX:G1MixedGCCountTarget=4 \
    -XX:InitiatingHeapOccupancyPercent=15 \
    -XX:G1MixedGCLiveThresholdPercent=90 \
    -XX:G1RSetUpdatingPauseTimePercent=5 \
    -XX:SurvivorRatio=32 \
    -XX:+PerfDisableSharedMem \
    -XX:MaxTenuringThreshold=1 \
    -Dusing.aikars.flags=https://mcflags.emc.gs \
    -Daikars.new.flags=true \
    -jar paper-1.21.1-133.jar nogui

# Save world data back to persistent storage on shutdown
echo "Server stopped, backing up world data..."
if [ ! -d "/data" ]; then
    mkdir -p /data
fi

cp -r ./world /data/ 2>/dev/null || echo "Failed to backup world"
cp -r ./world_nether /data/ 2>/dev/null || echo "Failed to backup nether"
cp -r ./world_the_end /data/ 2>/dev/null || echo "Failed to backup end"
cp -r ./plugins /data/ 2>/dev/null || echo "Failed to backup plugins"

echo "Backup complete!"
