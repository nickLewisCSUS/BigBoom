@echo off
setlocal EnableDelayedExpansion

for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do ( 
    set "IP=%%a"
    set "IP=!IP:~1!"
	)

:: Menu for selecting the mode
echo Select the mode to run:
echo 1. Single-Player Mode (Non-Networked)
echo 2. Run as Server
echo 3. Run as Client
set /p mode="Enter choice (1-3): "

if "%mode%"=="1" (
	:: Single-Player Mode
	echo Launching in Single-Player Mode...
	java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 Client.MyGame !IP! 6010 UDP
	exit
)

if "%mode%"=="2" (
	:: Server Mode
	echo Starting Server Mode...

	echo Server IP Address: !IP!
	echo Share this IP Address with clients.

	echo Server Running, Press Ctrl + C to exit.
	java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 Server.NetworkingServer 6010 UDP
	exit
)

if "%mode%"=="3" (
	:: Client Mode
	set /p serverIP="Enter Server IP Address: "
	echo Launching Client Mode...
	java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 Client.MyGame !serverIP! 6010 UDP	
	exit
)


echo Invalid choice! Please run the script again and choose a valid option.

endlocal