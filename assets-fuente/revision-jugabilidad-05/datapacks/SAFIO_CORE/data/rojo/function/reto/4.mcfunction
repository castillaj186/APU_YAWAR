function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 1
scoreboard players set #zb rj_k 60
scoreboard players set #sk rj_k 80
scoreboard players set #sp rj_k 100
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "LOBBY: Mobs"
tellraw @a ["",{"text":"ROJO - LOBBY: MOBS\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: zombies, esqueletos, arañas.","color":"red"}]
function rojo:start
