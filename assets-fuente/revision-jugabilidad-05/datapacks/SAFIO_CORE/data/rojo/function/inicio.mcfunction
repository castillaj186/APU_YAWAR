# ROJO - INICIO REAL (se llama al terminar la cuenta regresiva de 10 s).
function rojo:init
execute if score #activo rj_k matches 1 run tellraw @s ["",{"text":"● ROJO ","color":"red","bold":true},{"text":"Ya hay un reto activo.","color":"yellow"}]
execute if score #activo rj_k matches 1 run return 0

# Blindaje contra configuraciones vacías/stale: un reto siempre debe tener duración.
execute unless score #dur rj_k matches 1.. run scoreboard players set #dur rj_k 12000
# Todo reto de destino (LOBBY / BUNKER 1-4 / destino al azar) dura SIEMPRE 10:00 (12000 ticks).
execute if score #kind rj_k matches 1 run scoreboard players set #dur rj_k 12000

scoreboard players set #activo rj_k 1
scoreboard players set #evento_activo rj_k 1
scoreboard players set #e rj_k 0
scoreboard players operation #t rj_k = #dur rj_k
execute unless score #t rj_k matches 1.. run scoreboard players operation #t rj_k = #dur rj_k
scoreboard players set #last rj_k -1
scoreboard players set #compass_cd rj_k 0
scoreboard players set #pulse rj_k 0
schedule clear rojo:tick
schedule clear rojo:cuenta
scoreboard players set #cuenta rj_k 0

# Participantes: exactamente los jugadores presentes y no excluidos.
tag @a remove rj_player
tag @a remove rj_in
tag @a remove rj_ok
tag @a remove rj_noti
tag @a remove rj_gaswarn
tag @a[tag=!sin_ruleta,gamemode=!spectator] add rj_player
scoreboard players set @a[tag=rj_player] rj_gas 0

# Bossbar: SOLO el timer se muestra arriba. No actionbar, no title, no texto flotante.
bossbar remove rojo:timer
bossbar add rojo:timer {"text":"ROJO","color":"red","bold":true}
bossbar set rojo:timer color red
bossbar set rojo:timer style notched_20
bossbar set rojo:timer visible true
bossbar set rojo:timer players @a[tag=rj_player,gamemode=!spectator]
execute store result bossbar rojo:timer max run scoreboard players get #dur rj_k
execute store result bossbar rojo:timer value run scoreboard players get #t rj_k

# Destino: preparación, coordenadas, brújula, chunks y marcador luminoso.
execute if score #kind rj_k matches 1 if score #dest rj_k matches 0 run function rojo:dest_rand
execute as @a[tag=rj_player] store result score @s rj_y run data get entity @s Pos[1]
execute if score #kind rj_k matches 1 run function rojo:dest_prepare
execute if score #kind rj_k matches 1 run function rojo:dest_info
execute if score #kind rj_k matches 1 run function rojo:dest_compass
execute if score #kind rj_k matches 1 run function rojo:dest_force
execute if score #kind rj_k matches 1 run function rojo:dest_glow_start

function rojo:ui
scoreboard players set #n_ok rj_k 0
scoreboard players set #n_all rj_k 0
execute as @a[tag=rj_player,gamemode=!spectator] run scoreboard players add #n_all rj_k 1
scoreboard players operation #ss rj_k = #t rj_k
scoreboard players add #ss rj_k 19
scoreboard players operation #ss rj_k /= #c20 rj_k
scoreboard players operation #mm rj_k = #ss rj_k
scoreboard players operation #mm rj_k /= #c60 rj_k
scoreboard players operation #s1 rj_k = #ss rj_k
scoreboard players operation #s1 rj_k %= #c60 rj_k
scoreboard players operation #s2 rj_k = #s1 rj_k
scoreboard players operation #s1 rj_k /= #c10 rj_k
scoreboard players operation #s2 rj_k %= #c10 rj_k
execute if score #kind rj_k matches 1 run bossbar set rojo:timer name {"text":"ROJO  •  ","color":"red","bold":true,"extra":[{"score":{"name":"#mm","objective":"rj_k"},"color":"white"},{"text":":","color":"white"},{"score":{"name":"#s1","objective":"rj_k"},"color":"white"},{"score":{"name":"#s2","objective":"rj_k"},"color":"white"},{"text":"  •  ","color":"dark_gray"},{"score":{"name":"#n_ok","objective":"rj_k"},"color":"green","bold":true},{"text":"/","color":"white"},{"score":{"name":"#n_all","objective":"rj_k"},"color":"white","bold":true}]}
execute unless score #kind rj_k matches 1 run bossbar set rojo:timer name {"text":"ROJO  •  ","color":"red","bold":true,"extra":[{"score":{"name":"#mm","objective":"rj_k"},"color":"white"},{"text":":","color":"white"},{"score":{"name":"#s1","objective":"rj_k"},"color":"white"},{"score":{"name":"#s2","objective":"rj_k"},"color":"white"}]}

execute if score #kind rj_k matches 3 run function rojo:alt_info
execute if score #kind rj_k matches 4 run function rojo:gas_info
execute if score #kind rj_k matches 5 run function rojo:nether_info
execute if score #kind rj_k matches 6 run function rojo:bajar_info
execute if score #onehp rj_k matches 1 as @a[tag=rj_player] run function rojo:uc_poner
execute if score #st rj_k matches 1.. run weather thunder
execute if score #st rj_k matches 1.. run scoreboard players set #storm rj_k 1

execute as @a[tag=rj_player] at @s run playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 0.5 1.4
schedule function rojo:tick 1t replace
