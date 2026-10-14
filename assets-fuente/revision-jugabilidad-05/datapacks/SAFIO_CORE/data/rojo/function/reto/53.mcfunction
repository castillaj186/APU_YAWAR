function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #hd rj_k 160
scoreboard players set #hn rj_k 14
scoreboard players set #hu rj_k 100
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Hordas imparables"
tellraw @a ["",{"text":"ROJO - HORDAS IMPARABLES\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: husks, hordas de zombies.","color":"red"}]
function rojo:start
