scoreboard players operation #j rj_k = @s rj_id
scoreboard players operation #j rj_k %= #n_tw rj_k
scoreboard players add #j rj_k 1
tag @s add rj_tpme
execute as @e[type=marker,tag=rj_m] if score @s rj_id = #j rj_k at @s run tp @a[tag=rj_tpme,limit=1] ~ ~ ~
tag @s remove rj_tpme
playsound minecraft:entity.enderman.teleport master @s ~ ~ ~ 1 1
