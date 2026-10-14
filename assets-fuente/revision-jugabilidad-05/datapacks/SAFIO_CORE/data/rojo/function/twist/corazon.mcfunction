execute as @a[tag=rj_tw] run function rojo:uc_poner
scoreboard players set #uc_t rj_k 600
tellraw @a ["",{"text":"● ROJO - UN SOLO CORAZÓN\n","color":"red","bold":true},{"text":"Los que siguen fuera tienen 1 corazón durante 30 segundos.","color":"red"}]
execute as @a[tag=rj_tw] at @s run playsound minecraft:entity.wither.ambient master @s ~ ~ ~ 0.6 0.7
