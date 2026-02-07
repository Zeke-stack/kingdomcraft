#!/bin/bash
# Minecraft Server Domain Troubleshooting for continents.cc
# Run this to diagnose domain/connection issues

echo ""
echo "======================================"
echo "Minecraft Domain Troubleshooting"
echo "continents.cc"
echo "======================================"
echo ""

# Test 1: DNS Resolution
echo "[1/5] Testing DNS Resolution..."
nslookup continents.cc
echo ""

# Test 2: Detailed DNS lookup
echo "[2/5] Detailed DNS lookup..."
dig continents.cc
echo ""

# Test 3: Test port connectivity
echo "[3/5] Testing port 25565 connectivity..."
if timeout 5 bash -c '</dev/tcp/continents.cc/25565' 2>/dev/null; then
    echo "✓ SUCCESS: Port 25565 is reachable!"
else
    echo "✗ FAILED: Cannot reach port 25565"
fi
echo ""

# Test 4: Check resolved IP
echo "[4/5] Getting resolved IP address..."
nslookup continents.cc | grep "Address:" | tail -1
echo ""

# Test 5: Information needed
echo "[5/5] Information Needed"
echo "======================================"
echo "Please provide the following:"
echo ""
echo "1. Your Railway Public URL:"
echo "   (Go to: KingdomCraft service > Networking > Public URL)"
echo "   Format: your-project-XXXXX.railway.app"
echo ""
echo "2. Is KingdomCraft service RUNNING in Railway?"
echo ""
echo "3. Copy the test results above"
echo ""
echo "======================================"
