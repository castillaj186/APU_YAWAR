function naranja:reset_silent
scoreboard players set #init naranja_meta 1
schedule function naranja:loop 1t replace
tellraw @a ["",{"text":"● NARANJA ","color":"gold","bold":true},{"text":"Todos los cambios de dificultad fueron desactivados (100).","color":"#FFD588"}]
