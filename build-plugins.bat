@echo off
REM Staff System Build Script for Windows
REM This script compiles both custom plugins

echo ╔════════════════════════════════════════╗
echo ║  KingdomCraft Staff System Builder    ║
echo ╚════════════════════════════════════════╝
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven is not installed!
    echo Please install Maven: https://maven.apache.org/install.html
    pause
    exit /b 1
)

echo ✓ Maven detected
echo.

REM Build AdvancedInvViewer
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo Building AdvancedInvViewer...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
cd plugins\AdvancedInvViewer
call mvn clean package -q
if %ERRORLEVEL% EQU 0 (
    copy target\AdvancedInvViewer.jar ..\..\AdvancedInvViewer.jar >nul
    echo ✓ AdvancedInvViewer.jar created
) else (
    echo ❌ Failed to build AdvancedInvViewer
    cd ..\..
    pause
    exit /b 1
)
cd ..\..
echo.

REM Build StaffCommands
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo Building StaffCommands...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
cd plugins\StaffCommands
call mvn clean package -q
if %ERRORLEVEL% EQU 0 (
    copy target\StaffCommands.jar ..\..\StaffCommands.jar >nul
    echo ✓ StaffCommands.jar created
) else (
    echo ❌ Failed to build StaffCommands
    cd ..\..
    pause
    exit /b 1
)
cd ..\..
echo.

echo ╔════════════════════════════════════════╗
echo ║  ✓ Build Complete!                    ║
echo ╚════════════════════════════════════════╝
echo.
echo Plugins created in plugins\ directory:
echo   • AdvancedInvViewer.jar
echo   • StaffCommands.jar
echo.
echo Next steps:
echo 1. Download LuckPerms and place in plugins\
echo 2. Restart the server
echo 3. Run setup commands from plugins\LuckPerms\setup-permissions.txt
echo.
pause
