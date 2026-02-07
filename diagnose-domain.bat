@echo off
REM Minecraft Server Domain Troubleshooting for continents.cc
REM Run this to diagnose domain/connection issues

echo.
echo ======================================
echo Minecraft Domain Troubleshooting
echo continents.cc
echo ======================================
echo.

REM Test 1: DNS Resolution
echo [1/5] Testing DNS Resolution...
nslookup continents.cc
echo.

REM Test 2: Detailed DNS lookup
echo [2/5] Detailed DNS lookup for continents.cc...
nslookup continents.cc 8.8.8.8
echo.

REM Test 3: Test port connectivity
echo [3/5] Testing port 25565 connectivity...
echo Attempting connection to continents.cc:25565...
timeout /t 2 >nul
powershell -Command "Try { $connection = New-Object System.Net.Sockets.TcpClient; $connection.Connect('continents.cc', 25565); if($connection.Connected) { Write-Host 'SUCCESS: Port 25565 is reachable!' -ForegroundColor Green } $connection.Close() } Catch { Write-Host 'FAILED: Cannot reach port 25565' -ForegroundColor Red; Write-Host $_.Exception.Message }"
echo.

REM Test 4: Check what IP it resolves to
echo [4/5] Getting resolved IP address...
powershell -Command "try { $ip = [System.Net.Dns]::GetHostAddresses('continents.cc') | Select-Object -First 1; Write-Host "Resolved IP: $ip" } catch { Write-Host 'Could not resolve domain' -ForegroundColor Red }"
echo.

REM Test 5: Information to provide
echo [5/5] Collecting information...
echo.
echo IMPORTANT INFORMATION NEEDED:
echo ======================================
echo Please provide the following from Railway dashboard:
echo.
echo 1. Your Railway Public URL:
echo    (Go to: KingdomCraft service ^> Networking ^> Public URL)
echo    Format: your-project-XXXXX.railway.app
echo.
echo 2. Is the KingdomCraft service RUNNING?
echo    (Check in Railway dashboard - should show green status)
echo.
echo 3. Copy the results above and share them
echo.
echo ======================================
pause
