function rojo:cfg_reset
scoreboard players set #kind rj_k 3
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 0
scoreboard players set #ph rj_k 100
scoreboard players set #sp rj_k 80
scoreboard players set #mc rj_k 2
scoreboard players set #alt rj_k 180
data modify storage rojo:estado nombre set value "Subir hasta Y 180"
tellraw @a ["",{"text":"ROJO - SUBIR HASTA Y 180\n","color":"red","bold":true},{"text":"Sube hasta Y 180 en 5 minutos. Peligros: arañas, phantoms.","color":"red"}]
function rojo:start
