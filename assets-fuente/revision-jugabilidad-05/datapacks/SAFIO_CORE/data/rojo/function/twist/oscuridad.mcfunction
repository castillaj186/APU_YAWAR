effect give @a[tag=rj_tw] minecraft:darkness 60 0 true
tellraw @a ["",{"text":"● ROJO - OSCURIDAD\n","color":"red","bold":true},{"text":"La oscuridad cae sobre quienes siguen fuera durante 1 minuto.","color":"red"}]
execute as @a[tag=rj_tw] at @s run playsound minecraft:entity.warden.heartbeat master @s ~ ~ ~ 1 0.8
