function rojo:cfg_reset
scoreboard players set #kind rj_k 3
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 0
scoreboard players set #tnt rj_k 60
scoreboard players set #tn rj_k 1
scoreboard players set #zb rj_k 100
scoreboard players set #mc rj_k 1
scoreboard players set #alt rj_k 150
data modify storage rojo:estado nombre set value "Subir hasta Y 150"
tellraw @a ["",{"text":"ROJO - SUBIR HASTA Y 150\n","color":"red","bold":true},{"text":"Sube hasta Y 150 en 5 minutos. Peligros: TNT sobre ti cada 2 s, zombies.","color":"red"}]
function rojo:start
