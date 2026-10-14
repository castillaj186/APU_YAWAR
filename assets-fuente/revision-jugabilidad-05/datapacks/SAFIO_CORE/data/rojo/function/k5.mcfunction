# RETOS DEL NETHER (89-96): todos los jugadores activos deben estar en el Nether.
# Se comprueba la dimensión REAL de cada jugador en cada ciclo del reto.
tag @a remove rj_ok
execute as @a[tag=rj_player,gamemode=!spectator] at @s if dimension minecraft:the_nether run tag @s add rj_ok

# Si TODOS los jugadores activos están en el Nether, el reto termina automáticamente.
# Esto funciona tanto con 1 jugador como con varios.
execute unless entity @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] if entity @a[tag=rj_player,gamemode=!spectator] run function rojo:exito
