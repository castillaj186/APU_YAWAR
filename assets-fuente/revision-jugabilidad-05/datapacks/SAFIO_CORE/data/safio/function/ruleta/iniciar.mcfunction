# SAFIO 1.6.2 - usa TODOS los fotogramas audiovisuales Club Pala.
# La logica del sorteo y los resultados sigue siendo exclusiva de SAFIO.
execute if score #forzado safio.ruleta matches 1..8 run scoreboard players operation #resultado safio.ruleta = #forzado safio.ruleta
execute unless score #forzado safio.ruleta matches 1..8 store result score #resultado safio.ruleta run random value 1..8
# El color no puede cambiar mientras la ruleta gira.
scoreboard players operation #color_bloqueado safio.ruleta = #resultado safio.ruleta
scoreboard players set #forzado safio.ruleta 0
scoreboard players set #hold safio.anim 0
scoreboard players set #hold_ticks safio.anim 0
scoreboard players set #media_frame safio.anim 0
# 375 cuadros para cada uno de los 8 resultados APU YAWAR
scoreboard players set #media_total safio.anim 375
scoreboard players add #media_seq safio.anim 1
scoreboard players set #fase_anim safio.anim 5
scoreboard players set #animando safio.anim 1
tellraw @a [{"text":"[SAFIO] ","color":"gold"},{"text":"La ruleta de APU YAWAR esta girando...","color":"aqua"}]
execute as @a at @s run playsound safio_media:ruleta master @s ~ ~ ~ 0.85 1 1
