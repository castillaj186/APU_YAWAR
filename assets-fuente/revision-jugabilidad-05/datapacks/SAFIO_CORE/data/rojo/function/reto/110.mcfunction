function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #vx rj_k 100
scoreboard players set #rv rj_k 600
scoreboard players set #gh rj_k 300
data modify storage rojo:estado nombre set value "Caos infernal"
tellraw @a ["",{"text":"ROJO - CAOS INFERNAL\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Vexes, ravagers y ghasts al mismo tiempo.","color":"red"}]
function rojo:start
