function rojo:cfg_reset
scoreboard players set #kind rj_k 5
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #tnt rj_k 60
scoreboard players set #tn rj_k 1
data modify storage rojo:estado nombre set value "Ve al Nether bajo TNT"
tellraw @a ["",{"text":"ROJO - VE AL NETHER BAJO TNT\n","color":"red","bold":true},{"text":"Viaja al Nether en 5 minutos. Peligros: TNT sobre ti cada 2 s.","color":"red"}]
function rojo:start
