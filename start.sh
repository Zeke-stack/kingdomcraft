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
    
    # Persistent storage has the REAL latest data — it always wins
    # Copy everything from persistent storage, overwriting Docker image files
    cp -rf /data/world/* ./world/ 2>/dev/null || true
    cp -rf /data/world_nether/* ./world_nether/ 2>/dev/null || true
    cp -rf /data/world_the_end/* ./world_the_end/ 2>/dev/null || true
    
    # Explicitly ensure playerdata, advancements, and stats are restored
    if [ -d "/data/world/playerdata" ]; then
        echo "Restoring player data from persistent storage..."
        mkdir -p ./world/playerdata
        cp -rf /data/world/playerdata/* ./world/playerdata/ 2>/dev/null || true
    fi
    if [ -d "/data/world/advancements" ]; then
        echo "Restoring advancements from persistent storage..."
        mkdir -p ./world/advancements
        cp -rf /data/world/advancements/* ./world/advancements/ 2>/dev/null || true
    fi
    if [ -d "/data/world/stats" ]; then
        echo "Restoring stats from persistent storage..."
        mkdir -p ./world/stats
        cp -rf /data/world/stats/* ./world/stats/ 2>/dev/null || true
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

# Force-enable RCON in server.properties (in case persistent volume overwrote it)
echo "Ensuring RCON is enabled..."
sed -i 's/enable-rcon=false/enable-rcon=true/' server.properties
sed -i 's/rcon.password=$/rcon.password=kc-rcon-2025/' server.properties
grep -q "enable-rcon=true" server.properties && echo "RCON: enabled" || echo "RCON: FAILED to enable!"
grep -q "rcon.password=kc-rcon-2025" server.properties && echo "RCON password: set" || echo "RCON password: MISSING!"

# Start Discord bot in background (uses localhost RCON since it's in the same container)
echo "Starting Discord bot..."
export RCON_HOST=localhost
export RCON_PORT=25575
export RCON_PASSWORD=kc-rcon-2025
cd /server/discord-bot && node index.js &
DISCORD_BOT_PID=$!
echo "Discord bot started (PID: $DISCORD_BOT_PID)"
cd /server

# Set the Discord bot URL for the MC plugin (localhost since same container)
export DISCORD_BOT_URL=http://localhost:3000

# Ensure Lottcha always has OP (re-add on every startup)
echo "Ensuring Lottcha has OP..."
# ops.json is loaded by the server on start, make sure Lottcha is in it
python3 -c "
import json, sys
try:
    with open('ops.json') as f: ops = json.load(f)
except: ops = []
uuids = [o['uuid'] for o in ops]
if '5bbf23d0-e968-4e47-854a-02090deeba3a' not in uuids:
    ops.append({'uuid':'5bbf23d0-e968-4e47-854a-02090deeba3a','name':'Lottcha','level':4,'bypassesPlayerLimit':True})
    with open('ops.json','w') as f: json.dump(ops,f,indent=2)
    print('Lottcha added to ops.json')
else:
    # Ensure bypass is set
    for o in ops:
        if o['uuid'] == '5bbf23d0-e968-4e47-854a-02090deeba3a':
            o['bypassesPlayerLimit'] = True
            o['level'] = 4
    with open('ops.json','w') as f: json.dump(ops,f,indent=2)
    print('Lottcha already in ops.json (verified)')
" 2>/dev/null || echo "Python not available, ops.json unchanged"

# ── Shutdown handler: backup world data to persistent storage ──
backup_and_exit() {
    echo ""
    echo "=== SHUTDOWN SIGNAL RECEIVED ==="
    
    # Tell the MC server to save and stop
    if [ -n "$MC_PID" ] && kill -0 $MC_PID 2>/dev/null; then
        echo "Sending 'stop' to MC server..."
        # Save all first
        kill -SIGTERM $MC_PID 2>/dev/null || true
        # Wait for MC server to exit (up to 30 seconds)
        for i in $(seq 1 30); do
            if ! kill -0 $MC_PID 2>/dev/null; then
                echo "MC server stopped after ${i}s."
                break
            fi
            sleep 1
        done
        # Force kill if still alive
        if kill -0 $MC_PID 2>/dev/null; then
            echo "Force killing MC server..."
            kill -9 $MC_PID 2>/dev/null || true
        fi
    fi

    echo "Backing up world data to persistent storage..."
    mkdir -p /data

    cp -rf ./world /data/ 2>/dev/null && echo "  world: OK" || echo "  world: FAILED"
    cp -rf ./world_nether /data/ 2>/dev/null && echo "  nether: OK" || echo "  nether: SKIPPED"
    cp -rf ./world_the_end /data/ 2>/dev/null && echo "  end: OK" || echo "  end: SKIPPED"
    cp -rf ./plugins /data/ 2>/dev/null && echo "  plugins: OK" || echo "  plugins: FAILED"

    echo "Backup complete!"

    # Stop Discord bot
    if [ -n "$DISCORD_BOT_PID" ] && kill -0 $DISCORD_BOT_PID 2>/dev/null; then
        echo "Stopping Discord bot..."
        kill $DISCORD_BOT_PID 2>/dev/null || true
    fi

    echo "=== SHUTDOWN COMPLETE ==="
    exit 0
}

# Trap SIGTERM and SIGINT so backup always runs
trap backup_and_exit SIGTERM SIGINT

# Start the server in the background so we can trap signals
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
    -jar paper-1.21.1-133.jar nogui &
MC_PID=$!
echo "MC server started (PID: $MC_PID)"

# Wait for the MC server process — if it exits on its own, also backup
wait $MC_PID 2>/dev/null
MC_EXIT=$?
echo "MC server exited with code: $MC_EXIT"

# Run backup (in case server exited on its own, not via signal)
backup_and_exit
