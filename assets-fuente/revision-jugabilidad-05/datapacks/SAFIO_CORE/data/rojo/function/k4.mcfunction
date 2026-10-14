# RETOS DE GAS (79-88): la meta es una Y ABSOLUTA (#alt).
# Para variantes de gas ascendente (#rise=1), la línea de gas sube progresivamente
# desde Y0 hasta la meta absoluta. Para gas fijo, la zona peligrosa queda en la meta.
scoreboard players operation #thr rj_k = #alt rj_k
execute if score #rise rj_k matches 1 run scoreboard players operation #thr rj_k *= #e rj_k
execute if score #rise rj_k matches 1 run scoreboard players operation #thr rj_k /= #dur rj_k

# Y actual.
execute as @a[tag=rj_player,gamemode=!spectator] store result score @s rj_cy run data get entity @s Pos[1]

# Victoria: TODOS los jugadores activos deben alcanzar la Y absoluta indicada.
tag @a remove rj_ok
execute as @a[tag=rj_player,gamemode=!spectator] if score @s rj_cy >= #alt rj_k run tag @s add rj_ok
function rojo:prog
# Si no queda ningún jugador activo fuera de rj_ok, termina. Funciona también con 1 jugador.
execute unless entity @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] if entity @a[tag=rj_player,gamemode=!spectator] run function rojo:exito
execute unless score #activo rj_k matches 1 run return 0

# Daño del gas.
# Gas fijo: daña a quienes siguen debajo de la Y objetivo después del retardo.
# Gas ascendente: daña a quienes están por debajo de la altura actual del gas.
execute if score #rise rj_k matches 0 as @a[tag=rj_player,gamemode=!spectator] if score #e rj_k >= #gg rj_k if score @s rj_cy < #thr rj_k run function rojo:gas_dano
execute if score #rise rj_k matches 1 as @a[tag=rj_player,gamemode=!spectator] if score #e rj_k >= #gg rj_k if score @s rj_cy < #thr rj_k run function rojo:gas_dano
execute as @a[tag=rj_player,gamemode=!spectator] if score @s rj_cy >= #thr rj_k run function rojo:gas_fuera
