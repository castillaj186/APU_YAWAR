execute store result score #n_tw rj_k if entity @a[tag=rj_tw]
execute if score #n_tw rj_k matches ..1 run tellraw @a ["",{"text":"● ROJO - INTERCAMBIO\n","color":"red","bold":true},{"text":"No hay suficientes jugadores fuera. Esta vez se salvan.","color":"red"}]
execute if score #n_tw rj_k matches ..1 run return 0
tellraw @a ["",{"text":"● ROJO - INTERCAMBIO\n","color":"red","bold":true},{"text":"Los jugadores fuera del destino cambian de lugar entre ellos.","color":"red"}]
kill @e[type=marker,tag=rj_m]
scoreboard players set #i rj_k 0
execute as @a[tag=rj_tw] at @s run function rojo:twist/swap_mark
execute as @a[tag=rj_tw] run function rojo:twist/swap_go
kill @e[type=marker,tag=rj_m]
