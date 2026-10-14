function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #cr rj_k 50
scoreboard players set #crn rj_k 2
scoreboard players set #ce rj_k 80
scoreboard players set #cen rj_k 1
scoreboard players set #zb rj_k 40
scoreboard players set #sk rj_k 60
scoreboard players set #sp rj_k 60
scoreboard players set #ph rj_k 100
scoreboard players set #hu rj_k 80
scoreboard players set #mc rj_k 3
scoreboard players set #st rj_k 60
scoreboard players set #sn rj_k 3
data modify storage rojo:estado nombre set value "Infierno sin respiro"
tellraw @a ["",{"text":"ROJO - INFIERNO SIN RESPIRO\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: creepers encendidos sobre ti cada 2 s, creepers ELÉCTRICOS, zombies, esqueletos, arañas, phantoms, husks, tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
