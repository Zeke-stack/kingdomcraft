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
# FORCE_SEED: When set, the Docker image world OVERWRITES the volume
# Use this when your local world has the correct builds
FORCE_SEED=${FORCE_SEED:-"true"}

if [ "$FORCE_SEED" = "true" ]; then
    echo "=== FORCE SEED MODE ==="
    echo "Pushing Docker image world INTO persistent storage (your local builds win)..."
    mkdir -p /data
    cp -rf ./world /data/ && echo "  world -> /data: SEEDED" || echo "  world: FAILED"
    cp -rf ./world_the_end /data/ 2>/dev/null && echo "  end -> /data: SEEDED" || true
    echo "Volume now has your local world. Future deploys will restore from it."
    echo "=== SEED COMPLETE ==="
elif [ -d "/data/world" ] && [ "$(ls -A /data/world 2>/dev/null)" ]; then
    echo "Found existing world data in persistent storage!"
    echo "Wiping Docker image world and replacing with saved data..."
    
    # COMPLETELY replace image worlds with volume worlds
    rm -rf ./world
    cp -rf /data/world ./world
    echo "  world: RESTORED"
    
    if [ -d "/data/world_nether" ]; then
        rm -rf ./world_nether
        cp -rf /data/world_nether ./world_nether
        echo "  nether: RESTORED"
    fi
    if [ -d "/data/world_the_end" ]; then
        rm -rf ./world_the_end
        cp -rf /data/world_the_end ./world_the_end
        echo "  end: RESTORED"
    fi
    
    # Restore plugin data (configs, databases, etc.)
    if [ -d "/data/plugins" ]; then
        echo "Restoring plugin data..."
        for dir in /data/plugins/*/; do
            dirname=$(basename "$dir")
            mkdir -p "./plugins/$dirname"
            find "$dir" -maxdepth 1 -type f \( -name "*.yml" -o -name "*.json" -o -name "*.txt" -o -name "*.db" -o -name "*.properties" \) -exec cp -f {} "./plugins/$dirname/" \; 2>/dev/null || true
            find "$dir" -mindepth 1 -maxdepth 1 -type d -exec cp -rf {} "./plugins/$dirname/" \; 2>/dev/null || true
        done
        echo "  plugins: RESTORED"
    fi
    
    echo "World restore complete!"
else
    echo "No existing world data in /data — using fresh server files from Docker image."
    echo "Seeding volume with Docker image world for future restores..."
    mkdir -p /data
    cp -rf ./world /data/ && echo "  world: SEEDED" || echo "  world: FAILED"
    cp -rf ./world_the_end /data/ 2>/dev/null || true
    echo "(First deploy seeding done)"
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
# Disable spawn protection so players can build everywhere
sed -i 's/spawn-protection=[0-9]*/spawn-protection=0/' server.properties
grep -q "enable-rcon=true" server.properties && echo "RCON: enabled" || echo "RCON: FAILED to enable!"
grep -q "rcon.password=kc-rcon-2025" server.properties && echo "RCON password: set" || echo "RCON password: MISSING!"
grep -q "spawn-protection=0" server.properties && echo "Spawn protection: disabled" || echo "Spawn protection: FAILED to disable!"

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

    # Stop auto-save loop
    if [ -n "$AUTO_SAVE_PID" ] && kill -0 $AUTO_SAVE_PID 2>/dev/null; then
        kill $AUTO_SAVE_PID 2>/dev/null || true
    fi

    echo "=== SHUTDOWN COMPLETE ==="
    exit 0
}

# Trap SIGTERM and SIGINT so backup always runs
trap backup_and_exit SIGTERM SIGINT

# ── Periodic auto-save to persistent storage (every 5 minutes) ──
auto_save_loop() {
    sleep 120  # Wait 2 min for server to fully start
    while true; do
        sleep 300  # Every 5 minutes
        if [ -n "$MC_PID" ] && kill -0 $MC_PID 2>/dev/null; then
            echo "[AUTO-SAVE] Syncing world data to persistent storage..."
            mkdir -p /data
            cp -rf ./world /data/ 2>/dev/null && echo "[AUTO-SAVE] world: OK" || echo "[AUTO-SAVE] world: FAILED"
            cp -rf ./world_the_end /data/ 2>/dev/null || true
            cp -rf ./plugins /data/ 2>/dev/null && echo "[AUTO-SAVE] plugins: OK" || true
            echo "[AUTO-SAVE] Complete."
        else
            echo "[AUTO-SAVE] MC server not running, stopping auto-save loop."
            break
        fi
    done
}
auto_save_loop &
AUTO_SAVE_PID=$!
echo "Auto-save loop started (PID: $AUTO_SAVE_PID) — syncs to /data every 5 minutes"

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
