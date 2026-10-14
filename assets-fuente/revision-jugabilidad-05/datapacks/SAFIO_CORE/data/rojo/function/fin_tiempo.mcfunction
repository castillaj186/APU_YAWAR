tellraw @a ["",{"text":"● ROJO ","color":"red","bold":true},{"text":"¡SE ACABÓ EL TIEMPO!","color":"red"}]
execute if score #kind rj_k matches 3 run function rojo:k3
execute if score #kind rj_k matches 5 run function rojo:k5
execute if score #kind rj_k matches 6 run function rojo:k6
execute if score #kind rj_k matches 1 run function rojo:k1
execute unless score #activo rj_k matches 1 run return 0
execute if score #kind rj_k matches 1 as @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] run function rojo:matar
execute if score #kind rj_k matches 3 as @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] run function rojo:matar
execute if score #kind rj_k matches 5 as @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] run function rojo:matar
execute if score #kind rj_k matches 6 as @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] run function rojo:matar
tellraw @a ["",{"text":"✓ ROJO ","color":"green","bold":true},{"text":"Los supervivientes reciben su recompensa.","color":"green"}]
execute as @a[tag=rj_player,gamemode=!spectator] run function rojo:premio
function rojo:limpiar
