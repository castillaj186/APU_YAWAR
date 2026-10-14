effect give @a[tag=rj_tw] minecraft:blindness 60 0 true
tellraw @a ["",{"text":"● ROJO - CEGUERA\n","color":"red","bold":true},{"text":"Los que siguen fuera del destino quedan ciegos durante 1 minuto.","color":"red"}]
execute as @a[tag=rj_tw] at @s run playsound minecraft:entity.elder_guardian.curse master @s ~ ~ ~ 0.5 0.8
