$ErrorActionPreference='Stop'
$stage=$PSScriptRoot
$project=Split-Path (Split-Path $stage -Parent) -Parent
$jdk='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin'
$classes=Join-Path $stage 'clases-validacion'
New-Item -ItemType Directory -Force $classes | Out-Null
$source=Join-Path $project 'apuyawar-systems/src/main/java/pe/apuyawar/systems'
& "$jdk/javac.exe" -encoding UTF-8 -d $classes "$source/roulette/RouletteTimeline.java" "$source/roulette/RoulettePalette.java" "$source/roulette/RouletteFillSequence.java" "$source/roulette/RouletteEvents.java" "$source/client/RouletteParticles.java" "$stage/ValidateRoulette.java"
if($LASTEXITCODE){throw 'javac failed'}
& "$jdk/java.exe" -cp $classes ValidateRoulette
if($LASTEXITCODE){throw 'validation failed'}
$assets=Join-Path $project 'apuyawar-systems/src/main/resources/assets/apuyawar_systems'
$sounds=Get-Content "$assets/sounds.json" -Raw | ConvertFrom-Json
foreach($name in 'appear','tick','result','fill','finish','outro') {
    $id="roulette_$name"
    if($sounds.$id.sounds[0].name -ne "apuyawar_systems:roulette/$id"){throw "Wrong id $id"}
    & ffmpeg -v error -i "$assets/sounds/roulette/$id.ogg" -f null NUL
    if($LASTEXITCODE){throw "Invalid OGG $id"}
}
Write-Output 'PASS: six OGG files decoded, all sound identifiers resolve to existing resources.'
