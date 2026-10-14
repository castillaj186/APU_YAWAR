function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #hd rj_k 220
scoreboard players set #hn rj_k 12
scoreboard players set #zb rj_k 60
scoreboard players set #mc rj_k 2
scoreboard players set #onehp rj_k 1
data modify storage rojo:estado nombre set value "Un corazón y horda"
tellraw @a ["",{"text":"ROJO - UN CORAZÓN Y HORDA\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: zombies, hordas de zombies, UN SOLO CORAZÓN.","color":"red"}]
function rojo:start
