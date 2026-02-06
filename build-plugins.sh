#!/bin/bash

# Staff System Build Script
# This script compiles both custom plugins

echo "╔════════════════════════════════════════╗"
echo "║  KingdomCraft Staff System Builder    ║"
echo "╚════════════════════════════════════════╝"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed!"
    echo "Please install Maven: https://maven.apache.org/install.html"
    exit 1
fi

echo "✓ Maven detected"
echo ""

# Build AdvancedInvViewer
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Building AdvancedInvViewer..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd plugins/AdvancedInvViewer
mvn clean package -q
if [ $? -eq 0 ]; then
    cp target/AdvancedInvViewer.jar ../../AdvancedInvViewer.jar
    echo "✓ AdvancedInvViewer.jar created"
else
    echo "❌ Failed to build AdvancedInvViewer"
    exit 1
fi
cd ../..
echo ""

# Build StaffCommands
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Building StaffCommands..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd plugins/StaffCommands
mvn clean package -q
if [ $? -eq 0 ]; then
    cp target/StaffCommands.jar ../../StaffCommands.jar
    echo "✓ StaffCommands.jar created"
else
    echo "❌ Failed to build StaffCommands"
    exit 1
fi
cd ../..
echo ""

# Build KingdomCraft
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Building KingdomCraft..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd plugins/KingdomCraft
mvn clean package -q
if [ $? -eq 0 ]; then
    cp target/KingdomCraft.jar ../../KingdomCraft.jar
    echo "✓ KingdomCraft.jar created"
else
    echo "❌ Failed to build KingdomCraft"
    exit 1
fi
cd ../..
echo ""

echo "╔════════════════════════════════════════╗"
echo "║  ✓ Build Complete!                    ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "Plugins created in plugins/ directory:"
echo "  • AdvancedInvViewer.jar"
echo "  • StaffCommands.jar"
echo "  • KingdomCraft.jar"
echo ""
echo "Next steps:"
echo "1. Download LuckPerms and place in plugins/"
echo "2. Restart the server"
echo "3. Run setup commands from plugins/LuckPerms/setup-permissions.txt"
echo ""
