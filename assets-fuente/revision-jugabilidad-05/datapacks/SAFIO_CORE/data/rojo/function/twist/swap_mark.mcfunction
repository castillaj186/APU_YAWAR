scoreboard players add #i rj_k 1
scoreboard players operation @s rj_id = #i rj_k
summon marker ~ ~ ~ {Tags:["rj_m","rj_new"]}
execute as @e[type=marker,tag=rj_new,limit=1] run scoreboard players operation @s rj_id = #i rj_k
tag @e[type=marker,tag=rj_new] remove rj_new
