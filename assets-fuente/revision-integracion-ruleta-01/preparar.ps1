param([Parameter(Mandatory=$true)][string]$Salida)
$ErrorActionPreference = 'Stop'
$assetsRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$destination = [IO.Path]::GetFullPath($Salida)
if (-not $destination.StartsWith($assetsRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'La salida debe ser una carpeta nueva dentro de assets-fuente.'
}
if (Test-Path -LiteralPath $destination) { throw 'La salida ya existe; no se sobrescribe.' }
$textures = Join-Path $destination 'texturas'
New-Item -ItemType Directory -Path $destination, $textures | Out-Null
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -Path (Join-Path $PSScriptRoot 'PrepararRuleta.cs')
Add-Type -ReferencedAssemblies System.Drawing -Path (Join-Path $PSScriptRoot 'ValidarRuleta.cs')
[PrepararRuleta]::Layers((Join-Path $assetsRoot 'referencias'), $textures)
[PrepararRuleta]::Sectors((Join-Path $PSScriptRoot 'arte-generado'), (Join-Path $assetsRoot 'revision-texturas-01\sector-rojo-maestro\sector_rojo.png'), $textures)
[ValidarRuleta]::Run($textures)
[PrepararRuleta]::Previews($textures, $destination)
# No instala recursos en el mod ni genera arte: solo procesa fuentes ya guardadas.
