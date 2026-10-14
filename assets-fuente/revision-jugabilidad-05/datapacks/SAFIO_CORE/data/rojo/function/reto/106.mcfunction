function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #vx rj_k 60
data modify storage rojo:estado nombre set value "Enjambre de vexes"
tellraw @a ["",{"text":"ROJO - ENJAMBRE DE VEXES\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Vexes con espada te persiguen y atraviesan paredes.","color":"red"}]
function rojo:start
