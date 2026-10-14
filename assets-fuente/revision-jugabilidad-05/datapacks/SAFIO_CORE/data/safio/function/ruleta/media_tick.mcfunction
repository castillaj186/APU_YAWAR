# SAFIO 1.6.6: giro de un solo resultado + hold visible del color final.
# Se conservan TODOS los frames; al terminar se muestra el color ganador 1 segundo.
execute if score #color_bloqueado safio.ruleta matches 1..8 run scoreboard players operation #resultado safio.ruleta = #color_bloqueado safio.ruleta
execute if score #hold safio.anim matches 1 run scoreboard players add #hold_ticks safio.anim 1
execute if score #hold safio.anim matches 1 if score #hold_ticks safio.anim matches 20.. run function safio:ruleta/detener
execute unless score #hold safio.anim matches 1 run scoreboard players add #media_frame safio.anim 1
execute unless score #hold safio.anim matches 1 if score #media_frame safio.anim >= #media_total safio.anim run function safio:ruleta/retener_resultado
