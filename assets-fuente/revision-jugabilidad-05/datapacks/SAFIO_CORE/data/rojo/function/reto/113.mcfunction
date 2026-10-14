function rojo:cfg_reset
scoreboard players set #kind rj_k 3
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 0
scoreboard players set #alt rj_k 250
scoreboard players set #vx rj_k 100
data modify storage rojo:estado nombre set value "Subir hasta Y 250 (vexes)"
tellraw @a ["",{"text":"ROJO - SUBIR HASTA Y 250\n","color":"red","bold":true},{"text":"Sube hasta Y 250 en 5 minutos. Peligros: vexes.","color":"red"}]
function rojo:start
