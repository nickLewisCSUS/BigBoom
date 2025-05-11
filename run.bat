@echo off
setlocal EnableDelayedExpansion

for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    set "IP=%%a"
    set "IP=!IP:~1!"
)

:: Menu
echo Select the mode to run:
echo 1. Single-Player Mode (Non-Networked)
echo 2. Run as Server
echo 3. Run as Client
goto :menuchoice

:menuchoice
set /p mode="Enter choice (1-3): "

if "%mode%"=="1" goto :singleplayer
if "%mode%"=="2" goto :server
if "%mode%"=="3" goto :client

echo Invalid choice! Please choose a valid option.
goto :menuchoice

:singleplayer
echo.
echo --- Tank Selection ---
echo 1. Fast Tank (default)
echo 2. Slow Tank
set /p tankChoice="Enter tank choice (1 or 2): "
if "!tankChoice!"=="2" (
    set "tankType=slow"
) else (
    set "tankType=fast"
)
echo Launching in Single-Player Mode with !tankType! tank...
java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 Client.MyGame !IP! 6010 UDP !tankType!
goto :eof

:server
echo.
echo Starting Server Mode...
echo Select IP mode:
echo 1. Home Developer (defaultm first IPv4 found)
echo 2. School Demo (130.68.xxx.xxx)
goto :serverchoice

:serverchoice
set /p ipmode="Enter choice (1-2): "
set IP=
if "%ipmode%" == "1" goto :homedeveloper
if "%ipmode%" == "2" goto :schooldemo

echo Invalid choice! Please choose a valid option.
goto :serverchoice

:homedeveloper
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    set "IP=%%a"
    goto :gotIP   
)

:schooldemo
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    echo %%a | findstr "130.86." >nul
    if not errorlevel 1 (
        set "IP=%%a"
        goto :gotIP
    )
)

:gotIP
set "IP=!IP:~1!"
echo Server IP Address: !IP!
echo Share this IP Address with clients.
echo Server Running, Press Ctrl + C to exit.
java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 Server.NetworkingServer 6010 UDP
goto :eof

:client
set /p serverIP="Enter Server IP Address: "
echo.
echo --- Tank Selection ---
echo 1. Fast Tank (default)
echo 2. Slow Tank
set /p tankChoice="Enter tank choice (1 or 2): "
if "!tankChoice!"=="2" (
    set "tankType=slow"
) else (
    set "tankType=fast"
)
echo Launching Client Mode with !tankType! tank...
java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 Client.MyGame !serverIP! 6010 UDP !tankType!
goto :eof