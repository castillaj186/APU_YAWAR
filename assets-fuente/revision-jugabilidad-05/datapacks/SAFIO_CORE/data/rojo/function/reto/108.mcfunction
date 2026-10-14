function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #mt rj_k 20
scoreboard players set #ce rj_k 60
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
data modify storage rojo:estado nombre set value "Cielo rojo"
tellraw @a ["",{"text":"ROJO - CIELO ROJO\n","color":"red","bold":true},{"text":"El cielo se teñirá de rojo durante 5 minutos. Sobrevive a los meteoritos, los creepers eléctricos y una tormenta de rayos triples; mantén la distancia de las amenazas y busca un lugar donde protegerte.","color":"red"}]
function rojo:start
