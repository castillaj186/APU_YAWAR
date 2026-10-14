execute as @a[tag=rj_ok,tag=!rj_noti] run function rojo:llego
scoreboard players set #n_ok rj_k 0
scoreboard players set #n_all rj_k 0
execute as @a[tag=rj_ok,gamemode=!spectator] run scoreboard players add #n_ok rj_k 1
execute as @a[tag=rj_player,gamemode=!spectator] run scoreboard players add #n_all rj_k 1
scoreboard players operation #last rj_k = #n_ok rj_k
