execute store result score #n_tw rj_k if entity @a[tag=rj_tw]
execute if score #n_tw rj_k matches ..1 run tellraw @a ["",{"text":"● ROJO - TELETRANSPORTE\n","color":"red","bold":true},{"text":"No hay suficientes jugadores fuera. Esta vez se salvan.","color":"red"}]
execute if score #n_tw rj_k matches ..1 run return 0
tellraw @a ["",{"text":"● ROJO - TELETRANSPORTE\n","color":"red","bold":true},{"text":"Cada jugador fuera del destino es llevado hacia otro jugador.","color":"red"}]
execute as @a[tag=rj_tw] at @s run function rojo:twist/tp_uno
