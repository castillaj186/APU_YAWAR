# Termina la animacion y resuelve el evento oficial.
scoreboard players set #animando safio.anim 0
scoreboard players set #fase_anim safio.anim 0
scoreboard players set #ticks_anim safio.anim 0
scoreboard players set #restantes safio.anim 0
title @a clear
title @a times 5 65 15
# Revalidar color antes de resolver y anunciar el resultado.
execute if score #color_bloqueado safio.ruleta matches 1..8 run scoreboard players operation #resultado safio.ruleta = #color_bloqueado safio.ruleta
function safio:ruleta/resolver
# Marca final efectivo para que el calendario confirme el resultado; /reload no equivale a completar.
scoreboard players add #roulette_done safio.cal 1

scoreboard players set #color_bloqueado safio.ruleta 0
scoreboard players set #hold safio.anim 0
scoreboard players set #hold_ticks safio.anim 0
