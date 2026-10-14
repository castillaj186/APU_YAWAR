function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #cr rj_k 40
scoreboard players set #crn rj_k 2
scoreboard players set #blind rj_k 1
data modify storage rojo:estado nombre set value "Ceguera total y creepers"
tellraw @a ["",{"text":"ROJO - CEGUERA TOTAL Y CREEPERS\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Peligros: creepers encendidos sobre ti cada 2 s, ceguera permanente.","color":"red"}]
function rojo:start
