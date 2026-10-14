# Sonidos sintéticos originales de Inti. No usa ni lee audio de referencias externas.
param([string]$Output = "E:\APU_YAWAR\apuyawar-systems\src\main\resources\assets\apuyawar_systems\sounds\inti")
New-Item -ItemType Directory -Force -Path $Output | Out-Null
$fx = @{
  inti_arrival     = 'sine=frequency=78:duration=0.55,volume=0.20[a];sine=frequency=466:duration=0.18,volume=0.10,afade=t=out:st=0.08:d=0.10[b];[a][b]amix=inputs=2:normalize=0'
  inti_glitch      = 'sine=frequency=180:duration=0.16,volume=0.10,asetrate=22050*1.35,aresample=22050,afade=t=out:st=0.06:d=0.10'
  inti_materialize = 'sine=frequency=220:duration=0.75,volume=0.10,afade=t=in:st=0:d=0.20,afade=t=out:st=0.48:d=0.27[a];sine=frequency=659:duration=0.75,volume=0.055,afade=t=in:st=0.18:d=0.24,afade=t=out:st=0.51:d=0.24[b];[a][b]amix=inputs=2:normalize=0'
  inti_reveal      = 'sine=frequency=110:duration=0.68,volume=0.17[a];sine=frequency=880:duration=0.30,volume=0.09,adelay=110,afade=t=out:st=0.22:d=0.19[b];[a][b]amix=inputs=2:normalize=0'
  inti_judgment    = 'sine=frequency=196:duration=0.70,volume=0.18[a];sine=frequency=784:duration=0.42,volume=0.09,adelay=90,afade=t=out:st=0.22:d=0.20[b];[a][b]amix=inputs=2:normalize=0'
  inti_fade        = 'sine=frequency=440:duration=0.55,volume=0.08,asetrate=22050*0.72,aresample=22050,afade=t=out:st=0.15:d=0.40'
}
foreach($name in $fx.Keys) {
  & ffmpeg -hide_banner -loglevel error -f lavfi -i $fx[$name] -c:a libvorbis -q:a 5 -y (Join-Path $Output "$name.ogg")
  if($LASTEXITCODE -ne 0) { throw "No se pudo generar $name" }
}
