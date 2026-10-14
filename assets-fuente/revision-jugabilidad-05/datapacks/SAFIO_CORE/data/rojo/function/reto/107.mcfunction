function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #rv rj_k 400
data modify storage rojo:estado nombre set value "Ravager"
tellraw @a ["",{"text":"ROJO - RAVAGER\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Un ravager enfurecido va por ti; cuando muere, aparece otro.","color":"red"}]
function rojo:start
