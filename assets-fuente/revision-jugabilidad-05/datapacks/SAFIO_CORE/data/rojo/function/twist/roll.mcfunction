tellraw @a ["",{"text":"● ROJO ","color":"red","bold":true},{"text":"¡ÚLTIMO MINUTO! La ruleta gira de nuevo...","color":"yellow"}]
tag @a remove rj_tw
tag @a[tag=rj_player,tag=!rj_in,gamemode=!spectator] add rj_tw
scoreboard players set #n rj_k 5
execute as @a[limit=1,sort=random] at @s run function rojo:rand
execute as @a at @s run playsound minecraft:block.note_block.bell master @s ~ ~ ~ 0.8 0.7
execute if score #r rj_k matches 1 run function rojo:twist/swap
execute if score #r rj_k matches 2 run function rojo:twist/tp
execute if score #r rj_k matches 3 run function rojo:twist/ceguera
execute if score #r rj_k matches 4 run function rojo:twist/oscuridad
execute if score #r rj_k matches 5 run function rojo:twist/corazon
tag @a remove rj_tw
