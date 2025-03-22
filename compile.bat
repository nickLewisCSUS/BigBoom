@echo off
echo Compiling Client and Server files...

:: Delete old log files
if exist compile_client_errors.txt del compile_client_errors.txt
if exist compile_server_errors.txt del compile_server_errors.txt

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

:: Displaying errors in the terminal
type compile_client_errors.txt
type compile_server_errors.txt

pause