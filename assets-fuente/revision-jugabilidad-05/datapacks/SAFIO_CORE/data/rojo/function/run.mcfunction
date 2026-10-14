scoreboard players remove #t rj_k 1
scoreboard players add #e rj_k 1
scoreboard players remove #compass_cd rj_k 1
scoreboard players add #pulse rj_k 1
execute if score #pulse rj_k matches 20.. run scoreboard players set #pulse rj_k 0
execute if score #activo rj_k matches 1 run function rojo:dest_glow_tick
scoreboard players operation #m5 rj_k = #e rj_k
scoreboard players operation #m5 rj_k %= #c5 rj_k
execute if score #kind rj_k matches 1 if score #m5 rj_k matches 0 run function rojo:k1
execute unless score #activo rj_k matches 1 run return 0
scoreboard players operation #m rj_k = #e rj_k
scoreboard players operation #m rj_k %= #c20 rj_k
execute if score #m rj_k matches 0 run function rojo:seg
execute unless score #activo rj_k matches 1 run return 0
execute if score #e rj_k matches 100.. run function rojo:hazards
execute if score #twist rj_k matches 1 if score #t rj_k matches 1200 run function rojo:twist/roll
execute if score #t rj_k matches ..0 run function rojo:fin_tiempo
