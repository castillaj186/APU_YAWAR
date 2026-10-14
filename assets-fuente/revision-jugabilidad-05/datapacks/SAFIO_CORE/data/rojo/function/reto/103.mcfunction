function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #mt rj_k 10
scoreboard players set #zb rj_k 80
scoreboard players set #mc rj_k 1
data modify storage rojo:estado nombre set value "Meteoritos extremos"
tellraw @a ["",{"text":"ROJO - METEORITOS EXTREMOS\n","color":"red","bold":true},{"text":"Resiste durante 5 minutos una lluvia casi continua de meteoritos mientras los zombies te presionan desde el suelo. Muévete con cuidado, aprovecha cualquier cobertura y no dejes que te acorralen.","color":"red"}]
function rojo:start
