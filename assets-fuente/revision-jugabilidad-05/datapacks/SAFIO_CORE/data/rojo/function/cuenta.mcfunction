# Cuenta regresiva previa (10..1). Cuando llega a 0 arranca el reto.
execute unless score #cuenta_on rj_k matches 1 run return 0
execute if score #cuenta rj_k matches 0 run function rojo:cuenta_fin
execute unless score #cuenta_on rj_k matches 1 run return 0
title @a[tag=!sin_ruleta,gamemode=!spectator] actionbar ["",{"text":"● ROJO ","color":"red","bold":true},{"text":"empieza en ","color":"gray"},{"score":{"name":"#cuenta","objective":"rj_k"},"color":"yellow","bold":true},{"text":" s","color":"gray"}]
execute as @a[tag=!sin_ruleta,gamemode=!spectator] at @s run playsound minecraft:block.note_block.hat master @s ~ ~ ~ 0.7 1
execute if score #cuenta rj_k matches ..3 as @a[tag=!sin_ruleta,gamemode=!spectator] at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 0.8 1.6
scoreboard players remove #cuenta rj_k 1
schedule function rojo:cuenta 20t replace
