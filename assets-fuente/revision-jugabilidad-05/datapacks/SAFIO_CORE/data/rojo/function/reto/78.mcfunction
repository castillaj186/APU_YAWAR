function rojo:cfg_reset
scoreboard players set #kind rj_k 3
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 0
scoreboard players set #tnt rj_k 60
scoreboard players set #tn rj_k 1
scoreboard players set #cr rj_k 60
scoreboard players set #crn rj_k 1
scoreboard players set #ph rj_k 120
scoreboard players set #sp rj_k 80
scoreboard players set #mc rj_k 2
scoreboard players set #alt rj_k 200
data modify storage rojo:estado nombre set value "Subir hasta Y 200"
tellraw @a ["",{"text":"ROJO - SUBIR HASTA Y 200\n","color":"red","bold":true},{"text":"Sube hasta Y 200 en 5 minutos. Peligros: TNT sobre ti cada 2 s, creepers encendidos sobre ti cada 2 s, arañas, phantoms.","color":"red"}]
function rojo:start
