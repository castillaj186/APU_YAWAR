@echo off
cd /d "%~dp0"
call gradlew.bat :apuyawar-systems:runClient
if errorlevel 1 pause
