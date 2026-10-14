function rojo:cfg_reset
scoreboard players set #kind rj_k 3
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 0
scoreboard players set #sk rj_k 80
scoreboard players set #zb rj_k 100
scoreboard players set #mc rj_k 1
scoreboard players set #alt rj_k 130
data modify storage rojo:estado nombre set value "Subir hasta Y 130"
tellraw @a ["",{"text":"ROJO - SUBIR HASTA Y 130\n","color":"red","bold":true},{"text":"Sube hasta Y 130 en 4 minutos. Peligros: zombies, esqueletos.","color":"red"}]
function rojo:start
