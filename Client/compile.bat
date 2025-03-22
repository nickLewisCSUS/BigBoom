@echo off
echo Compiling Client and Server files...

:: Compiling Client Files
javac -Xlint:unchecked Client/*.java > compile_client_errors.txt 2>&1
if %errorlevel% neq 0 (
	echo Errors found in client compilation. Check compile_client_errors.txt for details.
) else (
	echo Client files compiled successfully.
)

:: Compiling Server Files
javac -Xlint:unchecked Server/*.java > compile_server_errors.txt 2>&1
if %errorlevel% neq 0 (
	echo Errors found in server compilation. Check compile_server_errors.txt for details.
) else (
	echo Server files compiled successfully.
)