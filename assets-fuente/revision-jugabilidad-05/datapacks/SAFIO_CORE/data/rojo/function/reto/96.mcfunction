function rojo:cfg_reset
scoreboard players set #kind rj_k 5
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #tnt rj_k 60
scoreboard players set #tn rj_k 1
scoreboard players set #cr rj_k 60
scoreboard players set #crn rj_k 1
scoreboard players set #sk rj_k 60
scoreboard players set #ph rj_k 120
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Ve al Nether: infierno mixto"
tellraw @a ["",{"text":"ROJO - VE AL NETHER: INFIERNO MIXTO\n","color":"red","bold":true},{"text":"Viaja al Nether en 5 minutos. Peligros: TNT sobre ti cada 2 s, creepers encendidos sobre ti cada 2 s, esqueletos, phantoms, tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
