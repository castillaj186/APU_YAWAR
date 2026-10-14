@echo off
cd /d "%~dp0"
call gradlew.bat clean :apuyawar-systems:build :apuyawar-world:build
if errorlevel 1 (
 echo ERROR: Compilacion fallida. Revisa el mensaje superior.
 pause
 exit /b 1
)
echo MODS generados en las carpetas build\libs\ de cada modulo.
pause
