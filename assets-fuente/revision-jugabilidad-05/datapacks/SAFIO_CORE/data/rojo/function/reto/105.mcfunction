function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #gh rj_k 200
scoreboard players set #mt rj_k 30
data modify storage rojo:estado nombre set value "Ghasts y meteoritos"
tellraw @a ["",{"text":"ROJO - GHASTS Y METEORITOS\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Ghasts desde el cielo y meteoritos cayendo.","color":"red"}]
function rojo:start
