@echo off
echo ===================================================
echo    Philippine Express Bus Ticketing System
echo    CC 104 - Java Programming Final Project
echo ===================================================
echo.

if exist out rmdir /s /q out
mkdir out

echo Compiling source files...
javac -d out src\ticketing\*.java

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed. Make sure Java JDK is installed.
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Launching ticketing system...
java -cp out;src ticketing.TicketingSystemGUI
