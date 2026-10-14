function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #mt rj_k 20
data modify storage rojo:estado nombre set value "Lluvia de meteoritos"
tellraw @a ["",{"text":"ROJO - LLUVIA DE METEORITOS\n","color":"red","bold":true},{"text":"Aguanta 4 minutos mientras una lluvia de meteoritos cruza el cielo y golpea la zona a tu alrededor. No te quedes quieto: busca refugio entre cada impacto y vigila por dónde cae el siguiente.","color":"red"}]
function rojo:start
