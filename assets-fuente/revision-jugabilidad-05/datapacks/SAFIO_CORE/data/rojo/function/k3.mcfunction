# RETOS DE ALTURA (71-78): todos los jugadores activos deben alcanzar la Y absoluta indicada.
tag @a remove rj_ok
execute as @a[tag=rj_player,gamemode=!spectator] store result score @s rj_cy run data get entity @s Pos[1]
execute as @a[tag=rj_player,gamemode=!spectator] if score @s rj_cy >= #alt rj_k run tag @s add rj_ok
function rojo:prog
# Si no queda ningún jugador activo fuera de rj_ok, termina. Esto también funciona con 1 solo jugador.
execute unless entity @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] if entity @a[tag=rj_player,gamemode=!spectator] run function rojo:exito
