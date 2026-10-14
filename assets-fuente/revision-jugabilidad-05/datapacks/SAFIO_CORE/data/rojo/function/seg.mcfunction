function rojo:ui
scoreboard players set #n_ok rj_k 0
scoreboard players set #n_all rj_k 0
execute as @a[tag=rj_player,gamemode=!spectator] run scoreboard players add #n_all rj_k 1
execute as @a[tag=rj_ok,gamemode=!spectator] run scoreboard players add #n_ok rj_k 1
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
bossbar set rojo:timer players @a[tag=rj_player,gamemode=!spectator]
execute unless entity @a[tag=rj_player,gamemode=!spectator] run function rojo:sin_vivos
execute unless score #activo rj_k matches 1 run return 0
execute if score #kind rj_k matches 3 run function rojo:k3
execute if score #kind rj_k matches 4 run function rojo:k4
execute if score #kind rj_k matches 5 run function rojo:k5
execute if score #kind rj_k matches 6 run function rojo:k6
execute unless score #activo rj_k matches 1 run return 0
execute if score #blind rj_k matches 1 as @a[tag=rj_player,tag=!rj_in,gamemode=!spectator] run effect give @s minecraft:blindness 3 0 true
execute if score #blind rj_k matches 2 as @a[tag=rj_player,tag=!rj_in,gamemode=!spectator] run effect give @s minecraft:darkness 5 0 true
execute if score #uc_t rj_k matches 1.. run scoreboard players remove #uc_t rj_k 20
execute if score #uc_t rj_k matches ..0 unless score #onehp rj_k matches 1 as @a[tag=rj_uc] run function rojo:uc_quitar
execute if score #uc_t rj_k matches ..0 run scoreboard players set #uc_t rj_k 0
