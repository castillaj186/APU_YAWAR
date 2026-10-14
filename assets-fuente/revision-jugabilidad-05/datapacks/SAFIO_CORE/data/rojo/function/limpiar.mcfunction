bossbar set rojo:timer players
bossbar set rojo:timer visible false
execute if score #kind rj_k matches 1 run function rojo:dest_unforce
kill @e[type=armor_stand,tag=rojo_dest_glow]
schedule clear rojo:tick
schedule clear rojo:cuenta
scoreboard players set #cuenta rj_k 0
scoreboard players set #cuenta_on rj_k 0
scoreboard players set #activo rj_k 0
scoreboard players set #noshift rj_k 0
scoreboard players set #evento_activo rj_k 0
kill @e[tag=rj_mob]
tag @a[tag=rj_player] remove rj_in
tag @a[tag=rj_player] remove rj_ok
tag @a[tag=rj_player] remove rj_noti
tag @a[tag=rj_player] remove rj_tw
tag @a[tag=rj_player] remove rj_gaswarn
scoreboard players set @a[tag=rj_player] rj_gas 0
effect clear @a[tag=rj_player] minecraft:blindness
effect clear @a[tag=rj_player] minecraft:darkness
execute if score #storm rj_k matches 1 run weather clear
scoreboard players set #storm rj_k 0
scoreboard players set #last rj_k -1
clear @a[tag=rj_player] minecraft:compass[minecraft:custom_data~{rj:1b}]
scoreboard players set #uc_t rj_k 0
scoreboard players set #onehp rj_k 0
execute as @a[tag=rj_player,tag=rj_uc] run function rojo:uc_quitar
tag @a remove rj_player
function rojo:cielo/off
