function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 1
scoreboard players set #hd rj_k 240
scoreboard players set #hn rj_k 12
scoreboard players set #zb rj_k 80
scoreboard players set #mc rj_k 2
scoreboard players set #blind rj_k 1
data modify storage rojo:estado nombre set value "Lobby: horda y ceguera"
tellraw @a ["",{"text":"ROJO - LOBBY: HORDA Y CEGUERA\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: zombies, hordas de zombies, ceguera permanente.","color":"red"}]
function rojo:start
