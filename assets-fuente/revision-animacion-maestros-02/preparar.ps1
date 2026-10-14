param([Parameter(Mandatory=$true)][string]$Salida)
$ErrorActionPreference = 'Stop'
$assetsRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$destination = [IO.Path]::GetFullPath($Salida)
if (-not $destination.StartsWith($assetsRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'La salida debe estar dentro de assets-fuente.'
}
if (Test-Path -LiteralPath $destination) { throw 'La salida ya existe; no se sobrescribe.' }
$textures = Join-Path $destination 'texturas'
New-Item -ItemType Directory -Path $destination, $textures | Out-Null
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -Path (Join-Path $PSScriptRoot 'PrepararMaestros.cs'), (Join-Path $PSScriptRoot 'ValidarMaestros.cs')
[PrepararMaestros]::Layers((Join-Path $assetsRoot 'referencias'), $textures)
[PrepararMaestros]::Masters((Join-Path $assetsRoot 'referencias'), (Join-Path $assetsRoot 'revision-texturas-01\sector-rojo-maestro\sector_rojo.png'), $textures)
[ValidarMaestros]::Run($textures)
# Genera únicamente 13 maestros de prueba; no integra ni cambia originales.
