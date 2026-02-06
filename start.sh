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
    echo "Found existing world data in persistent storage, copying to server..."
    cp -r /data/world ./world 2>/dev/null || echo "World already present"
    cp -r /data/world_nether ./world_nether 2>/dev/null || echo "Nether already present"
    cp -r /data/world_the_end ./world_the_end 2>/dev/null || echo "End already present"
    cp -r /data/plugins ./plugins 2>/dev/null || echo "Plugins already present"
fi

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
