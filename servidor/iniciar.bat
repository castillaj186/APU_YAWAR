@echo off
setlocal
title APU YAWAR - EL JUICIO DE LOS DIOSES - SERVIDOR
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java no esta instalado o no figura en PATH.
    pause
    exit /b 1
)

echo ==============================================================
echo  APU YAWAR - SERVIDOR DE PRUEBAS - MINECRAFT FABRIC 1.21.1
echo ==============================================================
java -version
java -Xms2G -Xmx3G -jar fabric-server.jar nogui

if errorlevel 1 echo Se detecto un problema al cerrar el servidor.
echo.
echo Servidor detenido. Revisa los logs si hubo errores.
pause
