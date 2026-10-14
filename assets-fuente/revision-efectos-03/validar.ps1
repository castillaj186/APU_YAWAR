param([switch]$Simular)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$jdkBin = Join-Path $env:JAVA_HOME 'bin'
$sources = @(
    (Join-Path $projectRoot 'apuyawar-systems\src\main\java\pe\apuyawar\systems\roulette\RouletteTimeline.java'),
    (Join-Path $projectRoot 'apuyawar-systems\src\main\java\pe\apuyawar\systems\roulette\RoulettePalette.java'),
    (Join-Path $projectRoot 'apuyawar-systems\src\main\java\pe\apuyawar\systems\client\RouletteGem.java'),
    (Join-Path $PSScriptRoot 'EffectsCheck.java')
)
$classes = Join-Path $PSScriptRoot 'clases-test'
& (Join-Path $jdkBin 'javac.exe') -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'La compilación de las pruebas falló.' }
& (Join-Path $jdkBin 'java.exe') -cp $classes EffectsCheck (Join-Path $PSScriptRoot 'simulacion.csv')
if ($LASTEXITCODE -ne 0) { throw 'La validación de la timeline falló.' }
$resources = Join-Path $projectRoot 'apuyawar-systems\src\main\resources\assets\apuyawar_systems\textures\gui\roulette'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -Path (Join-Path $PSScriptRoot 'ValidarLuz.cs')
[ValidarLuz]::Run($resources)
if ($Simular) {
    Add-Type -ReferencedAssemblies System.Drawing -Path (Join-Path $PSScriptRoot 'SimularEfectos.cs')
    [SimularEfectos]::Run($resources, (Join-Path $PSScriptRoot 'simulacion.csv'), $PSScriptRoot)
    & ffmpeg -hide_banner -loglevel error -y -framerate 30 -i (Join-Path $PSScriptRoot 'frames-nueva\f%04d.png') -c:v libx264 -crf 20 -pix_fmt yuv420p -movflags +faststart (Join-Path $PSScriptRoot 'simulacion-nueva.mp4')
    if ($LASTEXITCODE -ne 0) { throw 'La generación del video falló.' }
}
# Escribe solamente artefactos de prueba en esta revisión; no cambia el mod ni referencias.
