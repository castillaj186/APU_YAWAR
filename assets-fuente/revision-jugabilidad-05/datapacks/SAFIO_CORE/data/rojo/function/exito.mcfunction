tellraw @a ["",{"text":"✓ ROJO SUPERADO ","color":"green","bold":true},{"text":"Todos llegaron a tiempo. ¡Reto completado!","color":"green"}]
execute as @a[tag=rj_player,gamemode=!spectator] run function rojo:premio
function rojo:limpiar
