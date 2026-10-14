function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 3600
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #mt rj_k 40
data modify storage rojo:estado nombre set value "Meteoritos"
tellraw @a ["",{"text":"ROJO - METEORITOS\n","color":"red","bold":true},{"text":"Durante 3 minutos, mantente alerta: meteoritos ardientes caerán cerca de ti cada 2 segundos. Busca cobertura, muévete constantemente y evita quedarte expuesto en campo abierto.","color":"red"}]
function rojo:start
